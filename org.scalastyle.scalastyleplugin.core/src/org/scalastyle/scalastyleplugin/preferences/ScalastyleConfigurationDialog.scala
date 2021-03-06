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

import org.eclipse.jface.dialogs.MessageDialog
import org.eclipse.jface.dialogs.TitleAreaDialog
import org.eclipse.jface.viewers.TableViewer
import org.eclipse.jface.window.Window
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.widgets.Button
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Control
import org.eclipse.swt.widgets.Shell
import org.eclipse.swt.widgets.Text
import org.scalastyle.ConfigurationChecker
import org.scalastyle.DefinitionChecker
import org.scalastyle.Level
import org.scalastyle.MessageHelper
import org.scalastyle.ParameterType
import org.scalastyle.ScalastyleConfiguration
import org.scalastyle.ScalastyleDefinition
import org.scalastyle.scalastyleplugin.ExceptionUtils.handleError
import org.scalastyle.scalastyleplugin.ScalastylePlugin
import org.scalastyle.scalastyleplugin.StringUtils.isEmpty
import org.scalastyle.scalastyleplugin.SwtUtils.Container
import org.scalastyle.scalastyleplugin.SwtUtils.DialogColumn
import org.scalastyle.scalastyleplugin.SwtUtils.ModelContentProvider
import org.scalastyle.scalastyleplugin.SwtUtils.PropertiesLabelProvider
import org.scalastyle.scalastyleplugin.SwtUtils.TableLine
import org.scalastyle.scalastyleplugin.SwtUtils.TableSorter
import org.scalastyle.scalastyleplugin.SwtUtils.button
import org.scalastyle.scalastyleplugin.SwtUtils.checkbox
import org.scalastyle.scalastyleplugin.SwtUtils.composite
import org.scalastyle.scalastyleplugin.SwtUtils.gridData
import org.scalastyle.scalastyleplugin.SwtUtils.gridLayout
import org.scalastyle.scalastyleplugin.SwtUtils.group
import org.scalastyle.scalastyleplugin.SwtUtils.label
import org.scalastyle.scalastyleplugin.SwtUtils.table
import org.scalastyle.scalastyleplugin.SwtUtils.text
import org.scalastyle.scalastyleplugin.config.Persistence

import com.typesafe.config.ConfigFactory

// scalastyle:off magic.number

case class ModelChecker(definitionChecker: DefinitionChecker, _configurationChecker: Option[ConfigurationChecker]) extends TableLine {
  def enabled: Boolean = _configurationChecker.isDefined && _configurationChecker.get.enabled
  private[this] var configurationChecker = copyConfigurationChecker()
  var dirty = false

  def set(level: Level, enabled: Boolean, parameters: Map[String, String], customMessage: Option[String], customId: Option[String]): Unit = {
    configurationChecker = configurationChecker.copy(level = level, enabled = enabled, parameters = parameters,
        customMessage = customMessage, customId = customId)
    dirty = true
  }

  def typeOf(name: String): ParameterType = {
    definitionChecker.parameters.get(name).get.typeName
  }

  def isMultiple(name: String): Boolean = {
    definitionChecker.parameters.get(name).get.multiple
  }

  // TODO put some tests in here to ensure values are copied correctly etc.
  def configurationChecker(): ConfigurationChecker = configurationChecker

  private[this] def copyConfigurationChecker() = {
    var configurationChecker = _configurationChecker.getOrElse(definitionToConfiguration(definitionChecker))

    val parameters = definitionChecker.parameters.map(dpm => {
      val p = configurationChecker.parameters.get(dpm._1)
      val value = if (p.isEmpty) dpm._2.defaultValue else p.get
      (dpm._1 -> value)
    });
    configurationChecker.copy(parameters = parameters)
  }

  def definitionToConfiguration(checker: DefinitionChecker): ConfigurationChecker = {
    ConfigurationChecker(checker.className, checker.level, false, checker.parameters.map(m => (m._1, m._2.defaultValue)).toMap, None, None)
  }
}

case class Model(definition: ScalastyleDefinition, configuration: ScalastyleConfiguration) extends Container[ModelChecker] {
  val list: List[ModelChecker] = definition.checkers.map(dc => ModelChecker(dc, configuration.checks.find(c => c.className == dc.className)))

  def elements: List[ModelChecker] = list

  def dirty: Boolean = list.find(_.dirty).isDefined

  def toConfiguration(name: String, ignoreCommentFilters: Boolean): ScalastyleConfiguration = {
    val checkers = list.map(mc => ConfigurationChecker(mc.configurationChecker.className, mc.configurationChecker.level,
                                mc.configurationChecker.enabled, mc.configurationChecker.parameters, mc.configurationChecker.customMessage,
                                mc.configurationChecker.customId))
    ScalastyleConfiguration(name, ignoreCommentFilters, checkers)
  }
}

