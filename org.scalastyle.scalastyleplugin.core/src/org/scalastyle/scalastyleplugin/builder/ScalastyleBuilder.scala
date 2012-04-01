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

import org.eclipse.core.resources.IContainer
import org.eclipse.core.resources.IFile
import org.eclipse.core.resources.IMarker
import org.eclipse.core.resources.IProject
import org.eclipse.core.resources.IResource
import org.eclipse.core.resources.IResourceDelta
import org.eclipse.core.resources.IWorkspace
import org.eclipse.core.resources.IncrementalProjectBuilder
import org.eclipse.core.resources.ResourcesPlugin
import org.eclipse.core.runtime.CoreException
import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.core.runtime.IStatus
import org.eclipse.core.runtime.Status
import org.eclipse.osgi.util.NLS
import org.eclipse.ui.texteditor.MarkerUtilities
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.HashMap
import org.scalastyle.scalastyleplugin.ScalastylePlugin
import org.scalastyle.scalastyleplugin.ExceptionUtils._
import org.scalastyle.scalastyleplugin.ScalastylePluginException
import org.scalastyle.scalastyleplugin.nature.ScalastyleNature
import org.scalastyle.ScalastyleConfiguration
import org.scalastyle.TextOutput
import org.scalastyle.scalastyleplugin.config._
import org.scalastyle.ScalastyleChecker
import org.scalastyle._
import scala.collection.JavaConversions._
import org.eclipse.jface.dialogs.MessageDialog

class EclipseFileSpec(val name: String, val resource: IResource) extends FileSpec

object ScalastyleBuilder {
  val BuilderId = ScalastylePlugin.PluginId + ".ScalastyleBuilder" //$NON-NLS-1$

  def buildProject(project: IProject) = {
    val buildJob = BuildProjectJob(project, IncrementalProjectBuilder.FULL_BUILD)
    buildJob.setRule(ResourcesPlugin.getWorkspace().getRoot());
    buildJob.schedule();
  }

  def buildAllProjects() = {
    val workspace = ResourcesPlugin.getWorkspace();
    val projects = workspace.getRoot().getProjects();

    buildProjects(projects);
  }

  def buildProjects(projects: Array[IProject]) = {
    val scalastyleProjects = projects.filter(project => {
      project.exists() && project.isOpen() && project.hasNature(ScalastyleNature.NatureId) && Persistence.loadWorkspace().configurations.size > 0
    })

    val buildJob = BuildProjectJob(scalastyleProjects, IncrementalProjectBuilder.FULL_BUILD)
    buildJob.setRule(ResourcesPlugin.getWorkspace().getRoot());
    buildJob.schedule();
  }
}

class ScalastyleBuilder extends IncrementalProjectBuilder {
  def build(kind: Int, args: java.util.Map[_, _], monitor: IProgressMonitor): Array[IProject] = {
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

      // categoryId enables own category under Java Problem Type
      // setting for Problems view (RFE 1530366)
      val markerAttributes = Map[Object, Object](IMarker.PRIORITY -> Integer.valueOf(IMarker.PRIORITY_HIGH),
        IMarker.SEVERITY -> Integer.valueOf(IMarker.SEVERITY_ERROR),
        IMarker.MESSAGE -> ("Project builder is not in correct order (should be after scala builder) for project " + project.getName()),
        "categoryId" -> Integer.valueOf(999));

      // create a marker for the project
      MarkerUtilities.createMarker(project, markerAttributes, ScalastyleMarker.MarkerId);
    }

    Array(project)
  }

  // TODO when we do an incremental build, all files in the project seem to be marked as changed
  private def handleBuildSelection(resources: Array[IResource], monitor: IProgressMonitor, project: IProject, kind: Int): Unit = {
    println("hello world")
    handleException {
      // on full build remove all markers
      if (kind == IncrementalProjectBuilder.FULL_BUILD) {
        project.deleteMarkers(ScalastyleMarker.MarkerId, false, IResource.DEPTH_INFINITE);
      }

      val projectConfiguration = Persistence.loadProject(project)
      if (projectConfiguration.enabled && projectConfiguration.file.isDefined) {

        val file = Persistence.findConfiguration(projectConfiguration.file.get)
        if (file.isDefined && !file.get.exists) {
          throw ScalastylePluginException("cannot find file " + file.get.getAbsolutePath())
        } else {
          val configuration = ScalastyleConfiguration.readFromXml(file.get.getAbsolutePath())

          val messages = new ScalastyleChecker[EclipseFileSpec]().checkFiles(configuration, resources.map(r => {
            new EclipseFileSpec(r.getLocation().toFile().getAbsolutePath(), r)
          }).toList)

          new EclipseOutput().output(messages);
        }
      }
    }
  }

  def isDeltaAddedOrChanged(delta: IResourceDelta) = (delta.getKind() == IResourceDelta.ADDED) || (delta.getKind() == IResourceDelta.CHANGED)

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
  def isEnabled() = true
  def accept(resource: IResource) = "scala" == resource.getFileExtension()
}

class EclipseOutput extends Output[EclipseFileSpec] {
  private[this] val messageHelper = new MessageHelper(this.getClass().getClassLoader())
  override def output(messages: List[Message[EclipseFileSpec]]) = messages.foreach(message)

  private def message(m: Message[EclipseFileSpec]) = m match {
    case StartWork() => {}
    case EndWork() => {}
    case StartFile(file) => {
      // remove markers on this file
      file.resource.deleteMarkers(ScalastyleMarker.MarkerId, false, IResource.DEPTH_ZERO);

      // remove markers from package as well, not sure if this is necessary
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

    val markerAttributes: java.util.Map[String, Any] = HashMap(ScalastyleMarker.ModuleName -> "module",
      ScalastyleMarker.MessageKey -> error.key,
      IMarker.PRIORITY -> IMarker.PRIORITY_NORMAL,
      IMarker.SEVERITY -> severity,
      "categoryId" -> 999)

    MarkerUtilities.setLineNumber(markerAttributes, error.lineNumber.getOrElse(1));
    MarkerUtilities.setMessage(markerAttributes, messageHelper.message(error.clazz.getClassLoader(), error.key, error.args));

    // create a marker for the file
    MarkerUtilities.createMarker(error.fileSpec.resource, markerAttributes, ScalastyleMarker.MarkerId)
  }
}