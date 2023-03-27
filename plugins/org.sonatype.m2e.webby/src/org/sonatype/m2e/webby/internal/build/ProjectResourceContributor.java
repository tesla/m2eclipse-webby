package org.sonatype.m2e.webby.internal.build;

import java.io.*;

import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.*;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.sonatype.m2e.webby.internal.config.*;
import org.sonatype.m2e.webby.internal.util.PathCollector;

public class ProjectResourceContributor extends ResourceContributor {

  private IMavenProjectFacade mvnFacade;

  private OverlayConfiguration overlayConfig;

  private IResourceDelta resDelta;

  public ProjectResourceContributor(int ordinal, IMavenProjectFacade mvnFacade, OverlayConfiguration overlayConfig,
      IResourceDelta resDelta) {
    super(ordinal);
    this.mvnFacade = mvnFacade;
    this.overlayConfig = overlayConfig;
    this.resDelta = resDelta;
  }

  public void contribute(WarAssembler assembler, IProgressMonitor monitor) {
    try {
      File warDir;
      try {
        warDir = new File(new WarConfigurationExtractor().getWorkDirectory(mvnFacade
                                                                                    .getMavenProject(new NullProgressMonitor())),
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
          try {
            InputStream is = new FileInputStream(sourceFile);
            try {
              assembler.copyResourceFile(is, targetPath, filtering, encoding, sourceFile.lastModified());
            } finally {
              is.close();
            }
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
