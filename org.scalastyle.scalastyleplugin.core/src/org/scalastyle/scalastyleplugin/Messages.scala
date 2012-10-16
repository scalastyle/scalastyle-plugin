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

import org.eclipse.osgi.util.NLS;

class Messages

object Messages extends NLS {
  private val BUNDLE_NAME = "org.scalastyle.scalastyleplugin"

  NLS.initializeMessages(BUNDLE_NAME, classOf[Messages])

  val buildAllProjects = "Building all projects"
  val buildSingleProject = "Building one project"

  val addNatureToProjectJob = "Adding Scalastyle nature"
  val addNatureToProjectsJob = "Adding Scalastyle nature to project(s)"

  val removeNatureFromProjectJob = "Removing Scalastyle nature"
  val removeNatureFromProjectsJob = "Removing Scalastyle nature from project(s)"
}
