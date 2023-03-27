package org.sonatype.m2e.webby.internal.util;

import java.io.File;
import java.util.*;

import org.codehaus.plexus.util.DirectoryScanner;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;

public class PathCollector {

  private final DirectoryScannerEx scanner;

  public PathCollector(List<String> includes, List<String> excludes) {
    scanner = new DirectoryScannerEx();
    if (includes != null && !includes.isEmpty()) {
      scanner.setIncludes(includes.toArray(new String[includes.size()]));
    } else {
      scanner.setIncludes(new String[] { "**" });
    }
    if (excludes != null) {
      scanner.setExcludes(excludes.toArray(new String[excludes.size()]));
    } else {
      scanner.setExcludes(new String[0]);
    }
    scanner.addDefaultExcludes();
    scanner.setupMatchPatterns();
  }

  public String[] collectFiles(String basedir) {
    return collectFiles(new File(basedir));
  }

  public String[] collectFiles(File basedir) {
    String[] files;
    if (!basedir.isDirectory()) {
      files = new String[0];
    } else {
      scanner.setBasedir(basedir);
      scanner.scan();
      files = scanner.getIncludedFiles();
      scanner.clear();
    }
    return files;
  }

  public String[][] collectFiles(IResourceDelta resDelta) {
    if (resDelta == null) {
      return new String[2][0];
    }

    final int baseSegments = resDelta.getFullPath().segmentCount();

    final List<String> included = new ArrayList<>();
    final List<String> deleted = new ArrayList<>();

    try {
      resDelta.accept(delta -> {
        String path = delta.getFullPath().removeFirstSegments(baseSegments).toOSString();

        boolean recurse = false;

        if (delta.getResource().getType() != IResource.FILE) {
          recurse = scanner.couldHoldIncluded(path);
        } else if (scanner.isSelected(path)) {
          if (delta.getKind() == IResourceDelta.CHANGED || delta.getKind() == IResourceDelta.ADDED) {
            included.add(path);
          } else if (delta.getKind() == IResourceDelta.REMOVED) {
            deleted.add(path);
          }
        }

        return recurse;
      });
    } catch (CoreException e) {
      throw new IllegalStateException(e);
    }

    String[][] results = new String[2][];
    results[0] = included.toArray(new String[included.size()]);
    results[1] = deleted.toArray(new String[deleted.size()]);
    return results;
  }

  @Override
  public String toString() {
    return "includes = " + Arrays.asList(scanner.getIncludes()) + ", excludes = "
        + Arrays.asList(scanner.getExcludes());
  }

  static class DirectoryScannerEx extends DirectoryScanner {

    public String[] getIncludes() {
      return includes;
    }

    public String[] getExcludes() {
      return excludes;
    }

    public void clear() {
      filesIncluded = null;
      filesNotIncluded = null;
      filesExcluded = null;
      filesDeselected = null;
      dirsIncluded = null;
      dirsNotIncluded = null;
      dirsExcluded = null;
      dirsDeselected = null;
    }

    @Override
    public boolean couldHoldIncluded(String path) {
      return super.couldHoldIncluded(path);
    }

    public boolean isSelected(String path) {
      return isIncluded(path) && !isExcluded(path);
    }

    @Override
    public void setupMatchPatterns() {
      super.setupMatchPatterns();
    }
  }

}
