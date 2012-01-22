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
import org.scalastyle.scalastyleplugin.config.ProjectConfiguration
import org.scalastyle.scalastyleplugin.config.ProjectConfigurations

class ScalastylePreferencePage extends PreferencePage with IWorkbenchPreferencePage {
  /** text field containing the location. */
  var filenameText: Text = _

  /** browse button. */
  var browseButton: Button = _

  setPreferenceStore(ScalastylePlugin.getDefault().getPreferenceStore())

  def createContents(ancestor: Composite): Control = {
    noDefaultAndApplyButton()

    val parentComposite = new Composite(ancestor, SWT.NULL)
    val layout = new FormLayout()
    parentComposite.setLayout(layout)

    val generalComposite = createGeneralContents(parentComposite)
    val fd1 = new FormData()
    fd1.left = new FormAttachment(0)
    fd1.top = new FormAttachment(0)
    fd1.right = new FormAttachment(100)
    generalComposite.setLayoutData(fd1)

    parentComposite
  }

  private[this] def gridLayout(columns: Int, margin: Int = 0): GridLayout = {
    val gridLayout = new GridLayout();
    gridLayout.numColumns = columns;
    gridLayout.marginHeight = margin;
    gridLayout.marginWidth = margin;
    gridLayout
  }

  def compositeGrid(parent: Composite, layout: GridLayout): Composite = {
    val composite = new Composite(parent, SWT.NULL);
    composite.setLayout(layout);
    composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL))
    composite
  }

  def group(parent: Composite, text: String, layout: GridLayout): Group = {
    val group = new Group(parent, SWT.NULL)
    group.setText(text);
    group.setLayout(layout);
    group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL))
    group
  }

  def label(parent: Composite, text: String): Label = {
    val label = new Label(parent, SWT.NULL)
    label.setText(text)
    label
  }

  def text(parent: Composite, textLimit: Int, widthHint: Int, contents: String): Text = {
    val text = new Text(parent, SWT.SINGLE | SWT.BORDER);
    text.setTextLimit(textLimit);

    text.setText(contents);
    val gd = new GridData()
    gd.widthHint = widthHint;
    text.setLayoutData(gd);

    text
  }

  def button(parent: Composite, text: String, fn: => Unit): Button = {
    val button = new Button(parent, SWT.PUSH);
    button.setText(text);
    button.addSelectionListener(new AllListeners(fn));
    button
  }

  class AllListeners(fn: => Unit) extends SelectionListener {
    def widgetSelected(e: SelectionEvent): Unit = fn
    def widgetDefaultSelected(e: SelectionEvent): Unit = {}
  }

  def createGeneralContents(parent: Composite): Control = {
    val generalComposite = group(parent, "General", gridLayout(1, 10))

    val configurationComposite = compositeGrid(generalComposite, gridLayout(3, 10))

    val configurationLabel = label(configurationComposite, "Configuration (full path)")

    val configuration = ProjectConfigurations.get(null)

    filenameText = text(configurationComposite, 500, 300, if (configuration.files.size == 0) "" else configuration.files(0))

    val editButton = button(configurationComposite, "Edit", { editConfiguration(null) })
    generalComposite
  }
  
  private[this] def editConfiguration(config: String) = {
	  val dialog = new ScalastyleConfigurationDialog(getShell(), config, ProjectConfigurations.get(null).files(0));
	  dialog.setBlockOnOpen(true);
	  dialog.open();
  }

  def init(workbench: IWorkbench): Unit = {}

  override def performOk(): Boolean = {
    try {
      val configurationFile = filenameText.getText();
      ProjectConfigurations.save(ProjectConfiguration(null, List(configurationFile)))

      true
    } catch {
      case e: Exception =>
        {
          // TODO log something here
          println("caught exception")
          e.printStackTrace(System.out)
        }
        false
    }
  }
}