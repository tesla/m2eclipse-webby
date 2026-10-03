package org.sonatype.m2e.webby.internal.launch;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.project.MavenProject;
import org.codehaus.cargo.container.ContainerType;
import org.codehaus.cargo.container.InstalledLocalContainer;
import org.codehaus.cargo.container.configuration.ConfigurationType;
import org.codehaus.cargo.container.configuration.LocalConfiguration;
import org.codehaus.cargo.container.deployable.DeployableType;
import org.codehaus.cargo.container.deployable.WAR;
import org.codehaus.cargo.container.jetty.JettyPropertySet;
import org.codehaus.cargo.container.property.GeneralPropertySet;
import org.codehaus.cargo.container.property.ServletPropertySet;
import org.codehaus.cargo.container.tomcat.TomcatPropertySet;
import org.codehaus.cargo.container.tomcat.internal.AbstractCatalinaStandaloneLocalConfiguration;
import org.codehaus.cargo.generic.DefaultContainerFactory;
import org.codehaus.cargo.generic.configuration.DefaultConfigurationFactory;
import org.codehaus.cargo.generic.deployable.DefaultDeployableFactory;
import org.codehaus.cargo.util.CargoException;
import org.codehaus.cargo.util.XmlReplacement.ReplacementBehavior;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMRunner;
import org.eclipse.jdt.launching.JavaLaunchDelegate;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.swt.program.Program;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.sonatype.m2e.webby.internal.IWebApp;
import org.sonatype.m2e.webby.internal.WebbyPlugin;
import org.sonatype.m2e.webby.internal.config.WarConfiguration;
import org.sonatype.m2e.webby.internal.config.WarConfigurationExtractor;
import org.sonatype.m2e.webby.internal.launch.ui.CargoConsoleLogger;
import org.sonatype.m2e.webby.internal.launch.ui.ConsoleManager;
import org.sonatype.m2e.webby.internal.util.MavenUtils;
import org.sonatype.m2e.webby.internal.util.PathSelector;

/**
 * Launches a WAR project in a servlet container installed locally, using Cargo.
 */
public class WebbyLaunchDelegate extends JavaLaunchDelegate {

