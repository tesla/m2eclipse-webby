package org.sonatype.m2e.webby.internal.config;

import java.util.List;

import org.apache.maven.model.Resource;
import org.sonatype.m2e.webby.internal.util.WarUtils;

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
    return eq(getDirectory(), that.getDirectory()) && eq(getIncludes(), that.getIncludes())
        && eq(getExcludes(), that.getExcludes()) && isFiltering() == that.isFiltering()
        && eq(getTargetPath(), that.getTargetPath()) && eq(getEncoding(), that.getEncoding());
  }

  private static <T> boolean eq(T s1, T s2) {
    return s1 != null ? s1.equals(s2) : s2 == null;
  }

  @Override
  public int hashCode() {
    int hash = 17;
    hash = hash * 31 + hash(getDirectory());
    hash = hash * 31 + hash(getIncludes());
    hash = hash * 31 + hash(getExcludes());
    return hash;
  }

  private static int hash(Object obj) {
    return obj != null ? obj.hashCode() : 0;
  }

}
