package org.sonatype.m2e.webby.internal.launch.ui;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.ILaunchShortcut;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.sonatype.m2e.webby.internal.WebbyPlugin;
import org.sonatype.m2e.webby.internal.launch.WebbyLaunchConstants;

/**
 * "Run As > Webby" / "Debug As > Webby": reuses the launch configuration named after the project, or creates it.
 */
public class WebbyLaunchShortcut implements ILaunchShortcut {

  @Override
  public void launch(ISelection selection, String mode) {
    if (selection instanceof IStructuredSelection structuredSelection) {
      launch(getProject(structuredSelection.getFirstElement()), mode);
    }
  }

  @Override
  public void launch(IEditorPart editor, String mode) {
    IEditorInput editorInput = editor.getEditorInput();
    if (editorInput instanceof IFileEditorInput fileEditorInput) {
      launch(fileEditorInput.getFile().getProject(), mode);
    }
  }

  private void launch(IProject project, String mode) {
    ILaunchConfiguration launchConfig = getLaunchConfiguration(project);
    if (launchConfig != null) {
      DebugUITools.launch(launchConfig, mode);
    }
  }

  static IProject getProject(Object element) {
    if (element == null) {
      return null;
    }
    IResource resource = Adapters.adapt(element, IProject.class);
    if (resource == null) {
      resource = Adapters.adapt(element, IFolder.class);
    }
    if (resource == null) {
      resource = Adapters.adapt(element, IFile.class);
    }
    if (resource == null) {
      resource = Adapters.adapt(element, IContainer.class);
    }
    return resource != null ? resource.getProject() : null;
  }

  /**
   * @return the existing Webby launch configuration named after (or else bound to) the project, or a new one,
   *         {@code null} on failure
   */
  public static ILaunchConfiguration getLaunchConfiguration(IProject project) {
    if (project == null) {
      return null;
    }

    try {
      ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
      ILaunchConfigurationType type = launchManager.getLaunchConfigurationType(WebbyLaunchConstants.TYPE_ID);

      ILaunchConfiguration[] launchConfigurations = launchManager.getLaunchConfigurations(type);
      for (ILaunchConfiguration launchConfiguration : launchConfigurations) {
        if (launchConfiguration.getName().equals(project.getName())) {
          return launchConfiguration;
        }
      }
      for (ILaunchConfiguration launchConfiguration : launchConfigurations) {
        if (project.getName().equals(
            launchConfiguration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, ""))) {
          return launchConfiguration;
        }
      }

      ILaunchConfigurationWorkingCopy workingCopy = type.newInstance(null,
          launchManager.generateLaunchConfigurationName(project.getName()));
      workingCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, project.getName());
      workingCopy.setAttribute(WebbyLaunchConstants.ATTR_CONTAINER_ID, WebbyLaunchConstants.DEFAULT_CONTAINER_ID);
      workingCopy.setAttribute(WebbyLaunchConstants.ATTR_LOG_LEVEL, WebbyLaunchConstants.DEFAULT_LOG_LEVEL);
      workingCopy.setMappedResources(new IResource[] { project });
      return workingCopy.doSave();
    } catch (CoreException e) {
      WebbyPlugin.log(e);
      return null;
    }
  }

}
