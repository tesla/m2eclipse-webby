package org.sonatype.m2e.webby.internal.build;

import java.io.*;
import java.lang.ref.*;
import java.util.*;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.InputLocation;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.configurator.AbstractBuildParticipant;
import org.sonatype.m2e.webby.internal.WebbyPlugin;
import org.sonatype.m2e.webby.internal.config.*;
import org.sonatype.m2e.webby.internal.util.*;
import org.sonatype.plexus.build.incremental.BuildContext;

public class WebbyBuildParticipant extends AbstractBuildParticipant {

  private static final String PROP_WAR_RESOURCES = "org.sonatype.m2e.webby.war.resources";

  @Override
  public Set<IProject> build(int kind, IProgressMonitor monitor) throws Exception {
    SubMonitor pm = SubMonitor.convert(monitor, "Creating WAR project output...", 100);
    try {
      boolean incremental = kind != IncrementalProjectBuilder.FULL_BUILD;

      IMavenProjectFacade mvnFacade = getMavenProjectFacade();

      MavenProject mvnProject = mvnFacade.getMavenProject(pm.newChild(15));

      getBuildContext().removeMessages(mvnProject.getFile());

      MavenSession mvnSession = getSession();

      WarConfiguration warConfig = new WarConfigurationExtractor().getConfiguration(mvnFacade, mvnProject,
          pm.newChild(15));

      Map<String, Artifact> overlayArtifacts = WarUtils.getOverlayArtifacts(mvnProject);

      File warDir = new File(warConfig.getWarDirectory());
      IFolder warFolder = getFolder(warDir.getAbsolutePath());

      String[] files = warDir.list();
      if (files == null || files.length <= 0) {
        incremental = false;
      }

      IResourceDelta theDelta = incremental ? getDelta(mvnFacade.getProject()) : null;

      File warConfigFile = new File(warConfig.getWorkDirectory(), "config.ser");
      if (incremental && (theDelta == null || theDelta.findMember(mvnFacade.getPom().getProjectRelativePath()) != null)) {
        Object previousWarConfiguration = null;
        if (warConfigFile.isFile()) {
          try {
            previousWarConfiguration = WarConfiguration.load(warConfigFile);
          } catch (IOException e) {
            WebbyPlugin.log(e, IStatus.WARNING);
          }
        }
        if (!warConfig.equals(previousWarConfiguration)) {
          incremental = false;
          theDelta = null;
          if (warFolder != null) {
            warFolder.delete(true, null);
          }
        }
      }
      if (!incremental || !warConfigFile.exists()) {
        try {
          warConfig.save(warConfigFile);
        } catch (IOException e) {
          WebbyPlugin.log(e, IStatus.WARNING);
        }
      }

      File resourceRegistryFile = new File(warConfig.getWorkDirectory(), "resources.ser");
      ResourceRegistry resourceRegistry = null;
      if (incremental) {
        Object obj = mvnFacade.getSessionProperty(PROP_WAR_RESOURCES);
        if (obj instanceof Reference) {
          obj = ((Reference<?>) obj).get();
        }
        if (obj instanceof ResourceRegistry) {
          resourceRegistry = (ResourceRegistry) obj;
        } else {
          if (resourceRegistryFile.isFile()) {
            try {
              resourceRegistry = ResourceRegistry.load(resourceRegistryFile);
              mvnFacade.setSessionProperty(PROP_WAR_RESOURCES, new SoftReference<Object>(resourceRegistry));
            } catch (IOException e) {
              WebbyPlugin.log(e, IStatus.WARNING);
            }
          }
        }
      }
      if (resourceRegistry == null) {
        incremental = false;
        theDelta = null;
        resourceRegistry = new ResourceRegistry();
        mvnFacade.setSessionProperty(PROP_WAR_RESOURCES, new SoftReference<Object>(resourceRegistry));
      }

      Map<IProject, IResourceDelta> resDeltas = new IdentityHashMap<IProject, IResourceDelta>();
      List<ResourceContributor> resourceContributors = new ArrayList<ResourceContributor>();
      int overlayOrdinal = 0;
      for (OverlayConfiguration overlayConfig : warConfig.getOverlays()) {
        if (overlayConfig.isSkip()) {
          continue;
        }
        overlayOrdinal++;
        ResourceContributor resourceContributor;
        if (overlayConfig.isMain()) {
          resourceContributor = new MainResourceContributor(overlayOrdinal, mvnFacade, warConfig, theDelta);
        } else {
          Artifact overlayArtifact = overlayArtifacts.get(overlayConfig.getArtifactKey());
          if (overlayArtifact == null) {
            addConfigurationError(mvnProject, "The overlay " + overlayConfig.getId()
                + " refers to a non-existing artifact");
            continue;
          }
          IMavenProjectFacade overlayFacade = MavenUtils.getFacade(overlayArtifact.getGroupId(),
              overlayArtifact.getArtifactId(), overlayArtifact.getBaseVersion());
          if (overlayFacade == null) {
            if (overlayArtifact.getFile() == null) {
              // unresolved, this should already have been reported by m2e core
              continue;
            }
            if (incremental) {
              continue;
            }
            resourceContributor = new ArtifactResourceContributor(overlayOrdinal, overlayArtifact.getFile(),
                overlayConfig);
          } else {
            IProject overlayProject = overlayFacade.getProject();
            IResourceDelta resDelta;
            if (resDeltas.containsKey(overlayProject)) {
              resDelta = resDeltas.get(overlayProject);
            } else {
              resDelta = incremental ? getDelta(overlayProject) : null;
              resDeltas.put(overlayProject, resDelta);
            }
            resourceContributor = new ProjectResourceContributor(overlayOrdinal, overlayFacade, overlayConfig, resDelta);
          }
        }
        resourceContributors.add(resourceContributor);
      }

      FilteringHandler filteringHandler = new FilteringHandler(warConfig, mvnProject, mvnSession);
      WarAssembler warAssembler = new WarAssembler(warDir, filteringHandler, resourceRegistry);

      SubMonitor spm = SubMonitor.convert(pm.newChild(65), resourceContributors.size());
      for (ResourceContributor resourceContributor : resourceContributors) {
        if (pm.isCanceled()) {
          throw new OperationCanceledException();
        }
        resourceContributor.contribute(warAssembler, spm.newChild(1));
      }

      try {
        resourceRegistry.save(resourceRegistryFile);
      } catch (IOException e) {
        WebbyPlugin.log(e, IStatus.WARNING);
      }

      if (warFolder != null) {
        warFolder.refreshLocal(IResource.DEPTH_INFINITE, pm.newChild(5));
      }

      return new HashSet<IProject>(resDeltas.keySet());
    } finally {
      if (monitor != null) {
        monitor.done();
      }
    }
  }

