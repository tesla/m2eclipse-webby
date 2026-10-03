package org.sonatype.m2e.webby.internal.config;

import java.util.List;
import java.util.Objects;

import org.apache.maven.model.Resource;
import org.sonatype.m2e.webby.internal.util.WarUtils;

/**
 * A web resource of the maven-war-plugin, the main one being the WAR source directory.
 */
public class ResourceConfiguration extends Resource {

  private static final long serialVersionUID = -7661495998647821682L;

  private String encoding;

  public ResourceConfiguration() {
    // enables no-arg constructor
  }

  public ResourceConfiguration(String directory, List<String> includes, List<String> excludes) {
    setDirectory(directory);
    setIncludes(includes);
    setExcludes(excludes);
  }

  public String getEncoding() {
    return encoding;
  }

  public void setEncoding(String encoding) {
    this.encoding = encoding;
  }

  @Override
  public void setTargetPath(String targetPath) {
    targetPath = (targetPath != null) ? targetPath : "";
    if (targetPath.endsWith("/")) {
      targetPath = targetPath.substring(0, targetPath.length() - 1);
    }
    if (".".equals(targetPath)) {
      targetPath = "";
    }
    super.setTargetPath(targetPath);
  }

  public String getTargetPath(String sourcePath) {
    return WarUtils.getTargetPath(getTargetPath(), sourcePath);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof ResourceConfiguration)) {
      return false;
    }
    ResourceConfiguration that = (ResourceConfiguration) obj;
    return Objects.equals(getDirectory(), that.getDirectory()) && Objects.equals(getIncludes(), that.getIncludes())
        && Objects.equals(getExcludes(), that.getExcludes()) && isFiltering() == that.isFiltering()
        && Objects.equals(getTargetPath(), that.getTargetPath()) && Objects.equals(getEncoding(), that.getEncoding());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getDirectory(), getIncludes(), getExcludes());
  }

}
