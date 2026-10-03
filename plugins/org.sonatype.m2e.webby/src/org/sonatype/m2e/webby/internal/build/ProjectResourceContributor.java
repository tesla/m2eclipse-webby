package org.sonatype.m2e.webby.internal.build;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.sonatype.m2e.webby.internal.config.OverlayConfiguration;
import org.sonatype.m2e.webby.internal.config.WarConfigurationExtractor;
import org.sonatype.m2e.webby.internal.util.PathCollector;

/**
 * Contributes the resources of an overlay that is a WAR project of the workspace, from its own WAR directory.
 */
public class ProjectResourceContributor extends ResourceContributor {

  private final IMavenProjectFacade mvnFacade;

  private final OverlayConfiguration overlayConfig;

  private final IResourceDelta resDelta;

  public ProjectResourceContributor(int ordinal, IMavenProjectFacade mvnFacade, OverlayConfiguration overlayConfig,
      IResourceDelta resDelta) {
    super(ordinal);
    this.mvnFacade = mvnFacade;
    this.overlayConfig = overlayConfig;
    this.resDelta = resDelta;
  }

  @Override
  public void contribute(WarAssembler assembler, IProgressMonitor monitor) {
    try {
      File warDir;
      try {
        warDir = new File(
            new WarConfigurationExtractor().getWorkDirectory(mvnFacade.getMavenProject(new NullProgressMonitor())),
            "war");
      } catch (CoreException e) {
        assembler.addError(e);
        return;
      }

      PathCollector pathCollector = new PathCollector(overlayConfig.getIncludes(), overlayConfig.getExcludes());

      String[][] files;

      if (resDelta != null) {
        IResourceDelta childDelta = ResourceDeltaUtils.findChildDelta(resDelta, mvnFacade.getProject(), warDir);
        files = pathCollector.collectFiles(childDelta);
      } else {
        files = new String[2][];
        files[0] = pathCollector.collectFiles(warDir);
        files[1] = new String[0];
      }

      for (String file : files[1]) {
        String targetPath = overlayConfig.getTargetPath(file);
        assembler.unregisterTargetPath(targetPath, ordinal);
      }

      if (resDelta != null) {
        files[0] = assembler.appendDirtyTargetPaths(files[0], ordinal, warDir.getAbsolutePath(),
            overlayConfig.getTargetPath());
      }

      boolean filtering = overlayConfig.isFiltering();
      String encoding = overlayConfig.getEncoding();

      for (String file : files[0]) {
        String targetPath = overlayConfig.getTargetPath(file);
        if (assembler.registerTargetPath(targetPath, ordinal)) {
          File sourceFile = new File(warDir, file);
          try (InputStream is = Files.newInputStream(sourceFile.toPath())) {
            assembler.copyResourceFile(is, targetPath, filtering, encoding, sourceFile.lastModified());
          } catch (IOException e) {
            assembler.addError(sourceFile.getAbsolutePath(), targetPath, e);
          }
        }
      }
    } finally {
      if (monitor != null) {
        monitor.done();
      }
    }
  }

}
