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
  val NATURE_ID = ScalastylePlugin.PLUGIN_ID + ".ScalastyleNature"
  val ScalaBuilderId = "org.scala-ide.sdt.core.scalabuilder"

  /**
   * Checks if the ordering of the builders of the given project is correct,
   * more specifically if the ScalastyleBuilder is set to run after the
   * ScalaBuilder.
   */
  def hasCorrectBuilderOrder(project: IProject): Boolean = {
    val commands = project.getDescription().getBuildSpec()

    var scalaBuilderIndex = builderIndex(commands, ScalaBuilderId)
    var scalastyleBuilderIndex = builderIndex(commands, ScalastyleBuilder.BUILDER_ID)

    scalaBuilderIndex < scalastyleBuilderIndex
  }
  
  def builderIndex(commands: Array[ICommand], builderId: String): Int = commands.indexWhere(b => b.getBuilderName() == builderId)
}

class ScalastyleNature extends IProjectNature {
  private[this] var project: IProject = _;

  def configure(): Unit = {
    val description = project.getDescription();
    val commands = description.getBuildSpec();

    if (!commands.exists(c => c.getBuilderName().equals(ScalastyleBuilder.BUILDER_ID))) {
      // add builder to project
      val command = description.newCommand();
      command.setBuilderName(ScalastyleBuilder.BUILDER_ID);

      description.setBuildSpec(commands :+ command);
      project.setDescription(description, new NullProgressMonitor())
    }
  }

  def deconfigure(): Unit = {
    val description = project.getDescription();
    val commands = description.getBuildSpec();

    val newCommands = commands.filter(c => !c.getBuilderName().equals(ScalastyleBuilder.BUILDER_ID))

    description.setBuildSpec(newCommands);
    project.setDescription(description, new NullProgressMonitor());

    // remove markers from the project
    getProject().deleteMarkers(ScalastyleMarker.MARKER_ID, true, IResource.DEPTH_INFINITE);
  }

  def getProject: IProject = project
  def setProject(project: IProject) = this.project = project
}