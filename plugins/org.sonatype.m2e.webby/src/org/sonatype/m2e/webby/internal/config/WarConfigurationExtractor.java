package org.sonatype.m2e.webby.internal.config;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.InputLocation;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.sonatype.m2e.webby.internal.WebbyPlugin;
import org.sonatype.m2e.webby.internal.util.PathSelector;

/**
 * Reads the configuration of the maven-war-plugin from the effective POM of a WAR project.
 */
public class WarConfigurationExtractor {

  private static final String WAR_PLUGIN_GID = "org.apache.maven.plugins";

  private static final String WAR_PLUGIN_AID = "maven-war-plugin";

  public InputLocation getConfigurationLocation(MavenProject mvnProject) {
    Plugin plugin = getWarPlugin(mvnProject);
    if (plugin != null) {
      return plugin.getLocation("artifactId");
    }
    return null;
  }

  private Plugin getWarPlugin(MavenProject mvnProject) {
    for (Plugin plugin : mvnProject.getBuildPlugins()) {
      if (WAR_PLUGIN_GID.equals(plugin.getGroupId()) && WAR_PLUGIN_AID.equals(plugin.getArtifactId())) {
        return plugin;
      }
    }
    return null;
  }

  public String getWorkDirectory(MavenProject mvnProject) {
    String basedir = mvnProject.getBasedir().getAbsolutePath();
    return resolve(basedir, mvnProject.getBuild().getDirectory() + "/m2e-webby");
  }

