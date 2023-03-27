package org.sonatype.m2e.webby.internal.build;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.sonatype.m2e.webby.internal.config.*;
import org.sonatype.m2e.webby.internal.util.*;

public class MainResourceContributor extends ResourceContributor {

  private static final String WEB_XML = "WEB-INF/web.xml";

  private IProject project;

  private MavenProject mvnProject;

  private WarConfiguration warConfig;

  private IResourceDelta resDelta;

  public MainResourceContributor(int ordinal, IMavenProjectFacade mvnFacade, WarConfiguration warConfig,
      IResourceDelta resDelta) {
    super(ordinal);
    this.project = mvnFacade.getProject();
    this.mvnProject = mvnFacade.getMavenProject();
    this.warConfig = warConfig;
    this.resDelta = resDelta;
  }

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
            try {
              InputStream is = getInputStream(sourceFile, file, resDelta);
              try {
                assembler.copyResourceFile(is, targetPath, filtering, encoding, sourceFile.lastModified());
              } finally {
                is.close();
              }
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
            try {
              InputStream is = new FileInputStream(webXml);
              try {
                assembler.copyResourceFile(is, WEB_XML, warConfig.isWebXmlFiltered(), null, 0);
              } finally {
                is.close();
              }
            } catch (IOException e) {
              assembler.addError(webXml, WEB_XML, e);
            }
          } else {
            assembler.unregisterTargetPath(WEB_XML, ordinal);
          }
        }
      }

      if (resDelta == null || resDelta.findMember(Path.fromOSString(mvnProject.getFile().getName())) != null) {
        FilenameMapper filenameMapper = new FilenameMapper(warConfig.getFilenameMapping());
        Map<String, Artifact> targetPaths = filenameMapper.getTargetPaths(mvnProject.getArtifacts());
        for (Map.Entry<String, Artifact> entry : targetPaths.entrySet()) {
          File file = entry.getValue().getFile();
          if (file == null || !file.isFile()) {
            continue;
          }
          String targetPath = entry.getKey();
          if (!targetPath.startsWith("WEB-INF/lib/") && packagingSelector.isSelected(targetPath)) {
            try {
              InputStream is = new FileInputStream(file);
              try {
                assembler.copyResourceFile(is, targetPath, false, null, file.lastModified());
              } finally {
                is.close();
              }
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

  private InputStream getInputStream(File sourceFile, String file, IResourceDelta resDelta) throws FileNotFoundException {
    InputStream is = null;
    try {
      is = new FileInputStream(sourceFile);
    } catch (FileNotFoundException e) {
      try {
        // Fix for virtual folder
        AtomicReference<IFile> _file = new AtomicReference<>();
        resDelta.accept(new IResourceDeltaVisitor() {
          @Override
          public boolean visit(IResourceDelta delta) throws CoreException {
            if (_file.get() != null) {
              return false;
            }
            if (delta.getResource() instanceof IFile) {
              IFile res = (IFile) delta.getResource();
              if (res.getFullPath().toOSString().contains(file)) {
                _file.set(res);
              }
            }
            return true;
          }
        });
        if (_file.get() != null) {
          return _file.get().getContents();
        }
      } catch (CoreException e1) {
        throw e;
      }
    }
    return is;
  }

  private String getWebXml() {
    String webXml = warConfig.getWebXml();
    if (webXml != null && webXml.length() > 0) {
      return webXml;
    } else if (!warConfig.getResources().isEmpty()) {
      File file = new File(warConfig.getResources().get(0).getDirectory(), WEB_XML);
      return file.getAbsolutePath();
    }
    return null;
  }

}
