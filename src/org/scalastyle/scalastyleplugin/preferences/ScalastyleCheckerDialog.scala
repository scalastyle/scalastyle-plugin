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

package org.scalastyle.scalastyleplugin.preferences;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.widgets._;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout._;
import org.eclipse.swt.events._;
import org.eclipse.jface.dialogs._;
import org.scalastyle._;
import org.scalastyle.scalastyleplugin.SwtUtils._;
import org.scalastyle.scalastyleplugin.StringUtils._;

class ScalastyleCheckerDialog(parent: Shell, messageHelper: MessageHelper, modelChecker: ModelChecker) extends TitleAreaDialog(parent) {
  setShellStyle(getShellStyle() | SWT.RESIZE);
  var resetButton: Button = _
  var enabledCheckbox: Button = _
  var severityCombo: Combo = _
  var parameterControls = Map[String, Text]()

  override def createDialogArea(parent: Composite): Control = {
    val dialogArea = super.createDialogArea(parent).asInstanceOf[Composite];

    val contents = composite(dialogArea, Some(gridData(GridData.FILL_BOTH)), layout = gridLayout(1));

    val allContents = composite(contents, layout = gridLayout(2));

    // TODO make it look nice
    // TODO all text to scalastyle_messages.properties

    val id = modelChecker.definitionChecker.id
    label(allContents, "Id")
    text(allContents, id, false, false)

    label(allContents, "Description")
    text(allContents, messageHelper.description(id), false, false)

    label(allContents, "Class")
    text(allContents, modelChecker.configurationChecker.className, false, false)

    label(allContents, "Enabled")
    enabledCheckbox = checkbox(allContents, modelChecker.configurationChecker.enabled)

    label(allContents, "Severity")
    severityCombo = combo(allContents, Array("warning", "error"), modelChecker.configurationChecker.level.name)

    if (modelChecker.configurationChecker.parameters.size > 0) {
      val parameterGroup = group(contents, "Parameters", layout = gridLayout(2), layoutData = Some(gridData(GridData.FILL_HORIZONTAL)))

      parameterControls = modelChecker.configurationChecker.parameters.map({
        case (name, value) => {
          label(parameterGroup, messageHelper.label(id + "." + name))
          (name, text(parameterGroup, value, true, modelChecker.typeOf(name) == "multistring", messageHelper.description(id + "." + name)))
        }
      })
    }

    contents
  }

  def isNumeric(s: String): Boolean = s.forall(_.isDigit)

  override def okPressed(): Unit = {
    val enabled = enabledCheckbox.getSelection()
    val level = Level(severityCombo.getItem(severityCombo.getSelectionIndex()))

    val errors = parameterControls.flatMap(s => s match {
      case (name, text) => {
        val contents = text.getText()
        val error = if (isEmpty(contents)) {
          Some((name, "must have a value"))
        } else {
          modelChecker.typeOf(name) match {
            case IntegerType => if (!isNumeric(contents)) Some((name, "must be an integer")) else None
            case StringType => None
          }
        }
        error
      }
    })

    if (errors.size > 0) {
      val message = errors.map({ case (name, message) => "Parameter " + name + " " + message }).mkString("\n")
      MessageDialog.open(MessageDialog.ERROR, getShell(), "Scalastyle parameter error", message, SWT.OK)
    } else {
      val parameters = parameterControls.map({ case (name, text) => (name, text.getText()) }).toMap

      modelChecker.set(level, enabled, parameters)

      super.okPressed()
    }
  }
}
