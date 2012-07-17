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

package org.scalastyle.scalastyleplugin.actions;

import org.eclipse.core.resources.IProject
import org.eclipse.core.resources.WorkspaceJob
import org.eclipse.core.runtime.CoreException
import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.core.runtime.IStatus
import org.eclipse.core.runtime.Status
import org.eclipse.jface.action.IAction
import org.eclipse.jface.viewers.ISelection
import org.eclipse.jface.viewers.IStructuredSelection
import org.eclipse.jface.dialogs.MessageDialog
import org.eclipse.ui.IObjectActionDelegate
import org.eclipse.ui.IWorkbenchPart
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.scalastyle.scalastyleplugin.Messages;
import org.scalastyle.scalastyleplugin.nature.ScalastyleNature;

abstract class ScalastyleNatureAction extends IObjectActionDelegate {
  private var part: IWorkbenchPart = _
  private var selectedProjects: Array[IProject] = _
  protected def job(projects: Array[IProject]): WorkspaceJob

  def setActivePart(action: IAction, targetPart: IWorkbenchPart) { part = targetPart }

  def selectionChanged(action: IAction, selection: ISelection) {
    selection match {
      case s: IStructuredSelection => selectedProjects = s.toArray().map(_.asInstanceOf[IProject])
      case _ =>
    }
  }

  def run(action: IAction) { job(selectedProjects).schedule() }
}

// TODO when removing, the markers disappear. When adding, the project needs to get rebuilt. Ask?
class AddScalastyleNatureAction extends ScalastyleNatureAction {
  def job(projects: Array[IProject]): WorkspaceJob = new AddNatureJobs(projects)

  class AddNatureJobs(projects: Array[IProject]) extends WorkspaceJob(Messages.addNatureToProjectsJob) {
    def runInWorkspace(monitor: IProgressMonitor): IStatus = {

      projects.foreach(project => {
        if (project.isOpen() && !project.hasNature(ScalastyleNature.NatureId)) {
          new AddNatureJob(project, ScalastyleNature.NatureId).schedule()
        }
      });

      Status.OK_STATUS;
    }
  }

  class AddNatureJob(project: IProject, natureId: String) extends AddRemoveNatureJob(natureId, Messages.addNatureToProjectJob) {
    def runInWorkspace(monitor: IProgressMonitor): IStatus = {
      runInWorkspace(monitor, {
        val desc = project.getDescription()
        desc.setNatureIds(desc.getNatureIds() ++ Array(natureId))
        project.setDescription(desc, monitor)
      })
    }
  }
}

class RemoveScalastyleNatureAction extends ScalastyleNatureAction {
  def job(projects: Array[IProject]): WorkspaceJob = new RemoveNatureJobs(projects)

  class RemoveNatureJobs(projects: Array[IProject]) extends WorkspaceJob(Messages.removeNatureFromProjectsJob) {
    def runInWorkspace(monitor: IProgressMonitor): IStatus = {
      projects.foreach(project => {
        if (project.isOpen() && project.hasNature(ScalastyleNature.NatureId)) {
          new RemoveNatureJob(project, ScalastyleNature.NatureId).schedule()
        }
      });

      Status.OK_STATUS;
    }
  }

  class RemoveNatureJob(project: IProject, natureId: String) extends AddRemoveNatureJob(natureId, Messages.removeNatureFromProjectJob) {
    def runInWorkspace(monitor: IProgressMonitor): IStatus = {
      runInWorkspace(monitor, {
        val desc = project.getDescription()
        desc.setNatureIds(desc.getNatureIds().filter(_ != natureId))
        project.setDescription(desc, monitor)
      })
    }
  }
}

abstract class AddRemoveNatureJob(natureId: String, message: String) extends WorkspaceJob(NLS.bind(message, natureId)) {
  private var monitor: IProgressMonitor = _;

  protected def runInWorkspace(monitor: IProgressMonitor, fn: => Unit): IStatus = {
    var status: IStatus = null

    this.monitor = monitor

    try {
      fn;
      status = Status.OK_STATUS
    } finally {
      monitor.done()
    }

    status
  }
}
