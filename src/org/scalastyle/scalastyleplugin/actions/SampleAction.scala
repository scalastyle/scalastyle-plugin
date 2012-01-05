package org.scalastyle.scalastyleplugin.actions

import org.eclipse.jface.action.IAction
import org.eclipse.jface.viewers.ISelection
import org.eclipse.ui.IWorkbenchWindow
import org.eclipse.ui.IWorkbenchWindowActionDelegate
import org.eclipse.jface.dialogs.MessageDialog

class SampleAction extends IWorkbenchWindowActionDelegate {
  private[this] var window: IWorkbenchWindow = _;

  def run(action: IAction): Unit = {
    MessageDialog.openInformation(
      window.getShell(),
      "Scalastyle-plugin",
      "Hello, Eclipse world");
  }

  def selectionChanged(action: IAction, selection: ISelection): Unit = {}
  def dispose() = {}

  def init(window: IWorkbenchWindow) = this.window = window
}