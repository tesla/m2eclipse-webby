package org.sonatype.m2e.webby.internal.launch;

import java.io.File;

import org.eclipse.core.runtime.*;
import org.sonatype.m2e.webby.internal.config.OverlayConfiguration;

public class ArtifactClasspathContributor extends ClasspathContributor {

  public ArtifactClasspathContributor(int ordinal, File path, OverlayConfiguration overlayConfig) {
    super(ordinal);
  }

  @Override
  public void contribute(WarClasspath classpath, IProgressMonitor monitor) throws CoreException {
    try {

    } finally {
      if (monitor != null) {
        monitor.done();
      }
    }
  }

}
