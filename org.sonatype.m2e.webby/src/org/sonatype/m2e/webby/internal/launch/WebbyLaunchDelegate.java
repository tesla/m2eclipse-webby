/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.sonatype.m2e.webby.internal.launch;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.codehaus.cargo.container.Container;
import org.codehaus.cargo.container.ContainerType;
import org.codehaus.cargo.container.InstalledLocalContainer;
import org.codehaus.cargo.container.configuration.Configuration;
import org.codehaus.cargo.container.configuration.ConfigurationType;
import org.codehaus.cargo.container.configuration.LocalConfiguration;
import org.codehaus.cargo.container.deployable.DeployableType;
import org.codehaus.cargo.container.deployable.WAR;
import org.codehaus.cargo.container.jetty.JettyPropertySet;
import org.codehaus.cargo.container.property.GeneralPropertySet;
import org.codehaus.cargo.container.property.ServletPropertySet;
import org.codehaus.cargo.container.tomcat.TomcatPropertySet;
import org.codehaus.cargo.generic.ContainerFactory;
import org.codehaus.cargo.generic.DefaultContainerFactory;
import org.codehaus.cargo.generic.configuration.ConfigurationFactory;
import org.codehaus.cargo.generic.configuration.DefaultConfigurationFactory;
import org.codehaus.cargo.generic.deployable.DefaultDeployableFactory;
import org.codehaus.cargo.generic.deployable.DeployableFactory;
import org.codehaus.cargo.util.CargoException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMRunner;
import org.eclipse.jdt.launching.JavaLaunchDelegate;
import org.eclipse.jdt.launching.SocketUtil;
import org.eclipse.jdt.launching.VMRunnerConfiguration;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.sonatype.m2e.webby.internal.IWebApp;
import org.sonatype.m2e.webby.internal.WebbyPlugin;
import org.sonatype.m2e.webby.internal.config.WarConfiguration;
import org.sonatype.m2e.webby.internal.config.WarConfigurationExtractor;
import org.sonatype.m2e.webby.internal.launch.boot.EmbeddedServerBooter;
import org.sonatype.m2e.webby.internal.launch.ui.CargoConsoleLogger;
import org.sonatype.m2e.webby.internal.launch.ui.ConsoleManager;
import org.sonatype.m2e.webby.internal.util.MavenUtils;



/**
 *
 */
public class WebbyLaunchDelegate extends JavaLaunchDelegate {

