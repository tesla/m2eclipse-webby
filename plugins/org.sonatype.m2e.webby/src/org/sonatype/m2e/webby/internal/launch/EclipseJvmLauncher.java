package org.sonatype.m2e.webby.internal.launch;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.tools.ant.types.Commandline;
import org.codehaus.cargo.container.spi.jvm.JvmLauncher;
import org.codehaus.cargo.container.spi.jvm.JvmLauncherException;
import org.codehaus.cargo.util.log.Logger;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.jdt.launching.IVMRunner;
import org.eclipse.jdt.launching.VMRunnerConfiguration;

/**
 * Starts the container JVM through the JDT VM runner, so that it is attached to the Eclipse launch (console, debugger,
 * hot code replace).
 */
public class EclipseJvmLauncher implements JvmLauncher {

  private final IVMRunner runner;

  private final ILaunch launch;

  private final IProgressMonitor monitor;

  private File workingDirectory;

  /** Environment variables by name, {@code null} to inherit the environment of Eclipse. */
  private Map<String, String> environment;

  private final List<String> jvmArguments = new ArrayList<>();

  private final Map<String, String> sysProperties = new LinkedHashMap<>();

  private final List<String> appArguments = new ArrayList<>();

  private final List<String> classpath = new ArrayList<>();

  private File jarFile;

  private String mainClass;

  private final Map<String, Object> vmSpecificAttributesMap;

  /**
   * @param envVariables the environment, each element having the format <i>name</i>=<i>value</i>, may be {@code null}
   */
  public EclipseJvmLauncher(IVMRunner runner, ILaunch launch, File workingDirectory, String[] envVariables,
      Map<String, Object> vmSpecificAttributesMap, IProgressMonitor monitor) {
    this.runner = runner;
    this.launch = launch;
    this.monitor = monitor;
    this.workingDirectory = workingDirectory;
    if (envVariables != null) {
      environment = new LinkedHashMap<>();
      for (String envVariable : envVariables) {
        if (envVariable == null) {
          continue;
        }
        int eq = envVariable.indexOf('=');
        if (eq > 0) {
          environment.put(envVariable.substring(0, eq), envVariable.substring(eq + 1));
        }
      }
    }
    this.vmSpecificAttributesMap = vmSpecificAttributesMap;
  }

  @Override
  public void setWorkingDirectory(File workingDirectory) {
    if (workingDirectory != null) {
      this.workingDirectory = workingDirectory;
    }
  }

  @Override
  public void setJvm(String command) {
    // ignored, JVM is controlled by launch configuration and already selected by VM runner instance
  }

  @Override
  public void addJvmArgument(File file) {
    if (file != null) {
      jvmArguments.add(file.getAbsolutePath());
    }
  }

  @Override
  public void addJvmArguments(String... values) {
    if (values != null) {
      Collections.addAll(jvmArguments, values);
    }
  }

  @Override
  public void addJvmArgumentLine(String line) {
    if (line != null) {
      Collections.addAll(jvmArguments, Commandline.translateCommandline(line));
    }
  }

  @Override
  public void addClasspathEntries(String... paths) {
    if (paths != null) {
      Collections.addAll(classpath, paths);
    }
  }

  @Override
  public void addClasspathEntries(File... paths) {
    if (paths != null) {
      for (File path : paths) {
        classpath.add(path.getAbsolutePath());
      }
    }
  }

  /**
   * @return the classpath, {@code null} if empty like Cargo expects (Jetty would get an empty extra classpath)
   */
  @Override
  public String getClasspath() {
    return classpath.isEmpty() ? null : String.join(File.pathSeparator, classpath);
  }

  @Override
  public void setSystemProperty(String name, String value) {
    if (name != null && !name.isEmpty()) {
      sysProperties.put(name, value != null ? value : "");
    }
  }

  @Override
  public void setJarFile(File jarFile) {
    this.jarFile = jarFile;
  }

  @Override
  public void setMainClass(String mainClass) {
    this.mainClass = mainClass;
  }

  @Override
  public void addAppArgument(File file) {
    if (file != null) {
      appArguments.add(file.getAbsolutePath());
    }
  }

