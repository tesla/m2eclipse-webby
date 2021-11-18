/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.sonatype.m2e.webby.internal.launch.ui;

import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
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
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.MavenProjectUtils;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.sonatype.m2e.webby.internal.WebbyPlugin;
import org.sonatype.m2e.webby.internal.config.JettyConfiguration;
import org.sonatype.m2e.webby.internal.config.JettyConfigurationExtractor;
import org.sonatype.m2e.webby.internal.launch.WebbyLaunchConstants;
import org.sonatype.m2e.webby.internal.util.MavenUtils;



/**
 *
 */
public class WebbyLaunchShortcut implements ILaunchShortcut {

  private void launch(IContainer container, String mode) {
    IProject project = container.getProject();
    ILaunchConfiguration launchConfig = getLaunchConfiguration(project);
    if(launchConfig != null) {
      DebugUITools.launch(launchConfig, mode);
    }
  }

  private ILaunchConfiguration getLaunchConfiguration(IProject project) {
    if(project == null) {
      return null;
    }

    String configName = project.getName();

    try {
      ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
      ILaunchConfigurationType launchConfigurationType = launchManager
          .getLaunchConfigurationType(WebbyLaunchConstants.TYPE_ID);

      ILaunchConfiguration[] launchConfigurations = launchManager.getLaunchConfigurations(launchConfigurationType);
      for(ILaunchConfiguration launchConfiguration : launchConfigurations) {
        if(launchConfiguration.getName().equals(configName)) {
          return launchConfiguration;
        }
      }

      ILaunchConfigurationWorkingCopy workingCopy = launchConfigurationType.newInstance(null, project.getName());
      workingCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, project.getName());
      initFromDefaults(workingCopy);
      initFromProject(workingCopy, project);
      workingCopy.setMappedResources(new IResource[] {project});
      return workingCopy.doSave();
    } catch(CoreException e) {
      WebbyPlugin.log(e);
      return null;
    }
  }

  private void initFromDefaults(ILaunchConfigurationWorkingCopy workingCopy) {
    workingCopy.setAttribute(WebbyLaunchConstants.ATTR_CONTAINER_ID, "jetty7x");
    workingCopy.setAttribute(WebbyLaunchConstants.ATTR_CONTAINER_TYPE, "embedded");
    workingCopy.setAttribute(WebbyLaunchConstants.ATTR_LOG_LEVEL, "medium");
  }

  private boolean initFromProject(ILaunchConfigurationWorkingCopy workingCopy, IProject project) {
    IMavenProjectFacade mvnFacade = MavenUtils.getFacade(project);
    if(mvnFacade == null) {
      return false;
    }
    JettyConfiguration jettyConfig;
    try {
      jettyConfig = new JettyConfigurationExtractor().getConfiguration(mvnFacade, new NullProgressMonitor());
    } catch(CoreException e) {
      WebbyPlugin.log(e, IStatus.WARNING);
      return false;
    }
    if(jettyConfig == null) {
      return false;
    }
    if(WebbyPlugin.getDefault().isEmbeddedContainerInstalled(jettyConfig.getContainerId())) {
      workingCopy.setAttribute(WebbyLaunchConstants.ATTR_CONTAINER_ID, jettyConfig.getContainerId());
    } else if(WebbyPlugin.getDefault().isEmbeddedContainerInstalled("jetty6x")) {
      workingCopy.setAttribute(WebbyLaunchConstants.ATTR_CONTAINER_ID, "jetty6x");
    } else {
      workingCopy.setAttribute(WebbyLaunchConstants.ATTR_CONTAINER_ID, "jetty7x");
    }
    workingCopy.setAttribute(WebbyLaunchConstants.ATTR_CONTEXT_NAME, jettyConfig.getContext());
    workingCopy.setAttribute(WebbyLaunchConstants.ATTR_CONTAINER_PORT, jettyConfig.getPort());
    StringBuilder buffer = new StringBuilder(256);
    for(Map.Entry<String, String> entry : jettyConfig.getSystemProperties().entrySet()) {
      if(buffer.length() > 0) {
        buffer.append(" ");
      }
      String prop = "-D" + entry.getKey() + "=" + transformPath(project, entry.getValue());
      if(prop.contains(" ")) {
        prop = "\"" + prop + "\"";
      }
      buffer.append(prop);
    }
    if(buffer.length() > 0) {
      workingCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, buffer.toString());
    }
    return true;
  }

  private String transformPath(IProject project, String value) {
    IPath path = MavenProjectUtils.getProjectRelativePath(project, value);
    if(path != null) {
      value = "${resource_loc:" + project.getFullPath().append(path).toString() + "}";
    }
    return value;
  }

  public void launch(ISelection selection, String mode) {
    if(selection instanceof IStructuredSelection) {
      IStructuredSelection structuredSelection = (IStructuredSelection) selection;
      Object object = structuredSelection.getFirstElement();

      IContainer container = null;
      if(object instanceof IProject || object instanceof IFolder) {
        container = (IContainer) object;
      } else if(object instanceof IFile) {
        container = ((IFile) object).getParent();
      } else if(object instanceof IAdaptable) {
        IAdaptable adaptable = (IAdaptable) object;
        Object adapter = adaptable.getAdapter(IProject.class);
        if(adapter != null) {
          container = (IContainer) adapter;
        } else {
          adapter = adaptable.getAdapter(IFolder.class);
          if(adapter != null) {
            container = (IContainer) adapter;
          } else {
            adapter = adaptable.getAdapter(IFile.class);
            if(adapter != null) {
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
    if(editorInput instanceof IFileEditorInput) {
      launch(((IFileEditorInput) editorInput).getFile().getParent(), mode);
    }
  }

}