  @Override
  public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor)
      throws CoreException {
    SubMonitor pm = SubMonitor.convert(monitor, 100);
    try {
      IJavaProject javaProject = verifyJavaProject(configuration);

      IMavenProjectFacade mvnFacade = MavenUtils.getFacade(javaProject.getProject());

      MavenProject mvnProject = mvnFacade.getMavenProject(pm.newChild(10));

      IMaven mvn = MavenPlugin.getMaven();
      MavenExecutionRequest mvnRequest = mvn.createExecutionRequest(pm.newChild(10));
      MavenSession mvnSession = mvn.createSession(mvnRequest, mvnProject);

      WarConfiguration warConfig = new WarConfigurationExtractor().getConfiguration(mvnFacade, mvnProject, mvnSession,
          pm.newChild(10));

      WarClasspath warClasspath = new WarClasspath();

      new WarClasspathPopulator().populate(warClasspath, mvnProject, warConfig, true, pm.newChild(30));

      File workDir = new File(warConfig.getWorkDirectory());

      CargoConfiguration cargo = new CargoConfiguration();

      cargo.setWorkDirectory(workDir);
      cargo.setWarDirectory(new File(warConfig.getWarDirectory()));
      cargo.setRuntimeClasspath(toClasspath(warClasspath.getRuntimeClasspath()));
      cargo.setProvidedClasspath(toClasspath(warClasspath.getProvidedClasspath()));
      cargo.setContextName(configuration.getAttribute(WebbyLaunchConstants.ATTR_CONTEXT_NAME, ""));
      if(cargo.getContextName().length() <= 0) {
        cargo.setContextName(mvnProject.getArtifactId());
      }
      cargo.setContainerId(configuration.getAttribute(WebbyLaunchConstants.ATTR_CONTAINER_ID, "jetty7x"));
      cargo.setContainerType(ContainerType.toType(configuration.getAttribute(WebbyLaunchConstants.ATTR_CONTAINER_TYPE,
          "embedded")));
      cargo.setContainerHome(configuration.getAttribute(WebbyLaunchConstants.ATTR_CONTAINER_HOME, ""));
      cargo.setContainerHome(expandVariables(cargo.getContainerHome()));
      cargo.setConfigHome(new File(workDir, "container").getAbsolutePath());
      cargo.setConfigType(ConfigurationType.STANDALONE);
      cargo.setLogLevel(configuration.getAttribute(WebbyLaunchConstants.ATTR_LOG_LEVEL, "medium"));
      try {
        cargo.setPort(Integer.toString(configuration.getAttribute(WebbyLaunchConstants.ATTR_CONTAINER_PORT, 8080)));
      } catch(CoreException e) {
        cargo.setPort(configuration.getAttribute(WebbyLaunchConstants.ATTR_CONTAINER_PORT, "8080"));
      }

      if(!cargo.getWarDirectory().exists()) {
        throw WebbyPlugin.newError("WAR base directory " + cargo.getWarDirectory()
            + " does not exist, please ensure your workspace is refreshed and has been built", null);
      }

      MessageConsole console = ConsoleManager.getConsole();
      console.clearConsole();

      MessageConsoleStream mcs = console.newMessageStream();
      mcs.println("Runtime classpath:");
      for(File file : warClasspath.getRuntimeClasspath()) {
        mcs.println("  " + file);
      }
      mcs.println("Provided classpath:");
      for(File file : warClasspath.getProvidedClasspath()) {
        mcs.println("  " + file);
      }
      try {
        mcs.close();
      } catch(IOException e) {
        WebbyPlugin.log(e);
      }

      IWebApp webApp;

      if(ContainerType.EMBEDDED.equals(cargo.getContainerType())) {
        webApp = launchEmbedded(cargo, configuration, mode, launch, pm.newChild(40));
      } else {
        webApp = launchInstalled(cargo, configuration, mode, launch, pm.newChild(40));
      }

      WebbyPlugin.getDefault().getWebAppRegistry().addWebApp(webApp);
    } finally {
      if(monitor != null) {
        monitor.done();
      }
    }
  }

  private String getVmArgs(ILaunchConfiguration config) throws CoreException {
    StringBuilder args = new StringBuilder(1024);
    args.append(getVMArguments(config));

    String sysPropFiles = config.getAttribute(WebbyLaunchConstants.ATTR_SYS_PROP_FILES, "");
    sysPropFiles = expandVariables(sysPropFiles);

    Properties sysProps = loadSystemProperties(sysPropFiles);

    for(Map.Entry<?, ?> entry : sysProps.entrySet()) {
      String key = entry.getKey().toString();
      String val = entry.getValue().toString();
      if(key.indexOf(' ') < 0 && val.indexOf(' ') < 0) {
        args.append(" -D").append(key).append('=').append(val);
      } else {
        args.append(" \"-D").append(key).append('=').append(val).append("\"");
      }
    }

    return args.toString();
  }

  private Properties loadSystemProperties(String sysPropFiles) throws CoreException {
    Properties sysProps = new Properties();

    if(sysPropFiles != null) {
      String[] lines = sysPropFiles.split("[\r\n]+");
      for(String line : lines) {
        line = line.trim();
        if(line.length() <= 0) {
          continue;
        }
        File propFile = new File(line).getAbsoluteFile();
        if(propFile.isFile()) {
          try {
            FileInputStream is = new FileInputStream(propFile);
            try {
              sysProps.load(is);
            } finally {
              is.close();
            }
          } catch(IOException e) {
            throw WebbyPlugin.newError("Failed to read system properties from " + propFile, e);
          }
        } else {
          WebbyPlugin.log("Ignoring non-existent properties file " + propFile, IStatus.WARNING);
        }
      }
    }

    return sysProps;
  }

  private IWebApp launchInstalled(CargoConfiguration cargo, ILaunchConfiguration configuration, String mode,
      ILaunch launch, IProgressMonitor monitor) throws CoreException {
    IVMInstall vm = verifyVMInstall(configuration);
    IVMRunner runner = getVMRunner(configuration, mode);

    String[] envVariables = getEnvironment(configuration);

    File javaHome = vm.getInstallLocation();
    if(new File(javaHome, "jre").isDirectory()) {
      javaHome = new File(javaHome, "jre");
    }

    CargoConsoleLogger logger = new CargoConsoleLogger(ConsoleManager.getConsole());
    try {
      ConfigurationFactory configFactory = new DefaultConfigurationFactory();
      Configuration config = configFactory.createConfiguration(cargo.getContainerId(), cargo.getContainerType(),
          cargo.getConfigType(), cargo.getConfigHome());
      config.setProperty(GeneralPropertySet.LOGGING, cargo.getLogLevel());
      config.setProperty(GeneralPropertySet.JAVA_HOME, javaHome.getAbsolutePath());
      config.setProperty(GeneralPropertySet.JVMARGS, getVmArgs(configuration));
      config.setProperty(ServletPropertySet.PORT, cargo.getPort());
      config.setProperty(JettyPropertySet.USE_FILE_MAPPED_BUFFER, "false");

      String portAJP = getNextPort(cargo.getPort());
      String portRMI = getNextPort(portAJP);
      config.setProperty(TomcatPropertySet.AJP_PORT, portAJP);
      config.setProperty(GeneralPropertySet.RMI_PORT, portRMI);

      DeployableFactory depFactory = new DefaultDeployableFactory();
      WAR dep = (WAR) depFactory.createDeployable(cargo.getContainerId(), cargo.getWarDirectory().getAbsolutePath(),
          DeployableType.WAR);
      if(cargo.getContextName().length() > 0) {
        dep.setContext(cargo.getContextName());
      }
      dep.setExtraClasspath(cargo.getRuntimeClasspath());

      LocalConfiguration localConfig = (LocalConfiguration) config;
      localConfig.addDeployable(dep);

      ContainerFactory containerFactory = new DefaultContainerFactory();
      Container container = containerFactory.createContainer(cargo.getContainerId(), cargo.getContainerType(), config);

      EclipseJvmLauncherFactory jvmLauncherFactory = new EclipseJvmLauncherFactory(runner, launch,
          cargo.getWorkDirectory(), envVariables, monitor);

      InstalledLocalContainer localContainer = (InstalledLocalContainer) container;
      localContainer.setLogger(logger);
      localContainer.setHome(cargo.getContainerHome());
      localContainer.setJvmLauncherFactory(jvmLauncherFactory);
      localContainer.setTimeout(60 * 1000);
      localContainer.start();

      return new InstalledContainerWebApp(launch, cargo, localContainer);
    } catch(CargoException e) {
      throw WebbyPlugin.newError("Failed to start container", e);
    }
  }

  private static String getNextPort(String port) {
    int portToInt = Integer.parseInt(port);
    portToInt++;
    return Integer.toString(portToInt);
  }

  private IWebApp launchEmbedded(CargoConfiguration cargo, ILaunchConfiguration configuration, String mode,
      ILaunch launch, IProgressMonitor monitor) throws CoreException {
    StringBuilder buffer = new StringBuilder(1024);
    buffer.append(".");
    for(String path : cargo.getRuntimeClasspath()) {
      buffer.append(File.pathSeparator).append(path);
    }

    int controlPort = SocketUtil.findFreePort();

    VMRunnerConfiguration config = new VMRunnerConfiguration(EmbeddedServerBooter.class.getName(),
        getEmbeddeClasspath(cargo.getContainerId()));
    if(cargo.getWorkDirectory() != null) {
      config.setWorkingDirectory(cargo.getWorkDirectory().getAbsolutePath());
    }
    config.setEnvironment(getEnvironment(configuration));
    config.setVMArguments(DebugPlugin.parseArguments(getVmArgs(configuration)));
    config.setProgramArguments(new String[] {Integer.toString(controlPort), cargo.getContainerId(),
        cargo.getContainerType().getType(), cargo.getConfigHome(), cargo.getConfigType().getType(), cargo.getPort(),
        cargo.getLogLevel(), cargo.getWarDirectory().getAbsolutePath(), buffer.toString(), cargo.getContextName()});

    IVMRunner runner = getVMRunner(configuration, mode);
    runner.run(config, launch, monitor);

    return new EmbeddedContainerWebApp(launch, cargo, controlPort);
  }

  private String[] getEmbeddeClasspath(String containerId) throws CoreException {
    List<String> paths = new ArrayList<String>();
    try {
      File webbyJar = FileLocator.getBundleFile(WebbyPlugin.getDefault().getBundle());
      if(webbyJar.isDirectory()) {
        File classesDir = new File(webbyJar, "target/classes");
        if(classesDir.isDirectory()) {
          webbyJar = classesDir;
        }
      }
      paths.add(webbyJar.getAbsolutePath());

      addJars(paths, "jars/cargo");

      addJars(paths, "jars/" + containerId);
    } catch(Exception e) {
      throw WebbyPlugin.newError("Failed to create container classpath", e);
    }
    return paths.toArray(new String[paths.size()]);
  }

  @SuppressWarnings("unchecked")
  private void addJars(List<String> paths, String basePath) throws Exception {
    for(URL url : Collections.list((Enumeration<URL>) WebbyPlugin.getDefault().getBundle()
        .findEntries(basePath, "*.jar", true))) {
      url = FileLocator.toFileURL(url);
      paths.add(toFile(url).getAbsolutePath());
    }
  }

  private File toFile(URL url) {
    try {
      return new File(url.toURI());
    } catch(URISyntaxException e) {
      // seen even in Eclipse 3.7 that FileLocator returns URLs with spaces so try harder
      try {
        return new File(new URI("file", null, url.getPath(), null));
      } catch(URISyntaxException e1) {
        return new File(url.getPath());
      }
    }
  }

  private String[] toClasspath(Collection<File> files) {
    String[] classpath = new String[files.size()];
    int i = 0;
    for(File file : files) {
      classpath[i] = file.getAbsolutePath();
      i++ ;
    }
    return classpath;
  }

  private String expandVariables(String str) throws CoreException {
    return VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(str);
  }

}
