package org.sonatype.m2e.webby.internal.launch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sonatype.m2e.webby.internal.config.WarConfiguration;

class WebbyLaunchDelegateTest {

  private ILaunchConfigurationWorkingCopy config;

  @BeforeEach
  void createConfiguration() throws CoreException {
    ILaunchConfigurationType type = DebugPlugin.getDefault().getLaunchManager()
        .getLaunchConfigurationType(WebbyLaunchConstants.TYPE_ID);
    config = type.newInstance(null, "delegate-test");
  }

  @Test
  void readsIntegerAttributes() throws CoreException {
    assertEquals(8080, WebbyLaunchDelegate.getPort(config));
    config.setAttribute(WebbyLaunchConstants.ATTR_CONTAINER_PORT, 9090);
    assertEquals(9090, WebbyLaunchDelegate.getPort(config));
    // Webby 0.2 stored the port as a string
    config.setAttribute(WebbyLaunchConstants.ATTR_CONTAINER_PORT, " 9091 ");
    assertEquals(9091, WebbyLaunchDelegate.getPort(config));
    config.setAttribute(WebbyLaunchConstants.ATTR_CONTAINER_PORT, "invalid");
    assertEquals(8080, WebbyLaunchDelegate.getPort(config));
    config.setAttribute(WebbyLaunchConstants.ATTR_CONTAINER_PORT, true);
    assertEquals(8080, WebbyLaunchDelegate.getPort(config));
  }

  @Test
  void createsCargoConfiguration(@TempDir Path tmp) throws CoreException {
    MavenProject project = new MavenProject(new Model());
    project.setArtifactId("my-app");
    WarConfiguration warConfig = new WarConfiguration();
    warConfig.setWorkDirectory(tmp.toString());
    WarClasspath classpath = new WarClasspath();
    classpath.addRuntimeClasspathEntry(tmp.resolve("classes").toFile());
    classpath.addProvidedClasspathEntry(tmp.resolve("servlet-api.jar").toFile());

    CargoConfiguration cargo = new WebbyLaunchDelegate().createCargoConfiguration(config, project, warConfig,
        classpath);
    assertEquals("my-app", cargo.getContextName(), "defaults to the artifact id");
    assertEquals(WebbyLaunchConstants.DEFAULT_CONTAINER_ID, cargo.getContainerId());
    assertEquals("8080", cargo.getPort());
    assertEquals(60_000L, cargo.getTimeout());
    assertEquals("medium", cargo.getLogLevel());
    assertEquals(tmp.toFile(), cargo.getWorkDirectory());
    assertEquals(tmp.resolve("war").toFile(), cargo.getWarDirectory());
    assertEquals(tmp.resolve("container").toFile().getAbsolutePath(), cargo.getConfigHome());
    assertEquals("installed", cargo.getContainerType().getType());
    assertEquals("standalone", cargo.getConfigType().getType());
    assertEquals(1, cargo.getRuntimeClasspath().length);
    assertEquals(1, cargo.getProvidedClasspath().length);

    config.setAttribute(WebbyLaunchConstants.ATTR_CONTEXT_NAME, "/ctx");
    config.setAttribute(WebbyLaunchConstants.ATTR_CONTAINER_ID, "jetty12x");
    config.setAttribute(WebbyLaunchConstants.ATTR_CONTAINER_HOME, "${system_property:java.io.tmpdir}/jetty");
    config.setAttribute(WebbyLaunchConstants.ATTR_CONTAINER_PORT, 9000);
    config.setAttribute(WebbyLaunchConstants.ATTR_CONTAINER_TIMEOUT, 5);
    config.setAttribute(WebbyLaunchConstants.ATTR_LOG_LEVEL, "high");
    cargo = new WebbyLaunchDelegate().createCargoConfiguration(config, project, warConfig, classpath);
    assertEquals("ctx", cargo.getContextName());
    assertEquals("jetty12x", cargo.getContainerId());
    assertEquals(System.getProperty("java.io.tmpdir") + "/jetty", cargo.getContainerHome(),
        "variables are expanded");
    assertEquals("9000", cargo.getPort());
    assertEquals(5_000L, cargo.getTimeout());
    assertEquals("high", cargo.getLogLevel());
  }

  @Test
  void mergesSystemPropertiesFiles(@TempDir Path tmp) throws IOException, CoreException {
    Path first = tmp.resolve("first.properties");
    Files.writeString(first, "a=1\nb=2\n");
    Path second = tmp.resolve("second.properties");
    Files.writeString(second, "b=3\nc=with space\n");

    Properties props = WebbyLaunchDelegate.loadSystemProperties(
        first + "\r\n\n   \n" + second + "\n" + tmp.resolve("missing.properties"));
    assertEquals("1", props.getProperty("a"));
    assertEquals("3", props.getProperty("b"), "latter files take precedence");
    assertEquals("with space", props.getProperty("c"));
    assertEquals(3, props.size());

    assertTrue(WebbyLaunchDelegate.loadSystemProperties(null).isEmpty());
  }

  @Test
  void reportsUnreadablePropertiesFile(@TempDir Path tmp) throws IOException {
    Path invalid = tmp.resolve("invalid.properties");
    Files.writeString(invalid, "a=\\u12");
    assertThrows(CoreException.class, () -> WebbyLaunchDelegate.loadSystemProperties(invalid.toString()));
  }

  @Test
  void quotesSystemPropertiesWithSpaces() {
    Properties props = new Properties();
    props.setProperty("plain", "value");
    StringBuilder args = new StringBuilder("-Xmx1g");
    WebbyLaunchDelegate.appendSystemProperties(args, props);
    assertEquals("-Xmx1g -Dplain=value", args.toString());

    props.clear();
    props.setProperty("spaced", "a value");
    args = new StringBuilder();
    WebbyLaunchDelegate.appendSystemProperties(args, props);
    assertEquals(" \"-Dspaced=a value\"", args.toString());
  }

  @Test
  void computesVmArguments(@TempDir Path tmp) throws IOException, CoreException {
    Path props = tmp.resolve("sys.properties");
    Files.writeString(props, "webby.flag=on\n");
    config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, "-Xmx512m");
    config.setAttribute(WebbyLaunchConstants.ATTR_SYS_PROP_FILES, props.toString());
    assertEquals("-Xmx512m -Dwebby.flag=on", new WebbyLaunchDelegate().getVmArgs(config));
  }

  @Test
  void computesPortsAndClasspath() {
    assertEquals("8081", WebbyLaunchDelegate.nextPort("8080"));
    String[] classpath = WebbyLaunchDelegate.toClasspath(List.of(new File("a/b"), new File("c.jar")));
    assertEquals(new File("a/b").getAbsolutePath(), classpath[0]);
    assertEquals(new File("c.jar").getAbsolutePath(), classpath[1]);
  }

}