  @Override
  public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor)
      throws CoreException {
    SubMonitor pm = SubMonitor.convert(monitor, 100);
    try {
      IJavaProject javaProject = verifyJavaProject(configuration);

      IMavenProjectFacade mvnFacade = MavenUtils.getFacade(javaProject.getProject());
      if (mvnFacade == null) {
        throw WebbyPlugin.newError("Project " + javaProject.getElementName() + " is not a Maven project", null);
      }

      MavenProject mvnProject = mvnFacade.getMavenProject(pm.split(10));

      WarConfiguration warConfig = new WarConfigurationExtractor().getConfiguration(mvnFacade, mvnProject,
          pm.split(10));

      WarClasspath warClasspath = new WarClasspath();
      new WarClasspathPopulator().populate(warClasspath, mvnProject, warConfig, true, pm.split(30));

      CargoConfiguration cargo = createCargoConfiguration(configuration, mvnProject, warConfig, warClasspath);

      if (!cargo.getWarDirectory().exists()) {
        throw WebbyPlugin.newError("WAR base directory " + cargo.getWarDirectory()
            + " does not exist, please ensure your workspace is refreshed and has been built", null);
      }

      printClasspath(warClasspath);

      IWebApp webApp = launchInstalled(cargo, configuration, mode, launch, pm.split(50));
      if (!launch.isTerminated()) {
        if (configuration.getAttribute(WebbyLaunchConstants.ATTR_OPEN_WHEN_STARTED,
            WebbyLaunchConstants.DEFAULT_OPEN_WHEN_STARTED)) {
          Program.launch(webApp.getUrl());
        }
        WebbyPlugin.getDefault().getWebAppRegistry().addWebApp(webApp);
      }
    } finally {
      if (monitor != null) {
        monitor.done();
      }
    }
  }

  CargoConfiguration createCargoConfiguration(ILaunchConfiguration configuration, MavenProject mvnProject,
      WarConfiguration warConfig, WarClasspath warClasspath) throws CoreException {
    File workDir = new File(warConfig.getWorkDirectory());

    CargoConfiguration cargo = new CargoConfiguration();
    cargo.setWorkDirectory(workDir);
    cargo.setWarDirectory(new File(warConfig.getWarDirectory()));
    cargo.setRuntimeClasspath(toClasspath(warClasspath.getRuntimeClasspath()));
    cargo.setProvidedClasspath(toClasspath(warClasspath.getProvidedClasspath()));
    cargo.setContextName(configuration.getAttribute(WebbyLaunchConstants.ATTR_CONTEXT_NAME, ""));
    if (cargo.getContextName().isEmpty()) {
      cargo.setContextName(mvnProject.getArtifactId());
    }
    cargo.setContainerId(configuration.getAttribute(WebbyLaunchConstants.ATTR_CONTAINER_ID,
        WebbyLaunchConstants.DEFAULT_CONTAINER_ID));
    cargo.setContainerType(ContainerType.INSTALLED);
    cargo.setContainerHome(expandVariables(configuration.getAttribute(WebbyLaunchConstants.ATTR_CONTAINER_HOME, "")));
    cargo.setConfigHome(new File(workDir, "container").getAbsolutePath());
    cargo.setConfigType(ConfigurationType.STANDALONE);
    cargo.setLogLevel(configuration.getAttribute(WebbyLaunchConstants.ATTR_LOG_LEVEL,
        WebbyLaunchConstants.DEFAULT_LOG_LEVEL));
    cargo.setPort(Integer.toString(getPort(configuration)));
    cargo.setTimeout(getIntAttribute(configuration, WebbyLaunchConstants.ATTR_CONTAINER_TIMEOUT,
        WebbyLaunchConstants.DEFAULT_TIMEOUT) * 1000L);
    return cargo;
  }

  static int getPort(ILaunchConfiguration configuration) throws CoreException {
    return getIntAttribute(configuration, WebbyLaunchConstants.ATTR_CONTAINER_PORT, WebbyLaunchConstants.DEFAULT_PORT);
  }

  /**
   * Reads an integer attribute that older versions of Webby stored as a string.
   */
  static int getIntAttribute(ILaunchConfiguration configuration, String name, int defaultValue) throws CoreException {
    Object value = configuration.getAttributes().get(name);
    if (value instanceof Integer intValue) {
      return intValue;
    }
    if (value instanceof String strValue) {
      try {
        return Integer.parseInt(strValue.trim());
      } catch (NumberFormatException e) {
        return defaultValue;
      }
    }
    return defaultValue;
  }

  private void printClasspath(WarClasspath warClasspath) {
    MessageConsole console = ConsoleManager.getConsole();
    console.clearConsole();

    try (MessageConsoleStream mcs = console.newMessageStream()) {
      mcs.println("Runtime classpath:");
      for (File file : warClasspath.getRuntimeClasspath()) {
        mcs.println("  " + file);
      }
      mcs.println("Provided classpath:");
      for (File file : warClasspath.getProvidedClasspath()) {
        mcs.println("  " + file);
      }
    } catch (IOException e) {
      WebbyPlugin.log(e);
    }
  }

  String getVmArgs(ILaunchConfiguration config) throws CoreException {
    StringBuilder args = new StringBuilder(1024);
    args.append(getVMArguments(config));

    String sysPropFiles = expandVariables(config.getAttribute(WebbyLaunchConstants.ATTR_SYS_PROP_FILES, ""));
    appendSystemProperties(args, loadSystemProperties(sysPropFiles));

    return args.toString();
  }

  static void appendSystemProperties(StringBuilder args, Properties sysProps) {
    for (Map.Entry<?, ?> entry : sysProps.entrySet()) {
      String key = entry.getKey().toString();
      String val = entry.getValue().toString();
      if (key.indexOf(' ') < 0 && val.indexOf(' ') < 0) {
        args.append(" -D").append(key).append('=').append(val);
      } else {
        args.append(" \"-D").append(key).append('=').append(val).append('"');
      }
    }
  }

  /**
   * Reads and merges the properties files listed one per line, latter files taking precedence.
   */
  static Properties loadSystemProperties(String sysPropFiles) throws CoreException {
    Properties sysProps = new Properties();

    if (sysPropFiles == null) {
      return sysProps;
    }

    for (String line : sysPropFiles.split("[\r\n]+")) {
      line = line.trim();
      if (line.isEmpty()) {
        continue;
      }
      File propFile = new File(line).getAbsoluteFile();
      if (propFile.isFile()) {
        try (InputStream is = Files.newInputStream(propFile.toPath())) {
          sysProps.load(is);
        } catch (IOException | IllegalArgumentException e) {
          throw WebbyPlugin.newError("Failed to read system properties from " + propFile, e);
        }
      } else {
        WebbyPlugin.log("Ignoring non-existent properties file " + propFile, IStatus.WARNING);
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
    if (new File(javaHome, "jre").isDirectory()) {
      javaHome = new File(javaHome, "jre");
    }

    try {
      LocalConfiguration config = (LocalConfiguration) new DefaultConfigurationFactory().createConfiguration(
          cargo.getContainerId(), cargo.getContainerType(), cargo.getConfigType(), cargo.getConfigHome());
      config.setProperty(GeneralPropertySet.LOGGING, cargo.getLogLevel());
      config.setProperty(GeneralPropertySet.JAVA_HOME, javaHome.getAbsolutePath());
      config.setProperty(GeneralPropertySet.JVMARGS, getVmArgs(configuration));
      config.setProperty(ServletPropertySet.PORT, cargo.getPort());
      config.setProperty(JettyPropertySet.USE_FILE_MAPPED_BUFFER, "false");
      config.setProperty(TomcatPropertySet.COPY_WARS, "false");

      String portAJP = nextPort(cargo.getPort());
      config.setProperty(TomcatPropertySet.AJP_PORT, portAJP);
      config.setProperty(GeneralPropertySet.RMI_PORT, nextPort(portAJP));

      WAR war = (WAR) new DefaultDeployableFactory().createDeployable(cargo.getContainerId(),
          PathSelector.normalizePath(cargo.getWarDirectory().getAbsolutePath()), DeployableType.WAR);
      if (!cargo.getContextName().isEmpty()) {
        war.setContext(cargo.getContextName());
      }
      war.setExtraClasspath(cargo.getRuntimeClasspath());
      config.addDeployable(war);

      if (config instanceof AbstractCatalinaStandaloneLocalConfiguration catalinaConfig
          && configuration.getAttribute(WebbyLaunchConstants.ATTR_CONTAINER_DISABLE_WS_SCI,
              WebbyLaunchConstants.DEFAULT_DISABLE_WS_SCI)) {
        catalinaConfig.addXmlReplacement("conf/server.xml", "//Server/Service/Engine/Host/Context",
            "containerSciFilter", "WsSci", ReplacementBehavior.IGNORE_IF_NON_EXISTING);
      }

      InstalledLocalContainer container = (InstalledLocalContainer) new DefaultContainerFactory().createContainer(
          cargo.getContainerId(), cargo.getContainerType(), config);
      container.setLogger(new CargoConsoleLogger(ConsoleManager.getConsole()));
      container.setHome(cargo.getContainerHome());
      container.setJvmLauncherFactory(new EclipseJvmLauncherFactory(runner, launch, cargo.getWorkDirectory(),
          envVariables, getVMSpecificAttributesMap(configuration), monitor));
      container.setTimeout(cargo.getTimeout());
      container.start();

      return new InstalledContainerWebApp(launch, cargo, container);
    } catch (CargoException e) {
      throw WebbyPlugin.newError("Failed to start container " + cargo.getContainerId() + ": " + e.getMessage(), e);
    }
  }

  static String nextPort(String port) {
    return Integer.toString(Integer.parseInt(port) + 1);
  }

  static String[] toClasspath(Collection<File> files) {
    return files.stream().map(file -> PathSelector.normalizePath(file.getAbsolutePath())).toArray(String[]::new);
  }

  private static String expandVariables(String str) throws CoreException {
    return VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(str);
  }

}
