package org.sonatype.m2e.webby.internal.build;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.sonatype.m2e.webby.internal.WebbyPlugin;
import org.sonatype.m2e.webby.internal.build.FilteringHandler.FilteringInput;
import org.sonatype.m2e.webby.internal.util.ResourceRegistry;
import org.sonatype.m2e.webby.internal.util.WarUtils;

/**
 * Assembles the WAR directory from the resources contributed by the WAR project and its overlays.
 */
public class WarAssembler {

  private final File outputDirectory;

  private final FilteringHandler filteringHandler;

  private final ResourceRegistry resourceRegistry;

  /** Paths deleted from the WAR directory which an overlay (identified by its ordinal) must provide again. */
  private final Map<Integer, Collection<String>> deletedPaths = new HashMap<>();

  private boolean changed;

  public WarAssembler(File outputDirectory, FilteringHandler filteringHandler, ResourceRegistry resourceRegistry) {
    this.outputDirectory = outputDirectory;
    this.filteringHandler = filteringHandler;
    this.resourceRegistry = (resourceRegistry != null) ? resourceRegistry : new ResourceRegistry();
  }

  /**
   * @return {@code false} if the target path is absolute or would escape the WAR directory
   */
  static boolean isSafeTargetPath(String targetPath) {
    if (targetPath == null || targetPath.isEmpty()) {
      return false;
    }
    String normalized = targetPath.replace('\\', '/');
    if (normalized.startsWith("/") || (normalized.length() > 1 && normalized.charAt(1) == ':')) {
      return false;
    }
    Path path = Path.of("war").resolve(normalized).normalize();
    return path.startsWith("war") && !path.equals(Path.of("war"));
  }

  public boolean registerTargetPath(String targetPath, int overlayOrdinal) {
    return resourceRegistry.register(targetPath, overlayOrdinal);
  }

  public void unregisterTargetPath(String targetPath, int overlayOrdinal) {
    int[] remaining = resourceRegistry.unregister(targetPath, overlayOrdinal);
    if (remaining == null) {
      return;
    }

    new File(outputDirectory, targetPath).delete();
    changed = true;

    if (remaining.length > 0) {
      deletedPaths.computeIfAbsent(remaining[0], k -> new HashSet<>()).add(targetPath);
    }

    Collection<String> paths = deletedPaths.get(overlayOrdinal);
    if (paths != null) {
      paths.remove(targetPath);
      if (paths.isEmpty()) {
        deletedPaths.remove(overlayOrdinal);
      }
    }
  }

  /**
   * @return the given files plus the files of the overlay that must be copied again because the resource of an overlay
   *         with higher precedence was deleted
   */
  public String[] appendDirtyTargetPaths(String[] files, int overlayOrdinal, String basedir, String targetDir) {
    Collection<String> overlayDeletedPaths = deletedPaths.get(overlayOrdinal);
    if (overlayDeletedPaths == null || overlayDeletedPaths.isEmpty()) {
      return files;
    }

    Collection<String> dirtyPaths = new HashSet<>();

    for (Iterator<String> it = overlayDeletedPaths.iterator(); it.hasNext();) {
      String sourcePath = WarUtils.getSourcePath(targetDir, it.next());
      if (sourcePath != null && new File(basedir, sourcePath).exists()) {
        dirtyPaths.add(sourcePath);
        it.remove();
      }
    }

    if (dirtyPaths.isEmpty()) {
      return files;
    }

    Collections.addAll(dirtyPaths, files);
    return dirtyPaths.toArray(String[]::new);
  }

  public void copyResourceFile(InputStream is, String targetPath, boolean filtering, String encoding, long lastModified)
      throws IOException {
    File target = new File(outputDirectory, targetPath);
    target.getParentFile().mkdirs();
    changed = true;

    FilteringInput fi = filtering ? filteringHandler.getReader(is, targetPath, encoding) : null;

    try (OutputStream os = Files.newOutputStream(target.toPath())) {
      if (fi != null) {
        try (Writer writer = getWriter(os, fi.encoding)) {
          fi.reader.transferTo(writer);
        }
      } else {
        is.transferTo(os);
      }
    }
  }

  private static Writer getWriter(OutputStream os, String encoding) {
    if (encoding != null && !encoding.isEmpty()) {
      return new OutputStreamWriter(os, Charset.forName(encoding));
    }
    return new OutputStreamWriter(os, Charset.defaultCharset());
  }

  /**
   * @return whether files of the WAR directory were written or deleted
   */
  public boolean isChanged() {
    return changed;
  }

  public void addError(String sourceFile, String targetPath, IOException e) {
    StringBuilder msg = new StringBuilder(512);
    msg.append("Failed to copy ").append(sourceFile);
    if (targetPath != null) {
      msg.append(" to ").append(new File(outputDirectory, targetPath));
    }
    msg.append(": ").append(e.getMessage());
    WebbyPlugin.log(new Status(IStatus.ERROR, WebbyPlugin.PLUGIN_ID, msg.toString(), e));
  }

  public void addError(CoreException e) {
    WebbyPlugin.log(e);
  }

}
