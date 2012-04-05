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

import org.eclipse.core.runtime.Platform
import org.eclipse.core.runtime.preferences.IEclipsePreferences
import org.eclipse.core.runtime.preferences.IPreferencesService
import org.eclipse.core.runtime.preferences.InstanceScope
import org.eclipse.jface.dialogs.IDialogConstants
import org.eclipse.jface.dialogs.MessageDialogWithToggle
import org.eclipse.jface.preference.IPreferenceStore
import org.eclipse.jface.preference.PreferencePage
import org.eclipse.jface.resource.ImageDescriptor
import org.eclipse.osgi.util.NLS
import org.eclipse.swt.SWT
import org.eclipse.swt.events.SelectionAdapter
import org.eclipse.swt.events.SelectionListener
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.swt.layout.FormAttachment
import org.eclipse.swt.layout.FormData
import org.eclipse.swt.layout.FormLayout
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.Button
import org.eclipse.swt.widgets.Combo
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Control
import org.eclipse.swt.widgets.Group
import org.eclipse.swt.widgets.Label
import org.eclipse.swt.widgets.Text
import org.eclipse.ui.IWorkbench
import org.eclipse.ui.IWorkbenchPreferencePage
import org.osgi.service.prefs.BackingStoreException
import org.scalastyle.scalastyleplugin.ScalastylePlugin
import org.scalastyle.scalastyleplugin.config._
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog
import org.eclipse.jface.dialogs.MessageDialog
import org.eclipse.ui.dialogs.ISelectionStatusValidator
import org.eclipse.ui.model.WorkbenchContentProvider
import org.eclipse.ui.model.WorkbenchLabelProvider
import org.eclipse.core.runtime.IStatus
import org.eclipse.core.resources.IFile
import org.eclipse.core.runtime.Path
import org.eclipse.core.runtime.IPath
import org.eclipse.core.runtime.Status
import org.eclipse.ui.PlatformUI
import org.eclipse.swt.widgets.Shell
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog
import org.eclipse.jface.window.Window
import org.scalastyle.scalastyleplugin.StringUtils._
import org.scalastyle.scalastyleplugin.SwtUtils._
import org.scalastyle.scalastyleplugin.ExceptionUtils._
import org.scalastyle._
import org.eclipse.jface.viewers.TableViewer
import org.eclipse.ui.dialogs.SaveAsDialog
import org.eclipse.core.resources.ResourcesPlugin
import org.eclipse.core.runtime.URIUtil

case class Configuration(location: String) extends TableLine

case class Configurations(var elements: List[Configuration]) extends Container[Configuration]

class ScalastylePreferencePage extends PreferencePage with IWorkbenchPreferencePage {
  setPreferenceStore(ScalastylePlugin.getDefault().getPreferenceStore())

  val LocationSorter = new TableSorter[Configuration, String](_.location, true)
  val classLoader = this.getClass().getClassLoader()
  val messageHelper = new MessageHelper(classLoader)
  val model = toConfigurations(Persistence.loadWorkspace())

  var editButton: Button = _
  var currentSelection: Configuration = _
  var browseButton: Button = _
  var removeButton: Button = _
  var Button: Button = _
  var tableViewer: TableViewer = _

  private[this] val columns = List(
    DialogColumn[Configuration]("Location", SWT.LEFT, LocationSorter, 100, { _.location }))

  def createContents(parent: Composite): Control = {
    noDefaultAndApplyButton()

    // there is a bug in SWT which means you can't mix & match FormLayout
    // and GridLayout, you get java.lang.ClassCastException: org.eclipse.swt.layout.GridData cannot be cast to org.eclipse.swt.layout.FormData
    // so we set the parent layout here

    parent.setLayout(gridLayout())
    parent.setLayoutData(gridData())
    val parentComposite = composite(parent)
    val spanColumns = 4

    val configurationsGroup = group(parentComposite, "Configurations", layout = gridLayout(spanColumns));

    val tableGridData = new GridData(GridData.FILL_BOTH)
    tableGridData.horizontalSpan = spanColumns;
    tableGridData.horizontalAlignment = GridData.FILL;

    tableViewer = table(configurationsGroup, model, columns, new ModelContentProvider(model),
        new PropertiesLabelProvider(columns), setSelection, refresh, layoutData = tableGridData)

    val browseButton = button(configurationsGroup, "Browse/Add", true, {
      browseForFile(this.getShell(), "Select a scalastyle configuration file") match {
        case Some(file) => addConfiguration(file)
        case None =>
      }
    })

    editButton = button(configurationsGroup, "Edit", false, {
      editConfiguration(currentSelection)
    })

    removeButton = button(configurationsGroup, "Remove", false, {
      removeConfiguration(currentSelection)
    })

    parentComposite
  }

  def addConfiguration(file: IFile) = {
    model.elements = model.elements ::: List(Configuration(file.getFullPath().toString()))
    refresh()
  }

  def removeConfiguration(configuration: Configuration) = {
    model.elements = model.elements.filter(_.location != configuration.location)
    refresh()
    editButton.setEnabled(false);
    removeButton.setEnabled(false);
  }

  private[this] def editConfiguration(configuration: Configuration) = {
    val dialog = new ScalastyleConfigurationDialog(getShell(), configuration.location);
    dialog.setBlockOnOpen(true);
    dialog.open();
  }

  def setSelection(selection: Configuration): Unit = {
    currentSelection = selection
    editButton.setEnabled(true);
    removeButton.setEnabled(true);
  }

  def refresh(): Unit = {
    tableViewer.refresh(true, true)
  }

  private[this] def browseForFile(shell: Shell, title: String): Option[IFile] = {
    val dialog = new ElementTreeSelectionDialog(shell, new WorkbenchLabelProvider(), new WorkbenchContentProvider());
    dialog.setTitle(title)
    dialog.setMessage(title)
    dialog.setBlockOnOpen(true)
    dialog.setAllowMultiple(false)
    // TODO add initial selection
    //    val initial = file match {
    //      case Some(name) => name
    //      case None => null
    //    }
    //    dialog.setInitialSelection(initial)
    dialog.setInput(ScalastylePlugin.getWorkspace().getRoot())

    dialog.setValidator(new ISelectionStatusValidator() {
      def validate(selection: Array[Object]): IStatus = {
        val valid = selection.length == 1 && selection(0).isInstanceOf[IFile]
        new Status(if (valid) IStatus.OK else IStatus.ERROR, PlatformUI.PLUGIN_ID, IStatus.ERROR, "", null)
      }
    });

    if (Window.OK == dialog.open()) {
      val result = dialog.getResult();
      val checkFile = result(0).asInstanceOf[IFile];
      Some(checkFile)
    } else {
      None
    }
  }

  def init(workbench: IWorkbench): Unit = {}

  override def performOk(): Boolean = {
    handleError(getShell()) {
      Persistence.saveWorkspace(toWorkspaceConfigurations(model))
    }
  }

  def toConfigurations(workspaceConfigurations: WorkspaceConfigurations) = {
    Configurations(workspaceConfigurations.configurations.map(c => Configuration(c.file)))
  }

  def toWorkspaceConfigurations(configurations: Configurations) = {
    WorkspaceConfigurations(configurations.elements.map(c => WorkspaceConfiguration(c.location)))
  }
}
