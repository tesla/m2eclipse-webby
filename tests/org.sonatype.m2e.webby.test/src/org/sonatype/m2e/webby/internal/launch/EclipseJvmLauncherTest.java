package org.sonatype.m2e.webby.internal.launch;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.codehaus.cargo.container.spi.jvm.DefaultJvmLauncher;
import org.codehaus.cargo.container.spi.jvm.JvmLauncherException;
import org.codehaus.cargo.container.spi.jvm.JvmLauncherRequest;
import org.codehaus.cargo.util.log.LoggedObject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.Launch;
import org.eclipse.jdt.launching.IVMRunner;
import org.eclipse.jdt.launching.VMRunnerConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EclipseJvmLauncherTest {

  private static class RecordingRunner implements IVMRunner {

    VMRunnerConfiguration config;

    ILaunch launch;

    boolean fail;

    @Override
    public void run(VMRunnerConfiguration configuration, ILaunch launch, IProgressMonitor monitor)
        throws CoreException {
      if (fail) {
        throw new CoreException(Status.error("expected by test"));
      }
      this.config = configuration;
      this.launch = launch;
    }

  }

  private final RecordingRunner runner = new RecordingRunner();

  private final ILaunch launch = new Launch(null, "run", null);

  private EclipseJvmLauncher launcher(File workDir, String[] env) {
    return new EclipseJvmLauncher(runner, launch, workDir, env, Map.of("key", "value"), new NullProgressMonitor());
  }

  @Test
  void runsMainClass(@TempDir Path tmp) throws JvmLauncherException {
    File workDir = tmp.resolve("work").toFile();
    EclipseJvmLauncher launcher = launcher(workDir, new String[] { "A=1", "B=x=y", "invalid", null });
    launcher.setJvm("ignored");
    launcher.setMainClass("org.apache.catalina.startup.Bootstrap");
    launcher.addClasspathEntries("a.jar", "b.jar");
    launcher.addClasspathEntries(new File("c.jar"));
    launcher.addClasspathEntries((String[]) null);
    launcher.addClasspathEntries((File[]) null);
    launcher.addJvmArguments("-Xmx1g");
    launcher.addJvmArguments((String[]) null);
    launcher.addJvmArgument(new File("agent.jar"));
    launcher.addJvmArgument(null);
    launcher.addJvmArgumentLine("-Da=1 \"-Db=with space\"");
    launcher.addJvmArgumentLine(null);
    launcher.setSystemProperty("catalina.home", "/opt/tomcat");
    launcher.setSystemProperty("empty", null);
    launcher.setSystemProperty("", "ignored");
    launcher.addAppArguments("start");
    launcher.addAppArguments((String[]) null);
    launcher.addAppArgument(new File("conf.xml"));
    launcher.addAppArgument(null);
    launcher.addAppArgumentLine("-x \"y z\"");
    launcher.addAppArgumentLine(null);
    launcher.setOutputFile(new File("ignored.log"));
    launcher.setOutputLogger(null, null);
    launcher.setAppendOutput(true);
    launcher.setTimeout(1000);
    launcher.setSpawn(true);
    launcher.setWorkingDirectory(null);

    assertEquals("a.jar" + File.pathSeparator + "b.jar" + File.pathSeparator + new File("c.jar").getAbsolutePath(),
        launcher.getClasspath());
    assertEquals("1", launcher.getEnvironmentVariable("A"));
    assertEquals("x=y", launcher.getEnvironmentVariable("B"));
    assertNull(launcher.getEnvironmentVariable("invalid"));
    launcher.setEnvironmentVariable("A", "2");
    launcher.setEnvironmentVariable("C", null);
    launcher.setEnvironmentVariable(null, "ignored");
    assertTrue(launcher.getCommandLine().contains("org.apache.catalina.startup.Bootstrap"));

    launcher.start();

    VMRunnerConfiguration config = runner.config;
    assertNotNull(config);
    assertSame(launch, runner.launch);
    assertEquals("org.apache.catalina.startup.Bootstrap", config.getClassToLaunch());
    assertArrayEquals(new String[] { "a.jar", "b.jar", new File("c.jar").getAbsolutePath() }, config.getClassPath());
    assertEquals(List.of("-Xmx1g", new File("agent.jar").getAbsolutePath(), "-Da=1", "-Db=with space",
        "-Dcatalina.home=/opt/tomcat", "-Dempty="), List.of(config.getVMArguments()));
    assertEquals(List.of("start", new File("conf.xml").getAbsolutePath(), "-x", "y z"),
        List.of(config.getProgramArguments()));
    assertEquals(List.of("A=2", "B=x=y", "C="), List.of(config.getEnvironment()));
    assertEquals(workDir.getAbsolutePath(), config.getWorkingDirectory());
    assertEquals(Map.of("key", "value"), config.getVMSpecificAttributesMap());
    assertTrue(workDir.isDirectory(), "working directory is created");
  }

  @Test
  void runsJarFile() throws JvmLauncherException {
    EclipseJvmLauncher launcher = launcher(null, null);
    assertNull(launcher.getClasspath(), "Cargo expects null for an empty classpath");
    launcher.setJarFile(new File("start.jar"));
    launcher.addAppArguments("--module=http");

    VMRunnerConfiguration config = launcher.createVMRunnerConfiguration();
    assertEquals("-jar", config.getClassToLaunch());
    assertEquals(List.of(new File("start.jar").getAbsolutePath(), "--module=http"),
        List.of(config.getProgramArguments()));
    assertNull(config.getEnvironment(), "environment of Eclipse is inherited");
    assertNull(config.getWorkingDirectory());
    assertTrue(launcher.getCommandLine().contains("-jar " + new File("start.jar")));
  }

  @Test
  void inheritsEnvironmentWhenNoneIsConfigured() throws JvmLauncherException {
    EclipseJvmLauncher launcher = launcher(null, null);
    assertEquals(System.getenv("PATH"), launcher.getEnvironmentVariable("PATH"));

    launcher.setEnvironmentVariable("WEBBY_TEST", "yes");
    launcher.setMainClass("Main");
    List<String> env = List.of(launcher.createVMRunnerConfiguration().getEnvironment());
    assertTrue(env.contains("WEBBY_TEST=yes"));
    if (System.getenv("PATH") != null) {
      assertTrue(env.contains("PATH=" + System.getenv("PATH")));
    }
  }

  @Test
  void requiresMainClassOrJar() {
    EclipseJvmLauncher launcher = launcher(null, null);
    assertThrows(JvmLauncherException.class, launcher::start);
    launcher.setMainClass("");
    assertThrows(JvmLauncherException.class, launcher::start);
    assertThrows(JvmLauncherException.class, launcher::execute);
  }

  @Test
  void reportsRunnerFailures() {
    runner.fail = true;
    EclipseJvmLauncher launcher = launcher(null, null);
    launcher.setMainClass("Main");
    JvmLauncherException e = assertThrows(JvmLauncherException.class, launcher::start);
    assertInstanceOf(CoreException.class, e.getCause());
  }

  @Test
  void killTerminatesLaunch() {
    boolean[] terminated = { false };
    ILaunch terminable = new Launch(null, "run", null) {
      @Override
      public void terminate() {
        terminated[0] = true;
      }
    };
    new EclipseJvmLauncher(runner, terminable, null, null, null, null).kill();
    assertTrue(terminated[0]);
  }

  @Test
  void factoryRunsOnlyServerInEclipse() {
    EclipseJvmLauncherFactory factory = new EclipseJvmLauncherFactory(runner, launch, null, null, Map.of(),
        new NullProgressMonitor());
    assertInstanceOf(EclipseJvmLauncher.class, factory.createJvmLauncher(new JvmLauncherRequest(true, new LoggedObject())));
    assertInstanceOf(DefaultJvmLauncher.class, factory.createJvmLauncher(new JvmLauncherRequest(false, new LoggedObject())));
  }

}