class ScalastyleConfigurationDialog(parent: Shell, filename: String) extends TitleAreaDialog(parent) {
  setShellStyle(getShellStyle() | SWT.RESIZE | SWT.MAX)
  val classLoader = this.getClass().getClassLoader()
  val definition = ScalastyleDefinition.readFromXml(classLoader.getResourceAsStream("/scalastyle_definition.xml"))
  val file = Persistence.findConfiguration(filename)
  val configuration = ScalastyleConfiguration.readFromXml(file.get.getAbsolutePath())
  val model = new Model(definition, configuration)
  val messageHelper = new MessageHelper(ConfigFactory.parseResources("reference.conf"))
  var nameText: Text = _
  var enableCommentFilterButton: Button = _
  var editButton: Button = _
  var newButton: Button = _
  var currentSelection: Option[ModelChecker] = None;
  var tableViewer: TableViewer = _
  val NameSorter = new TableSorter[ModelChecker, String](_.definitionChecker.id, true)
  val SeveritySorter = new TableSorter[ModelChecker, String](_.definitionChecker.level.name, true)
  val ClassSorter = new TableSorter[ModelChecker, String](_.definitionChecker.className, true)
  val ParamsSorter = new TableSorter[ModelChecker, String](_.definitionChecker.parameters.toString, true)

  private[this] val columns = List(
    DialogColumn[ModelChecker]("Enabled", SWT.LEFT, null, 15, { mc => if (mc.configurationChecker.enabled) "true" else "false" }),
    DialogColumn[ModelChecker]("Name", SWT.LEFT, NameSorter, 35, { mc => messageHelper.label(mc.definitionChecker.id) }),
    DialogColumn[ModelChecker]("Severity", SWT.LEFT, SeveritySorter, 15, { mc => messageHelper.text(mc.configurationChecker.level.name) }),
    DialogColumn[ModelChecker]("Params", SWT.LEFT, ParamsSorter, 20, { mc => string(mc.configurationChecker.parameters) }),
    DialogColumn[ModelChecker]("Class", SWT.LEFT, ClassSorter, 15, { mc => mc.definitionChecker.className })
  )

  private[this] def string(map: Map[String, String]): String = map.map(cp => cp._1 + "=" + cp._2).mkString(",")

  override def createDialogArea(parent: Composite): Control = {
    setTitleImage(ScalastylePlugin.PluginLogo);
    val contents = composite(parent, Some(gridData()));

    val headerComposite = composite(contents, layout = gridLayout(2), layoutData = Some(gridData()));
    label(headerComposite, "Name")
    nameText = text(headerComposite, model.configuration.name, true, false)
    label(headerComposite, "Comment Filters")
    enableCommentFilterButton = checkbox(headerComposite, model.configuration.commentFilter)

    val checkerGroup = group(contents, "Checkers")

    val tableGridData = new GridData(GridData.FILL_HORIZONTAL)
    tableGridData.heightHint = 500
    tableGridData.grabExcessHorizontalSpace = true

    tableViewer = table(checkerGroup, model, columns, new ModelContentProvider[ModelChecker](model),
                        new PropertiesLabelProvider(columns), setSelection, refresh, editChecker(currentSelection), layoutData = tableGridData)

    editButton = button(contents, "Edit", false, { editChecker(currentSelection) })

    contents
  }

  private[this] def refresh() = {
    tableViewer.refresh(true, true)
  }

  private[this] def editChecker(modelChecker: Option[ModelChecker]): Unit = {
    if (modelChecker.isDefined) {
      val dialog = new ScalastyleCheckerDialog(getShell(), messageHelper, modelChecker.get)
      if (Window.OK == dialog.open()) {
        refresh()
      }
    }
  }

  private[this] def setSelection(modelChecker: Any) = {
    editButton.setEnabled(true)
    currentSelection = Some(modelChecker.asInstanceOf[ModelChecker])
  }

  override def okPressed(): Unit = {
    if (isEmpty(nameText.getText())) {
      val message = "Name must have a value"
      MessageDialog.open(MessageDialog.ERROR, getShell(), "Scalastyle configuration error", message, SWT.OK)
    } else {
        if (nameText.getText() != model.configuration.name || model.dirty) {
          handleError(getShell()) {
            Persistence.saveConfiguration(file.get.getAbsolutePath(), model.toConfiguration(nameText.getText(), enableCommentFilterButton.getSelection()))
            super.okPressed();
          }
        } else {
          super.okPressed();
        }
    }
  }
}
