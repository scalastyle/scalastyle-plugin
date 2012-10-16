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

package org.scalastyle.scalastyleplugin.preferences

import org.eclipse.core.resources.IProject
import org.eclipse.core.runtime.IAdaptable
import org.eclipse.swt.widgets.Button
import org.eclipse.swt.widgets.Combo
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Control
import org.eclipse.ui.dialogs.PropertyPage
import org.scalastyle.scalastyleplugin.ExceptionUtils.handleError
import org.scalastyle.scalastyleplugin.SwtUtils.checkbox
import org.scalastyle.scalastyleplugin.SwtUtils.combo
import org.scalastyle.scalastyleplugin.SwtUtils.gridLayout
import org.scalastyle.scalastyleplugin.SwtUtils.group
import org.scalastyle.scalastyleplugin.SwtUtils.label
import org.scalastyle.scalastyleplugin.config.Persistence
import org.scalastyle.scalastyleplugin.config.ProjectConfiguration
import org.scalastyle.scalastyleplugin.config.WorkspaceConfigurations

case class PropertyModel(enabled: Boolean, configuration: Option[String])

// TODO this should only be available when scalastyle nature is set in the project
class ScalastylePropertyPage extends PropertyPage {
  var enableButton: Button = _
  var configurationCombo: Combo = _
  var project: IProject = _
  var model: PropertyModel = _

  def createContents(parent: Composite): Control = {
    noDefaultAndApplyButton();
    val projectGroup = group(parent, "Project", layout = gridLayout(2))

    model = toModel(project)

    label(projectGroup, "Enabled")
    enableButton = checkbox(projectGroup, model.enabled)
    label(projectGroup, "Configuration")
    val configurations = Persistence.loadWorkspace()
    val current: String = model.configuration match {
      case Some(file) => file
      case None => if (configurations.configurations.size > 0) configurations.configurations(0).file else ""
    }
    configurationCombo = combo(projectGroup, toArray(configurations), current)

    projectGroup
  }

  private[this] def toModel(project: IProject) = {
    val configuration = Persistence.loadProject(project)
    PropertyModel(configuration.enabled, configuration.file)
  }

  private[this] def toProjectConfiguration(model: PropertyModel) = {
    val index = configurationCombo.getSelectionIndex()
    val selected = if (index >= 0) {
      Some(configurationCombo.getItems()(index))
    } else {
      None
    }
    ProjectConfiguration(enableButton.getSelection(), selected)
  }

  override def setElement(element: IAdaptable): Unit = {
    this.project = element.asInstanceOf[IProject]
  }

  private[this] def toArray(configurations: WorkspaceConfigurations): Array[String] = {
    Array(configurations.configurations.map(c => c.file): _*)
  }

  override def isValid(): Boolean = {
    true
  }

  override def performOk(): Boolean = {
    handleError(getShell()) {
      val configuration = toProjectConfiguration(model)
      Persistence.saveProject(project, configuration)
    }
  }
}
