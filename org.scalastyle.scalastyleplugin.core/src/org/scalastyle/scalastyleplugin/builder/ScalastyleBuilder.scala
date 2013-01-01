// Copyright (C) 2011-2012 the original author or authors.
// See the LICENCE.txt file distributed with this work for additional
// information regarding copyright ownership.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.scalastyle.scalastyleplugin.builder

import scala.Array.canBuildFrom
import scala.Option.option2Iterable
import scala.collection.JavaConversions.mutableMapAsJavaMap
import scala.collection.mutable.HashMap

import org.eclipse.core.resources.IContainer
import org.eclipse.core.resources.IMarker
import org.eclipse.core.resources.IProject
import org.eclipse.core.resources.IResource
import org.eclipse.core.resources.IResourceDelta
import org.eclipse.core.resources.IncrementalProjectBuilder
import org.eclipse.core.resources.ResourcesPlugin
import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.ui.texteditor.MarkerUtilities
import org.scalastyle.scalastyleplugin.ExceptionUtils.handleException
import org.scalastyle.scalastyleplugin.config.Persistence
import org.scalastyle.scalastyleplugin.nature.ScalastyleNature
import org.scalastyle.scalastyleplugin.ScalastylePlugin
import org.scalastyle.scalastyleplugin.ScalastylePluginException
import org.scalastyle.Message
import org.scalastyle.StyleError
import org.scalastyle.EndFile
import org.scalastyle.EndWork
import org.scalastyle.ErrorLevel
import org.scalastyle.MessageHelper
import org.scalastyle.Output
import org.scalastyle.RealFileSpec
import org.scalastyle.ScalastyleChecker
import org.scalastyle.ScalastyleConfiguration
import org.scalastyle.StartFile
import org.scalastyle.StartWork
import org.scalastyle.StyleException
import org.scalastyle.WarningLevel

import ScalastyleBuilder.createMarker
import ScalastyleBuilder.root

class EclipseFileSpec(name: String, encoding: String, val resource: IResource) extends RealFileSpec(name, Some(encoding))

object ScalastyleBuilder {
  val BuilderId = ScalastylePlugin.PluginId + ".ScalastyleBuilder" //$NON-NLS-1$

  private def workspace() = ResourcesPlugin.getWorkspace()
  private def root() = workspace.getRoot()

  def buildProject(project: IProject) {
    val buildJob = BuildProjectJob(project, IncrementalProjectBuilder.FULL_BUILD)
    buildJob.setRule(ResourcesPlugin.getWorkspace().getRoot());
    buildJob.schedule();
  }

  def buildAllProjects() {
    buildProjects(root().getProjects());
  }

  def buildProjects(projects: Array[IProject]) {
    val scalastyleProjects = projects.filter(project => {
      project.exists() && project.isOpen() && project.hasNature(ScalastyleNature.NatureId) && Persistence.loadWorkspace().configurations.size > 0
    })

    val buildJob = BuildProjectJob(scalastyleProjects, IncrementalProjectBuilder.FULL_BUILD)
    buildJob.setRule(workspace.getRoot());
    buildJob.schedule();
  }

  def createMarker(resource: IResource, key: String, severity: Int, lineNumber: Option[Int], message: String): Unit = {
    val markerAttributes: java.util.Map[String, Any] = HashMap(ScalastyleMarker.ModuleName -> "module",
      ScalastyleMarker.MessageKey -> key,
      IMarker.PRIORITY -> IMarker.PRIORITY_NORMAL,
      IMarker.SEVERITY -> severity,
      IMarker.MESSAGE -> message,
      "categoryId" -> 999)

    if (lineNumber.isDefined) {
      MarkerUtilities.setLineNumber(markerAttributes, lineNumber.get)
    }

    MarkerUtilities.createMarker(resource, markerAttributes, ScalastyleMarker.MarkerId)

  }
}

class ScalastyleBuilder extends JavaScalastyleBuilder {
  private val categoryId = 999

  def delegatedBuild(kind: Int, monitor: IProgressMonitor): Array[IProject] = {
    // get the associated project for this builder
    val project = getProject();

    // remove project level error markers
    project.deleteMarkers(ScalastyleMarker.MarkerId, false, IResource.DEPTH_ZERO);

    if (ScalastyleNature.hasCorrectBuilderOrder(project)) {
      val resourceDelta = getDelta(project);
      val filters = Array[IFilter](new IFilter {})
      val files = if (resourceDelta != null) getDeltaFiles(resourceDelta, filters) else getProjectFiles(project, filters)
      handleBuildSelection(files, monitor, project, kind);
    } else {
      // the builder order is wrong. Refuse to check and create a error.
      // remove all existing markers
      project.deleteMarkers(ScalastyleMarker.MarkerId, false, IResource.DEPTH_INFINITE);

      val message = "Project builder is not in correct order (should be after scala builder) for project " + project.getName()

      // create a marker for the project
      createMarker(project, "key", IMarker.SEVERITY_ERROR, None, message)
    }

    Array(project)
  }

