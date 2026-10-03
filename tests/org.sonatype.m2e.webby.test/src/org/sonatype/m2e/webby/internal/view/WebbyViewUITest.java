package org.sonatype.m2e.webby.internal.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.Launch;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.finders.UIThreadRunnable;
import org.eclipse.swtbot.swt.finder.waits.DefaultCondition;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotToolbarButton;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonatype.m2e.webby.internal.WebAppRegistry;
import org.sonatype.m2e.webby.internal.WebbyPlugin;
import org.sonatype.m2e.webby.tests.FakeWebApp;
import org.sonatype.m2e.webby.tests.UITestSupport;

/**
 * Exercises the "Web Apps" view with fake web applications.
 */
class WebbyViewUITest {

  private static SWTWorkbenchBot bot;

  private final WebAppRegistry registry = WebbyPlugin.getDefault().getWebAppRegistry();

  private FakeWebApp app1;

  private FakeWebApp app2;

  private SWTBotView view;

  @BeforeAll
  static void createBot() {
    bot = UITestSupport.createBot();
  }

  @BeforeEach
  void showView() {
    app1 = new FakeWebApp(new Launch(null, ILaunchManager.RUN_MODE, null), "/shop", "8123", "tomcat11x");
    app2 = new FakeWebApp(new Launch(null, ILaunchManager.DEBUG_MODE, null), "admin", "9123", "jetty12x");
    registry.addWebApp(app1);

    UIThreadRunnable.syncExec(() -> {
      try {
        PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(WebbyView.ID);
      } catch (PartInitException e) {
        throw new IllegalStateException(e);
      }
    });
    view = bot.viewById(WebbyView.ID);
  }

  @AfterEach
  void cleanUp() {
    registry.removeWebApp(app1);
    registry.removeWebApp(app2);
    if (view != null) {
      view.close();
    }
  }

  private void waitForRows(SWTBotTable table, int rows) {
    bot.waitUntil(new DefaultCondition() {

      @Override
      public boolean test() {
        return table.rowCount() == rows;
      }

      @Override
      public String getFailureMessage() {
        return "expected " + rows + " rows but got " + table.rowCount();
      }

    });
  }

  @Test
  void listsRunningWebApps() {
    assertEquals("Web Apps", view.getTitle());
    SWTBotTable table = view.bot().table();
    assertEquals("Context", table.columns().get(0));
    assertEquals("Port", table.columns().get(1));
    assertEquals("Container", table.columns().get(2));

    waitForRows(table, 1);
    assertEquals("/shop", table.cell(0, 0));
    assertEquals("8123", table.cell(0, 1));
    assertEquals("tomcat11x", table.cell(0, 2));

    registry.addWebApp(app2);
    waitForRows(table, 2);
    assertEquals("/admin", table.cell(1, 0), "context is displayed with a leading slash");
    assertEquals("jetty12x", table.cell(1, 2));

    registry.removeWebApp(app1);
    waitForRows(table, 1);
    assertEquals("/admin", table.cell(0, 0));
  }

  @Test
  void stopsSelectedWebApps() {
    registry.addWebApp(app2);
    SWTBotTable table = view.bot().table();
    waitForRows(table, 2);

    SWTBotToolbarButton stop = view.toolbarButton("Stops the selected web applications");
    SWTBotToolbarButton browse = view
        .toolbarButton("Opens the selected web application, hold modifier key to open in external browser");
    SWTBotToolbarButton restart = view.toolbarButton("Restarts the selected web applications");
    table.unselect();
    assertFalse(stop.isEnabled());
    assertFalse(restart.isEnabled());
    assertFalse(browse.isEnabled());

    table.select(0, 1);
    assertTrue(stop.isEnabled());
    assertTrue(restart.isEnabled());
    assertFalse(browse.isEnabled(), "only a single web app can be browsed");

    table.select(0);
    assertTrue(browse.isEnabled());

    table.select(0, 1);
    stop.click();
    bot.waitUntil(new DefaultCondition() {

      @Override
      public boolean test() {
        return app1.stopCount.get() == 1 && app2.stopCount.get() == 1;
      }

      @Override
      public String getFailureMessage() {
        return "web apps were not stopped";
      }

    });
  }

  @Test
  void offersActionsInContextMenu() {
    SWTBotTable table = view.bot().table();
    waitForRows(table, 1);
    table.select(0);
    table.contextMenu("Browse");
    table.contextMenu("Restart");
    table.contextMenu("Stop").click();
    bot.waitUntil(new DefaultCondition() {

      @Override
      public boolean test() {
        return app1.stopCount.get() == 1;
      }

      @Override
      public String getFailureMessage() {
        return "web app was not stopped";
      }

    });
  }

  @Test
  void stopsListeningWhenClosed() {
    view.close();
    view = null;
    // must not fail although the view is disposed
    registry.addWebApp(app2);
    registry.removeWebApp(app2);
  }

}
