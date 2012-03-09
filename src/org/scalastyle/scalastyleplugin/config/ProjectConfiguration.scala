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

case class ProjectConfiguration(file: Option[String])

object ProjectConfigurations {
  import org.scalastyle.scalastyleplugin.StringUtils._
  
  def save(configuration: ProjectConfiguration): Unit = {
    val prefService = Platform.getPreferencesService();
    val prefs = new InstanceScope().getNode(ScalastylePlugin.PLUGIN_ID);

    prefs.put(ScalastylePlugin.PreferenceConfigurationFile, configuration.file.getOrElse(""))
  }

  def get(): ProjectConfiguration = {
    val prefService = Platform.getPreferencesService();
    val prefs = new InstanceScope().getNode(ScalastylePlugin.PLUGIN_ID);

    val configurationFile = prefs.get(ScalastylePlugin.PreferenceConfigurationFile, null)

    ProjectConfiguration(toOption(configurationFile))
  }
}