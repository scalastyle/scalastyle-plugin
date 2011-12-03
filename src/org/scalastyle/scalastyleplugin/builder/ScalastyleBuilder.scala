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
import org.scalastyle.scalastyleplugin.ScalastylePlugin
import org.scalastyle.scalastyleplugin.ScalastylePluginException
import org.scalastyle.scalastyleplugin.nature.ScalastyleNature

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
            val filters = Array[IFilter]()
//            val files = if (resourceDelta != null) getFiles(resourceDelta, filters) else getFiles(project, filters)
            val files = getFiles(project, filters)
            handleBuildSelection(files, monitor, project, kind);
        } else {
          // TODO not sure what to do here?
        }

        Array(project)
    }

    private def handleBuildSelection(resources: List[IResource], monitor: IProgressMonitor, project: IProject, kind: Int): Unit = {

        // on full build remove all previous checkstyle markers
        if (kind == IncrementalProjectBuilder.FULL_BUILD) {
            project.deleteMarkers(ScalastyleMarker.MARKER_ID, false, IResource.DEPTH_INFINITE);
        }

        try {
          System.out.println("build something here");
        } catch {
          case e: ScalastylePluginException => {
            val status = new Status(IStatus.ERROR, ScalastylePlugin.PLUGIN_ID, IStatus.ERROR, e.getLocalizedMessage(), e)
            throw new CoreException(status)
          }
          // TODO case _ => throw new CoreException(e)
        }
    }

//    private[this] def getFiles(delta: IResourceDelta, filters: Array[IFilter]): List[IResource] = {
//	  val resources = ListBuffer[IResource]();
//
//      val affectedChildren = delta.getAffectedChildren();
//
//      val resources = affectedChildren.map(childDelta => {
//            // check if a resource has changed
//            val deltaKind = childDelta.getKind();
//            if ((deltaKind == IResourceDelta.ADDED) || (deltaKind == IResourceDelta.CHANGED)) {
//                val resource = childDelta.getResource();
//                
//                resource ::  
//                
//                resource match {
//                  case c: IContainer => Some(getFiles(c, filters))
//                  case _ => None
//                }
//            }
//        });
//      
//        resources.toList
//    }

    private[this] def accept(resource: IResource, filter: IFilter) = !filter.isEnabled() || filter.accept(resource)
    private[this] def acceptAll(resource: IResource, filters: Array[IFilter]) = filters.exists(f => accept(resource, f)) 
    
    private[this] def getFiles(container: IContainer, filters: Array[IFilter]): List[IResource] = {
	  val resources = ListBuffer[IResource]();

      val children = container.members();

      children.foreach(child => {
            if (acceptAll(child, filters)) {
            	resources += child;
            }

                child match {
                  case c: IContainer => resources ++= getFiles(c, filters)
                  case _ => 
                }
        })
        
      resources.toList
    }

}

trait IFilter {
  def isEnabled() = true
  def accept(resource: IResource) = true
}