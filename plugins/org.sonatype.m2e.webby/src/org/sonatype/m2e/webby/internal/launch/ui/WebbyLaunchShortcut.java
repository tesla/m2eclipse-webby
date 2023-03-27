package org.sonatype.m2e.webby.internal.launch.ui;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.*;
import org.eclipse.debug.ui.*;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.viewers.*;
import org.eclipse.ui.*;
import org.sonatype.m2e.webby.internal.WebbyPlugin;
import org.sonatype.m2e.webby.internal.launch.WebbyLaunchConstants;

public class WebbyLaunchShortcut implements ILaunchShortcut {

  private void launch(IContainer container, String mode) {
    IProject project = container.getProject();
    ILaunchConfiguration launchConfig = getLaunchConfiguration(project);
    if (launchConfig != null) {
      DebugUITools.launch(launchConfig, mode);
    }
  }

  private ILaunchConfiguration getLaunchConfiguration(IProject project) {
    if (project == null) {
      return null;
    }

    String configName = project.getName();

    try {
      ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
      ILaunchConfigurationType launchConfigurationType = launchManager
                                                                      .getLaunchConfigurationType(WebbyLaunchConstants.TYPE_ID);

      ILaunchConfiguration[] launchConfigurations = launchManager.getLaunchConfigurations(launchConfigurationType);
      for (ILaunchConfiguration launchConfiguration : launchConfigurations) {
        if (launchConfiguration.getName().equals(configName)) {
          return launchConfiguration;
        }
      }

      ILaunchConfigurationWorkingCopy workingCopy = launchConfigurationType.newInstance(null, project.getName());
      workingCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, project.getName());
      initFromDefaults(workingCopy);
      workingCopy.setMappedResources(new IResource[] { project });
      return workingCopy.doSave();
    } catch (CoreException e) {
      WebbyPlugin.log(e);
      return null;
    }
  }

  private void initFromDefaults(ILaunchConfigurationWorkingCopy workingCopy) {
    workingCopy.setAttribute(WebbyLaunchConstants.ATTR_CONTAINER_ID, "tomcat10x");
    workingCopy.setAttribute(WebbyLaunchConstants.ATTR_LOG_LEVEL, "medium");
  }

  public void launch(ISelection selection, String mode) {
    if (selection instanceof IStructuredSelection) {
      IStructuredSelection structuredSelection = (IStructuredSelection) selection;
      Object object = structuredSelection.getFirstElement();

      IContainer container = null;
      if (object instanceof IProject || object instanceof IFolder) {
        container = (IContainer) object;
      } else if (object instanceof IFile) {
        container = ((IFile) object).getParent();
      } else if (object instanceof IAdaptable) {
        IAdaptable adaptable = (IAdaptable) object;
        Object adapter = adaptable.getAdapter(IProject.class);
        if (adapter != null) {
          container = (IContainer) adapter;
        } else {
          adapter = adaptable.getAdapter(IFolder.class);
          if (adapter != null) {
            container = (IContainer) adapter;
          } else {
            adapter = adaptable.getAdapter(IFile.class);
            if (adapter != null) {
              container = ((IFile) object).getParent();
            }
          }
        }
      }

      launch(container, mode);
    }
  }

  public void launch(IEditorPart editor, String mode) {
    IEditorInput editorInput = editor.getEditorInput();
    if (editorInput instanceof IFileEditorInput) {
      launch(((IFileEditorInput) editorInput).getFile().getParent(), mode);
    }
  }

}