  @Override
  public void clean(IProgressMonitor monitor) throws CoreException {
    SubMonitor pm = SubMonitor.convert(monitor, "Cleaning WAR project output...", 100);
    try {
      IMavenProjectFacade mvnFacade = getMavenProjectFacade();

      mvnFacade.setSessionProperty(PROP_WAR_RESOURCES, null);

      MavenProject mvnProject = mvnFacade.getMavenProject(pm.newChild(30));

      String workDir = new WarConfigurationExtractor().getWorkDirectory(mvnProject);

      IFolder workFolder = getFolder(workDir);

      if (workFolder != null) {
        workFolder.delete(true, pm.newChild(70));
      }
    } finally {
      if (monitor != null) {
        monitor.done();
      }
    }
  }

  private IFolder getFolder(String path) {
    IPath p = getMavenProjectFacade().getProjectRelativePath(path);
    if (p != null) {
      return getMavenProjectFacade().getProject().getFolder(p);
    }
    return null;
  }

  private void addConfigurationError(MavenProject mvnProject, String msg) {
    File pomFile = mvnProject.getFile();

    int line = 0;
    int column = 0;
    InputLocation location = new WarConfigurationExtractor().getConfigurationLocation(mvnProject);
    if (location != null && location.getSource() != null) {
      String modelId = mvnProject.getGroupId() + ":" + mvnProject.getArtifactId() + ':' + mvnProject.getVersion();
      if (location.getSource().getModelId().equals(modelId)) {
        line = location.getLineNumber();
        column = location.getColumnNumber();
      }
    }

    BuildContext buildContext = getBuildContext();
    buildContext.addMessage(pomFile, line, column, msg, BuildContext.SEVERITY_ERROR, null);
  }

}
