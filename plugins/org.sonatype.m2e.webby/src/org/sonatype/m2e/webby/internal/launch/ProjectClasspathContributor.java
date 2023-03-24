/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.sonatype.m2e.webby.internal.launch;

import java.io.File;
import java.util.Map;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.runtime.*;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.sonatype.m2e.webby.internal.WebbyPlugin;
import org.sonatype.m2e.webby.internal.config.*;
import org.sonatype.m2e.webby.internal.util.*;



/**
 */
public class ProjectClasspathContributor extends ClasspathContributor {

  private IMavenProjectFacade mvnFacade;

  private OverlayConfiguration overlayConfig;

  public ProjectClasspathContributor(int ordinal, IMavenProjectFacade mvnFacade, OverlayConfiguration overlayConfig) {
    super(ordinal);
    this.mvnFacade = mvnFacade;
    this.overlayConfig = overlayConfig;
  }

  @Override
  public void contribute(WarClasspath classpath, IProgressMonitor monitor) throws CoreException {
    SubMonitor pm = SubMonitor.convert(monitor, 100);
    try {
      if(overlayConfig.getTargetPath().length() > 0) {
        return;
      }

      MavenProject mvnProject = mvnFacade.getMavenProject(pm.newChild(30));

      WarConfiguration warConfig;
      try {
        warConfig = new WarConfigurationExtractor()
            .getConfiguration(mvnFacade, mvnProject, pm.newChild(10));
      } catch(CoreException e) {
        throw WebbyPlugin.newError("Could not read configuration of maven-war-plugin for overlay from project "
            + mvnProject.getId(), e);
      }

      PathSelector overlaySelector = new PathSelector(overlayConfig.getIncludes(), overlayConfig.getExcludes());
      PathSelector packagingSelector = new PathSelector(warConfig.getPackagingIncludes(),
          warConfig.getPackagingExcludes());

      PathCollector pathCollector = new PathCollector(null, null);
      String classesDir = warConfig.getClassesDirectory();
      for(String classFile : pathCollector.collectFiles(classesDir)) {
        String targetPath = "WEB-INF/classes/" + classFile;
        if(packagingSelector.isSelected(targetPath)) {
          if(overlaySelector.isSelected(targetPath)) {
            classpath.addRuntimeClasspathEntry(new File(classesDir));
            break;
          }
        }
      }

      OverlayClasspath overlayClasspath = new OverlayClasspath(classpath, packagingSelector, overlaySelector);

      FilenameMapper filenameMapper = new FilenameMapper(warConfig.getFilenameMapping());
      Map<String, Artifact> targetPaths = filenameMapper.getTargetPaths(mvnProject.getArtifacts());
      for(Map.Entry<String, Artifact> e : targetPaths.entrySet()) {
        File file = e.getValue().getFile();
        if(file == null) {
          continue;
        }
        String targetPath = e.getKey();
        if(overlayClasspath.registerTargetPath(targetPath, 0)) {
          classpath.addRuntimeClasspathEntry(file);
        }
      }

      new WarClasspathPopulator().populate(overlayClasspath, mvnProject, warConfig, false, pm.newChild(50));
    } finally {
      if(monitor != null) {
        monitor.done();
      }
    }
  }

  class OverlayClasspath extends WarClasspath {

    private WarClasspath classpath;

    private PathSelector packagingSelector;

    private PathSelector overlaySelector;

    public OverlayClasspath(WarClasspath classpath, PathSelector packagingSelector, PathSelector overlaySelector) {
      this.classpath = classpath;
      this.packagingSelector = packagingSelector;
      this.overlaySelector = overlaySelector;
    }

    @Override
    public boolean registerTargetPath(String targetPath, int overlayOrdinal) {
      if(!super.registerTargetPath(targetPath, overlayOrdinal)) {
        return false;
      }
      if(!packagingSelector.isSelected(targetPath)) {
        return false;
      }
      if(!overlaySelector.isSelected(targetPath)) {
        return false;
      }
      return classpath.registerTargetPath(targetPath, ordinal);
    }

    @Override
    public void addRuntimeClasspathEntry(File path) {
      classpath.addRuntimeClasspathEntry(path);
    }

    @Override
    public void addProvidedClasspathEntry(File path) {
      // not propagated
    }

  }

}
