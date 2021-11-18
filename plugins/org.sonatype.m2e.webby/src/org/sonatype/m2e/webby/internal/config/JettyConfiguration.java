/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.sonatype.m2e.webby.internal.config;

import java.util.LinkedHashMap;
import java.util.Map;


/**
 */
public class JettyConfiguration {

  private String containerId = "jetty7x";

  private String context;

  private int port = 8080;

  private Map<String, String> systemProperties = new LinkedHashMap<String, String>();

  public String getContainerId() {
    return containerId;
  }

  public void setContainerId(String containerId) {
    this.containerId = containerId;
  }

  public String getContext() {
    return context;
  }

  public void setContext(String context) {
    this.context = context;
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public Map<String, String> getSystemProperties() {
    return systemProperties;
  }

  public void setSystemProperties(Map<String, String> systemProperties) {
    this.systemProperties = systemProperties;
  }

  public void setSystemProperty(String key, String value) {
    if(value == null) {
      systemProperties.remove(key);
    } else {
      systemProperties.put(key, value);
    }
  }

}
