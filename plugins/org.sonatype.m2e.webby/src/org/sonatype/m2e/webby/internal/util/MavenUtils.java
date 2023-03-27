package org.sonatype.m2e.webby.internal.util;

import org.eclipse.core.resources.IProject;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.IMavenProjectFacade;

public class MavenUtils {
  public static IMavenProjectFacade getFacade(IProject project) {
    return MavenPlugin.getMavenProjectRegistry().getProject(project);
  }

  public static IMavenProjectFacade getFacade(String groupId, String artifactId, String baseVersion) {
    return MavenPlugin.getMavenProjectRegistry().getMavenProject(groupId, artifactId, baseVersion);
  }
}
