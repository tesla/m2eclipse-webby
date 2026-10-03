package org.sonatype.m2e.webby.internal.launch;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.sonatype.m2e.webby.internal.util.ResourceRegistry;

public class WarClasspath {

  private final Set<File> runtimeClasspath = new LinkedHashSet<>();

  private final Set<File> providedClasspath = new LinkedHashSet<>();

  private final ResourceRegistry resourceRegistry = new ResourceRegistry();

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
    return Collections.unmodifiableSet(runtimeClasspath);
  }

  public Collection<File> getProvidedClasspath() {
    return Collections.unmodifiableSet(providedClasspath);
  }

}
