package org.scalastyle.scalastyleplugin.preferences

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers._;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.jface.viewers.IStructuredContentProvider
import org.segl.scalastyle._
import org.eclipse.jface.viewers._

case class SSTableColumn()

case class ModelChecker(definitionChecker: DefinitionChecker, _configurationChecker: Option[ConfigurationChecker]) {
  def enabled = _configurationChecker.isDefined
  val configurationChecker = foo()
  
  def foo() = {
	  var configurationChecker = _configurationChecker.getOrElse(definitionToConfiguration(definitionChecker))
    
	  val parameters = definitionChecker.parameters.map(dpm => {
	      val p = configurationChecker.parameters.get(dpm._1)
	      val value = if (p.isEmpty) dpm._2.defaultValue else p.get
	      (dpm._1 -> value)
	    });
	configurationChecker.copy(parameters = parameters)    
  }

  def definitionToConfiguration(checker: DefinitionChecker): ConfigurationChecker = {
    ConfigurationChecker(checker.className, checker.level, checker.parameters.map(m => (m._1, m._2.defaultValue)).toMap)
  }
}

case class Model(definition: ScalastyleDefinition, configuration: ScalastyleConfiguration) {
  val list: List[ModelChecker] = definition.checkers.map(dc => ModelChecker(dc, configuration.checks.find(c => c.className == dc.className)))
  
  def checkers() = list
}

case class DialogColumn(name: String, alignment: Int, sorter: TableSorter[ModelChecker, String], weight: Int, getText: (ModelChecker) => String)

class ScalastyleConfigurationDialog(parent: Shell, config: String, file: String) extends TitleAreaDialog(parent) {
  setShellStyle(getShellStyle() | SWT.RESIZE | SWT.MAX)
  val classLoader = this.getClass().getClassLoader()
  val definition = ScalastyleDefinition.readFromXml(classLoader.getResourceAsStream("/scalastyle_definition.xml"))
  val configuration = ScalastyleConfiguration.readFromXml(file)
  val model = new Model(definition, configuration)
  val messageHelper = new MessageHelper(classLoader)
  // TODO fix sort
  val columns = Array(DialogColumn("Enabled", SWT.LEFT, null, 15, { mc => if (mc.enabled) "true" else "false" }),
      					DialogColumn("Name", SWT.LEFT, TableSorter.NameSorter, 15, { mc => messageHelper.name(mc.definitionChecker.id)}),
      					DialogColumn("Severity", SWT.LEFT, TableSorter.SeveritySorter, 15, {mc => messageHelper.text(mc.configurationChecker.level.name)}),
      					DialogColumn("Params", SWT.LEFT, TableSorter.ParamsSorter, 15, {mc => string(mc.configurationChecker.parameters)}),
      					DialogColumn("Class", SWT.LEFT, TableSorter.ClassSorter, 15, {mc => mc.definitionChecker.className}),
      					DialogColumn("Comments", SWT.LEFT, TableSorter.CommentSorter, 15, {mc => ""})
      					)

  def string(map: Map[String, String]): String = map.map(cp => cp._1 + "=" + cp._2).mkString(",")

  override def createDialogArea(parent: Composite): Control = {
    val composite = super.createDialogArea(parent).asInstanceOf[Composite];

    val contents = new Composite(composite, SWT.NULL);
    contents.setLayoutData(new GridData(GridData.FILL_BOTH));
    contents.setLayout(new GridLayout());

    val tableControl = configTable(contents, configuration);
    tableControl.setLayoutData(new GridData(GridData.FILL_BOTH));

    contents
  }

  private[this] def textEditor(table: Table) = {
    val te = new TextCellEditor(table)
    te.getControl().asInstanceOf[Text].setTextLimit(60);
    te
  }
  
  private[this] def configTable(parent: Composite, configuration: ScalastyleConfiguration) = {
    val group = new Group(parent, SWT.NULL)
    group.setLayout(new GridLayout())
    group.setText("Checkers")
    // TODO set width

    val table = new Table(group, SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION)
    table.setLayoutData(new GridData(GridData.FILL_BOTH))
    table.setHeaderVisible(true)
    table.setLinesVisible(true)

    val tableLayout = new TableLayout()
    table.setLayout(tableLayout)
    val tableViewer = new TableViewer(table);
    
    val editors: Array[CellEditor] = columns.map( c => {
      column(table, tableViewer, c.name, c.alignment, c.sorter)
      tableLayout.addColumnData(new ColumnWeightData(c.weight))
      textEditor(table)
    });
    
    val f = new PropertiesLabelProvider(tableViewer, messageHelper, configuration, columns)
    tableViewer.setLabelProvider(f)
    tableViewer.setContentProvider(new ArrayContentProvider())
    tableViewer.setInput(model)
    tableViewer.setContentProvider(new ModelContentProvider(model))

    group
  }

  private[this] def column(table: Table, tableViewer: TableViewer, text: String, alignment: Int, tableSorter: TableSorter[ModelChecker, String]): TableColumn = {
    val column = new TableColumn(table, SWT.NULL)
    column.setAlignment(alignment)
    column.setText(text)

    column.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
      override def widgetSelected(e: SelectionEvent): Unit = {
        if (tableSorter != null) {
          tableViewer.setSorter(tableSorter.flip())
          // TODO check that true, true is ok
          tableViewer.refresh(true, true)
        }
      }
    })

    column
  }
}

class ModelContentProvider(model: Model) extends IStructuredContentProvider {
  def inputChanged(v: Viewer, oldInput: java.lang.Object, newInput: java.lang.Object): Unit = {}
  def dispose(): Unit = {}
  def getElements(parent: Object): Array[java.lang.Object] = Array[java.lang.Object](model.checkers: _*)
}

class PropertiesLabelProvider(tableViewer: TableViewer, messageHelper: MessageHelper, configuration: ScalastyleConfiguration, columns: Array[DialogColumn]) extends LabelProvider with ITableLabelProvider {
  def getColumnImage(element: Any, columnIndex: Int): Image = null

  def getColumnText(element: java.lang.Object, columnIndex: Int): String = {
    var modelChecker = element.asInstanceOf[ModelChecker]

    if (columnIndex >= 0 && columnIndex < columns.length) {
      columns(columnIndex).getText(modelChecker)
    } else {
      ""
    }
  }
}

object TableSorter {
  val NameSorter = new TableSorter[ModelChecker, String](_.definitionChecker.id, true)
  val SeveritySorter = new TableSorter[ModelChecker, String](_.definitionChecker.level.name, true)
  val ClassSorter = new TableSorter[ModelChecker, String](_.definitionChecker.className, true)
  val ParamsSorter = new TableSorter[ModelChecker, String](_.definitionChecker.parameters.toString, true)
  val CommentSorter = new TableSorter[ModelChecker, String](_.definitionChecker.className, true)
}

class TableSorter[T, B <: java.lang.Comparable[B]](fn: (T) => B, var asc: Boolean) extends ViewerSorter {
  def flip(): this.type = {
    println("asc=" + asc)
    asc = !asc
    this
  }
  
  override def compare(viewer: Viewer, o1: java.lang.Object, o2: java.lang.Object): Int = {
    val f = fn(o1.asInstanceOf[T]).compareTo(fn(o2.asInstanceOf[T])) * (if (asc) 1 else -1)
    println("f=" + f + " asc1=" + asc)
    f
  }
}
