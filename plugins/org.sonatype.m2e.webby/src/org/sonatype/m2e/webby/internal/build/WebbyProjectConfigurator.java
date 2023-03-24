/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.sonatype.m2e.webby.internal.build;

import org.apache.maven.plugin.MojoExecution;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.core.lifecyclemapping.model.IPluginExecutionMetadata;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.configurator.AbstractBuildParticipant;
import org.eclipse.m2e.core.project.configurator.AbstractProjectConfigurator;
import org.eclipse.m2e.core.project.configurator.ILifecycleMappingConfiguration;
import org.eclipse.m2e.core.project.configurator.MojoExecutionKey;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;


public class WebbyProjectConfigurator extends AbstractProjectConfigurator {

  @Override
  public AbstractBuildParticipant getBuildParticipant(IMavenProjectFacade projectFacade, MojoExecution execution,
      IPluginExecutionMetadata executionMetadata) {
    return new WebbyBuildParticipant();
  }

  @Override
  public void configure(ProjectConfigurationRequest request, IProgressMonitor monitor) throws CoreException {
    // nothing to do
  }

  @Override
  public boolean hasConfigurationChanged(IMavenProjectFacade newFacade,
      ILifecycleMappingConfiguration oldProjectConfiguration, MojoExecutionKey key, IProgressMonitor monitor) {
    // our builder takes care about the configuration updates
    return false;
  }
}
