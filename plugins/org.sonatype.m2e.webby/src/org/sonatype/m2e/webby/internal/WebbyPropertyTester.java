package org.sonatype.m2e.webby.internal;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.IMavenProjectFacade;

/**
 * Tests whether the receiver is a Maven project with "war" packaging (known to m2e), or the POM of such a project.
 */
public class WebbyPropertyTester extends PropertyTester {

  static final String IS_WEB_APP = "isWebApp";

  private static final String POM_FILE_NAME = "pom.xml";

  private static final String WAR_PACKAGING = "war";

  @Override
  public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
    if (!IS_WEB_APP.equals(property)) {
      return false;
    }

    IProject project = Adapters.adapt(receiver, IProject.class);
    if (project == null) {
      IFile file = Adapters.adapt(receiver, IFile.class);
      if (file != null && POM_FILE_NAME.equals(file.getName()) && file.getFullPath().segmentCount() == 2) {
        project = file.getProject();
      }
    }
    return project != null && isWarProject(project);
  }

  static boolean isWarProject(IProject project) {
    if (!project.isAccessible()) {
      return false;
    }

    IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().getProject(project);
    return facade != null && WAR_PACKAGING.equals(facade.getPackaging());
  }

}
