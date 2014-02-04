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

import java.io.FileWriter
import java.io.PrintWriter
import java.util.Scanner

import org.eclipse.core.runtime.Path
import org.eclipse.jface.preference.PreferencePage
import org.eclipse.jface.viewers.TableViewer
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.widgets.Button
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Control
import org.eclipse.swt.widgets.FileDialog
import org.eclipse.swt.widgets.Shell
import org.eclipse.ui.IWorkbench
import org.eclipse.ui.IWorkbenchPreferencePage
import org.scalastyle.MessageHelper
import org.scalastyle.ScalastyleConfiguration
import org.scalastyle.scalastyleplugin.ExceptionUtils.handleError
import org.scalastyle.scalastyleplugin.ScalastylePlugin
import org.scalastyle.scalastyleplugin.SwtUtils.Container
import org.scalastyle.scalastyleplugin.SwtUtils.DialogColumn
import org.scalastyle.scalastyleplugin.SwtUtils.ModelContentProvider
import org.scalastyle.scalastyleplugin.SwtUtils.PropertiesLabelProvider
import org.scalastyle.scalastyleplugin.SwtUtils.TableLine
import org.scalastyle.scalastyleplugin.SwtUtils.TableSorter
import org.scalastyle.scalastyleplugin.SwtUtils.button
import org.scalastyle.scalastyleplugin.SwtUtils.composite
import org.scalastyle.scalastyleplugin.SwtUtils.gridData
import org.scalastyle.scalastyleplugin.SwtUtils.gridLayout
import org.scalastyle.scalastyleplugin.SwtUtils.group
import org.scalastyle.scalastyleplugin.SwtUtils.table
import org.scalastyle.scalastyleplugin.config.Persistence
import org.scalastyle.scalastyleplugin.config.WorkspaceConfiguration
import org.scalastyle.scalastyleplugin.config.WorkspaceConfigurations

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

  val weight = 100

  private[this] val columns = List(
    DialogColumn[Configuration]("Location", SWT.LEFT, LocationSorter, weight, { _.location }))

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
        new PropertiesLabelProvider(columns), setSelection, refresh, editConfiguration(currentSelection), layoutData = tableGridData)

    val browseButton = button(configurationsGroup, "Browse/Add", true, {
      browseForFile(this.getShell(), "Select a scalastyle configuration file") foreach addConfiguration
    })

    val newButton = button(configurationsGroup, "New", true, {
      browseForNewFile(getShell(), "Select a file to create with the default configuration") foreach createNewConfiguration
    })

    editButton = button(configurationsGroup, "Edit", false, {
      editConfiguration(currentSelection)
    })

    removeButton = button(configurationsGroup, "Remove", false, {
      removeConfiguration(currentSelection)
    })

    parentComposite
  }

  private[this] def createNewConfiguration(fileName: String): Unit = {
    val path = Path.fromOSString(fileName)
    val config = classLoader.getResourceAsStream(ScalastyleConfiguration.DefaultConfiguration)
    val printer = new PrintWriter(new FileWriter(fileName, false))
    val scanner = new Scanner(config).useDelimiter("\0")
    printer.append(scanner.next())
    printer.close()
    scanner.close()

    addConfiguration(fileName)
  }

  private[this] def browseForNewFile(shell: Shell, title: String): Option[String] = {
    val workspacePathName = ScalastylePlugin.getWorkspace().getRoot().getLocation().toOSString()
    val dialog = new FileDialog(getShell(), SWT.SAVE)
    dialog.setText(title)
    dialog.setFilterPath(workspacePathName)
    dialog.setFileName("scalastyle_configuration.xml")
    dialog.setOverwrite(true)

    Option(dialog.open())
  }

  private[this] def browseForFile(shell: Shell, title: String): Option[String] = {
    val dialog = new FileDialog(getShell(), SWT.OPEN)
    dialog.setText(title)
    dialog.setFilterExtensions(Array("*.xml", "*.*"))
    dialog.setFilterNames(Array("XML Files", "All Files (*)"))

    Option(dialog.open())
  }

  private[this] def addConfiguration(fileName: String) = {
    if (!model.elements.exists(_.location == fileName)) {
      model.elements :+= Configuration(fileName)
      refresh()
    }
  }

  private[this] def removeConfiguration(configuration: Configuration) = {
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

  def init(workbench: IWorkbench): Unit = {}

  override def performOk(): Boolean = {
    handleError(getShell()) {
      Persistence.saveWorkspace(toWorkspaceConfigurations(model))
    }
  }

  private[this] def toConfigurations(workspaceConfigurations: WorkspaceConfigurations) = {
    Configurations(workspaceConfigurations.configurations.map(c => Configuration(c.file)))
  }

  private[this] def toWorkspaceConfigurations(configurations: Configurations) = {
    WorkspaceConfigurations(configurations.elements.map(c => WorkspaceConfiguration(c.location)))
  }
}
