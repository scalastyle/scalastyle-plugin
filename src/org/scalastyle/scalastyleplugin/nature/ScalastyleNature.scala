package org.scalastyle.scalastyleplugin.nature

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.JavaCore;

import org.scalastyle.scalastyleplugin.ScalastylePlugin
import org.scalastyle.scalastyleplugin.builder.ScalastyleBuilder
import org.scalastyle.scalastyleplugin.builder.ScalastyleMarker

object ScalastyleNature {
  val NATURE_ID = ScalastylePlugin.PLUGIN_ID + ".ScalastyleNature"; //$NON-NLS-1$

  /**
   * Checks if the ordering of the builders of the given project is correct,
   * more specifically if the ScalastyleBuilder is set to run after the
   * JavaBuilder.
   *
   * @param project the project to check
   * @return <code>true</code> if the builder order for this project is
   *         correct, <code>false</code> otherwise
   * @throws CoreException error getting project description
   */
  def hasCorrectBuilderOrder(project: IProject): Boolean = {
    val description = project.getDescription()
    val commands = description.getBuildSpec()

    var javaBuilderIndex: Int = -1;
    var scalastyleBuilderIndex: Int = -1;

    commands.zipWithIndex.foreach(x => {
      if (x._1.getBuilderName().equals(ScalastyleBuilder.BUILDER_ID)) {
        scalastyleBuilderIndex = x._2;
      } else if (x._1.getBuilderName().equals(JavaCore.BUILDER_ID)) {
        javaBuilderIndex = x._2;
      }
    })

    return javaBuilderIndex < scalastyleBuilderIndex
  }
}

class ScalastyleNature extends IProjectNature {
  private[this] var project: IProject = _;

  def configure(): Unit = {
    val description = project.getDescription();
    val commands = description.getBuildSpec();
    var found = false;

    if (!commands.exists(c => c.getBuilderName().equals(ScalastyleBuilder.BUILDER_ID))) {
      // add builder to project
      val command: ICommand = description.newCommand();
      command.setBuilderName(ScalastyleBuilder.BUILDER_ID);
      val newCommands = commands :+ command

      description.setBuildSpec(newCommands);
      project.setDescription(description, null)
    }
  }

  def deconfigure(): Unit = {
    val description = project.getDescription();
    val commands = description.getBuildSpec();

    val newCommands = commands.filter(c => !c.getBuilderName().equals(ScalastyleBuilder.BUILDER_ID))

    description.setBuildSpec(newCommands);
    project.setDescription(description, new NullProgressMonitor());

    // remove checkstyle markers from the project
    getProject().deleteMarkers(ScalastyleMarker.MARKER_ID, true, IResource.DEPTH_INFINITE);
  }

  def getProject: IProject = project
  def setProject(project: IProject) = this.project = project

}