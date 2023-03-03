/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.sonatype.m2e.webby.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchesListener2;


/**
 */
public class WebAppRegistry {

  private final Map<IWebApp, Object> webApps = new ConcurrentHashMap<IWebApp, Object>();

  private final Collection<IWebAppListener> listeners = new CopyOnWriteArrayList<IWebAppListener>();

  private final WebAppLaunchListener listener;

  public WebAppRegistry() {
    listener = new WebAppLaunchListener();
    DebugPlugin.getDefault().getLaunchManager().addLaunchListener(listener);
  }

  public void dispose() {
    DebugPlugin.getDefault().getLaunchManager().removeLaunchListener(listener);
  }

  public void addListener(IWebAppListener listener) {
    if(listener == null) {
      return;
    }
    listeners.add(listener);
  }

  public void removeListener(IWebAppListener listener) {
    listeners.remove(listener);
  }

  public void addWebApp(IWebApp webApp) {
    if(webApp == null) {
      return;
    }
    webApps.put(webApp, Boolean.TRUE);
    for(IWebAppListener listener : listeners) {
      try {
        listener.webAppStarted(webApp);
      } catch(RuntimeException e) {
        WebbyPlugin.log(e);
      }
    }
  }

  public void removeWebApp(IWebApp webApp) {
    if(webApp == null) {
      return;
    }
    webApps.remove(webApp);

    for(IWebAppListener listener : listeners) {
      try {
        listener.webAppStopped(webApp);
      } catch(RuntimeException e) {
        WebbyPlugin.log(e);
      }
    }
  }

  public Collection<IWebApp> getWebApps() {
    return Collections.unmodifiableCollection(webApps.keySet());
  }

  class WebAppLaunchListener implements ILaunchesListener2 {

    public void launchesTerminated(ILaunch[] launches) {
      for(ILaunch launch : launches) {
        for(IWebApp webApp : webApps.keySet()) {
          if(webApp.getLaunch() == launch) {
            removeWebApp(webApp);
            break;
          }
        }
      }
    }

    public void launchesRemoved(ILaunch[] launches) {
      launchesTerminated(launches);
    }

    public void launchesAdded(ILaunch[] launches) {
      // irrelevant
    }

    public void launchesChanged(ILaunch[] launches) {
      // irrelevant
    }

  }

}
