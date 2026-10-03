package org.sonatype.m2e.webby.internal.launch;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class WebbyLaunchConfigurationListenerTest {

  private final ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();

  @AfterEach
  void deleteConfigurations() throws CoreException {
    for (ILaunchConfiguration config : launchManager.getLaunchConfigurations()) {
      if (config.getName().startsWith("listener-test")) {
        config.delete();
      }
    }
  }

  @Test
  void setsSourcePathProviderOfWebbyConfigurations() throws CoreException {
    ILaunchConfigurationType type = launchManager.getLaunchConfigurationType(WebbyLaunchConstants.TYPE_ID);
    ILaunchConfiguration config = type.newInstance(null, "listener-test-webby").doSave();
    assertEquals(WebbyLaunchConstants.SOURCE_PATH_PROVIDER_ID,
        config.getAttribute(IJavaLaunchConfigurationConstants.ATTR_SOURCE_PATH_PROVIDER, ""));
  }

  @Test
  void keepsCustomSourcePathProvider() throws CoreException {
    ILaunchConfigurationType type = launchManager.getLaunchConfigurationType(WebbyLaunchConstants.TYPE_ID);
    ILaunchConfigurationWorkingCopy wc = type.newInstance(null, "listener-test-custom");
    wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_SOURCE_PATH_PROVIDER, "custom");
    ILaunchConfiguration config = wc.doSave();
    assertEquals("custom", config.getAttribute(IJavaLaunchConfigurationConstants.ATTR_SOURCE_PATH_PROVIDER, ""));
  }

  @Test
  void ignoresOtherConfigurations() throws CoreException {
    ILaunchConfigurationType type = launchManager
        .getLaunchConfigurationType(IJavaLaunchConfigurationConstants.ID_JAVA_APPLICATION);
    ILaunchConfiguration config = type.newInstance(null, "listener-test-java").doSave();
    assertEquals("none", config.getAttribute(IJavaLaunchConfigurationConstants.ATTR_SOURCE_PATH_PROVIDER, "none"));
  }

}
