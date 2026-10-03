package org.sonatype.m2e.webby.internal.launch.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.finders.UIThreadRunnable;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotCombo;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.ui.PlatformUI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sonatype.m2e.webby.internal.launch.WebbyLaunchConstants;
import org.sonatype.m2e.webby.tests.UITestSupport;

/**
 * Edits a Webby launch configuration through the "Run Configurations" dialog.
 */
class WebbyLaunchConfigurationDialogUITest {

  private static final String PROJECT = "ui-test-project";

  private static final String CONFIG = "ui-test-config";

  private static SWTWorkbenchBot bot;

  private ILaunchConfiguration config;

  private SWTBotShell dialog;

  @TempDir
  Path containerHome;

  @BeforeAll
  static void createBot() {
    bot = UITestSupport.createBot();
  }

  @BeforeEach
  void createConfiguration() throws CoreException {
    IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(PROJECT);
    if (!project.exists()) {
      project.create(new NullProgressMonitor());
    }
    project.open(new NullProgressMonitor());

    ILaunchConfigurationWorkingCopy wc = DebugPlugin.getDefault().getLaunchManager()
        .getLaunchConfigurationType(WebbyLaunchConstants.TYPE_ID).newInstance(null, CONFIG);
    wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, PROJECT);
    wc.setAttribute(WebbyLaunchConstants.ATTR_CONTAINER_HOME, containerHome.toString());
    // as stored by older versions
    wc.setAttribute(WebbyLaunchConstants.ATTR_CONTAINER_PORT, "8181");
    config = wc.doSave();
  }

  @AfterEach
  void cleanUp() throws CoreException {
    if (dialog != null && dialog.isOpen()) {
      dialog.close();
    }
    for (ILaunchConfiguration c : DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurations()) {
      if (c.getName().startsWith(CONFIG)) {
        c.delete();
      }
    }
  }

  private void openDialog() {
    UIThreadRunnable.asyncExec(() -> DebugUITools.openLaunchConfigurationDialogOnGroup(
        PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), new StructuredSelection(config),
        IDebugUIConstants.ID_RUN_LAUNCH_GROUP));
    bot.waitUntil(Conditions.shellIsActive("Run Configurations"));
    dialog = bot.shell("Run Configurations");
    dialog.bot().cTabItem("Main").activate();
  }

  @Test
  void showsWebbyConfiguration() {
    openDialog();

    assertTrue(dialog.bot().tree().getTreeItem("Webby").expand().getNodes().contains(CONFIG));
    for (String tab : List.of("Main", "JRE", "Source", "Environment", "Common")) {
      dialog.bot().cTabItem(tab);
    }

    assertEquals(PROJECT, dialog.bot().textWithId(WebbyTab.WIDGET_ID_KEY, "webby.projectName").getText());
    assertEquals("", dialog.bot().textWithId(WebbyTab.WIDGET_ID_KEY, "webby.contextName").getText());
    assertEquals(containerHome.toString(),
        dialog.bot().textWithId(WebbyTab.WIDGET_ID_KEY, "webby.containerHome").getText());
    assertEquals(8181, dialog.bot().spinnerWithId(WebbyTab.WIDGET_ID_KEY, "webby.containerPort").getSelection(),
        "port stored as string is migrated");
    assertEquals(60, dialog.bot().spinnerWithId(WebbyTab.WIDGET_ID_KEY, "webby.containerTimeout").getSelection());
    assertTrue(dialog.bot().checkBoxWithId(WebbyTab.WIDGET_ID_KEY, "webby.openWhenStarted").isChecked());
    assertTrue(dialog.bot().checkBoxWithId(WebbyTab.WIDGET_ID_KEY, "webby.containerDisableWsSci").isChecked());
    assertEquals("medium", dialog.bot().comboBoxWithId(WebbyTab.WIDGET_ID_KEY, "webby.containerLogging").getText());

    SWTBotCombo provider = dialog.bot().comboBoxWithId(WebbyTab.WIDGET_ID_KEY, "webby.containerId");
    assertEquals(WebbyLaunchConstants.DEFAULT_CONTAINER_ID, provider.getText());
    List<String> items = List.of(provider.items());
    assertTrue(items.containsAll(List.of("tomcat10x", "tomcat11x", "tomee10x", "jetty12x", "glassfish8x")), items.toString());
    assertTrue(items.indexOf("tomcat9x") < items.indexOf("tomcat10x"), "versions are ordered numerically");
    assertEquals("installed", dialog.bot().comboBoxWithId(WebbyTab.WIDGET_ID_KEY, "webby.containerType").getText());

    assertTrue(dialog.bot().button("Run").isEnabled());
    dialog.bot().button("Close").click();
  }

  @Test
  void editsConfiguration() throws CoreException {
    openDialog();

    dialog.bot().textWithId(WebbyTab.WIDGET_ID_KEY, "webby.contextName").setText("/my-context");
    dialog.bot().comboBoxWithId(WebbyTab.WIDGET_ID_KEY, "webby.containerId").setSelection("jetty12x");
    assertFalse(dialog.bot().checkBoxWithId(WebbyTab.WIDGET_ID_KEY, "webby.containerDisableWsSci").isEnabled(),
        "WsSci only applies to Tomcat");
    dialog.bot().comboBoxWithId(WebbyTab.WIDGET_ID_KEY, "webby.containerLogging").setSelection("high");
    dialog.bot().spinnerWithId(WebbyTab.WIDGET_ID_KEY, "webby.containerPort").setSelection(9090);
    dialog.bot().spinnerWithId(WebbyTab.WIDGET_ID_KEY, "webby.containerTimeout").setSelection(30);
    dialog.bot().checkBoxWithId(WebbyTab.WIDGET_ID_KEY, "webby.openWhenStarted").deselect();

    dialog.bot().cTabItem("JRE").activate();
    dialog.bot().textWithId(WebbyTab.WIDGET_ID_KEY, "webby.sysPropsFiles").setText("/tmp/a.properties");

    dialog.bot().button("Apply").click();
    dialog.bot().button("Close").click();
    bot.waitUntil(Conditions.shellCloses(dialog));

    assertEquals("/my-context", config.getAttribute(WebbyLaunchConstants.ATTR_CONTEXT_NAME, ""));
    assertEquals("jetty12x", config.getAttribute(WebbyLaunchConstants.ATTR_CONTAINER_ID, ""));
    assertEquals("high", config.getAttribute(WebbyLaunchConstants.ATTR_LOG_LEVEL, ""));
    assertEquals(9090, config.getAttribute(WebbyLaunchConstants.ATTR_CONTAINER_PORT, 0));
    assertEquals(30, config.getAttribute(WebbyLaunchConstants.ATTR_CONTAINER_TIMEOUT, 0));
    assertFalse(config.getAttribute(WebbyLaunchConstants.ATTR_OPEN_WHEN_STARTED, true));
    assertEquals("/tmp/a.properties", config.getAttribute(WebbyLaunchConstants.ATTR_SYS_PROP_FILES, ""));
    assertEquals(PROJECT, config.getMappedResources()[0].getName());
  }

  @Test
  void validatesConfiguration() {
    openDialog();

    dialog.bot().textWithId(WebbyTab.WIDGET_ID_KEY, "webby.containerHome").setText("");
    assertFalse(dialog.bot().button("Run").isEnabled(), "container home is required");
    dialog.bot().textWithId(WebbyTab.WIDGET_ID_KEY, "webby.containerHome").setText(containerHome.toString());
    assertTrue(dialog.bot().button("Run").isEnabled());

    dialog.bot().textWithId(WebbyTab.WIDGET_ID_KEY, "webby.projectName").setText("");
    assertFalse(dialog.bot().button("Run").isEnabled(), "project is required");
    dialog.bot().textWithId(WebbyTab.WIDGET_ID_KEY, "webby.projectName").setText("missing-project");
    assertFalse(dialog.bot().button("Run").isEnabled(), "project must exist");
    dialog.bot().textWithId(WebbyTab.WIDGET_ID_KEY, "webby.projectName").setText(PROJECT);
    assertTrue(dialog.bot().button("Run").isEnabled());

    dialog.bot().button("Close").click();
  }

}
