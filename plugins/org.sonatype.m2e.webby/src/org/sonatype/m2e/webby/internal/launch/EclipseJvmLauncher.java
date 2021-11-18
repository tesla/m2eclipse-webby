/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.sonatype.m2e.webby.internal.launch;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.tools.ant.types.Commandline;
import org.codehaus.cargo.container.spi.jvm.JvmLauncher;
import org.codehaus.cargo.container.spi.jvm.JvmLauncherException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.jdt.launching.IVMRunner;
import org.eclipse.jdt.launching.VMRunnerConfiguration;


/**
 */
public class EclipseJvmLauncher implements JvmLauncher {

  private final IVMRunner runner;

  private final ILaunch launch;

  private final IProgressMonitor monitor;

  private File workingDirectory;

  /** format of each element : <i>name</i>=<i>value</i> */
  private List<String> envVariables;

  private List<String> jvmArguments = new ArrayList<String>();

  private Map<String, String> sysProperties = new HashMap<String, String>();

  private List<String> appArguments = new ArrayList<String>();

  private List<String> classpath = new ArrayList<String>();

  private File jarFile;

  private String mainClass;

  public EclipseJvmLauncher(IVMRunner runner, ILaunch launch, File workingDirectory, String[] envVariables,
      IProgressMonitor monitor) {
    this.runner = runner;
    this.launch = launch;
    this.monitor = monitor;
    this.workingDirectory = workingDirectory;
    if(envVariables != null) {
      this.envVariables = new ArrayList<String>(envVariables.length);
      Collections.addAll(this.envVariables, envVariables);
    }
  }

  public void setWorkingDirectory(File workingDirectory) {
    if(workingDirectory != null) {
      this.workingDirectory = workingDirectory;
    }
  }

  public void setJvm(String command) {
    // ignored, JVM is controlled by launch configuration and already selected by VM runner instance
  }

  public void addJvmArgument(File file) {
    if(file != null) {
      jvmArguments.add(file.getAbsolutePath());
    }
  }

  public void addJvmArguments(String... values) {
    if(values != null) {
      Collections.addAll(jvmArguments, values);
    }
  }

  public void addJvmArgumentLine(String line) {
    if(line != null) {
      Collections.addAll(jvmArguments, Commandline.translateCommandline(line));
    }
  }

  public void addClasspathEntries(String... paths) {
    if(paths != null) {
      Collections.addAll(classpath, paths);
    }
  }

  public void addClasspathEntries(File... paths) {
    if(paths != null) {
      for(File path : paths) {
        classpath.add(path.getAbsolutePath());
      }
    }
  }

  public String getClasspath() {
    StringBuilder buffer = new StringBuilder(1024);
    for(String path : classpath) {
      if(buffer.length() > 0) {
        buffer.append(File.pathSeparatorChar);
      }
      buffer.append(path);
    }
    return buffer.toString();
  }

  public void setSystemProperty(String name, String value) {
    if(name != null && name.length() > 0) {
      sysProperties.put(name, value != null ? value : "");
    }
  }

  public void setJarFile(File jarFile) {
    this.jarFile = jarFile;
  }

  public void setMainClass(String mainClass) {
    this.mainClass = mainClass;
  }

  public void addAppArgument(File file) {
    if(file != null) {
      appArguments.add(file.getAbsolutePath());
    }
  }

  public void addAppArguments(String... values) {
    if(values != null) {
      Collections.addAll(appArguments, values);
    }
  }

  public void addAppArgumentLine(String line) {
    if(line != null) {
      Collections.addAll(appArguments, Commandline.translateCommandline(line));
    }
  }

  public void setOutputFile(File outputFile) {
    // ignored, output is handled by Eclipse console
  }

  public void setAppendOutput(boolean appendOutput) {
    // ignored, output is handled by Eclipse console
  }

  public String getCommandLine() {
    return "";
  }

  public void setTimeout(long millis) {
    // ignored
  }

  public void start() throws JvmLauncherException {
    List<String> jvmArguments = new ArrayList<String>(this.jvmArguments);
    for(Map.Entry<String, String> entry : sysProperties.entrySet()) {
      jvmArguments.add("-D" + entry.getKey() + "=" + entry.getValue());
    }

    VMRunnerConfiguration config;
    if(jarFile != null) {
      /*
       * NOTE: VMRunnerConfiguration does not really support invocations of the form "java -jar <file>". To workaround
       * this, we specify "-jar" as the main class feed in the JAR file in the following app arguments. We must not use
       * the empty string for the main class as this gets interpreted as an empty/missing classname by the java launcher
       * on Unix-like platforms.
       */
      config = new VMRunnerConfiguration("-jar", toArray(classpath));
    } else if(mainClass != null && mainClass.length() > 0) {
      config = new VMRunnerConfiguration(mainClass, toArray(classpath));
    } else {
      throw new JvmLauncherException("neither main class nor JAR file have been specified");
    }

    config.setEnvironment(toArray(envVariables));
    config.setVMArguments(toArray(jvmArguments));
    config.setProgramArguments(prependJarFile(toArray(appArguments), jarFile));

    if(workingDirectory != null) {
      config.setWorkingDirectory(workingDirectory.getAbsolutePath());
      workingDirectory.mkdirs();
    }

    try {
      runner.run(config, launch, monitor);
    } catch(CoreException e) {
      throw new JvmLauncherException(e.getMessage(), e);
    }
  }

  public int execute() throws JvmLauncherException {
    throw new JvmLauncherException("not implemented");
  }

  private String[] prependJarFile(String[] appArgs, File jarFile) {
    if(jarFile == null) {
      return appArgs;
    }
    String[] result = new String[appArgs.length + 1];
    result[0] = jarFile.getAbsolutePath();
    System.arraycopy(appArgs, 0, result, 1, appArgs.length);
    return result;
  }

  private String[] toArray(Collection<String> coll) {
    if(coll == null) {
      return null;
    }
    return coll.toArray(new String[coll.size()]);
  }

  /**
   * From cargo 1.4.11, not implemented yet
   */
	public void kill() {
  }

	/**
   * From cargo 1.4.11, not implemented yet
   */
  public void setSpawn(boolean spawn) {
  }

  public String getEnvironmentVariable(String name) {
    for (String envVariable : envVariables) {
      if (envVariable != null) {
        String[] result = envVariable.split("=");
        if (result != null && result[0] != null && result[0].equals(name)) {
          return result[1];
        }
      }
    }
    return null;
  }

  public void setEnvironmentVariable(String name, String value) {
    if (name != null && name.length() > 0) {
      envVariables.add(name + "=" + value);
    }
  }

}
