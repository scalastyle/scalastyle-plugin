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

import org.eclipse.jface.dialogs._;
import org.eclipse.jface.viewers._;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.events._;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout._;
import org.eclipse.swt.widgets.{List => _, _}
import org.eclipse.jface.dialogs.Dialog;
import org.scalastyle._;
import org.eclipse.jface.viewers._
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.window.Window;
import org.scalastyle.scalastyleplugin.ScalastylePlugin;
import org.scalastyle.scalastyleplugin.SwtUtils._
import org.scalastyle.scalastyleplugin.config.Persistence

case class ModelChecker(definitionChecker: DefinitionChecker, _configurationChecker: Option[ConfigurationChecker]) extends TableLine {
  def enabled = _configurationChecker.isDefined && _configurationChecker.get.enabled
  private[this] var configurationChecker = copyConfigurationChecker()
  var dirty = false

  def set(level: Level, enabled: Boolean, parameters: Map[String, String]) = {
    configurationChecker = configurationChecker.copy(level = level, enabled = enabled, parameters = parameters)
    dirty = true
  }

  def typeOf(name: String) = {
    definitionChecker.parameters.get(name).get.typeName
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
    ConfigurationChecker(checker.className, checker.level, false, checker.parameters.map(m => (m._1, m._2.defaultValue)).toMap)
  }
}

case class Model(definition: ScalastyleDefinition, configuration: ScalastyleConfiguration) extends Container[ModelChecker] {
  val list: List[ModelChecker] = definition.checkers.map(dc => ModelChecker(dc, configuration.checks.find(c => c.className == dc.className)))

  def elements = list

  def dirty = list.find(_.dirty).isDefined

  def toConfiguration(name: String): ScalastyleConfiguration = {
    val checkers = list.map(mc => ConfigurationChecker(mc.configurationChecker.className, mc.configurationChecker.level,
                                mc.configurationChecker.enabled, mc.configurationChecker.parameters))
    ScalastyleConfiguration(name, checkers)
  }
}

class ScalastyleConfigurationDialog(parent: Shell, filename: String) extends TitleAreaDialog(parent) {
  setShellStyle(getShellStyle() | SWT.RESIZE | SWT.MAX)
  val classLoader = this.getClass().getClassLoader()
  val definition = ScalastyleDefinition.readFromXml(classLoader.getResourceAsStream("/scalastyle_definition.xml"))
  val file = Persistence.findConfiguration(filename)
  val configuration = ScalastyleConfiguration.readFromXml(file.get.getAbsolutePath())
  val model = new Model(definition, configuration)
  val messageHelper = new MessageHelper(classLoader)
  var nameText: Text = _
  var editButton: Button = _
  var newButton: Button = _
  var currentSelection: Option[ModelChecker] = None;
  var tableViewer: TableViewer = _
  val NameSorter = new TableSorter[ModelChecker, String](_.definitionChecker.id, true)
  val SeveritySorter = new TableSorter[ModelChecker, String](_.definitionChecker.level.name, true)
  val ClassSorter = new TableSorter[ModelChecker, String](_.definitionChecker.className, true)
  val ParamsSorter = new TableSorter[ModelChecker, String](_.definitionChecker.parameters.toString, true)
  val CommentSorter = new TableSorter[ModelChecker, String](_.definitionChecker.className, true)

  private[this] val columns = List(
    DialogColumn[ModelChecker]("Enabled", SWT.LEFT, null, 15, { mc => if (mc.configurationChecker.enabled) "true" else "false" }),
    DialogColumn[ModelChecker]("Name", SWT.LEFT, NameSorter, 15, { mc => messageHelper.label(mc.definitionChecker.id) }),
    DialogColumn[ModelChecker]("Severity", SWT.LEFT, SeveritySorter, 15, { mc => messageHelper.text(mc.configurationChecker.level.name) }),
    DialogColumn[ModelChecker]("Params", SWT.LEFT, ParamsSorter, 15, { mc => string(mc.configurationChecker.parameters) }),
    DialogColumn[ModelChecker]("Class", SWT.LEFT, ClassSorter, 15, { mc => mc.definitionChecker.className }),
    DialogColumn[ModelChecker]("Comments", SWT.LEFT, CommentSorter, 15, { mc => "" })
  )

  private[this] def string(map: Map[String, String]): String = map.map(cp => cp._1 + "=" + cp._2).mkString(",")

  override def createDialogArea(parent: Composite): Control = {
    // TODO set width
    val contents = composite(parent, Some(gridData()));

    label(contents, "Name")
    nameText = text(contents, model.configuration.name, true, false)

    val checkerGroup = group(contents, "Checkers")

    tableViewer = table(checkerGroup, model, columns, new ModelContentProvider[ModelChecker](model),
                        new PropertiesLabelProvider(columns), setSelection, refresh)

    editButton = button(contents, "Edit", false, { editChecker(currentSelection) })

    contents
  }

  private[this] def refresh() = {
    println("refresh")
    // TODO check that true, true is ok
    tableViewer.refresh(true, true)
  }

  private[this] def editChecker(modelChecker: Option[ModelChecker]): Unit = {
    println("editChecker")
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
    // TODO validation please
    println("ok pressed")

    if (nameText.getText() == "") {
      println("message please")
    }

    if (nameText.getText() != model.configuration.name || model.dirty) {

      println("writing")
      Persistence.saveConfiguration(file.get.getAbsolutePath(), model.toConfiguration(nameText.getText()))
    }
    super.okPressed();
  }
}
