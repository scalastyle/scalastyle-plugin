package org.scalastyle.scalastyleplugin.preferences

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
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
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.osgi.service.prefs.BackingStoreException;

import org.scalastyle.scalastyleplugin.ScalastylePlugin

class ScalastylePreferencePage extends PreferencePage with IWorkbenchPreferencePage {
  /** text field containing the location. */
  var filenameText: Text = _;

  /** browse button. */
  var browseButton: Button = _;

  setPreferenceStore(ScalastylePlugin.getDefault().getPreferenceStore());

  /**
   * {@inheritDoc}
   */
  def createContents(ancestor: Composite): Control = {
    noDefaultAndApplyButton();

    val parentComposite = new Composite(ancestor, SWT.NULL);
    val layout = new FormLayout();
    parentComposite.setLayout(layout);

    val generalComposite = createGeneralContents(parentComposite);
    val fd1 = new FormData();
    fd1.left = new FormAttachment(0);
    fd1.top = new FormAttachment(0);
    fd1.right = new FormAttachment(100);
    generalComposite.setLayoutData(fd1);

    return parentComposite;
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

  def createGeneralContents(parent: Composite): Control = {
    val generalComposite = group(parent, "General", gridLayout(1, 10))

    val prefs = Platform.getPreferencesService();

    val configurationComposite = compositeGrid(generalComposite, gridLayout(2, 10))

    val configurationLabel = label(configurationComposite, "Configuration (full path)")

    filenameText = text(configurationComposite, 500, 300, "foobar")

    return generalComposite;
  }

  def init(workbench: IWorkbench): Unit = {}

  override def performOk(): Boolean = {
    try {
      val prefService = Platform.getPreferencesService();
      val prefs = new InstanceScope().getNode(ScalastylePlugin.PLUGIN_ID);

      val configurationFile = filenameText.getText();
      prefs.put(ScalastylePlugin.PreferenceConfigurationFile, configurationFile);

      true
    } catch {
      case e: Exception => {
        // TODO log something here
        println("caught exception")
        e.printStackTrace(System.out)
      }
      false
    }
  }
}