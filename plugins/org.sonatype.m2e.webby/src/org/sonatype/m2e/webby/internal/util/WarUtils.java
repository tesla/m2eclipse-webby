package org.sonatype.m2e.webby.internal.util;

import java.util.*;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;

public class WarUtils {

  public static Map<String, Artifact> getOverlayArtifacts(MavenProject mvnProject) {
    Map<String, Artifact> overlayArtifacts = new LinkedHashMap<String, Artifact>();
    for (Artifact artifact : mvnProject.getArtifacts()) {
      String type = artifact.getType();
      if ("war".equals(type) || "zip".equals(type)) {
        overlayArtifacts.put(artifact.getDependencyConflictId(), artifact);
      }
    }
    return overlayArtifacts;
  }

  public static String getTargetPath(String targetDir, String sourcePath) {
    String targetPath = sourcePath.replace('\\', '/');

    if (targetDir != null && targetDir.length() > 0) {
      targetDir = targetDir.replace('\\', '/');
      if (targetDir.endsWith("/") && targetPath.startsWith("/")) {
        targetPath = targetDir + targetPath.substring(1);
      } else if (targetDir.endsWith("/") || targetPath.startsWith("/")) {
        targetPath = targetDir + targetPath;
      } else {
        targetPath = targetDir + '/' + targetPath;
      }
    }

    return targetPath;
  }

  public static String getSourcePath(String targetDir, String targetPath) {
    String sourcePath = targetPath.replace('\\', '/');

    if (targetDir != null && targetDir.length() > 0) {
      targetDir = targetDir.replace('\\', '/');
      if (!targetDir.endsWith("/")) {
        targetDir += '/';
      }
      if (sourcePath.startsWith(targetDir)) {
        sourcePath = sourcePath.substring(targetDir.length());
      } else {
        sourcePath = null;
      }
    }

    return sourcePath;
  }

}
