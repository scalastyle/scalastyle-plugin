package org.scalastyle.scalastyleplugin.preferences;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.widgets._;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout._;
import org.eclipse.swt.events._;
import org.segl.scalastyle._;
import org.scalastyle.scalastyleplugin.SwtUtils._;

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

    resetButton = button(contents, "Reset", true, { reset() })

    contents
  }

  private[this] def reset() = {
    // TODO reset
    println("reset")
  }
  
  override def okPressed(): Unit = {
    // TODO validation please
    val enabled = enabledCheckbox.getSelection()
    val level = Level(severityCombo.getItem(severityCombo.getSelectionIndex()))
    
    val parameters = parameterControls.map({case (name, text) => (name, text.getText())}).toMap
    
    modelChecker.set(level, enabled, parameters)
    
    super.okPressed()
  }
}
