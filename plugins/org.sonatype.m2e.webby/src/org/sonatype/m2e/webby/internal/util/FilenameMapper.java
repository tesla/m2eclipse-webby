package org.sonatype.m2e.webby.internal.util;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.maven.artifact.Artifact;

/**
 * Computes the location of dependencies in the WAR, like the maven-war-plugin with its outputFileNameMapping.
 */
public class FilenameMapper {

  private final String filenameMapping;

  public FilenameMapper(String filenameMapping) {
    this.filenameMapping = filenameMapping;
  }

  public String mapFilename(Artifact artifact) {
    Map<String, String> props = new LinkedHashMap<>();
    props.put("@{groupId}@", emptify(artifact.getGroupId()));
    props.put("@{artifactId}@", emptify(artifact.getArtifactId()));
    props.put("@{version}@", emptify(artifact.getVersion()));
    props.put("@{baseVersion}@", emptify(artifact.getBaseVersion()));
    props.put("@{classifier}@", emptify(artifact.getClassifier()));
    if (props.get("@{classifier}@").isEmpty()) {
      props.put("@{dashClassifier?}@", "");
      props.put("@{dashClassifier}@", "");
    } else {
      props.put("@{dashClassifier?}@", "-" + artifact.getClassifier());
      props.put("@{dashClassifier}@", "-" + artifact.getClassifier());
    }
    props.put("@{extension}@", emptify(artifact.getArtifactHandler().getExtension()));
    if ("par".equals(artifact.getType())) {
      props.put("@{extension}@", "jar");
    }

    String result = filenameMapping;
    for (Map.Entry<String, String> e : props.entrySet()) {
      result = result.replace(e.getKey(), e.getValue());
    }
    return result;
  }

  private static String emptify(String str) {
    return (str == null) ? "" : str;
  }

  public String getTargetPath(Artifact artifact) {
    String targetPath = null;
    String targetDir = getTargetDir(artifact);
    if (targetDir != null) {
      targetPath = targetDir + mapFilename(artifact);
    }
    return targetPath;
  }

  public Map<String, Artifact> getTargetPaths(Collection<Artifact> artifacts) {
    Map<String, Artifact> paths = new LinkedHashMap<>();

    for (Artifact artifact : artifacts) {
      String targetPath = getTargetPath(artifact);
      if (targetPath != null) {
        paths.put(targetPath, artifact);
      }
    }

    return paths;
  }

  public static String getTargetDir(Artifact artifact) {
    if (artifact.isOptional()) {
      return null;
    }

    String scope = artifact.getScope();
    if (!Artifact.SCOPE_RUNTIME.equals(scope) && !Artifact.SCOPE_COMPILE.equals(scope)) {
      return null;
    }

    String type = artifact.getType();
    if (type == null) {
      return null;
    }
    return switch (type) {
      case "tld" -> "WEB-INF/tld/";
      case "aar" -> "WEB-INF/services/";
      case "mar" -> "WEB-INF/modules/";
      case "jar", "ejb", "ejb-client", "test-jar", "par" -> "WEB-INF/lib/";
      default -> null;
    };
  }

}
