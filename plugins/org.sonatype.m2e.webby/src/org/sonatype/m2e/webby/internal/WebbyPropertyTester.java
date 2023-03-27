package org.sonatype.m2e.webby.internal;

import org.apache.maven.model.Model;
import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.m2e.core.MavenPlugin;

public class WebbyPropertyTester extends PropertyTester {

  private static final String IS_WEB_APP = "isWebApp";

  private static final String POM_FILE_NAME = "pom.xml";

  public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
    if (IS_WEB_APP.equals(property)) {
      IFile pomFile = getPomFile((IAdaptable) receiver);
      if (pomFile != null && pomFile.exists()) {
        try {
          Model pom = MavenPlugin.getMaven().readModel(pomFile.getContents(true));
          return "war".equals(pom.getPackaging());
        } catch (CoreException e) {
          WebbyPlugin.getDefault().getLog()
                     .log(new Status(IStatus.ERROR, WebbyPlugin.getPluginId(), e.getMessage(), e));
        }
      }
    }
    return false;
  }

  private IFile getPomFile(IAdaptable adaptable) {
    IProject project = adaptable.getAdapter(IProject.class);
    if (project != null) {
      return project.getFile(POM_FILE_NAME);
    }

    IFile file = adaptable.getAdapter(IFile.class);
    if (file != null && POM_FILE_NAME.equals(file.getName()) && file.getFullPath().segmentCount() == 2) {
      return file;
    }

    return null;
  }

}
