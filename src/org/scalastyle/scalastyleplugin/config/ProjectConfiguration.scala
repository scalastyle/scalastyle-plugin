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

package org.scalastyle.scalastyleplugin.config

import scala.xml.NodeSeq.seqToNodeSeq
import scala.xml.Elem
import scala.xml.Node
import scala.xml._

import org.eclipse.core.resources.IProject
import org.scalastyle.scalastyleplugin.ScalastylePlugin
import org.scalastyle.ScalastyleConfiguration
import org.scalastyle.XmlPrettyPrinter
import org.eclipse.core.resources.ResourcesPlugin

case class WorkspaceConfiguration(file: String)
case class WorkspaceConfigurations(configurations: List[WorkspaceConfiguration])

case class ProjectConfiguration(enabled: Boolean, file: Option[String])

object Persistence {
  val WorkspaceConfigurationFile = "scalastyle-configurations.xml"
  val ProjectConfigurationFile = ".scalastyle"
  val xmlWidth = 1000
  val xmlStep = 1

  import org.scalastyle.scalastyleplugin.StringUtils._

  def workspaceConfigFile() = {
    val configPath = ScalastylePlugin.getDefault().getStateLocation().append(WorkspaceConfigurationFile);
    configPath.toFile();
  }

  def loadWorkspace(): WorkspaceConfigurations = {
    val configFile = workspaceConfigFile()

    if (configFile.exists()) {
      val elem = XML.loadFile(configFile)

      WorkspaceConfigurations((elem \\ "configuration").map(toConfiguration).toList)
    } else {
      WorkspaceConfigurations(List())
    }

    // TODO error handling please
  }

  def saveWorkspace(workspaceConfigurations: WorkspaceConfigurations) = {
    write(workspaceConfigFile().getAbsolutePath(), toXml(workspaceConfigurations), xmlWidth, xmlStep)
  }

  def toConfiguration(node: Node): WorkspaceConfiguration = {
    val file = node.attribute("file").get.text
//    val name = node.attribute("name").get.text
    WorkspaceConfiguration(file)
  }

  def toXml(configuration: WorkspaceConfigurations): scala.xml.Elem = {
    val elements = if (configuration.configurations.size > 0) {
      val cs = configuration.configurations.map(c => <configuration file={ c.file }></configuration>)
      <configurations>{ cs }</configurations>
    } else {
      scala.xml.Null
    }

    <scalastyle-workspace-configuration>{ elements }</scalastyle-workspace-configuration>
  }

  private[this] def projectConfigFile(project: IProject) = {
    println("project config file=" + project.getFile(ProjectConfigurationFile).getLocation().toFile())
    project.getFile(ProjectConfigurationFile).getLocation().toFile()
  }

  def loadProject(project: IProject): ProjectConfiguration = {
    val configFile = projectConfigFile(project)

    if (configFile.exists()) {
      val elem = XML.loadFile(configFile)

      val enabled = elem.attribute("enabled").get.text == "true"
      val configuration = elem.attribute("file") match {
        case Some(x) => if (isEmpty(x.text)) None else Some(x.text)
        case None => None
      }

      ProjectConfiguration(enabled, configuration)
    } else {
      ProjectConfiguration(false, None)
    }

    // TODO error handling please
  }

  def toXml(projectConfiguration: ProjectConfiguration): scala.xml.Elem = {
    val enabled = projectConfiguration.enabled.toString().toLowerCase()
    val file = projectConfiguration.file match {
      case Some(x) => Attribute(None, "file", Text(x), Null)
      case None => scala.xml.Null
    }
    println("file=" + file)
    <scalastyle-project-configuration enabled={enabled}></scalastyle-project-configuration> % file
  }

  def saveProject(project: IProject, projectConfiguration: ProjectConfiguration) = {
    write(projectConfigFile(project).getAbsolutePath(), toXml(projectConfiguration), xmlWidth, xmlStep)
  }

  def saveConfiguration(filename: String, scalastyleConfiguration: ScalastyleConfiguration): Unit = {
    write(filename, ScalastyleConfiguration.toXml(scalastyleConfiguration), xmlWidth, xmlStep)
  }

  def findConfiguration(file: String): Option[java.io.File] = {
    val resource = ResourcesPlugin.getWorkspace().getRoot().findMember(file)
    if (resource != null) {
      Some(resource.getLocation().toFile())
    } else {
      None
    }
  }

  // TODO change filename to an IFile?
  private[this] def write(filename: String, elem: Elem, width: Int, step: Int): Unit = {
    println("writing to " + filename)
    val s = new XmlPrettyPrinter(width, step).format(elem)

    var out: java.io.Writer = null;

    try {
      out = new java.io.BufferedWriter(new java.io.FileWriter(filename))
      out.write(s)
    } catch {
      case e => throw e // TODO do something here
    } finally {
      try {
        if (out != null) {
          out.close();
        }
      } catch {
        case _ => // do nothing
      }
    }
  }
}