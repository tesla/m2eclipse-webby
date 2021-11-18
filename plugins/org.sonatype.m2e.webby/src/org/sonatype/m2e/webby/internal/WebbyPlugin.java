/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.sonatype.m2e.webby.internal;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.sonatype.m2e.webby.internal.launch.WebbyLaunchConfigurationListener;



/**
 *
 */
public class WebbyPlugin extends AbstractUIPlugin {

  private static WebbyPlugin plugin;

  private WebAppRegistry webAppRegistry;

  private WebbyLaunchConfigurationListener webbyLaunchConfigListener;

  @Override
  public void start(BundleContext context) throws Exception {
    super.start(context);
    plugin = this;
    webAppRegistry = new WebAppRegistry();
    webbyLaunchConfigListener = new WebbyLaunchConfigurationListener();
    DebugPlugin.getDefault().getLaunchManager().addLaunchConfigurationListener(webbyLaunchConfigListener);
  }

  @Override
  public void stop(BundleContext context) throws Exception {
    if(webAppRegistry != null) {
      webAppRegistry.dispose();
      webAppRegistry = null;
    }
    if(webbyLaunchConfigListener != null) {
      DebugPlugin.getDefault().getLaunchManager().removeLaunchConfigurationListener(webbyLaunchConfigListener);
      webbyLaunchConfigListener = null;
    }
    plugin = null;
    super.stop(context);
  }

  public static WebbyPlugin getDefault() {
    return plugin;
  }

  public static String getPluginId() {
    return getDefault().getBundle().getSymbolicName();
  }

  public static IStatus newStatus(String msg, Throwable cause) {
    return new Status(IStatus.ERROR, getPluginId(), msg, cause);
  }

  public static CoreException newError(String msg, Throwable cause) {
    return new CoreException(newStatus(msg, cause));
  }

  public static void log(Throwable e) {
    log(e, IStatus.ERROR);
  }

  public static void log(Throwable e, int severity) {
    WebbyPlugin plugin = getDefault();
    if(plugin == null || e == null) {
      return;
    }
    plugin.getLog().log(new Status(severity, getPluginId(), e.getMessage(), e));
  }

  public static void log(String msg, int severity) {
    WebbyPlugin plugin = getDefault();
    if(plugin == null || msg == null) {
      return;
    }
    plugin.getLog().log(new Status(severity, getPluginId(), msg));
  }

  public static void log(IStatus status) {
    WebbyPlugin plugin = getDefault();
    if(plugin == null || status == null) {
      return;
    }
    plugin.getLog().log(status);
  }

  public boolean isEmbeddedContainerInstalled(String containerId) {
    return getBundle().findEntries("jars/" + containerId, "*.jar", true) != null;
  }

  public WebAppRegistry getWebAppRegistry() {
    return webAppRegistry;
  }

}
