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