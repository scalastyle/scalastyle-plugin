package org.scalastyle.scalastyleplugin

import org.eclipse.osgi.util.NLS;

class Messages

object Messages extends NLS {
    private val BUNDLE_NAME = "com.atlassw.tools.eclipse.checkstyle.messages"; //$NON-NLS-1$

    NLS.initializeMessages(BUNDLE_NAME, classOf[Messages]);

    val buildAllProjects = "Building all projects";
    val buildSingleProject = "Building one project";
}