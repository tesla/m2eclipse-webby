package org.sonatype.m2e.webby.internal.build;

import org.apache.maven.plugin.MojoExecution;
import org.eclipse.core.runtime.*;
import org.eclipse.m2e.core.lifecyclemapping.model.IPluginExecutionMetadata;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.configurator.*;

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
