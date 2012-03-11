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

import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PropertyPage;
import org.scalastyle.scalastyleplugin.SwtUtils._
import org.scalastyle.scalastyleplugin.config._

case class PropertyModel(enabled: Boolean, configuration: Option[String])

// TODO this should only be available when scalastyle nature is set in the project
class ScalastylePropertyPage extends PropertyPage {
  var enableButton: Button = _
  var configurationCombo: Combo = _
  var project: IProject = _
  var model: PropertyModel = _

  def createContents(parent: Composite): Control = {
    println("createContents")
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
    println("index=" + index)
    val selected = if (index >= 0) {
      Some(configurationCombo.getItems()(index))
    } else {
      None
    }
    ProjectConfiguration(enableButton.getSelection(), selected)
  }

  override def setElement(element: IAdaptable): Unit = {
    val project = element.asInstanceOf[IProject]
    println("project=" + project.getName())
    this.project = project
  }

  private[this] def toArray(configurations: WorkspaceConfigurations): Array[String] = {
    Array(configurations.configurations.map(c => c.file): _*)
  }

  override def isValid(): Boolean = {
    true
  }

  override def performOk(): Boolean = {
    val configuration = toProjectConfiguration(model)

    Persistence.saveProject(project, configuration)

    true
  }
}