  public WarConfiguration getConfiguration(IMavenProjectFacade mvnFacade, MavenProject mvnProject,
      IProgressMonitor monitor) throws CoreException {
    SubMonitor pm = SubMonitor.convert(monitor, "Reading WAR configuration...", 100);
    try {
      WarConfiguration warConfig = new WarConfiguration();

      List<MojoExecution> mojoExecs = mvnFacade.getMojoExecutions(WAR_PLUGIN_GID, WAR_PLUGIN_AID, pm.split(90),
          "war");
      if (mojoExecs.isEmpty()) {
        throw WebbyPlugin.newError(
            "Could not locate configuration for maven-war-plugin in POM for " + mvnProject.getId(), null);
      }
      MojoExecution mojoExec = mojoExecs.get(0);

      // IMavenProjectFacade.getMojoParameterValue(MojoExecution, ...) does not exist in m2e.core 2.7
      IMaven maven = MavenPlugin.getMaven();

      String basedir = mvnProject.getBasedir().getAbsolutePath();

      String encoding = mvnProject.getProperties().getProperty("project.build.sourceEncoding");

      warConfig.setWorkDirectory(getWorkDirectory(mvnProject));

      warConfig.setClassesDirectory(resolve(basedir, mvnProject.getBuild().getOutputDirectory()));

      Set<String> overlayKeys = new HashSet<>();
      Object[] overlays = maven.getMojoParameterValue(mvnProject, mojoExec, "overlays", Object[].class, null);
      boolean mainConfigured = false;
      if (overlays != null) {
        for (Object overlay : overlays) {
          OverlayConfiguration overlayConfig = new OverlayConfiguration(overlay);
          if (overlayConfig.isMain()) {
            if (mainConfigured) {
              continue;
            }
            mainConfigured = true;
          }
          warConfig.getOverlays().add(overlayConfig);
          overlayKeys.add(overlayConfig.getArtifactKey());
        }
      }
      if (!mainConfigured) {
        // like the maven-war-plugin, the project itself comes first unless configured otherwise
        warConfig.getOverlays().add(0, new OverlayConfiguration(null, null, null, null));
      }

      Map<String, Artifact> overlayArtifacts = new LinkedHashMap<>();
      for (Artifact artifact : mvnProject.getArtifacts()) {
        if ("war".equals(artifact.getType())) {
          overlayArtifacts.put(artifact.getDependencyConflictId(), artifact);
        }
      }

      for (Map.Entry<String, Artifact> e : overlayArtifacts.entrySet()) {
        if (!overlayKeys.contains(e.getKey())) {
          Artifact a = e.getValue();
          OverlayConfiguration warOverlay = new OverlayConfiguration(a.getGroupId(), a.getArtifactId(),
              a.getClassifier(), a.getType());
          warConfig.getOverlays().add(warOverlay);
        }
      }

      for (OverlayConfiguration overlay : warConfig.getOverlays()) {
        overlay.setEncoding(encoding);
      }

      String warSrcDir = maven.getMojoParameterValue(mvnProject, mojoExec, "warSourceDirectory", String.class, null);
      String warSrcInc = maven.getMojoParameterValue(mvnProject, mojoExec, "warSourceIncludes", String.class, null);
      String warSrcExc = maven.getMojoParameterValue(mvnProject, mojoExec, "warSourceExcludes", String.class, null);
      warConfig.getResources().add(new ResourceConfiguration(warSrcDir, split(warSrcInc), split(warSrcExc)));

      ResourceConfiguration[] resources = maven.getMojoParameterValue(mvnProject, mojoExec, "webResources",
          ResourceConfiguration[].class, null);
      if (resources != null) {
        warConfig.getResources().addAll(Arrays.asList(resources));
      }

      for (ResourceConfiguration resource : warConfig.getResources()) {
        resource.setDirectory(resolve(basedir, resource.getDirectory()));
        resource.setEncoding(encoding);
      }

      String filenameMapping = maven.getMojoParameterValue(mvnProject, mojoExec, "outputFileNameMapping",
          String.class, null);
      warConfig.setFilenameMapping(filenameMapping);

      String escapeString = maven.getMojoParameterValue(mvnProject, mojoExec, "escapeString", String.class, null);
      warConfig.setEscapeString(escapeString);

      String webXml = maven.getMojoParameterValue(mvnProject, mojoExec, "webXml", String.class, null);
      warConfig.setWebXml(resolve(basedir, webXml));

      Boolean webXmlFiltered = maven.getMojoParameterValue(mvnProject, mojoExec, "filteringDeploymentDescriptors",
          Boolean.class, null);
      warConfig.setWebXmlFiltered(Boolean.TRUE.equals(webXmlFiltered));

      Boolean backslashesEscaped = maven.getMojoParameterValue(mvnProject, mojoExec, "escapedBackslashesInFilePath",
          Boolean.class, null);
      warConfig.setBackslashesInFilePathEscaped(Boolean.TRUE.equals(backslashesEscaped));

      String[] nonFilteredFileExtensions = maven.getMojoParameterValue(mvnProject, mojoExec, "nonFilteredFileExtensions", String[].class, null);
      if (nonFilteredFileExtensions != null) {
        warConfig.getNonFilteredFileExtensions().addAll(Arrays.asList(nonFilteredFileExtensions));
      }

      String[] filters = maven.getMojoParameterValue(mvnProject, mojoExec, "filters", String[].class, null);
      if (filters != null) {
        for (String filter : filters) {
          warConfig.getFilters().add(resolve(basedir, filter));
        }
      }

      String packagingIncludes = maven.getMojoParameterValue(mvnProject, mojoExec, "packagingIncludes", String.class,
          null);
      warConfig.setPackagingIncludes(split(packagingIncludes));
      String packagingExcludes = maven.getMojoParameterValue(mvnProject, mojoExec, "packagingExcludes", String.class,
          null);
      warConfig.setPackagingExcludes(split(packagingExcludes));

      return warConfig;
    } finally {
      if (monitor != null) {
        monitor.done();
      }
    }
  }

  /**
   * Splits a comma-separated list of patterns, like the maven-war-plugin does.
   */
  static List<String> split(String list) {
    List<String> result = new ArrayList<>();
    if (list != null) {
      for (String item : list.split(",")) {
        item = item.trim();
        if (!item.isEmpty()) {
          result.add(item);
        }
      }
    }
    return result;
  }

  /**
   * Makes a path from the POM absolute, relative paths being relative to the base directory of the project.
   */
  static String resolve(String basedir, String path) {
    String result = path;
    if (path != null && basedir != null) {
      path = PathSelector.normalizePath(path);
      File file = new File(path);
      if (file.isAbsolute()) {
        // path was already absolute, just normalize file separator and we're done
        result = file.getPath();
      } else if (file.getPath().startsWith(File.separator)) {
        // drive-relative Windows path, don't align with project directory but with drive root
        result = file.getAbsolutePath();
      } else {
        // an ordinary relative path, align with project directory
        result = new File(new File(basedir, path).toURI().normalize()).getAbsolutePath();
      }
    }
    return result;
  }

}
