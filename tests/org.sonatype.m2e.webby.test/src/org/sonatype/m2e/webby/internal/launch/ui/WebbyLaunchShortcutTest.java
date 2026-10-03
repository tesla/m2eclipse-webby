package org.sonatype.m2e.webby.internal.launch.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonatype.m2e.webby.internal.launch.WebbyLaunchConstants;

class WebbyLaunchShortcutTest {

  private IProject project;

  @BeforeEach
  void createProject() throws CoreException {
    project = ResourcesPlugin.getWorkspace().getRoot().getProject("shortcut-test");
    project.create(new NullProgressMonitor());
    project.open(new NullProgressMonitor());
  }

  @AfterEach
  void cleanUp() throws CoreException {
    for (ILaunchConfiguration config : DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurations()) {
      if (config.getName().startsWith("shortcut-test")) {
        config.delete();
      }
    }
    project.delete(true, new NullProgressMonitor());
  }

  @Test
  void findsProjectOfSelectedElement() throws CoreException {
    IFolder folder = project.getFolder("src");
    folder.create(true, true, null);
    IFile file = folder.getFile("pom.xml");

    assertEquals(project, WebbyLaunchShortcut.getProject(project));
    assertEquals(project, WebbyLaunchShortcut.getProject(folder));
    assertEquals(project, WebbyLaunchShortcut.getProject(file));
    assertEquals(project, WebbyLaunchShortcut.getProject(new IAdaptable() {
      @Override
      public <T> T getAdapter(Class<T> adapter) {
        return adapter == IFile.class ? adapter.cast(file) : null;
      }
    }));
    assertNull(WebbyLaunchShortcut.getProject("not a resource"));
    assertNull(WebbyLaunchShortcut.getProject(null));
  }

  @Test
  void createsAndReusesLaunchConfiguration() throws CoreException {
    assertNull(WebbyLaunchShortcut.getLaunchConfiguration(null));

    ILaunchConfiguration config = WebbyLaunchShortcut.getLaunchConfiguration(project);
    assertNotNull(config);
    assertEquals("shortcut-test", config.getName());
    assertEquals(WebbyLaunchConstants.TYPE_ID, config.getType().getIdentifier());
    assertEquals("shortcut-test", config.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, ""));
    assertEquals(WebbyLaunchConstants.DEFAULT_CONTAINER_ID,
        config.getAttribute(WebbyLaunchConstants.ATTR_CONTAINER_ID, ""));
    assertEquals(project, config.getMappedResources()[0]);

    assertEquals(config, WebbyLaunchShortcut.getLaunchConfiguration(project));

    ILaunchConfigurationWorkingCopy wc = config.getWorkingCopy();
    wc.rename("shortcut-test renamed");
    ILaunchConfiguration renamed = wc.doSave();
    assertEquals(renamed, WebbyLaunchShortcut.getLaunchConfiguration(project),
        "configurations bound to the project are reused");
  }

}
