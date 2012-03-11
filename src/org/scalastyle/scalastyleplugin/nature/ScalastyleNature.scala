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