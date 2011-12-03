package org.scalastyle.scalastyleplugin.builder

import org.scalastyle.scalastyleplugin.ScalastylePlugin

class ScalastyleMarker

object ScalastyleMarker {
    val MARKER_ID = ScalastylePlugin.PLUGIN_ID + ".ScalastyleMarker"; //$NON-NLS-1$
    val MODULE_NAME = "ModuleName"; //$NON-NLS-1$
    val MESSAGE_KEY = "MessageKey"; //$NON-NLS-1$
    val ERROR_TYPE = ScalastylePlugin.PLUGIN_ID + ".error"; //$NON-NLS-1$
    val WARNING_TYPE = ScalastylePlugin.PLUGIN_ID + ".warning"; //$NON-NLS-1$
    val INFO_TYPE = ScalastylePlugin.PLUGIN_ID + ".info"; //$NON-NLS-1$
}