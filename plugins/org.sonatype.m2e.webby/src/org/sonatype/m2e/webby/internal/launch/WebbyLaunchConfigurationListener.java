package org.sonatype.m2e.webby.internal.launch;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationListener;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.sonatype.m2e.webby.internal.WebbyPlugin;

/**
 * Makes sure Webby launch configurations use the Webby source path provider.
 */
public class WebbyLaunchConfigurationListener implements ILaunchConfigurationListener {

  private void setDefaults(ILaunchConfiguration configuration) throws CoreException {
    if (configuration instanceof ILaunchConfigurationWorkingCopy workingCopy) {
      setDefaults(workingCopy);
    } else if (!hasSourcePathProvider(configuration)) {
      ILaunchConfigurationWorkingCopy wc = configuration.getWorkingCopy();
      setDefaults(wc);
      wc.doSave();
    }
  }

  private static void setDefaults(ILaunchConfigurationWorkingCopy configuration) throws CoreException {
    if (!hasSourcePathProvider(configuration)) {
      configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_SOURCE_PATH_PROVIDER,
          WebbyLaunchConstants.SOURCE_PATH_PROVIDER_ID);
    }
  }

  private static boolean hasSourcePathProvider(ILaunchConfiguration configuration) throws CoreException {
    return configuration.hasAttribute(IJavaLaunchConfigurationConstants.ATTR_SOURCE_PATH_PROVIDER);
  }

  private static boolean isWebbyLaunch(ILaunchConfiguration configuration) throws CoreException {
    return configuration.exists() && WebbyLaunchConstants.TYPE_ID.equals(configuration.getType().getIdentifier());
  }

  private void update(ILaunchConfiguration configuration) {
    try {
      if (isWebbyLaunch(configuration)) {
        setDefaults(configuration);
      }
    } catch (CoreException e) {
      WebbyPlugin.log(e);
    }
  }

  @Override
  public void launchConfigurationAdded(ILaunchConfiguration configuration) {
    update(configuration);
  }

  @Override
  public void launchConfigurationChanged(ILaunchConfiguration configuration) {
    update(configuration);
  }

  @Override
  public void launchConfigurationRemoved(ILaunchConfiguration configuration) {
    // nothing to do
  }

}
