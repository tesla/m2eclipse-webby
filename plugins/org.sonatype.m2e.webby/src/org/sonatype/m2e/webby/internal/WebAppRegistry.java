package org.sonatype.m2e.webby.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.ILaunchesListener2;

/**
 * Keeps track of the web applications started by Webby, until their launch terminates.
 */
public class WebAppRegistry {

  private final Set<IWebApp> webApps = ConcurrentHashMap.newKeySet();

  private final Collection<IWebAppListener> listeners = new CopyOnWriteArrayList<>();

  private final ILaunchManager launchManager;

  private final WebAppLaunchListener launchListener = new WebAppLaunchListener();

  public WebAppRegistry() {
    this(DebugPlugin.getDefault().getLaunchManager());
  }

  WebAppRegistry(ILaunchManager launchManager) {
    this.launchManager = launchManager;
    launchManager.addLaunchListener(launchListener);
  }

  public void dispose() {
    launchManager.removeLaunchListener(launchListener);
  }

  public void addListener(IWebAppListener listener) {
    if (listener != null) {
      listeners.add(listener);
    }
  }

  public void removeListener(IWebAppListener listener) {
    listeners.remove(listener);
  }

  public void addWebApp(IWebApp webApp) {
    if (webApp == null || !webApps.add(webApp)) {
      return;
    }
    for (IWebAppListener listener : listeners) {
      try {
        listener.webAppStarted(webApp);
      } catch (RuntimeException e) {
        WebbyPlugin.log(e);
      }
    }
  }

  public void removeWebApp(IWebApp webApp) {
    if (webApp == null || !webApps.remove(webApp)) {
      return;
    }
    for (IWebAppListener listener : listeners) {
      try {
        listener.webAppStopped(webApp);
      } catch (RuntimeException e) {
        WebbyPlugin.log(e);
      }
    }
  }

  public Collection<IWebApp> getWebApps() {
    return Collections.unmodifiableCollection(webApps);
  }

  void launchesTerminated(ILaunch[] launches) {
    for (ILaunch launch : launches) {
      for (IWebApp webApp : webApps) {
        if (webApp.getLaunch() == launch) {
          removeWebApp(webApp);
        }
      }
    }
  }

  private class WebAppLaunchListener implements ILaunchesListener2 {

    @Override
    public void launchesTerminated(ILaunch[] launches) {
      WebAppRegistry.this.launchesTerminated(launches);
    }

    @Override
    public void launchesRemoved(ILaunch[] launches) {
      WebAppRegistry.this.launchesTerminated(launches);
    }

    @Override
    public void launchesAdded(ILaunch[] launches) {
      // irrelevant
    }

    @Override
    public void launchesChanged(ILaunch[] launches) {
      // irrelevant
    }

  }

}
