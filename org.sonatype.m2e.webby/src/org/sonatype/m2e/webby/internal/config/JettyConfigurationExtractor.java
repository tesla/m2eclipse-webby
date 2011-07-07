/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.sonatype.m2e.webby.internal.config;

import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginContainer;
import org.apache.maven.model.PluginExecution;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.m2e.core.project.IMavenProjectFacade;


/**
 */
public class JettyConfigurationExtractor {

  private static final String JETTY_PLUGIN_GID = "org.mortbay.jetty";

  private static final String JETTY_6_PLUGIN_AID = "maven-jetty-plugin";

  private static final String JETTY_7_PLUGIN_AID = "jetty-maven-plugin";

  public JettyConfiguration getConfiguration(IMavenProjectFacade mvnFacade, IProgressMonitor monitor)
      throws CoreException {
    SubMonitor pm = SubMonitor.convert(monitor, "Reading Jetty configuration...", 100);
    try {
      JettyConfiguration jettyConfig = null;

      Model pom = mvnFacade.getMavenProject(pm.newChild(60)).getModel();

      Plugin jettyPlugin = findJettyPlugin(pom);
      if(jettyPlugin == null) {
        return null;
      }

      jettyConfig = new JettyConfiguration();

      if(JETTY_6_PLUGIN_AID.equals(jettyPlugin.getArtifactId())) {
        jettyConfig.setContainerId("jetty6x");
      } else {
        jettyConfig.setContainerId("jetty7x");
      }

      Xpp3Dom dom = getConfig(jettyPlugin);

      Xpp3Dom contextPath;
      Xpp3Dom webAppConfig = dom.getChild("webAppConfig");
      if(webAppConfig != null) {
        contextPath = webAppConfig.getChild("contextPath");
      } else {
        contextPath = dom.getChild("contextPath");
      }
      if(contextPath != null) {
        jettyConfig.setContext(contextPath.getValue());
      }

      Xpp3Dom connectors = dom.getChild("connectors");
      if(connectors != null) {
        Xpp3Dom connector = null;
        for(int i = 0, n = connectors.getChildCount(); i < n && connector == null; i++ ) {
          Xpp3Dom conn = connectors.getChild(i);
          String impl = conn.getAttribute("implementation");
          if(impl != null && impl.contains(".Ssl")) {
            continue;
          }
          connector = conn;
        }
        if(connector != null) {
          Xpp3Dom port = connector.getChild("port");
          if(port != null) {
            try {
              jettyConfig.setPort(Integer.parseInt(port.getValue()));
            } catch(NumberFormatException e) {
              // oh well, we tried
            }
          }
        }
      }

      Xpp3Dom sysProps = dom.getChild("systemProperties");
      if(sysProps != null) {
        for(int i = 0, n = sysProps.getChildCount(); i < n; i++ ) {
          Xpp3Dom sysProp = sysProps.getChild(i);
          Xpp3Dom key = sysProp.getChild("key");
          Xpp3Dom val = sysProp.getChild("value");
          if(key != null && val != null) {
            jettyConfig.setSystemProperty(key.getValue(), val.getValue());
          }
        }
      }

      return jettyConfig;
    } finally {
      if(monitor != null) {
        monitor.done();
      }
    }
  }

  private Plugin findJettyPlugin(Model model) {
    Build build = model.getBuild();
    if(build == null) {
      return null;
    }
    Plugin plugin = findPlugin(build, JETTY_PLUGIN_GID, JETTY_7_PLUGIN_AID);
    if(plugin != null) {
      return plugin;
    }
    plugin = findPlugin(build, JETTY_PLUGIN_GID, JETTY_6_PLUGIN_AID);
    if(plugin != null) {
      return plugin;
    }
    PluginContainer pluginMngt = build.getPluginManagement();
    if(pluginMngt == null) {
      return null;
    }
    plugin = findPlugin(pluginMngt, JETTY_PLUGIN_GID, JETTY_7_PLUGIN_AID);
    if(plugin != null) {
      return plugin;
    }
    plugin = findPlugin(pluginMngt, JETTY_PLUGIN_GID, JETTY_6_PLUGIN_AID);
    if(plugin != null) {
      return plugin;
    }
    return null;
  }

  private Plugin findPlugin(PluginContainer plugins, String groupId, String artifactId) {
    for(Plugin plugin : plugins.getPlugins()) {
      if(groupId.equals(plugin.getGroupId()) && artifactId.equals(plugin.getArtifactId())) {
        return plugin;
      }
    }
    return null;
  }

  private Xpp3Dom getConfig(Plugin plugin) {
    Xpp3Dom dom = (Xpp3Dom) plugin.getConfiguration();
    for(PluginExecution exec : plugin.getExecutions()) {
      if(exec.getGoals().contains("run")) {
        dom = (Xpp3Dom) exec.getConfiguration();
        break;
      }
    }
    if(dom == null) {
      dom = new Xpp3Dom("configuration");
    }
    return dom;
  }

}
