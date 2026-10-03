package org.sonatype.m2e.webby.internal.launch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.stream.Stream;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonatype.m2e.webby.internal.IWebApp;
import org.sonatype.m2e.webby.internal.WebbyPlugin;
import org.sonatype.m2e.webby.tests.ProcessRecorder;
import org.sonatype.m2e.webby.tests.TestProjects;

/**
 * Runs the sample WAR project in real containers. The containers are provided by the build through the system
 * properties webby.test.&lt;containerId&gt;.home, the tests are skipped for the missing ones.
 */
class WebbyLaunchIntegrationTest {

  private static final Duration STARTUP_TIMEOUT = Duration.ofMinutes(3);

  private static final Duration SHUTDOWN_TIMEOUT = Duration.ofMinutes(1);

  private static IProject project;

  private ILaunch launch;

  @TempDir
  Path tmp;

  @BeforeAll
  static void importProject() throws Exception {
    project = TestProjects.importProject(TestProjects.SAMPLE);
  }

  @AfterEach
  void terminate() throws Exception {
    if (launch != null && !launch.isTerminated()) {
      launch.terminate();
    }
    if (launch != null) {
      DebugPlugin.getDefault().getLaunchManager().removeLaunch(launch);
    }
  }

  static Stream<Arguments> containers() {
    return Stream.of(
        Arguments.of("tomcat11x", ILaunchManager.RUN_MODE),
        Arguments.of("tomcat11x", ILaunchManager.DEBUG_MODE),
        Arguments.of("tomcat10x", ILaunchManager.RUN_MODE),
        Arguments.of("jetty12x", ILaunchManager.RUN_MODE));
  }

  private static String getContainerHome(String containerId) {
    String home = System.getProperty("webby.test." + containerId + ".home");
    assumeTrue(home != null && Files.isDirectory(Path.of(home)), containerId + " is not available");
    return home;
  }

