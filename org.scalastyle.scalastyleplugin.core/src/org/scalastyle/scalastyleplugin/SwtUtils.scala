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

package org.scalastyle.scalastyleplugin

import org.eclipse.jface.viewers.ArrayContentProvider
import org.eclipse.jface.viewers.CellEditor
import org.eclipse.jface.viewers.ColumnWeightData
import org.eclipse.jface.viewers.IStructuredContentProvider
import org.eclipse.jface.viewers.ITableLabelProvider
import org.eclipse.jface.viewers.LabelProvider
import org.eclipse.jface.viewers.StructuredSelection
import org.eclipse.jface.viewers.TableLayout
import org.eclipse.jface.viewers.TableViewer
import org.eclipse.jface.viewers.TextCellEditor
import org.eclipse.jface.viewers.Viewer
import org.eclipse.jface.viewers.ViewerSorter
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.swt.events.SelectionListener
import org.eclipse.swt.graphics.Image
import org.eclipse.swt.layout.FormAttachment
import org.eclipse.swt.layout.FormData
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.Button
import org.eclipse.swt.widgets.Combo
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Event
import org.eclipse.swt.widgets.Group
import org.eclipse.swt.widgets.Label
import org.eclipse.swt.widgets.Layout
import org.eclipse.swt.widgets.Listener
import org.eclipse.swt.widgets.Table
import org.eclipse.swt.widgets.TableColumn
import org.eclipse.swt.widgets.Text
import org.eclipse.swt.SWT
import org.eclipse.jface.viewers.IDoubleClickListener
import org.eclipse.jface.viewers.DoubleClickEvent

// scalastyle:off magic.number

object SwtUtils {
  def composite(parent: Composite, layoutData: Option[Any] = Some(defaultGridData), layout: Layout = defaultGridLayout): Composite = {
    val composite = new Composite(parent, SWT.NULL);
    if (layoutData.isDefined) {
      composite.setLayoutData(layoutData.get)
    }

    composite.setLayout(layout);

    composite
  }

  private[this] val defaultGridData = gridData()
  private[this] val defaultGridLayout = gridLayout(1)

  def gridData(style: Int = GridData.FILL_BOTH): GridData = new GridData(style)

  def gridLayout(columns: Int = 1, margin: Int = 5): GridLayout = {
    val gridLayout = new GridLayout()
    gridLayout.numColumns = columns
    gridLayout.marginHeight = margin;
    gridLayout.marginWidth = margin;
    gridLayout
  }

  def group(parent: Composite, label: String, layoutData: Option[Any] = Some(defaultGridData), layout: Layout = defaultGridLayout): Group = {
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

  def formData(): FormData = {
    val fd = new FormData()
    fd.left = new FormAttachment(0)
    fd.top = new FormAttachment(0)
    fd.right = new FormAttachment(100)

    fd
  }

  case class DialogColumn[T](name: String, alignment: Int, sorter: TableSorter[T, String], weight: Int, getText: (T) => String)

  def table[T](parent: Composite, model: Any, columns: List[DialogColumn[T]], contentProvider: IStructuredContentProvider,
                  labelProvider: ITableLabelProvider, setSelection: (T) => Unit, refresh: => Unit, dblClick: => Unit,
                  layoutData: Any = new GridData(GridData.FILL_BOTH)): TableViewer = {
    val table = new Table(parent, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION)
    table.setLayoutData(layoutData)
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
      def handleEvent(event: Event): Unit = {
        val ss: StructuredSelection = tableViewer.getSelection().asInstanceOf[StructuredSelection];
        setSelection(ss.getFirstElement().asInstanceOf[T])
      }
    });

    tableViewer.addDoubleClickListener(new IDoubleClickListener {
      override def doubleClick(event: DoubleClickEvent): Unit = {
        dblClick
      }
    })
    tableViewer
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
