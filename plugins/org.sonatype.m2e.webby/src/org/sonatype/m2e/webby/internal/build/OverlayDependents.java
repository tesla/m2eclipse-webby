package org.sonatype.m2e.webby.internal.build;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.sonatype.m2e.webby.internal.util.WarUtils;

/**
 * Rebuilds the WAR projects of the workspace that use a project as overlay once the WAR directory of that project
 * changed. The Maven builder of m2e does not run build participants of projects without changes of their own, so
 * these projects would otherwise keep stale copies of the overlay.
 */
final class OverlayDependents {

  static final String MAVEN_BUILDER_ID = "org.eclipse.m2e.core.maven2Builder";

  private OverlayDependents() {
  }

  static void scheduleUpdate(IMavenProjectFacade overlay) {
    List<IProject> dependents = findDependents(overlay);
    if (dependents.isEmpty()) {
      return;
    }
    WorkspaceJob job = new WorkspaceJob("Updating web applications overlaid with " + overlay.getProject().getName()) {
      @Override
      public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
        SubMonitor pm = SubMonitor.convert(monitor, dependents.size());
        for (IProject dependent : dependents) {
          if (dependent.isAccessible()) {
            dependent.build(IncrementalProjectBuilder.FULL_BUILD, MAVEN_BUILDER_ID, Map.of(), pm.split(1));
          }
        }
        return Status.OK_STATUS;
      }

      @Override
      public boolean belongsTo(Object family) {
        return family == ResourcesPlugin.FAMILY_MANUAL_BUILD;
      }
    };
    job.setRule(ResourcesPlugin.getWorkspace().getRuleFactory().buildRule());
    job.setSystem(true);
    job.schedule();
  }

  static List<IProject> findDependents(IMavenProjectFacade overlay) {
    List<IProject> dependents = new ArrayList<>();
    MavenProject overlayProject = overlay.getMavenProject();
    if (overlayProject == null) {
      return dependents;
    }
    for (IMavenProjectFacade facade : MavenPlugin.getMavenProjectRegistry().getProjects()) {
      MavenProject mvnProject = facade.getMavenProject();
      if (facade.equals(overlay) || mvnProject == null || !"war".equals(facade.getPackaging())) {
        continue;
      }
      for (Artifact artifact : WarUtils.getOverlayArtifacts(mvnProject).values()) {
        if (overlayProject.getGroupId().equals(artifact.getGroupId())
            && overlayProject.getArtifactId().equals(artifact.getArtifactId())
            && overlayProject.getVersion().equals(artifact.getBaseVersion())) {
          dependents.add(facade.getProject());
          break;
        }
      }
    }
    return dependents;
  }

}
