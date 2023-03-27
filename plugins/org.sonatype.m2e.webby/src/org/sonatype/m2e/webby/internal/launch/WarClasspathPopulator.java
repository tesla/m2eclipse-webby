package org.sonatype.m2e.webby.internal.launch;

import java.util.*;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.runtime.*;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.sonatype.m2e.webby.internal.config.*;
import org.sonatype.m2e.webby.internal.util.*;

public class WarClasspathPopulator {

  public void populate(WarClasspath classpath, MavenProject mvnProject, WarConfiguration warConfig, boolean main,
      IProgressMonitor monitor) throws CoreException {
    try {
      Map<String, Artifact> overlayArtifacts = WarUtils.getOverlayArtifacts(mvnProject);

      List<ClasspathContributor> classpathContributors = new ArrayList<>();

      int overlayOrdinal = 0;
      for (OverlayConfiguration overlay : warConfig.getOverlays()) {
        ClasspathContributor classpathContributor;
        if (overlay.isSkip()) {
          continue;
        }
        if (overlay.isMain()) {
          if (!main) {
            continue;
          }
          classpathContributor = new MainClasspathContributor(mvnProject, warConfig);
        } else {
          overlayOrdinal++;
          Artifact overlayArtifact = overlayArtifacts.get(overlay.getArtifactKey());
          if (overlayArtifact == null) {
            continue;
          }
          IMavenProjectFacade overlayFacade = MavenUtils.getFacade(overlayArtifact.getGroupId(),
              overlayArtifact.getArtifactId(), overlayArtifact.getBaseVersion());
          if (overlayFacade == null) {
            if (overlayArtifact.getFile() == null) {
              continue;
            }
            classpathContributor = new ArtifactClasspathContributor(overlayOrdinal, overlayArtifact.getFile(), overlay);
          } else {
            classpathContributor = new ProjectClasspathContributor(overlayOrdinal, overlayFacade, overlay);
          }
        }
        if (overlay.isMain()) {
          classpathContributors.add(0, classpathContributor);
        } else {
          classpathContributors.add(classpathContributor);
        }
      }

      SubMonitor pm = SubMonitor.convert(monitor, classpathContributors.size());

      for (ClasspathContributor classpathContributor : classpathContributors) {
        if (pm.isCanceled()) {
          throw new OperationCanceledException();
        }
        classpathContributor.contribute(classpath, pm.newChild(1));
      }
    } finally {
      if (monitor != null) {
        monitor.done();
      }
    }
  }

}
