package org.scalastyle.scalastyleplugin.preferences;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.widgets._;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout._;
import org.eclipse.swt.events._;

class ScalastyleCheckerDialog(parent: Shell, modelChecker: ModelChecker) extends TitleAreaDialog(parent) {
  import ScalastyleUI._;
  setShellStyle(getShellStyle() | SWT.RESIZE);
  var resetButton: Button = _
  var enabledCheckbox: Button = _
  var severityCombo: Combo = _
  var parameterControls = Map[String, Text]()

  override def createDialogArea(parent: Composite): Control = {
    val composite = super.createDialogArea(parent).asInstanceOf[Composite];

    val contents = new Composite(composite, SWT.NULL);
    contents.setLayoutData(new GridData(GridData.FILL_BOTH));
    contents.setLayout(gridLayout(1));

    val allContents = new Composite(contents, SWT.NULL);
    allContents.setLayoutData(new GridData(GridData.FILL_BOTH));
    allContents.setLayout(gridLayout(2));

    // TODO make it look nice
    // TODO add description

    label(allContents, "Id")
    text(allContents, modelChecker.definitionChecker.id, false)

    label(allContents, "Class")
    text(allContents, modelChecker.configurationChecker.className, false)

    label(allContents, "Enabled")
    enabledCheckbox = checkbox(allContents, true)

    label(allContents, "Severity")
    severityCombo = combo(allContents, Array("warning", "error"), modelChecker.configurationChecker.level.name)

    if (modelChecker.configurationChecker.parameters.size > 0) {
      val parameterGroup = group(contents, "Parameters")

      parameterControls = modelChecker.configurationChecker.parameters.map({
        case (name, value) => {
          label(parameterGroup, name)
          (name, text(parameterGroup, value, true))
        }
      })
    }

    resetButton = button(contents, "Reset", true, { reset() })

    contents
  }

  private[this] def reset() = {
    println("reset")
  }
  
  override def okPressed(): Unit = {
    // TODO validation please
    
    // TODO update model here
    println("enabled=" + enabledCheckbox.getSelection())
    println("severity=" + severityCombo.getItem(severityCombo.getSelectionIndex()))
    parameterControls.foreach({case (name, text) => {
       println(name + "=" + text.getText())
    }})

    super.okPressed()
  }
}

object ScalastyleUI {
  def button(parent: Composite, text: String, enabled: Boolean, fn: => Unit) = {
    val button = new Button(parent, SWT.PUSH);
    button.setText(text);
    button.setEnabled(enabled);
    val gd = new GridData();
    button.setLayoutData(gd);

    button.addSelectionListener(new SelectionListener() {
      def widgetSelected(e: SelectionEvent): Unit = fn
      def widgetDefaultSelected(e: SelectionEvent): Unit = {}
    });

    button
  }

  def label(parent: Composite, text: String): Label = {
    val label = new Label(parent, SWT.NULL)

    label.setText(text)
    label.setLayoutData(new GridData())

    label
  }

  def text(parent: Composite, defaultText: String, editable: Boolean): Text = {
    val text = new Text(parent, SWT.LEFT | SWT.SINGLE | SWT.BORDER)

    text.setEditable(editable)
    text.setText(defaultText);
    text.setLayoutData(new GridData(GridData.FILL_BOTH))

    text
  }

  def checkbox(parent: Composite, checked: Boolean): Button = {
    val checkbox = new Button(parent, SWT.CHECK)

    checkbox.setSelection(checked)
    checkbox.setLayoutData(new GridData())

    checkbox
  }

  def combo(parent: Composite, list: Array[String], value: String): Combo = {
    val combo = new Combo(parent, SWT.NONE | SWT.DROP_DOWN | SWT.READ_ONLY);
    combo.setLayoutData(new GridData());
    combo.setItems(list);
    combo.select(list.indexOf(value));

    combo
  }

  def group(parent: Composite, text: String): Group = {
    val group = new Group(parent, SWT.NULL)
    group.setLayout(gridLayout(2))
    group.setLayoutData(new GridData(GridData.FILL_BOTH))
    group.setText(text)
    group
  }

  def gridLayout(columns: Int): GridLayout = {
    val gridLayout = new GridLayout()
    gridLayout.numColumns = columns
    gridLayout
  }
}