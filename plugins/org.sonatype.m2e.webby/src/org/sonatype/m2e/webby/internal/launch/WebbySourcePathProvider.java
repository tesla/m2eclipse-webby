package org.sonatype.m2e.webby.internal.launch;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.StandardSourcePathProvider;
import org.eclipse.m2e.jdt.IClasspathManager;
import org.eclipse.m2e.jdt.MavenJdtPlugin;
import org.eclipse.m2e.jdt.internal.MavenClasspathHelpers;

/**
 * Source lookup for Webby launches: the project itself plus its Maven dependencies, including the ones that are
 * workspace projects.
 */
@SuppressWarnings("restriction")
public class WebbySourcePathProvider extends StandardSourcePathProvider {

  @Override
  public IRuntimeClasspathEntry[] computeUnresolvedClasspath(ILaunchConfiguration configuration) throws CoreException {
    boolean useDefault = configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_DEFAULT_SOURCE_PATH, true);
    IRuntimeClasspathEntry[] entries;
    if (useDefault) {
      IJavaProject javaProject = JavaRuntime.getJavaProject(configuration);

      IRuntimeClasspathEntry jreEntry = JavaRuntime.computeJREEntry(configuration);
      IRuntimeClasspathEntry projectEntry = JavaRuntime.newProjectRuntimeClasspathEntry(javaProject);
      IRuntimeClasspathEntry mavenEntry = JavaRuntime.newRuntimeContainerClasspathEntry(
          IPath.fromPortableString(IClasspathManager.CONTAINER_ID), IRuntimeClasspathEntry.USER_CLASSES);

      if (jreEntry == null) {
        entries = new IRuntimeClasspathEntry[] { projectEntry, mavenEntry };
      } else {
        entries = new IRuntimeClasspathEntry[] { jreEntry, projectEntry, mavenEntry };
      }
    } else {
      entries = recoverRuntimePath(configuration, IJavaLaunchConfigurationConstants.ATTR_SOURCE_PATH);
    }
    return entries;
  }

  @Override
  public IRuntimeClasspathEntry[] resolveClasspath(IRuntimeClasspathEntry[] entries, ILaunchConfiguration configuration)
      throws CoreException {
    IProgressMonitor monitor = new NullProgressMonitor();
    int scope = IClasspathManager.CLASSPATH_RUNTIME;
    Set<IRuntimeClasspathEntry> all = new LinkedHashSet<>(entries.length);
    for (IRuntimeClasspathEntry entry : entries) {
      if (entry.getType() == IRuntimeClasspathEntry.CONTAINER
          && MavenClasspathHelpers.isMaven2ClasspathContainer(entry.getPath())) {
        addMavenClasspathEntries(all, entry, configuration, scope, monitor);
      } else if (entry.getType() == IRuntimeClasspathEntry.PROJECT) {
        IJavaProject javaProject = JavaRuntime.getJavaProject(configuration);
        if (javaProject.getPath().equals(entry.getPath())) {
          addProjectEntries(all, entry.getPath());
        } else {
          addStandardClasspathEntries(all, entry, configuration);
        }
      } else {
        addStandardClasspathEntries(all, entry, configuration);
      }
    }
    return all.toArray(IRuntimeClasspathEntry[]::new);
  }

  private void addStandardClasspathEntries(Set<IRuntimeClasspathEntry> all, IRuntimeClasspathEntry entry,
      ILaunchConfiguration configuration) throws CoreException {
    IRuntimeClasspathEntry[] resolved = JavaRuntime.resolveRuntimeClasspathEntry(entry, configuration);
    Collections.addAll(all, resolved);
  }

  private void addMavenClasspathEntries(Set<IRuntimeClasspathEntry> resolved,
      IRuntimeClasspathEntry runtimeClasspathEntry, ILaunchConfiguration configuration, int scope,
      IProgressMonitor monitor) throws CoreException {
    IJavaProject javaProject = JavaRuntime.getJavaProject(configuration);
    MavenJdtPlugin plugin = MavenJdtPlugin.getDefault();
    IClasspathManager buildpathManager = plugin.getBuildpathManager();
    IClasspathEntry[] cp = buildpathManager.getClasspath(javaProject.getProject(), scope, false, monitor);
    for (IClasspathEntry entry : cp) {
      switch (entry.getEntryKind()) {
        case IClasspathEntry.CPE_PROJECT:
          addProjectEntries(resolved, entry.getPath());
          break;
        case IClasspathEntry.CPE_LIBRARY:
          resolved.add(JavaRuntime.newArchiveRuntimeClasspathEntry(entry.getPath()));
          break;
        default:
          break;
      }
    }
  }

  private void addProjectEntries(Set<IRuntimeClasspathEntry> resolved, IPath path) {
    IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
    IProject project = root.getProject(path.segment(0));
    IJavaProject javaProject = JavaCore.create(project);
    resolved.add(JavaRuntime.newProjectRuntimeClasspathEntry(javaProject));
  }

}