  // TODO when we do an incremental build, all files in the project seem to be marked as changed
  private def handleBuildSelection(resources: Array[IResource], monitor: IProgressMonitor, project: IProject, kind: Int): Unit = {
    handleException {
      // on full build remove all markers
      if (kind == IncrementalProjectBuilder.FULL_BUILD) {
        project.deleteMarkers(ScalastyleMarker.MarkerId, false, IResource.DEPTH_INFINITE);
      }

      val projectConfiguration = Persistence.loadProject(project)
      if (projectConfiguration.enabled && projectConfiguration.file.isDefined) {

        val file = Persistence.findConfiguration(projectConfiguration.file.get)
        file match {
          case None =>
          case Some(x) if (!x.exists) => throw ScalastylePluginException("cannot find file " + file.get.getAbsolutePath())
          case Some(x) if (x.exists) => {
            val configuration = ScalastyleConfiguration.readFromXml(file.get.getAbsolutePath())

            val messages = new ScalastyleChecker().checkFiles(configuration, resources.map(fileSpec).toList)

            new EclipseOutput().output(messages)
          }
        }
      }
    }
  }

  private def fileSpec(r: IResource) = new EclipseFileSpec(r.getLocation().toFile().getAbsolutePath(),
                                                          root().getFileForLocation(r.getLocation()).getCharset(),
                                                          r)

  private def isDeltaAddedOrChanged(delta: IResourceDelta) = (delta.getKind() == IResourceDelta.ADDED) || (delta.getKind() == IResourceDelta.CHANGED)

  private[this] def accept(resource: IResource, filter: IFilter): Boolean = !filter.isEnabled() || filter.accept(resource)
  private[this] def accept(resource: IResource, filters: Array[IFilter]): Boolean = filters.size == 0 || filters.exists(accept(resource, _))

  private[this] def getDeltaFiles(delta: IResourceDelta, filters: Array[IFilter]): Array[IResource] = {
    delta.getAffectedChildren().filter(isDeltaAddedOrChanged).map(_.getResource()).flatMap(traverse(_, filters)).flatten
  }

  private[this] def getProjectFiles(project: IProject, filters: Array[IFilter]): Array[IResource] = traverse(project, filters).flatten

  private[this] def traverse(resource: IResource, filters: Array[IFilter]): Array[Option[IResource]] = {
    resource match {
      case c: IContainer => c.members().flatMap(m => traverse(m, filters))
      case r: IResource => Array(if (accept(r, filters)) Some(r) else None)
      case _ => Array()
    }
  }
}

trait IFilter {
  def isEnabled(): Boolean = true
  def accept(resource: IResource): Boolean = "scala" == resource.getFileExtension()
}

class EclipseOutput extends Output[EclipseFileSpec] {

  private val messageHelper = new MessageHelper(this.getClass().getClassLoader())

  override def message(m: Message[EclipseFileSpec]): Unit = m match {
    case StartWork() => {}
    case EndWork() => {}
    case StartFile(file) => {
      // remove markers on this file
      file.resource.deleteMarkers(ScalastyleMarker.MarkerId, false, IResource.DEPTH_ZERO);

      // remove markers from parent as well, not sure if this is necessary
      file.resource.getParent().deleteMarkers(ScalastyleMarker.MarkerId, false, IResource.DEPTH_ZERO);
    }
    case EndFile(file) => {}
    case error: StyleError[_] => addError(messageHelper, error)
    case StyleException(file, clazz, message, stacktrace, line, column) => {
      // TODO cope with StyleException please
    }
  }

  private def addError(messageHelper: MessageHelper, error: StyleError[EclipseFileSpec]): Unit = {
    // TODO limit number of errors, do we limit number of errors and number of warnings? or both together
    // TODO rule metadata

    val severity = error.level match {
      case WarningLevel => IMarker.SEVERITY_WARNING
      case ErrorLevel => IMarker.SEVERITY_ERROR
      case _ => IMarker.SEVERITY_WARNING
    }

    val message = Output.findMessage(messageHelper, error.clazz, error.key, error.args, error.customMessage)
    createMarker(error.fileSpec.resource, error.key, severity, Some(error.lineNumber.getOrElse(1)), message)
  }
}
