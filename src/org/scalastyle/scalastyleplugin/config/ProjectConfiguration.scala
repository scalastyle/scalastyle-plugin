package org.scalastyle.scalastyleplugin.config

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.osgi.util.NLS;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.core.runtime.preferences.InstanceScope;

import org.eclipse.core.resources.IProject;
import org.scalastyle.scalastyleplugin.ScalastylePlugin

case class ProjectConfiguration(project: IProject, files: List[String])

object ProjectConfigurations {
    val PROJECT_CONFIGURATION_FILE = ".scalastyle";

    val CURRENT_FILE_FORMAT_VERSION = "1";

    def configuration(project: IProject): ProjectConfiguration = {
      // for the minute, we'll just use a global configuration
      val prefService = Platform.getPreferencesService();
      val prefs = new InstanceScope().getNode(ScalastylePlugin.PLUGIN_ID);

      val configurationFile = prefs.get(ScalastylePlugin.PreferenceConfigurationFile, null)
      
      val list = if (configurationFile == null) List[String]() else List(configurationFile)
      ProjectConfiguration(project, list)
    }
    
//    def configuration(project: IProject) = {
//        val file = project.getFile(PROJECT_CONFIGURATION_FILE);
//        if (!file.exists()) {
//        	// create one
//          // TODO NEXT: make this relative to the project, maybe even set config correctly in a window?
//          // relative to project, relative to workspace, standard config in the jar
//          // create project config based upon standard config(s)
//          // do we need a overall configuration thing for scalastyle, which describes what can be done?
//          // TODO do we save this?, probably not yet
//          ProjectConfiguration(project, List("c:/code/scalastyle/scalastyle/src/main/resources/scalastyle_config.xml"));
//        } else {
//          val elem = scala.xml.XML.loadFile(file.getLocation().toFile())
//          
//          ProjectConfiguration(project, (elem \\ "files" \\ "file").map(_.text).toList)
//        }
//    }
}