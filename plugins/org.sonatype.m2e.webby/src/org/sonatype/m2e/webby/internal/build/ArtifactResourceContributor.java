package org.sonatype.m2e.webby.internal.build;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.sonatype.m2e.webby.internal.config.OverlayConfiguration;
import org.sonatype.m2e.webby.internal.util.PathCollector;
import org.sonatype.m2e.webby.internal.util.PathSelector;

/**
 * Contributes the resources of an overlay that is not open in the workspace, from its WAR file or directory.
 */
public class ArtifactResourceContributor extends ResourceContributor {

  private final File path;

  private final OverlayConfiguration overlayConfig;

  public ArtifactResourceContributor(int ordinal, File path, OverlayConfiguration overlayConfig) {
    super(ordinal);
    this.path = path;
    this.overlayConfig = overlayConfig;
  }

  @Override
  public void contribute(WarAssembler assembler, IProgressMonitor monitor) {
    try {
      if (path.isDirectory()) {
        processDirectory(assembler, monitor);
      } else {
        processFile(assembler, monitor);
      }
    } finally {
      if (monitor != null) {
        monitor.done();
      }
    }
  }

  private void processFile(WarAssembler assembler, IProgressMonitor monitor) {
    boolean filtering = overlayConfig.isFiltering();
    String encoding = overlayConfig.getEncoding();

    PathSelector pathSelector = new PathSelector(overlayConfig.getIncludes(), overlayConfig.getExcludes());

    long lastModified = path.lastModified();

    try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(path.toPath()))) {
      InputStream ncis = new NonClosingInputStream(zis);
      for (ZipEntry ze = zis.getNextEntry(); ze != null; ze = zis.getNextEntry()) {
        String entryName = ze.getName();
        if (ze.isDirectory() || !pathSelector.isSelected(entryName)) {
          continue;
        }
        String targetPath = overlayConfig.getTargetPath(entryName);
        if (!WarAssembler.isSafeTargetPath(targetPath)) {
          assembler.addError(path.getAbsolutePath() + "!/" + entryName, null,
              new IOException("Entry would be extracted outside of the WAR directory"));
          continue;
        }
        if (assembler.registerTargetPath(targetPath, ordinal)) {
          try {
            assembler.copyResourceFile(ncis, targetPath, filtering, encoding, lastModified);
          } catch (IOException e) {
            assembler.addError(path.getAbsolutePath() + "!/" + entryName, targetPath, e);
          }
        }
      }
    } catch (IOException e) {
      assembler.addError(this.path.getAbsolutePath(), null, e);
    }
  }

  private void processDirectory(WarAssembler assembler, IProgressMonitor monitor) {
    boolean filtering = overlayConfig.isFiltering();
    String encoding = overlayConfig.getEncoding();

    PathCollector pathCollector = new PathCollector(overlayConfig.getIncludes(), overlayConfig.getExcludes());

    for (String file : pathCollector.collectFiles(path)) {
      String targetPath = overlayConfig.getTargetPath(file);
      if (!assembler.registerTargetPath(targetPath, ordinal)) {
        continue;
      }
      if (targetPath.startsWith("WEB-INF/lib/") || targetPath.startsWith("WEB-INF/classes/")) {
        continue;
      }
      File sourceFile = new File(path, file);
      try (InputStream is = Files.newInputStream(sourceFile.toPath())) {
        assembler.copyResourceFile(is, targetPath, filtering, encoding, sourceFile.lastModified());
      } catch (IOException e) {
        assembler.addError(sourceFile.getAbsolutePath(), targetPath, e);
      }
    }
  }

}
