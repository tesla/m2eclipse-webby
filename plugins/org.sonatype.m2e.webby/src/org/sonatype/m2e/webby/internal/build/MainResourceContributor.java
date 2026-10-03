package org.sonatype.m2e.webby.internal.build;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.sonatype.m2e.webby.internal.config.ResourceConfiguration;
import org.sonatype.m2e.webby.internal.config.WarConfiguration;
import org.sonatype.m2e.webby.internal.util.FilenameMapper;
import org.sonatype.m2e.webby.internal.util.PathCollector;
import org.sonatype.m2e.webby.internal.util.PathSelector;

/**
 * Contributes the resources of the WAR project itself: web resources, web.xml and dependencies that are not
 * libraries (e.g. TLDs).
 */
public class MainResourceContributor extends ResourceContributor {

  private static final String WEB_XML = "WEB-INF/web.xml";

  private final IProject project;

  private final MavenProject mvnProject;

  private final WarConfiguration warConfig;

  private final IResourceDelta resDelta;

  public MainResourceContributor(int ordinal, IMavenProjectFacade mvnFacade, WarConfiguration warConfig,
      IResourceDelta resDelta) {
    super(ordinal);
    this.project = mvnFacade.getProject();
    this.mvnProject = mvnFacade.getMavenProject();
    this.warConfig = warConfig;
    this.resDelta = resDelta;
  }

  @Override
  public void contribute(WarAssembler assembler, IProgressMonitor monitor) {
    try {
      PathSelector packagingSelector = new PathSelector(warConfig.getPackagingIncludes(),
          warConfig.getPackagingExcludes());

      List<ResourceConfiguration> resources = warConfig.getResources();
      for (ResourceConfiguration resource : resources) {
        String basedir = resource.getDirectory();

        PathCollector pathCollector = new PathCollector(resource.getIncludes(), resource.getExcludes());

        String[][] files;

        if (resDelta != null) {
          IResourceDelta childDelta = ResourceDeltaUtils.findChildDelta(resDelta, project, basedir);
          files = pathCollector.collectFiles(childDelta);
        } else {
          files = new String[2][];
          files[0] = pathCollector.collectFiles(basedir);
          files[1] = new String[0];
        }

        for (String file : files[1]) {
          String targetPath = resource.getTargetPath(file);
          assembler.unregisterTargetPath(targetPath, ordinal);
        }

        if (resDelta != null) {
          files[0] = assembler.appendDirtyTargetPaths(files[0], ordinal, basedir, "");
        }

        boolean filtering = resource.isFiltering();
        String encoding = resource.getEncoding();

        for (String file : files[0]) {
          String targetPath = resource.getTargetPath(file);
          if (!packagingSelector.isSelected(targetPath)) {
            continue;
          }
          if (assembler.registerTargetPath(targetPath, ordinal)) {
            File sourceFile = new File(basedir, file);
            try (InputStream is = getInputStream(sourceFile, file)) {
              assembler.copyResourceFile(is, targetPath, filtering, encoding, sourceFile.lastModified());
            } catch (IOException e) {
              assembler.addError(sourceFile.getAbsolutePath(), targetPath, e);
            }
          }
        }
      }

      String webXml = getWebXml();
      if (webXml != null) {
        if (resDelta == null || ResourceDeltaUtils.findChildDelta(resDelta, project, webXml) != null) {
          if (new File(webXml).exists()) {
            try (InputStream is = Files.newInputStream(new File(webXml).toPath())) {
              assembler.copyResourceFile(is, WEB_XML, warConfig.isWebXmlFiltered(), null, 0);
            } catch (IOException e) {
              assembler.addError(webXml, WEB_XML, e);
            }
          } else {
            assembler.unregisterTargetPath(WEB_XML, ordinal);
          }
        }
      }

      if (resDelta == null || resDelta.findMember(IPath.fromOSString(mvnProject.getFile().getName())) != null) {
        FilenameMapper filenameMapper = new FilenameMapper(warConfig.getFilenameMapping());
        Map<String, Artifact> targetPaths = filenameMapper.getTargetPaths(mvnProject.getArtifacts());
        for (Map.Entry<String, Artifact> entry : targetPaths.entrySet()) {
          File file = entry.getValue().getFile();
          if (file == null || !file.isFile()) {
            continue;
          }
          String targetPath = entry.getKey();
          if (!targetPath.startsWith("WEB-INF/lib/") && packagingSelector.isSelected(targetPath)) {
            try (InputStream is = Files.newInputStream(file.toPath())) {
              assembler.copyResourceFile(is, targetPath, false, null, file.lastModified());
            } catch (IOException e) {
              assembler.addError(file.getAbsolutePath(), targetPath, e);
            }
          }
        }
      }
    } finally {
      if (monitor != null) {
        monitor.done();
      }
    }
  }

  /**
   * Opens a source file, falling back to the workspace for files of linked/virtual folders.
   */
  private InputStream getInputStream(File sourceFile, String file) throws IOException {
    try {
      return Files.newInputStream(sourceFile.toPath());
    } catch (NoSuchFileException e) {
      if (resDelta == null) {
        throw e;
      }
      AtomicReference<IFile> found = new AtomicReference<>();
      try {
        resDelta.accept(delta -> {
          if (found.get() != null) {
            return false;
          }
          if (delta.getResource() instanceof IFile res && res.getFullPath().toOSString().contains(file)) {
            found.set(res);
          }
          return true;
        });
        if (found.get() != null) {
          return found.get().getContents();
        }
      } catch (CoreException ce) {
        e.addSuppressed(ce);
      }
      throw e;
    }
  }

  private String getWebXml() {
    String webXml = warConfig.getWebXml();
    if (webXml != null && !webXml.isEmpty()) {
      return webXml;
    } else if (!warConfig.getResources().isEmpty()) {
      File file = new File(warConfig.getResources().get(0).getDirectory(), WEB_XML);
      return file.getAbsolutePath();
    }
    return null;
  }

}
