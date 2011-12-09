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
import org.scalastyle.scalastyleplugin.ScalastylePluginException
import org.scalastyle.scalastyleplugin.nature.ScalastyleNature
import org.segl.scalastyle.ScalastyleConfiguration
import org.segl.scalastyle.TextOutput
import org.scalastyle.scalastyleplugin.config.ProjectConfigurations
import org.segl.scalastyle.ScalastyleChecker
import org.segl.scalastyle._
import scala.collection.JavaConversions._

class EclipseFileSpec(val name: String, val resource: IResource) extends FileSpec

object ScalastyleBuilder {
  /** Eclipse extension point ID for the builder. */
  val BUILDER_ID = ScalastylePlugin.PLUGIN_ID + ".ScalastyleBuilder" //$NON-NLS-1$

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
      project.exists() && project.isOpen() && project.hasNature(ScalastyleNature.NATURE_ID)
    })

    val buildJob = BuildProjectJob(scalastyleProjects, IncrementalProjectBuilder.FULL_BUILD)
    buildJob.setRule(ResourcesPlugin.getWorkspace().getRoot());
    buildJob.schedule();
  }
}

class ScalastyleBuilder extends IncrementalProjectBuilder {
  /**
   * @see org.eclipse.core.internal.events.InternalBuilder #build(int,
   *      java.util.Map, org.eclipse.core.runtime.IProgressMonitor)
   */
  def build(kind: Int, args: java.util.Map[_, _], monitor: IProgressMonitor): Array[IProject] = {
    // get the associated project for this builder
    val project = getProject();

    // remove project level error markers
    project.deleteMarkers(ScalastyleMarker.MARKER_ID, false, IResource.DEPTH_ZERO);

    if (ScalastyleNature.hasCorrectBuilderOrder(project)) {
      val resourceDelta = getDelta(project);
      val filters = Array[IFilter](new IFilter {})
      val files = if (resourceDelta != null) getDeltaFiles(resourceDelta, filters) else getProjectFiles(project, filters)
      handleBuildSelection(files, monitor, project, kind);
    } else {
      // TODO not sure what to do here?
    }

    Array(project)
  }

  // TODO when we do an incremental build, all files in the project seem to be marked as changed
  private def handleBuildSelection(resources: Array[IResource], monitor: IProgressMonitor, project: IProject, kind: Int): Unit = {

    // on full build remove all markers
    if (kind == IncrementalProjectBuilder.FULL_BUILD) {
      project.deleteMarkers(ScalastyleMarker.MARKER_ID, false, IResource.DEPTH_INFINITE);
    }

    try {
      val projectConfiguration = ProjectConfigurations.configuration(project)
      val configuration = ScalastyleConfiguration.readFromXml(projectConfiguration.files(0))

      val messages = new ScalastyleChecker[EclipseFileSpec]().checkFiles(configuration, resources.map(r => {
        new EclipseFileSpec(r.getLocation().toFile().getAbsolutePath(), r)
      }).toList)

      //  new XmlOutput().output(messages);
      new EclipseOutput().output(messages);

    } catch {
      // TODO probably add something to the error log and marker?
      case e: Exception => throw new CoreException(new Status(IStatus.ERROR, ScalastylePlugin.PLUGIN_ID, IStatus.ERROR, e.getLocalizedMessage(), e))
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

  override def output(messages: List[Message[EclipseFileSpec]]) = messages.foreach(message)

  private def message(m: Message[EclipseFileSpec]) = m match {
    case StartWork() => {}
    case EndWork() => {}
    case StartFile(file) => {
      // remove markers on this file
      file.resource.deleteMarkers(ScalastyleMarker.MARKER_ID, false, IResource.DEPTH_ZERO);

      // remove markers from package as well, not sure if this is necessary
      file.resource.getParent().deleteMarkers(ScalastyleMarker.MARKER_ID, false, IResource.DEPTH_ZERO);
    }
    case EndFile(file) => {}
    case error: StyleError[_] => addError(error)
    case StyleException(file, message, stacktrace, line, column) => {
      // TODO cope with StyleException please
    }
  }

  private def addError(error: StyleError[EclipseFileSpec]): Unit = {
    println("error file=" + error.fileSpec.name + " key=" + error.key + " lineNumber=" + error.lineNumber + " column=" + error.column)
    // TODO limit number of errors, do we limit number of errors and number of warnings? or both together
    // TODO severity level

    // TODO rule metadata

    try {
      val markerAttributes: java.util.Map[String, Any] = HashMap(ScalastyleMarker.MODULE_NAME -> "module",
															        ScalastyleMarker.MESSAGE_KEY -> error.key,
															        IMarker.PRIORITY -> IMarker.PRIORITY_NORMAL,
															        IMarker.SEVERITY -> IMarker.SEVERITY_WARNING,
															        "categoryId" -> 998)

      MarkerUtilities.setLineNumber(markerAttributes, error.lineNumber.getOrElse(1));
      MarkerUtilities.setMessage(markerAttributes, error.key);

      //                        // TODO calculate offset for editor annotations
      //                        calculateMarkerOffset(error, mMarkerAttributes);

      // create a marker for the file
      MarkerUtilities.createMarker(error.fileSpec.resource, markerAttributes, ScalastyleMarker.MARKER_ID)
    } catch {
      case _ =>
      // TODO log exception
    }
  }

}