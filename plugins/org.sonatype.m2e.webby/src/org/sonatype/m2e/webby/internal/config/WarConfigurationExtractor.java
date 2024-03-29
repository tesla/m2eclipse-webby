package org.sonatype.m2e.webby.internal.config;

import java.io.File;
import java.util.*;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.InputLocation;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.runtime.*;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.sonatype.m2e.webby.internal.WebbyPlugin;
import org.sonatype.m2e.webby.internal.util.PathSelector;

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
    List<Plugin> plugins = mvnProject.getBuildPlugins();
    for (Plugin plugin : plugins) {
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

      List<MojoExecution> mojoExecs = mvnFacade.getMojoExecutions(WAR_PLUGIN_GID, WAR_PLUGIN_AID, pm.newChild(90),
          "war");

      IMaven maven = MavenPlugin.getMaven();

      String basedir = mvnProject.getBasedir().getAbsolutePath();

      String encoding = mvnProject.getProperties().getProperty("project.build.sourceEncoding");

      warConfig.setWorkDirectory(getWorkDirectory(mvnProject));

      warConfig.setClassesDirectory(resolve(basedir, mvnProject.getBuild().getOutputDirectory()));

      if (!mojoExecs.isEmpty()) {
        MojoExecution mojoExec = mojoExecs.get(0);

        Set<String> overlayKeys = new HashSet<String>();
        Object[] overlays = maven.getMojoParameterValue(mvnProject, mojoExec, "overlays", Object[].class, null);
        if (overlays != null) {
          boolean mainConfigured = false;
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
          if (!mainConfigured) {
            warConfig.getOverlays().add(0, new OverlayConfiguration(null, null, null, null));
          }
        }

        Map<String, Artifact> overlayArtifacts = new LinkedHashMap<String, Artifact>();
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
        warConfig.setWebXmlFiltered(webXmlFiltered.booleanValue());

        Boolean backslashesEscaped = maven.getMojoParameterValue(mvnProject, mojoExec, "escapedBackslashesInFilePath",
            Boolean.class, null);
        warConfig.setBackslashesInFilePathEscaped(backslashesEscaped.booleanValue());

        String[] nonFilteredFileExtensions = maven.getMojoParameterValue(mvnProject, mojoExec,
            "nonFilteredFileExtensions", String[].class, null);
        warConfig.getNonFilteredFileExtensions().addAll(Arrays.asList(nonFilteredFileExtensions));

        String[] filters = maven.getMojoParameterValue(mvnProject, mojoExec, "filters", String[].class, null);
        for (String filter : filters) {
          warConfig.getFilters().add(resolve(basedir, filter));
        }

        String packagingIncludes = maven.getMojoParameterValue(mvnProject, mojoExec, "packagingIncludes", String.class, null);
        warConfig.setPackagingIncludes(split(packagingIncludes));
        String packagingExcludes = maven.getMojoParameterValue(mvnProject, mojoExec, "packagingExcludes", String.class, null);
        warConfig.setPackagingExcludes(split(packagingExcludes));
      } else {
        throw WebbyPlugin.newError(
            "Could not locate configuration for maven-war-plugin in POM for " + mvnProject.getId(), null);
      }

      return warConfig;
    } finally {
      if (monitor != null) {
        monitor.done();
      }
    }
  }

  private List<String> split(String list) {
    List<String> result = new ArrayList<String>();
    if (list != null && list.length() > 0) {
      Collections.addAll(result, list.split(","));
    }
    return result;
  }

  private String resolve(String basedir, String path) {
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
