/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.sonatype.m2e.webby.internal.launch;

import java.io.File;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import org.sonatype.m2e.webby.internal.util.ResourceRegistry;



/**
 */
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
