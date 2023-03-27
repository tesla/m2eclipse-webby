package org.sonatype.m2e.webby.internal.launch;

import java.io.File;
import java.util.*;

import org.sonatype.m2e.webby.internal.util.ResourceRegistry;

public class WarClasspath {

  private Set<File> runtimeClasspath = new LinkedHashSet<File>();

  private Set<File> providedClasspath = new LinkedHashSet<File>();

  private ResourceRegistry resourceRegistry;

  public WarClasspath() {
    this.resourceRegistry = new ResourceRegistry();
  }

  public boolean registerTargetPath(String targetPath, int overlayOrdinal) {
    return resourceRegistry.register(targetPath, overlayOrdinal);
  }

  public void addRuntimeClasspathEntry(File path) {
    runtimeClasspath.add(path);
  }

  public void addProvidedClasspathEntry(File path) {
    providedClasspath.add(path);
  }

  public Collection<File> getRuntimeClasspath() {
    return runtimeClasspath;
  }

  public Collection<File> getProvidedClasspath() {
    return providedClasspath;
  }

}
