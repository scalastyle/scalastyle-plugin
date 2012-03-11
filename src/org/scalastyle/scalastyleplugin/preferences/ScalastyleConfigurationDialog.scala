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
import org.segl.scalastyle._;
import org.eclipse.jface.viewers._
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.window.Window;
import org.scalastyle.scalastyleplugin.ScalastylePlugin;
import org.scalastyle.scalastyleplugin.SwtUtils._

case class ModelChecker(definitionChecker: DefinitionChecker, _configurationChecker: Option[ConfigurationChecker]) {
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
  
  // TODO pur some tests in here to ensure values are copied correctly etc.
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

case class Model(definition: ScalastyleDefinition, configuration: ScalastyleConfiguration) {
  val list: List[ModelChecker] = definition.checkers.map(dc => ModelChecker(dc, configuration.checks.find(c => c.className == dc.className)))

  def checkers = list
  
  def dirty = list.find(_.dirty).isDefined
  
  def toConfiguration(name: String): ScalastyleConfiguration = {
    val checkers = list.map(mc => ConfigurationChecker(mc.configurationChecker.className, mc.configurationChecker.level, mc.configurationChecker.enabled, mc.configurationChecker.parameters))
    ScalastyleConfiguration(name, checkers)
  }
}

case class DialogColumn(name: String, alignment: Int, sorter: TableSorter[ModelChecker, String], weight: Int, getText: (ModelChecker) => String)

class ScalastyleConfigurationDialog(parent: Shell, file: String) extends TitleAreaDialog(parent) {
  setShellStyle(getShellStyle() | SWT.RESIZE | SWT.MAX)
  val classLoader = this.getClass().getClassLoader()
  val definition = ScalastyleDefinition.readFromXml(classLoader.getResourceAsStream("/scalastyle_definition.xml"))
  val rootFilename = ScalastylePlugin.getWorkspace().getRoot().getLocation().toFile().getAbsolutePath()
  val configuration = ScalastyleConfiguration.readFromXml(rootFilename + file)
  val model = new Model(definition, configuration)
  val messageHelper = new MessageHelper(classLoader)
  var nameText: Text = _
  var editButton: Button = _
  var newButton: Button = _
  var currentSelection: Option[ModelChecker] = None;
  var tableViewer: TableViewer = _

  private[this] val columns = Array(
    DialogColumn("Enabled", SWT.LEFT, null, 15, { mc => if (mc.configurationChecker.enabled) "true" else "false" }),
    DialogColumn("Name", SWT.LEFT, TableSorter.NameSorter, 15, { mc => messageHelper.label(mc.definitionChecker.id) }),
    DialogColumn("Severity", SWT.LEFT, TableSorter.SeveritySorter, 15, { mc => messageHelper.text(mc.configurationChecker.level.name) }),
    DialogColumn("Params", SWT.LEFT, TableSorter.ParamsSorter, 15, { mc => string(mc.configurationChecker.parameters) }),
    DialogColumn("Class", SWT.LEFT, TableSorter.ClassSorter, 15, { mc => mc.definitionChecker.className }),
    DialogColumn("Comments", SWT.LEFT, TableSorter.CommentSorter, 15, { mc => "" })
  )

  private[this] def string(map: Map[String, String]): String = map.map(cp => cp._1 + "=" + cp._2).mkString(",")

  override def createDialogArea(parent: Composite): Control = {
    // TODO set width
    val contents = composite(parent, gridData());

    label(contents, "Name")
    nameText = text(contents, model.configuration.name, true, false)
    
    val checkerGroup = group(contents, "Checkers");

    table(checkerGroup)

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

  private[this] def setSelection(modelChecker: ModelChecker) = currentSelection = Some(modelChecker)

  private[this] def textEditor(table: Table) = {
    val te = new TextCellEditor(table)
    te.getControl().asInstanceOf[Text].setTextLimit(60);
    te
  }
  
  override def okPressed(): Unit = {
    // TODO validation please
    println("ok pressed")

    if (nameText.getText() == "") {
      println("message please")
    }
    
    if (nameText.getText() != model.configuration.name || model.dirty) {
      
      println("writing")
      ConfigurationFile.write(rootFilename + file, model.toConfiguration(nameText.getText()))
    }
    super.okPressed();
  }

  private[this] def table(parent: Composite): Table = {
    val table = new Table(parent, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION)
    table.setLayoutData(new GridData(GridData.FILL_BOTH))
    table.setHeaderVisible(true)
    table.setLinesVisible(true)

    val tableLayout = new TableLayout()
    table.setLayout(tableLayout)
    // TODO don't assign this here
    tableViewer = new TableViewer(table);

    val editors: Array[CellEditor] = columns.map(c => {
      column(table, tableViewer, c.name, c.alignment, c.sorter, refresh)
      tableLayout.addColumnData(new ColumnWeightData(c.weight))
      textEditor(table)
    });

    val f = new PropertiesLabelProvider(tableViewer, messageHelper, columns)
    tableViewer.setLabelProvider(f)
    tableViewer.setContentProvider(new ArrayContentProvider())
    tableViewer.setInput(model)
    tableViewer.setContentProvider(new ModelContentProvider(model))

    table.addListener(SWT.Selection, new Listener() {
      def handleEvent(event: Event) = {
        val ss: StructuredSelection = tableViewer.getSelection().asInstanceOf[StructuredSelection];
        editButton.setEnabled(ss != null);
        setSelection(ss.getFirstElement().asInstanceOf[ModelChecker])
      }
    });
    
    table

  }

}

class ModelContentProvider(model: Model) extends IStructuredContentProvider {
  def inputChanged(v: Viewer, oldInput: java.lang.Object, newInput: java.lang.Object): Unit = {}
  def dispose(): Unit = {}
  def getElements(parent: Object): Array[java.lang.Object] = Array[java.lang.Object](model.checkers: _*)
}

class PropertiesLabelProvider(tableViewer: TableViewer, messageHelper: MessageHelper, columns: Array[DialogColumn]) extends LabelProvider with ITableLabelProvider {
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
