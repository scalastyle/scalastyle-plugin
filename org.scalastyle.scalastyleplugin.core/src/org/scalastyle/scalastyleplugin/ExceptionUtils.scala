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

import org.eclipse.core.runtime.Status
import org.eclipse.core.runtime.IStatus
import org.eclipse.swt.widgets.Shell
import org.eclipse.jface.dialogs.ErrorDialog

object ExceptionUtils {
  private[this] def eclipseLog() = ScalastylePlugin.getDefault().getLog()

  def log(t: Throwable): Unit = log(t.getLocalizedMessage(), t)

  def log(message: String, t: Throwable): Unit = {
    val status = new Status(IStatus.ERROR, ScalastylePlugin.PluginId, IStatus.OK, message, t);
    t.printStackTrace(System.err)
    eclipseLog().log(status);
  }

  def errorDialog(shell: Shell, message: String, t: Throwable) {
    val status = new Status(IStatus.ERROR, ScalastylePlugin.PluginId, IStatus.OK, message, t);

    ErrorDialog.openError(shell, "Error", message, status);
    log(message, t);
  }

  def handleError(shell: Shell)(fn: => Unit): Boolean = {
    try {
      fn
      true
    } catch {
      case e: Exception => errorDialog(shell, e.getLocalizedMessage(), e);
      false
    }
  }

  def handleException(fn: => Unit): Boolean = {
    try {
      fn
      true
    } catch {
      case e: Exception => log(e.getLocalizedMessage(), e);
      false
    }
  }
}
