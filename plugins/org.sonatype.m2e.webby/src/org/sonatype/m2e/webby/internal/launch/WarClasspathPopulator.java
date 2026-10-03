package org.sonatype.m2e.webby.internal.launch;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.sonatype.m2e.webby.internal.config.OverlayConfiguration;
import org.sonatype.m2e.webby.internal.config.WarConfiguration;
import org.sonatype.m2e.webby.internal.util.MavenUtils;
import org.sonatype.m2e.webby.internal.util.WarUtils;

/**
 * Computes the classpath of a web application from the WAR project itself and its overlays. Overlays that are not
 * open in the workspace contribute nothing here: their WEB-INF/classes and WEB-INF/lib are extracted into the WAR
 * directory by the build participant and picked up by the container from there.
 */
public class WarClasspathPopulator {

  public void populate(WarClasspath classpath, MavenProject mvnProject, WarConfiguration warConfig, boolean main,
      IProgressMonitor monitor) throws CoreException {
    try {
      Map<String, Artifact> overlayArtifacts = WarUtils.getOverlayArtifacts(mvnProject);

      List<ClasspathContributor> classpathContributors = new ArrayList<>();

      int overlayOrdinal = 0;
      for (OverlayConfiguration overlay : warConfig.getOverlays()) {
        if (overlay.isSkip()) {
          continue;
        }
        if (overlay.isMain()) {
          if (main) {
            classpathContributors.add(0, new MainClasspathContributor(mvnProject, warConfig));
          }
          continue;
        }
        overlayOrdinal++;
        Artifact overlayArtifact = overlayArtifacts.get(overlay.getArtifactKey());
        if (overlayArtifact == null) {
          continue;
        }
        IMavenProjectFacade overlayFacade = MavenUtils.getFacade(overlayArtifact.getGroupId(),
            overlayArtifact.getArtifactId(), overlayArtifact.getBaseVersion());
        if (overlayFacade != null) {
          classpathContributors.add(new ProjectClasspathContributor(overlayOrdinal, overlayFacade, overlay));
        }
      }

      SubMonitor pm = SubMonitor.convert(monitor, classpathContributors.size());
      for (ClasspathContributor classpathContributor : classpathContributors) {
        if (pm.isCanceled()) {
          throw new OperationCanceledException();
        }
        classpathContributor.contribute(classpath, pm.split(1));
      }
    } finally {
      if (monitor != null) {
        monitor.done();
      }
    }
  }

}
