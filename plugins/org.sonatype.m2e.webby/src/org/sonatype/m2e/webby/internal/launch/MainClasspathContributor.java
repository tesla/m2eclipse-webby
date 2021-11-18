/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.sonatype.m2e.webby.internal.launch;

import java.io.File;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.sonatype.m2e.webby.internal.config.WarConfiguration;
import org.sonatype.m2e.webby.internal.util.FilenameMapper;



/**
 */
public class MainClasspathContributor extends ClasspathContributor {

  private MavenProject mvnProject;

  private WarConfiguration warConfig;

  public MainClasspathContributor(MavenProject mvnProject, WarConfiguration warConfig) {
    super(0);
    this.mvnProject = mvnProject;
    this.warConfig = warConfig;
  }

  @Override
  public void contribute(WarClasspath classpath, IProgressMonitor monitor) throws CoreException {
    try {
      classpath.addRuntimeClasspathEntry(new File(warConfig.getClassesDirectory()));

      FilenameMapper filenameMapper = new FilenameMapper(warConfig.getFilenameMapping());

      for(Artifact artifact : mvnProject.getArtifacts()) {
        if(!artifact.getArtifactHandler().isAddedToClasspath()) {
          continue;
        }

        File file = artifact.getFile();
        if(file == null) {
          continue;
        }

        String scope = artifact.getScope();
        if(Artifact.SCOPE_PROVIDED.equals(scope)) {
          classpath.addProvidedClasspathEntry(file);
        } else if(Artifact.SCOPE_RUNTIME.equals(scope) || Artifact.SCOPE_COMPILE.equals(scope)) {
          String targetPath = filenameMapper.getTargetPath(artifact);
          if(targetPath != null && targetPath.startsWith("WEB-INF/lib/")) {
            classpath.addRuntimeClasspathEntry(file);
            classpath.registerTargetPath(targetPath, ordinal);
          }
        }
      }
    } finally {
      if(monitor != null) {
        monitor.done();
      }
    }
  }

}
