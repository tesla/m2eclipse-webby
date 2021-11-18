/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.sonatype.m2e.webby.internal.config;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.sonatype.m2e.webby.internal.util.WarUtils;



/**
 */
public class OverlayConfiguration implements Serializable {

  private static final long serialVersionUID = 8370499954146691677L;

  private static final String[] DEFAULT_INCLUDES = new String[] {"**/**"};

  private static final String[] DEFAULT_EXCLUDES = new String[] {"META-INF/MANIFEST.MF"};

  private static final String DEFAULT_TYPE = "war";

  private String id;

  private String groupId = "";

  private String artifactId = "";

  private String classifier = "";

  private String type = DEFAULT_TYPE;

  private List<String> includes = toList(DEFAULT_INCLUDES);

  private List<String> excludes = toList(DEFAULT_EXCLUDES);

  private String targetPath = "";

  private boolean filtering = false;

  private String encoding;

  private boolean skip = false;

  private static List<String> toList(String[] array) {
    List<String> list = new ArrayList<String>();
    if(array != null) {
      Collections.addAll(list, array);
    }
    return list;
  }

  public OverlayConfiguration(String groupId, String artifactId, String classifier, String type) {
    setGroupId(groupId);
    setArtifactId(artifactId);
    setClassifier(classifier);
    setType(type);
  }

  public OverlayConfiguration(Object overlay) {
    id = ReflectionUtils.getProperty(overlay, "id", String.class, null);
    if("null:null".equals(id)) {
      id = null;
    }
    setGroupId(ReflectionUtils.getProperty(overlay, "groupId", String.class, groupId));
    setArtifactId(ReflectionUtils.getProperty(overlay, "artifactId", String.class, artifactId));
    setClassifier(ReflectionUtils.getProperty(overlay, "classifier", String.class, classifier));
    setIncludes(toList(ReflectionUtils.getProperty(overlay, "includes", String[].class, DEFAULT_INCLUDES)));
    setExcludes(toList(ReflectionUtils.getProperty(overlay, "excludes", String[].class, DEFAULT_EXCLUDES)));
    setTargetPath(ReflectionUtils.getProperty(overlay, "targetPath", String.class, targetPath));
    setType(ReflectionUtils.getProperty(overlay, "type", String.class, type));
    setFiltering(ReflectionUtils.getProperty(overlay, "filtered", Boolean.class, Boolean.valueOf(filtering)));
    setSkip(ReflectionUtils.getProperty(overlay, "skip", Boolean.class, Boolean.valueOf(skip)));
  }

  public String getId() {
    if(id == null || id.length() <= 0) {
      if(isMain()) {
        return "(web project)";
      }
      return getArtifactKey();
    }
    return id;
  }

  public String getArtifactKey() {
    StringBuilder buffer = new StringBuilder(128);
    buffer.append(getGroupId());
    buffer.append(':').append(getArtifactId());
    buffer.append(':').append(getType());
    if(getClassifier().length() > 0) {
      buffer.append(':').append(getClassifier());
    }
    return buffer.toString();
  }

  public void setId(String id) {
    this.id = id;
  }

  public boolean isMain() {
    return getGroupId().length() <= 0 && getArtifactId().length() <= 0;
  }

  public String getGroupId() {
    return groupId;
  }

  public void setGroupId(String groupId) {
    this.groupId = (groupId != null) ? groupId : "";
  }

  public String getArtifactId() {
    return artifactId;
  }

  public void setArtifactId(String artifactId) {
    this.artifactId = (artifactId != null) ? artifactId : "";
  }

  public String getClassifier() {
    return classifier;
  }

  public void setClassifier(String classifier) {
    this.classifier = (classifier != null) ? classifier : "";
  }

  public List<String> getIncludes() {
    return includes;
  }

  public void setIncludes(List<String> includes) {
    this.includes = includes;
  }

  public List<String> getExcludes() {
    return excludes;
  }

  public void setExcludes(List<String> excludes) {
    this.excludes = excludes;
  }

  public boolean isFiltering() {
    return filtering;
  }

  public void setFiltering(boolean filtering) {
    this.filtering = filtering;
  }

  public String getEncoding() {
    return encoding;
  }

  public void setEncoding(String encoding) {
    this.encoding = encoding;
  }

  public boolean isSkip() {
    return skip;
  }

  public void setSkip(boolean skip) {
    this.skip = skip;
  }

  public String getTargetPath() {
    return targetPath;
  }

  public void setTargetPath(String targetPath) {
    this.targetPath = (targetPath != null) ? targetPath : "";
    if(this.targetPath.endsWith("/")) {
      this.targetPath = this.targetPath.substring(0, this.targetPath.length() - 1);
    }
    if(".".equals(this.targetPath)) {
      this.targetPath = "";
    }
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = (type != null && type.length() > 0) ? type : DEFAULT_TYPE;
  }

  public String getTargetPath(String sourcePath) {
    return WarUtils.getTargetPath(getTargetPath(), sourcePath);
  }

  @Override
  public String toString() {
    return getId();
  }

  @Override
  public boolean equals(Object obj) {
    if(this == obj) {
      return true;
    }
    if(!(obj instanceof OverlayConfiguration)) {
      return false;
    }
    OverlayConfiguration that = (OverlayConfiguration) obj;
    return eq(getArtifactId(), that.getArtifactId()) && eq(getGroupId(), that.getGroupId())
        && eq(getClassifier(), that.getClassifier()) && eq(getType(), that.getType())
        && eq(getIncludes(), that.getIncludes()) && eq(getExcludes(), that.getExcludes())
        && isFiltering() == that.isFiltering() && eq(getEncoding(), that.getEncoding())
        && eq(getTargetPath(), that.getTargetPath()) && isSkip() == that.isSkip();
  }

  private static <T> boolean eq(T s1, T s2) {
    return s1 != null ? s1.equals(s2) : s2 == null;
  }

  @Override
  public int hashCode() {
    int hash = 17;
    hash = hash * 31 + hash(getGroupId());
    hash = hash * 31 + hash(getArtifactId());
    hash = hash * 31 + hash(getClassifier());
    hash = hash * 31 + hash(getType());
    hash = hash * 31 + hash(getIncludes());
    hash = hash * 31 + hash(getExcludes());
    return hash;
  }

  private static int hash(Object obj) {
    return obj != null ? obj.hashCode() : 0;
  }

}
