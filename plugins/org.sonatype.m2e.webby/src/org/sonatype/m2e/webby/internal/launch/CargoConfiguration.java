/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.sonatype.m2e.webby.internal.launch;

import java.io.File;

import org.codehaus.cargo.container.ContainerType;
import org.codehaus.cargo.container.configuration.ConfigurationType;


/**
 */
class CargoConfiguration {

  private File workDirectory;

  private String containerId;

  private ContainerType containerType;

  private String containerHome;

  private ConfigurationType configType;

  private String configHome;

  private String logLevel = "high";

  private String contextName = "";

  private String port;

  private long timeout;

  private File warDirectory;

  private String[] runtimeClasspath;

  private String[] providedClasspath;

  public File getWorkDirectory() {
    return workDirectory;
  }

  public void setWorkDirectory(File workDirectory) {
    this.workDirectory = workDirectory;
  }

  public String getContainerId() {
    return containerId;
  }

  public void setContainerId(String containerId) {
    this.containerId = containerId;
  }

  public ContainerType getContainerType() {
    return containerType;
  }

  public void setContainerType(ContainerType containerType) {
    this.containerType = containerType;
  }

  public String getContainerHome() {
    return containerHome;
  }

  public void setContainerHome(String containerHome) {
    this.containerHome = containerHome;
  }

  public ConfigurationType getConfigType() {
    return configType;
  }

  public void setConfigType(ConfigurationType configType) {
    this.configType = configType;
  }

  public String getConfigHome() {
    return configHome;
  }

  public void setConfigHome(String configHome) {
    this.configHome = configHome;
  }

  public String getLogLevel() {
    return logLevel;
  }

  public void setLogLevel(String logLevel) {
    this.logLevel = logLevel;
  }

  public String getContextName() {
    return contextName;
  }

  public void setContextName(String contextName) {
    if(contextName != null && contextName.startsWith("/")) {
      contextName = contextName.substring(1);
    }
    this.contextName = contextName;
  }

  public String getPort() {
    return port;
  }

  public void setPort(String port) {
    this.port = port;
  }

  public long getTimeout() {
    return timeout;
  }

  public void setTimeout(long timeout) {
    this.timeout = timeout;
  }
  
  public File getWarDirectory() {
    return warDirectory;
  }

  public void setWarDirectory(File warDirectory) {
    this.warDirectory = warDirectory;
  }

  public String[] getRuntimeClasspath() {
    return runtimeClasspath;
  }

  public void setRuntimeClasspath(String[] runtimeClasspath) {
    this.runtimeClasspath = runtimeClasspath;
  }

  public String[] getProvidedClasspath() {
    return providedClasspath;
  }

  public void setProvidedClasspath(String[] providedClasspath) {
    this.providedClasspath = providedClasspath;
  }
}