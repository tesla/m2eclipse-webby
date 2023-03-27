package org.sonatype.m2e.webby.internal.util;

import java.util.*;

import org.apache.maven.artifact.Artifact;

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
    if (props.get("@{classifier}@").length() <= 0) {
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

  private String emptify(String str) {
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
    if ("tld".equals(type)) {
      return "WEB-INF/tld/";
    } else if ("aar".equals(type)) {
      return "WEB-INF/services/";
    } else if ("mar".equals(type)) {
      return "WEB-INF/modules/";
    } else if ("jar".equals(type) || "ejb".equals(type) || "ejb-client".equals(type) || "test-jar".equals(type)
        || "par".equals(type)) {
      return "WEB-INF/lib/";
    }

    return null;
  }

}