  private ILaunchConfigurationWorkingCopy createConfiguration(String containerId, String home, int port)
      throws CoreException, IOException {
    Path sysProps = tmp.resolve("webby.properties");
    Files.writeString(sysProps, "webby.sample.property=" + containerId + "\n");

    ILaunchConfigurationType type = DebugPlugin.getDefault().getLaunchManager()
        .getLaunchConfigurationType(WebbyLaunchConstants.TYPE_ID);
    ILaunchConfigurationWorkingCopy config = type.newInstance(null, "integration-" + containerId);
    config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, project.getName());
    config.setAttribute(WebbyLaunchConstants.ATTR_CONTEXT_NAME, "/sample");
    config.setAttribute(WebbyLaunchConstants.ATTR_CONTAINER_ID, containerId);
    config.setAttribute(WebbyLaunchConstants.ATTR_CONTAINER_HOME, home);
    config.setAttribute(WebbyLaunchConstants.ATTR_CONTAINER_PORT, port);
    config.setAttribute(WebbyLaunchConstants.ATTR_CONTAINER_TIMEOUT, Integer.getInteger("webby.test.timeout", (int) STARTUP_TIMEOUT.toSeconds()));
    config.setAttribute(WebbyLaunchConstants.ATTR_OPEN_WHEN_STARTED, false);
    config.setAttribute(WebbyLaunchConstants.ATTR_SYS_PROP_FILES, sysProps.toString());
    config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, "-Xmx256m");
    return config;
  }

  @ParameterizedTest(name = "{0} ({1})")
  @MethodSource("containers")
  void runsWebApp(String containerId, String mode) throws Exception {
    String home = getContainerHome(containerId);
    try (ProcessRecorder recorder = new ProcessRecorder()) {
      try {
        runWebApp(containerId, mode, home);
      } catch (Exception | AssertionError e) {
        System.out.println("Output of " + containerId + ":\n" + recorder.getOutput());
        throw e;
      }
    }
  }

  private void runWebApp(String containerId, String mode, String home) throws Exception {
    int port = findFreePorts();

    ILaunchConfigurationWorkingCopy config = createConfiguration(containerId, home, port);
    launch = config.launch(mode, new NullProgressMonitor());
    assertFalse(launch.isTerminated(), "container is running");
    assertTrue(launch.getProcesses().length > 0);
    if (ILaunchManager.DEBUG_MODE.equals(mode)) {
      assertTrue(launch.getDebugTargets().length > 0, "debugger is attached");
    }

    IWebApp webApp = WebbyPlugin.getDefault().getWebAppRegistry().getWebApps().stream()
        .filter(app -> app.getLaunch() == launch).findFirst().orElse(null);
    assertNotNull(webApp, "web app is registered");
    assertEquals("sample", webApp.getContext());
    assertEquals(Integer.toString(port), webApp.getPort());
    assertEquals(containerId, webApp.getContainerId());
    assertEquals("http://localhost:" + port + "/sample", webApp.getUrl());

    HttpResponse<String> response = get(webApp.getUrl() + "/hello");
    assertEquals(200, response.statusCode());
    assertEquals("Hello from Webby (" + containerId + ")", response.body(),
        "servlet from target/classes, filtered resource and system properties file are used");

    assertEquals(200, get(webApp.getUrl() + "/index.html").statusCode());

    webApp.stop();
    waitForTermination(launch);
    assertFalse(WebbyPlugin.getDefault().getWebAppRegistry().getWebApps().contains(webApp),
        "web app is unregistered");
  }

  @Test
  void failsWithoutContainerHome() throws Exception {
    ILaunchConfigurationWorkingCopy config = createConfiguration("tomcat11x", tmp.resolve("missing").toString(),
        findFreePorts());
    config.setAttribute(WebbyLaunchConstants.ATTR_CONTAINER_TIMEOUT, 10);
    assertThrows(CoreException.class, () -> launch = config.launch(ILaunchManager.RUN_MODE,
        new NullProgressMonitor()));
  }

  @Test
  void failsForNonMavenProject() throws Exception {
    ILaunchConfigurationWorkingCopy config = createConfiguration("tomcat11x", tmp.toString(), findFreePorts());
    config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, "does-not-exist");
    assertThrows(CoreException.class, () -> launch = config.launch(ILaunchManager.RUN_MODE,
        new NullProgressMonitor()));
  }

  private static HttpResponse<String> get(String url) throws Exception {
    HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(30)).build();
    long deadline = System.currentTimeMillis() + Duration.ofSeconds(60).toMillis();
    while (true) {
      try {
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 503 || System.currentTimeMillis() > deadline) {
          return response;
        }
      } catch (IOException e) {
        if (System.currentTimeMillis() > deadline) {
          throw e;
        }
      }
      Thread.sleep(500);
    }
  }

  private static void waitForTermination(ILaunch launch) throws Exception {
    long deadline = System.currentTimeMillis() + SHUTDOWN_TIMEOUT.toMillis();
    while (!launch.isTerminated() && System.currentTimeMillis() < deadline) {
      Thread.sleep(200);
    }
    assertTrue(launch.isTerminated(), "container stopped");
    // the registry is notified asynchronously by the launch manager
    deadline = System.currentTimeMillis() + 10_000;
    while (System.currentTimeMillis() < deadline && WebbyPlugin.getDefault().getWebAppRegistry().getWebApps()
        .stream().anyMatch(app -> app.getLaunch() == launch)) {
      Thread.sleep(100);
    }
  }

  /**
   * @return a free port followed by two free ports, Webby uses them for AJP and RMI
   */
  static int findFreePorts() throws IOException {
    for (int attempt = 0; attempt < 20; attempt++) {
      int port;
      try (ServerSocket socket = new ServerSocket(0)) {
        port = socket.getLocalPort();
      }
      if (port + 2 <= 65535 && isFree(port + 1) && isFree(port + 2)) {
        return port;
      }
    }
    throw new IOException("No free ports");
  }

  private static boolean isFree(int port) {
    try (ServerSocket socket = new ServerSocket(port)) {
      return true;
    } catch (IOException e) {
      return false;
    }
  }

}
