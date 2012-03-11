package org.scalastyle.scalastyleplugin

import org.eclipse.jface.dialogs._;
import org.eclipse.jface.viewers._;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.events._;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout._;
import org.eclipse.swt.widgets.{ List => _, _ }
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers._
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.window.Window;

object SwtUtils {
  def composite(parent: Composite, layoutData: Option[Any] = Some(defaultGridData), layout: Layout = defaultGridLayout) = {
    val composite = new Composite(parent, SWT.NULL);
    if (layoutData.isDefined) {
      composite.setLayoutData(layoutData.get)
    }

    composite.setLayout(layout);

    composite
  }

  private[this] val defaultGridData = gridData()
  private[this] val defaultGridLayout = gridLayout(1)

  def gridData(style: Int = GridData.FILL_BOTH) = new GridData(style)

  def gridLayout(columns: Int = 1, margin: Int = 5): GridLayout = {
    val gridLayout = new GridLayout()
    gridLayout.numColumns = columns
    gridLayout.marginHeight = margin;
    gridLayout.marginWidth = margin;
    gridLayout
  }

  def group(parent: Composite, label: String, layoutData: Option[Any] = Some(defaultGridData), layout: Layout = defaultGridLayout) = {
    val group = new Group(parent, SWT.NULL)

    if (layoutData.isDefined) {
      group.setLayoutData(layoutData.get)
    }

    group.setLayout(layout)
    group.setText(label)

    group
  }

  def column(table: Table, tableViewer: TableViewer, text: String, alignment: Int, tableSorter: TableSorter[Any, String], refresh: => Unit): TableColumn = {
    val column = new TableColumn(table, SWT.NULL)
    column.setAlignment(alignment)
    column.setText(text)

    column.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
      override def widgetSelected(e: SelectionEvent): Unit = {
        if (tableSorter != null) {
          tableViewer.setSorter(tableSorter.flip())
          refresh;
        }
      }
    })

    column
  }

  class TableSorter[+T, B <: java.lang.Comparable[B]](fn: (T) => B, var asc: Boolean) extends ViewerSorter {
    var mult = if (asc) 1 else -1
    def flip(): this.type = {
      asc = !asc
      mult = mult * -1
      this
    }

    override def compare(viewer: Viewer, o1: java.lang.Object, o2: java.lang.Object): Int = {
      fn(o1.asInstanceOf[T]).compareTo(fn(o2.asInstanceOf[T])) * mult
    }
  }

  def button(parent: Composite, text: String, enabled: Boolean, fn: => Unit): Button = {
    val button = new Button(parent, SWT.PUSH);
    button.setText(text);
    button.setEnabled(enabled);
    button.setLayoutData(new GridData())

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

  def text(parent: Composite, defaultText: String, editable: Boolean, multiLine: Boolean, tooltip: String = null): Text = {
    val text = new Text(parent, SWT.LEFT | (if (multiLine) SWT.MULTI else SWT.SINGLE) | SWT.BORDER)

    text.setEditable(editable)
    text.setText(defaultText);

    val gridData =
      new GridData(
        GridData.FILL_BOTH | GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_FILL);

    if (multiLine) {
      gridData.heightHint = 200;
    }

    gridData.grabExcessVerticalSpace = true;

    text.setLayoutData(gridData);
    text.setToolTipText(tooltip);

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

  def formData() = {
    val fd = new FormData()
    fd.left = new FormAttachment(0)
    fd.top = new FormAttachment(0)
    fd.right = new FormAttachment(100)

    fd
  }

  case class DialogColumn[T](name: String, alignment: Int, sorter: TableSorter[T, String], weight: Int, getText: (T) => String)

  def table[T](parent: Composite, model: Any, columns: List[DialogColumn[T]], contentProvider: IStructuredContentProvider, labelProvider: ITableLabelProvider, setSelection: (T) => Unit, refresh: => Unit): Table = {
    val table = new Table(parent, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION)
    table.setLayoutData(new GridData(GridData.FILL_BOTH))
    table.setHeaderVisible(true)
    table.setLinesVisible(true)

    val tableLayout = new TableLayout()
    table.setLayout(tableLayout)
    // TODO don't assign this here
    val tableViewer = new TableViewer(table);

    val editors: Array[CellEditor] = columns.map(c => {
      column(table, tableViewer, c.name, c.alignment, c.sorter, refresh)
      tableLayout.addColumnData(new ColumnWeightData(c.weight))
      textEditor(table)
    }).toArray;

    //    val f = new PropertiesLabelProvider(messageHelper, columns)
    tableViewer.setLabelProvider(labelProvider)
    tableViewer.setContentProvider(new ArrayContentProvider())
    tableViewer.setInput(model)
    tableViewer.setContentProvider(contentProvider)

    table.addListener(SWT.Selection, new Listener() {
      def handleEvent(event: Event) = {
        val ss: StructuredSelection = tableViewer.getSelection().asInstanceOf[StructuredSelection];
        setSelection(ss.getFirstElement().asInstanceOf[T])
      }
    });

    table
  }

  private[this] def textEditor(table: Table) = {
    val te = new TextCellEditor(table)
    te.getControl().asInstanceOf[Text].setTextLimit(60);
    te
  }

  trait TableLine

  trait Container[+T <: TableLine] {
    def elements(): List[T]
  }

  class ModelContentProvider[T <: TableLine](model: Container[T]) extends IStructuredContentProvider {
    def inputChanged(v: Viewer, oldInput: java.lang.Object, newInput: java.lang.Object): Unit = {}
    def dispose(): Unit = {}
    def getElements(parent: Object): Array[java.lang.Object] = Array[java.lang.Object](model.elements: _*)
  }

  class PropertiesLabelProvider[T <: TableLine](columns: List[DialogColumn[T]]) extends LabelProvider with ITableLabelProvider {
    def getColumnImage(element: Any, columnIndex: Int): Image = null

    def getColumnText(element: java.lang.Object, columnIndex: Int): String = {
      var line = element.asInstanceOf[T]

      if (columnIndex >= 0 && columnIndex < columns.length) {
        columns(columnIndex).getText(line)
      } else {
        ""
      }
    }
  }

}