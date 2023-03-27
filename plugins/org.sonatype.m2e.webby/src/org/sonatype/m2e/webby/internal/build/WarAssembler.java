package org.sonatype.m2e.webby.internal.build;

import java.io.*;
import java.util.*;

import org.codehaus.plexus.util.IOUtil;
import org.eclipse.core.runtime.*;
import org.sonatype.m2e.webby.internal.WebbyPlugin;
import org.sonatype.m2e.webby.internal.build.FilteringHandler.FilteringInput;
import org.sonatype.m2e.webby.internal.util.*;

public class WarAssembler {

  private File outputDirectory;

  private FilteringHandler filteringHandler;

  private ResourceRegistry resourceRegistry;

  private Map<Integer, Collection<String>> deletedPaths = new HashMap<Integer, Collection<String>>();

  public WarAssembler(File outputDirectory, FilteringHandler filteringHandler, ResourceRegistry resourceRegistry) {
    this.outputDirectory = outputDirectory;
    this.filteringHandler = filteringHandler;
    this.resourceRegistry = (resourceRegistry != null) ? resourceRegistry : new ResourceRegistry();
  }

  public boolean registerTargetPath(String targetPath, int overlayOrdinal) {
    return resourceRegistry.register(targetPath, overlayOrdinal);
  }

  public void unregisterTargetPath(String targetPath, int overlayOrdinal) {
    int[] remaining = resourceRegistry.unregister(targetPath, overlayOrdinal);
    if (remaining != null) {
      File target = new File(outputDirectory, targetPath);
      target.delete();

      if (remaining.length > 0) {
        Integer key = Integer.valueOf(remaining[0]);
        Collection<String> paths = deletedPaths.get(key);
        if (paths == null) {
          paths = new HashSet<String>();
          deletedPaths.put(key, paths);
        }
        paths.add(targetPath);
      }

      Integer key = Integer.valueOf(overlayOrdinal);
      Collection<String> paths = deletedPaths.get(key);
      if (paths != null) {
        paths.remove(targetPath);
        if (paths.isEmpty()) {
          deletedPaths.remove(key);
        }
      }
    }
  }

  public String[] appendDirtyTargetPaths(String[] files, int overlayOrdinal, String basedir, String targetDir) {
    Collection<String> deletedPaths = this.deletedPaths.get(Integer.valueOf(overlayOrdinal));
    if (deletedPaths == null || deletedPaths.isEmpty()) {
      return files;
    }

    Collection<String> dirtyPaths = Collections.emptyList();

    for (Iterator<String> it = deletedPaths.iterator(); it.hasNext();) {
      String targetPath = it.next();
      String sourcePath = WarUtils.getSourcePath(targetDir, targetPath);
      if (sourcePath != null) {
        File sourceFile = new File(basedir, sourcePath);
        if (sourceFile.exists()) {
          if (dirtyPaths.isEmpty()) {
            dirtyPaths = new HashSet<String>();
          }
          dirtyPaths.add(sourcePath);
          it.remove();
        }
      }
    }

    if (dirtyPaths.isEmpty()) {
      return files;
    }

    Collections.addAll(dirtyPaths, files);
    return dirtyPaths.toArray(new String[dirtyPaths.size()]);
  }

  public void copyResourceFile(InputStream is, String targetPath, boolean filtering, String encoding, long lastModified)
      throws IOException {
    File target = new File(outputDirectory, targetPath);

    target.getParentFile().mkdirs();

    FilteringInput fi = getReader(is, targetPath, filtering, encoding);

    if (fi == null && lastModified != 0 && target.lastModified() > lastModified) {
      // return;
    }

    OutputStream os = new FileOutputStream(target);
    try {
      if (fi != null) {
        IOUtil.copy(fi.reader, getWriter(os, fi.encoding), 64 * 1024);
      } else {
        IOUtil.copy(is, os, 64 * 1024);
      }
    } finally {
      os.close();
    }
  }

  private FilteringInput getReader(InputStream is, String targetPath, boolean filtering, String encoding)
      throws IOException {
    return filtering ? filteringHandler.getReader(is, targetPath, encoding) : null;
  }

  private Writer getWriter(OutputStream os, String encoding) throws IOException {
    if (encoding != null && encoding.length() > 0) {
      return new OutputStreamWriter(os, encoding);
    }
    return new OutputStreamWriter(os);
  }

  public void addError(String sourceFile, String targetPath, IOException e) {
    StringBuilder msg = new StringBuilder(512);
    msg.append("Failed to copy ");
    msg.append(sourceFile);
    if (targetPath != null) {
      msg.append(" to ").append(new File(outputDirectory, targetPath));
    }
    msg.append(": ").append(e.getMessage());
    Status error = new Status(IStatus.ERROR, WebbyPlugin.getPluginId(), 0, msg.toString(), e);
    WebbyPlugin.log(error);
  }

  public void addError(CoreException e) {
    WebbyPlugin.log(e);
  }

}
