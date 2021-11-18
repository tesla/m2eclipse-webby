/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.sonatype.m2e.webby.internal.launch;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationListener;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.sonatype.m2e.webby.internal.WebbyPlugin;



/**
 */
public class WebbyLaunchConfigurationListener implements ILaunchConfigurationListener {

  private void setDefaults(ILaunchConfiguration configuration) throws CoreException {
    if(configuration instanceof ILaunchConfigurationWorkingCopy) {
      setDefaults((ILaunchConfigurationWorkingCopy) configuration);
    } else {
      ILaunchConfigurationWorkingCopy wc = configuration.getWorkingCopy();
      setDefaults(wc);
      if(wc.isDirty()) {
        wc.doSave();
      }
    }
  }

  private void setDefaults(ILaunchConfigurationWorkingCopy configuration) throws CoreException {
    if(!configuration.getAttributes().containsKey(IJavaLaunchConfigurationConstants.ATTR_SOURCE_PATH_PROVIDER)) {
      configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_SOURCE_PATH_PROVIDER,
          "org.sonatype.m2e.webby.sourcepathProvider");
    }
  }

  private boolean isWebbyLaunch(ILaunchConfiguration configuration) throws CoreException {
    return WebbyLaunchConstants.TYPE_ID.equals(configuration.getType().getIdentifier());
  }

  public void launchConfigurationAdded(ILaunchConfiguration configuration) {
    try {
      if(isWebbyLaunch(configuration)) {
        setDefaults(configuration);
      }
    } catch(CoreException e) {
      WebbyPlugin.log(e);
    }
  }

  public void launchConfigurationChanged(ILaunchConfiguration configuration) {
    try {
      if(isWebbyLaunch(configuration)) {
        setDefaults(configuration);
      }
    } catch(CoreException e) {
      WebbyPlugin.log(e);
    }
  }

  public void launchConfigurationRemoved(ILaunchConfiguration configuration) {
  }

}
