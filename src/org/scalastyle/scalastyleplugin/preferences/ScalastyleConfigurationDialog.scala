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

case class ModelChecker(definitionChecker: DefinitionChecker, configurationChecker: Option[ConfigurationChecker])
case class Model(definition: ScalastyleDefinition, configuration: ScalastyleConfiguration) {
  val list: List[ModelChecker] = definition.checkers.map(dc => ModelChecker(dc, configuration.checks.find(c => c.className == dc.className)))
  
  def checkers() = list
}

class ScalastyleConfigurationDialog(parent: Shell, config: String, file: String) extends TitleAreaDialog(parent) {
  setShellStyle(getShellStyle() | SWT.RESIZE | SWT.MAX)
  val classLoader = this.getClass().getClassLoader()
  val definition = ScalastyleDefinition.readFromXml(classLoader.getResourceAsStream("/scalastyle_definition.xml"))
  val configuration = ScalastyleConfiguration.readFromXml(file)
  val model = new Model(definition, configuration)
  val messageHelper = new MessageHelper(classLoader)
  val columns = Array()

  override def createDialogArea(parent: Composite): Control = {
    val composite = super.createDialogArea(parent).asInstanceOf[Composite];

    val contents = new Composite(composite, SWT.NULL);
    contents.setLayoutData(new GridData(GridData.FILL_BOTH));
    contents.setLayout(new GridLayout());

    val tableControl = configTable(contents, configuration);
    tableControl.setLayoutData(new GridData(GridData.FILL_BOTH));

    contents
  }

  private[this] def textEditor(table: Table) = new TextCellEditor(table, SWT.READ_ONLY)
  private[this] def checkboxEditor(table: Table) = new CheckboxCellEditor(table)

  private[this] def configTable(parent: Composite, configuration: ScalastyleConfiguration) = {
    val group = new Group(parent, SWT.NULL)
    group.setLayout(new GridLayout())
    group.setText("Checkers")
    // TODO set width

    val table = new Table(group, SWT.CHECK | SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION)
    table.setLayoutData(new GridData(GridData.FILL_BOTH))
    table.setHeaderVisible(true)
    table.setLinesVisible(true)

    val tableLayout = new TableLayout()
    table.setLayout(tableLayout)
    val tableViewer = new TableViewer(table);

    // Create the cell editors
    val editors = Array[CellEditor](checkboxEditor(table), textEditor(table), textEditor(table), textEditor(table), textEditor(table))

    val column1 = new TableColumn(table, SWT.NULL)
    column1.setAlignment(SWT.CENTER)
    column1.setText("Enabled")
    tableLayout.addColumnData(new ColumnWeightData(10))

    column(table, tableViewer, "Name", SWT.LEFT, TableSorter.NameSorter)
    tableLayout.addColumnData(new ColumnWeightData(30))

    column(table, tableViewer, "Severity", SWT.LEFT, TableSorter.LevelSorter)
    tableLayout.addColumnData(new ColumnWeightData(15))

    column(table, tableViewer, "Params", SWT.LEFT, TableSorter.ParamsSorter)
    tableLayout.addColumnData(new ColumnWeightData(30))

    column(table, tableViewer, "Class", SWT.LEFT, TableSorter.ClassNameSorter)
    tableLayout.addColumnData(new ColumnWeightData(30))

    column(table, tableViewer, "Comment", SWT.LEFT, TableSorter.CommentSorter)
    tableLayout.addColumnData(new ColumnWeightData(30))

    val f = new PropertiesLabelProvider(tableViewer, messageHelper, configuration)
    tableViewer.setLabelProvider(f)
    tableViewer.setContentProvider(new ArrayContentProvider())
    tableViewer.setInput(model)
    tableViewer.setContentProvider(new ModelContentProvider(model))

    group
  }

  private[this] def column(table: Table, tableViewer: TableViewer, text: String, alignment: Int, tableSorter: ViewerSorter): TableColumn = {
    val column = new TableColumn(table, SWT.NULL)
    column.setAlignment(alignment)
    column.setText(text)

    column.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
      override def widgetSelected(e: SelectionEvent): Unit = tableViewer.setSorter(tableSorter)
    })

    column
  }
}

class ModelContentProvider(model: Model) extends IStructuredContentProvider {
  def inputChanged(v: Viewer, oldInput: java.lang.Object, newInput: java.lang.Object): Unit = {}
  def dispose(): Unit = {}
  def getElements(parent: Object): Array[java.lang.Object] = Array[java.lang.Object](model.checkers: _*)
}

class PropertiesLabelProvider(tableViewer: TableViewer, messageHelper: MessageHelper, configuration: ScalastyleConfiguration) extends LabelProvider with ITableLabelProvider with org.eclipse.jface.viewers.ICellModifier {
  def canModify(element: java.lang.Object, property: String): Boolean = { println("modify"); true }

  def modify(element: java.lang.Object, arg1: String, arg2: java.lang.Object): Unit = {
    println("modify")
  }

  def getValue(element: Object, property: String): java.lang.Object = {
    println("element=" + element + " property=" + property)
    "foo"
  }

  override def getText(element: java.lang.Object): String = element.toString()

  def getColumnImage(element: Any, columnIndex: Int): Image = null

  def getColumnText(element: java.lang.Object, columnIndex: Int): String = {
    var modelChecker = element.asInstanceOf[ModelChecker]

    var configurationChecker = modelChecker.configurationChecker.getOrElse(definitionToConfiguration(modelChecker.definitionChecker))
    
    val parameters = modelChecker.definitionChecker.parameters.map(dpm => {
      val p = configurationChecker.parameters.get(dpm._1)
      val value = if (p.isEmpty) dpm._2.defaultValue else p.get
      (dpm._1 -> value)
    });
    
    configurationChecker = configurationChecker.copy(parameters = parameters)
    
    // TODO sorting of columns
    // TODO save the xml please
    columnIndex match {
      case 0 => if (configuration.checks.exists(c => (c.className == modelChecker.definitionChecker.className))) "true" else "false"
      case 1 => messageHelper.name(modelChecker.definitionChecker.id)
      case 2 => messageHelper.text(configurationChecker.level.name)
      case 3 => string(configurationChecker.parameters)
      case 4 => modelChecker.definitionChecker.className
      case _ => ""
    }
  }
  
  def definitionToConfiguration(checker: DefinitionChecker): ConfigurationChecker = {
    ConfigurationChecker(checker.className, checker.level, checker.parameters.map(m => (m._1, m._2.defaultValue)).toMap)
  }

  def string(map: Map[String, String]): String = map.map(cp => cp._1 + "=" + cp._2).mkString(",")
  override def getImage(element: Any): Image = null
}

object TableSorter {
  val NameSorter = TableSorter[DefinitionChecker, String](_.id)
  val LevelSorter = TableSorter[DefinitionChecker, String](_.level.name)
  val ClassNameSorter = TableSorter[DefinitionChecker, String](_.className)
  val ParamsSorter = TableSorter[DefinitionChecker, String](_.parameters.toString)
  val CommentSorter = TableSorter[DefinitionChecker, String](_.className)
}

case class TableSorter[T, B <: java.lang.Comparable[B]](fn: (T) => B) extends ViewerSorter {
  // TODO including up/down
  override def compare(viewer: Viewer, o1: java.lang.Object, o2: java.lang.Object): Int = {
    fn(o1.asInstanceOf[T]).compareTo(fn(o2.asInstanceOf[T]))
  }
}
