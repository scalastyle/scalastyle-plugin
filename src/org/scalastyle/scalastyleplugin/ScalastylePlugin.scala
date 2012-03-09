package org.scalastyle.scalastyleplugin

import org.eclipse.jface.resource.ImageDescriptor
import org.eclipse.ui.plugin.AbstractUIPlugin
import org.osgi.framework.BundleContext
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.IWorkspace;

object ScalastylePlugin {
  val PLUGIN_ID = "scalastyle-plugin" //$NON-NLS-1$
  val PreferenceConfigurationFile = "scalastyle.preferenceConfigurationFile"

  private var plugin: ScalastylePlugin = _

  def getDefault(): ScalastylePlugin = plugin

  def getImageDescriptor(path: String): ImageDescriptor = AbstractUIPlugin.imageDescriptorFromPlugin(PLUGIN_ID, path)
  
  def getWorkspace(): IWorkspace = ResourcesPlugin.getWorkspace();
}

/**
 * The activator class controls the plug-in life cycle
 */
class ScalastylePlugin extends AbstractUIPlugin {
  override def start(context: BundleContext): Unit = {
    super.start(context)
    ScalastylePlugin.plugin = this
  }

  override def stop(context: BundleContext): Unit = {
    ScalastylePlugin.plugin = null
    super.stop(context)
  }
}
