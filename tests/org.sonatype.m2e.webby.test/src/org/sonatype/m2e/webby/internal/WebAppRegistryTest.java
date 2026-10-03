package org.sonatype.m2e.webby.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.Launch;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.sonatype.m2e.webby.tests.FakeWebApp;

class WebAppRegistryTest {

  private final ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();

  private final WebAppRegistry registry = new WebAppRegistry(launchManager);

  private final List<String> events = new ArrayList<>();

  private final IWebAppListener listener = new IWebAppListener() {

    @Override
    public void webAppStarted(IWebApp webApp) {
      events.add("started " + webApp.getContext());
    }

    @Override
    public void webAppStopped(IWebApp webApp) {
      events.add("stopped " + webApp.getContext());
    }

  };

  @AfterEach
  void dispose() {
    registry.dispose();
  }

  @Test
  void notifiesListeners() {
    registry.addListener(listener);
    registry.addListener(null);
    FakeWebApp app = new FakeWebApp(new Launch(null, ILaunchManager.RUN_MODE, null), "app", "8080", "tomcat11x");

    registry.addWebApp(app);
    registry.addWebApp(app);
    registry.addWebApp(null);
    assertEquals(List.of(app), List.copyOf(registry.getWebApps()));

    registry.removeWebApp(app);
    registry.removeWebApp(app);
    registry.removeWebApp(null);
    assertTrue(registry.getWebApps().isEmpty());
    assertEquals(List.of("started app", "stopped app"), events);

    registry.removeListener(listener);
    registry.addWebApp(app);
    assertEquals(2, events.size());
  }

  @Test
  void survivesFailingListeners() {
    registry.addListener(new IWebAppListener() {

      @Override
      public void webAppStarted(IWebApp webApp) {
        throw new IllegalStateException("expected by test");
      }

      @Override
      public void webAppStopped(IWebApp webApp) {
        throw new IllegalStateException("expected by test");
      }

    });
    registry.addListener(listener);
    FakeWebApp app = new FakeWebApp(new Launch(null, ILaunchManager.RUN_MODE, null), "app", "8080", "tomcat11x");
    registry.addWebApp(app);
    registry.removeWebApp(app);
    assertEquals(List.of("started app", "stopped app"), events);
  }

  @Test
  void removesWebAppsOfTerminatedLaunches() {
    ILaunch launch1 = new Launch(null, ILaunchManager.RUN_MODE, null);
    ILaunch launch2 = new Launch(null, ILaunchManager.DEBUG_MODE, null);
    FakeWebApp app1 = new FakeWebApp(launch1, "one", "8080", "tomcat11x");
    FakeWebApp app2 = new FakeWebApp(launch2, "two", "8090", "jetty12x");
    registry.addWebApp(app1);
    registry.addWebApp(app2);

    registry.launchesTerminated(new ILaunch[] { launch1 });
    assertEquals(List.of(app2), List.copyOf(registry.getWebApps()));
  }

  @Test
  void followsLaunchManager() throws Exception {
    ILaunch launch = new Launch(null, ILaunchManager.RUN_MODE, null);
    launchManager.addLaunch(launch);
    FakeWebApp app = new FakeWebApp(launch, "app", "8080", "tomcat11x");
    registry.addWebApp(app);

    launchManager.removeLaunch(launch);
    assertFalse(registry.getWebApps().contains(app));
  }

  @Test
  void computesUrl() {
    assertEquals("http://localhost:8080/app", new FakeWebApp(null, "app", "8080", "x").getUrl());
    assertEquals("http://localhost:8080/app", new FakeWebApp(null, "/app", "8080", "x").getUrl());
    assertEquals("http://localhost:9090/", new FakeWebApp(null, "", "9090", "x").getUrl());
    assertEquals("http://localhost:9090/", new FakeWebApp(null, null, "9090", "x").getUrl());
  }

}
