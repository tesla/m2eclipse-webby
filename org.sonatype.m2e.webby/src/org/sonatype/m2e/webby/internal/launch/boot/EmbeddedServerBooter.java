/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.sonatype.m2e.webby.internal.launch.boot;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

import org.codehaus.cargo.container.Container;
import org.codehaus.cargo.container.ContainerType;
import org.codehaus.cargo.container.LocalContainer;
import org.codehaus.cargo.container.configuration.Configuration;
import org.codehaus.cargo.container.configuration.ConfigurationType;
import org.codehaus.cargo.container.configuration.LocalConfiguration;
import org.codehaus.cargo.container.deployable.DeployableType;
import org.codehaus.cargo.container.deployable.WAR;
import org.codehaus.cargo.container.jetty.JettyPropertySet;
import org.codehaus.cargo.container.property.GeneralPropertySet;
import org.codehaus.cargo.container.property.LoggingLevel;
import org.codehaus.cargo.container.property.ServletPropertySet;
import org.codehaus.cargo.generic.ContainerFactory;
import org.codehaus.cargo.generic.DefaultContainerFactory;
import org.codehaus.cargo.generic.configuration.ConfigurationFactory;
import org.codehaus.cargo.generic.configuration.DefaultConfigurationFactory;
import org.codehaus.cargo.generic.deployable.DefaultDeployableFactory;
import org.codehaus.cargo.generic.deployable.DeployableFactory;
import org.codehaus.cargo.util.internal.log.AbstractLogger;
import org.codehaus.cargo.util.log.LogLevel;


/**
 */
public class EmbeddedServerBooter {

  public static void main(String[] args) throws Exception {
    System.out.println("Booting server...");

    String controlPort = args[0];
    String containerId = args[1];
    String containerType = args[2];
    String configHome = args[3];
    String configType = args[4];
    String port = args[5];
    String logLevel = args[6];
    String warFile = args[7];
    String extraClasspath = args[8];
    if(extraClasspath.startsWith("@")) {
      extraClasspath = readArgumentFile(new File(extraClasspath.substring(1)));
    }
    String contextName = "";
    if(args.length >= 10) {
      contextName = args[9];
    }

    System.out.println("  " + containerId + ":" + port + " (" + containerType + ")");
    System.out.println("  " + configHome + " (" + configType + ")");
    System.out.println("  " + warFile + " (" + contextName + ")");

    String[] classpath = System.getProperty("java.class.path", "").split("\\" + File.pathSeparatorChar);
    if(classpath != null) {
      for(String path : classpath) {
        System.out.println("  " + path);
      }
    }

    LocalContainer container = run(containerId, ContainerType.toType(containerType), configHome,
        ConfigurationType.toType(configType), port, logLevel, warFile, extraClasspath, contextName);

    ServerSocket control = new ServerSocket(Integer.parseInt(controlPort), 1);
    Socket socket = control.accept();
    try {
      socket.shutdownInput();
      socket.shutdownOutput();
      socket.close();
    } catch(IOException e) {
      e.printStackTrace();
    }
    container.stop();
  }

  private static String readArgumentFile(File pathname) throws Exception {
    StringBuilder buffer = new StringBuilder(1024 * 32);
    InputStreamReader rdr = new InputStreamReader(new BufferedInputStream(new FileInputStream(pathname)), "UTF-8");
    try {
      for(char[] chars = new char[1024 * 4];;) {
        int read = rdr.read(chars);
        if(read < 0) {
          break;
        }
        buffer.append(chars, 0, read);
      }
    } finally {
      rdr.close();
    }
    pathname.delete();
    return buffer.toString().trim();
  }

  private static LocalContainer run(String containerId, ContainerType containerType, String configHome,
      ConfigurationType configType, String port, String logLevel, String warFile, String extraClasspath,
      String contextName) throws Exception {
    if(LoggingLevel.HIGH.equalsLevel(logLevel) && containerId.startsWith("jetty")) {
      System.setProperty("DEBUG", "true");
      System.setProperty("org.eclipse.jetty.util.log.DEBUG", "true");
    }

    ConfigurationFactory configFactory = new DefaultConfigurationFactory();
    Configuration config = configFactory.createConfiguration(containerId, containerType, configType, configHome);
    config.setProperty(GeneralPropertySet.LOGGING, logLevel);
    config.setProperty(ServletPropertySet.PORT, port);
    config.setProperty(JettyPropertySet.USE_FILE_MAPPED_BUFFER, "false");

    ContainerFactory containerFactory = new DefaultContainerFactory();
    Container container = containerFactory.createContainer(containerId, containerType, config);

    DeployableFactory depFactory = new DefaultDeployableFactory();
    WAR dep = (WAR) depFactory.createDeployable(containerId, warFile, DeployableType.WAR);
    if(extraClasspath != null && extraClasspath.length() > 0 && !".".equals(extraClasspath)) {
      dep.setExtraClasspath(extraClasspath.split("\\" + File.pathSeparator));
    }
    if(contextName != null && contextName.length() > 0) {
      dep.setContext(contextName);
    }

    LocalConfiguration localConfig = (LocalConfiguration) config;
    localConfig.addDeployable(dep);

    String containerTimeoutProperty = System.getProperty('cargo.containers.timeout');
    int containerTimeout = (containerTimeoutProperty != null) ? Integer.valueOf(containerTimeoutProperty) : 30 * 1000;

    LocalContainer localContainer = (LocalContainer) container;
    localContainer.setLogger(new CargoLogger());
    localContainer.setTimeout(containerTimeout);
    localContainer.start();

    return localContainer;
  }

  static class CargoLogger extends AbstractLogger {

    @Override
    protected void doLog(LogLevel level, String message, String category) {
      System.out.println(message);
    }

  }

}