  @Override
  public void addAppArguments(String... values) {
    if (values != null) {
      Collections.addAll(appArguments, values);
    }
  }

  @Override
  public void addAppArgumentLine(String line) {
    if (line != null) {
      Collections.addAll(appArguments, Commandline.translateCommandline(line));
    }
  }

  @Override
  public void setOutputFile(File outputFile) {
    // ignored, output is handled by Eclipse console
  }

  @Override
  public void setOutputLogger(Logger outputLogger, String category) {
    // ignored, output is handled by Eclipse console
  }

  @Override
  public void setAppendOutput(boolean appendOutput) {
    // ignored, output is handled by Eclipse console
  }

  @Override
  public String getCommandLine() {
    return String.join(" ", createVMArguments()) + " " + (jarFile != null ? "-jar " + jarFile : mainClass) + " "
        + String.join(" ", appArguments);
  }

  @Override
  public void setTimeout(long millis) {
    // ignored, the container timeout applies
  }

  @Override
  public void setSpawn(boolean spawn) {
    // ignored, the container always runs as part of the Eclipse launch
  }

  @Override
  public String getEnvironmentVariable(String name) {
    if (environment == null) {
      return System.getenv(name);
    }
    return environment.get(name);
  }

  @Override
  public void setEnvironmentVariable(String name, String value) {
    if (name == null || name.isEmpty()) {
      return;
    }
    if (environment == null) {
      environment = new LinkedHashMap<>(System.getenv());
    }
    environment.put(name, value != null ? value : "");
  }

  VMRunnerConfiguration createVMRunnerConfiguration() throws JvmLauncherException {
    VMRunnerConfiguration config;
    if (jarFile != null) {
      /*
       * VMRunnerConfiguration does not really support invocations of the form "java -jar <file>". To workaround this,
       * we specify "-jar" as the main class and feed in the JAR file in the following app arguments. We must not use
       * the empty string for the main class as this gets interpreted as an empty/missing classname by the java launcher
       * on Unix-like platforms.
       */
      config = new VMRunnerConfiguration("-jar", classpath.toArray(String[]::new));
    } else if (mainClass != null && !mainClass.isEmpty()) {
      config = new VMRunnerConfiguration(mainClass, classpath.toArray(String[]::new));
    } else {
      throw new JvmLauncherException("neither main class nor JAR file have been specified");
    }
    config.setVMSpecificAttributesMap(vmSpecificAttributesMap);
    config.setEnvironment(toEnvironment(environment));
    config.setVMArguments(createVMArguments().toArray(String[]::new));
    config.setProgramArguments(createProgramArguments().toArray(String[]::new));
    if (workingDirectory != null) {
      config.setWorkingDirectory(workingDirectory.getAbsolutePath());
    }
    return config;
  }

  private List<String> createVMArguments() {
    List<String> args = new ArrayList<>(jvmArguments);
    for (Map.Entry<String, String> entry : sysProperties.entrySet()) {
      args.add("-D" + entry.getKey() + "=" + entry.getValue());
    }
    return args;
  }

  private List<String> createProgramArguments() {
    List<String> args = new ArrayList<>(appArguments.size() + 1);
    if (jarFile != null) {
      args.add(jarFile.getAbsolutePath());
    }
    args.addAll(appArguments);
    return args;
  }

  private static String[] toEnvironment(Map<String, String> environment) {
    if (environment == null) {
      return null;
    }
    Collection<String> env = new ArrayList<>(environment.size());
    environment.forEach((name, value) -> env.add(name + "=" + value));
    return env.toArray(String[]::new);
  }

  @Override
  public void start() throws JvmLauncherException {
    VMRunnerConfiguration config = createVMRunnerConfiguration();
    if (workingDirectory != null) {
      workingDirectory.mkdirs();
    }
    try {
      runner.run(config, launch, monitor);
    } catch (CoreException e) {
      throw new JvmLauncherException(e.getMessage(), e);
    }
  }

  @Override
  public int execute() throws JvmLauncherException {
    throw new JvmLauncherException("Synchronous execution is not supported, the container runs as an Eclipse launch");
  }

  @Override
  public void kill() {
    try {
      launch.terminate();
    } catch (Exception e) {
      // best effort, the process might already be gone
    }
  }

}
