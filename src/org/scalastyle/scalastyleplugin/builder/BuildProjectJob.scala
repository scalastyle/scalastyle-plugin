package org.scalastyle.scalastyleplugin.builder

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;

import org.scalastyle.scalastyleplugin.Messages
import org.scalastyle.scalastyleplugin.nature.ScalastyleNature

object BuildProjectJob {
  def apply(project: IProject, kind: Int) = new BuildProjectJob(Array[IProject](project), kind, NLS.bind(Messages.buildSingleProject, project.getName()))
  def apply(projects: Array[IProject], kind: Int) = new BuildProjectJob(projects, kind, Messages.buildAllProjects)
}

class BuildProjectJob(projects: Array[IProject], kind: Int, message: String) extends Job(message) {
    def run(monitor: IProgressMonitor): IStatus = {
        var status: IStatus = Status.OK_STATUS

        try {
          projects.foreach(project => { 
                if (project.isOpen() && project.hasNature(ScalastyleNature.NATURE_ID)) {
                    project.build(kind, monitor);
                }
          })
        } catch {
          case e: CoreException => status = e.getStatus()
        } finally {
            monitor.done();
        }

        status
    }
}