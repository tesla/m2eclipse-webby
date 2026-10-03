package org.sonatype.m2e.webby.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILogListener;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.junit.jupiter.api.Test;

class WebbyPluginTest {

  @Test
  void isActive() {
    WebbyPlugin plugin = WebbyPlugin.getDefault();
    assertNotNull(plugin);
    assertEquals(WebbyPlugin.PLUGIN_ID, plugin.getBundle().getSymbolicName());
    assertEquals(WebbyPlugin.PLUGIN_ID, WebbyPlugin.getPluginId());
    assertNotNull(plugin.getWebAppRegistry());
  }

  @Test
  void createsErrors() {
    Exception cause = new Exception("cause");
    CoreException error = WebbyPlugin.newError("message", cause);
    assertEquals(IStatus.ERROR, error.getStatus().getSeverity());
    assertEquals("message", error.getStatus().getMessage());
    assertSame(cause, error.getStatus().getException());
    assertEquals(WebbyPlugin.PLUGIN_ID, error.getStatus().getPlugin());
  }

  @Test
  void logs() {
    List<IStatus> logged = new ArrayList<>();
    ILogListener listener = (status, plugin) -> {
      if (WebbyPlugin.PLUGIN_ID.equals(status.getPlugin())) {
        logged.add(status);
      }
    };
    Platform.addLogListener(listener);
    try {
      WebbyPlugin.log(new Exception("expected by test 1"));
      WebbyPlugin.log(new Exception("expected by test 2"), IStatus.WARNING);
      WebbyPlugin.log("expected by test 3", IStatus.INFO);
      WebbyPlugin.log((Throwable) null);
      WebbyPlugin.log((String) null, IStatus.INFO);
      WebbyPlugin.log((IStatus) null);
    } finally {
      Platform.removeLogListener(listener);
    }
    assertEquals(List.of("expected by test 1", "expected by test 2", "expected by test 3"),
        logged.stream().map(IStatus::getMessage).toList());
    assertEquals(IStatus.WARNING, logged.get(1).getSeverity());
  }

  @Test
  void providesImages() {
    assertNotNull(WebbyImages.LAUNCH_CONFIG_DESC);
    assertNotNull(WebbyImages.LAUNCH_CONFIG);
    assertNotNull(WebbyImages.STOP_DESC);
    assertNotNull(WebbyImages.BROWSE_DESC);
    assertNotNull(WebbyImages.RESTART_DESC);
  }

}
