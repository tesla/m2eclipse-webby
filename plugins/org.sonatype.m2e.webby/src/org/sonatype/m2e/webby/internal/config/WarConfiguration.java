package org.sonatype.m2e.webby.internal.config;

import java.io.*;
import java.util.*;

public class WarConfiguration implements Serializable {

  private static final long serialVersionUID = -3252093653638950999L;

  private static String DEFAULT_FILENAME_MAPPING = "@{artifactId}@-@{version}@@{dashClassifier?}@.@{extension}@";

  private String classesDirectory;

  private String workDirectory;

  private List<OverlayConfiguration> overlays = new ArrayList<OverlayConfiguration>();

  private List<ResourceConfiguration> resources = new ArrayList<ResourceConfiguration>();

  private String filenameMapping = DEFAULT_FILENAME_MAPPING;

  private String webXml;

  private boolean webXmlFiltered;

  private boolean backslashesInFilePathEscaped;

  private String escapeString;

  private List<String> filters = new ArrayList<String>();

  private List<String> nonFilteredFileExtensions = new ArrayList<String>();

  private List<String> packagingIncludes = new ArrayList<String>();

  private List<String> packagingExcludes = new ArrayList<String>();

  public String getClassesDirectory() {
    return classesDirectory;
  }

  public void setClassesDirectory(String classesDirectory) {
    this.classesDirectory = classesDirectory;
  }

  public String getWorkDirectory() {
    return workDirectory;
  }

  public void setWorkDirectory(String workDirectory) {
    this.workDirectory = workDirectory;
  }

  public String getWarDirectory() {
    if (workDirectory == null) {
      return null;
    }
    return new File(workDirectory, "war").getPath();
  }

  public List<OverlayConfiguration> getOverlays() {
    return overlays;
  }

  public void setOverlays(List<OverlayConfiguration> overlays) {
    this.overlays = (overlays != null) ? overlays : new ArrayList<OverlayConfiguration>();
  }

  public List<ResourceConfiguration> getResources() {
    return resources;
  }

  public void setResources(List<ResourceConfiguration> resources) {
    this.resources = (resources != null) ? resources : new ArrayList<ResourceConfiguration>();
  }

  public String getFilenameMapping() {
    return filenameMapping;
  }

  public void setFilenameMapping(String filenameMapping) {
    this.filenameMapping = (filenameMapping != null && filenameMapping.length() > 0) ? filenameMapping
        : DEFAULT_FILENAME_MAPPING;
  }

  public String getEscapeString() {
    return escapeString;
  }

  public void setEscapeString(String escapeString) {
    this.escapeString = escapeString;
  }

  public String getWebXml() {
    return webXml;
  }

  public void setWebXml(String webXml) {
    this.webXml = webXml;
  }

  public boolean isWebXmlFiltered() {
    return webXmlFiltered;
  }

  public void setWebXmlFiltered(boolean webXmlFiltered) {
    this.webXmlFiltered = webXmlFiltered;
  }

  public List<String> getNonFilteredFileExtensions() {
    return nonFilteredFileExtensions;
  }

  public void setNonFilteredFileExtensions(List<String> nonFilteredFileExtensions) {
    this.nonFilteredFileExtensions = (nonFilteredFileExtensions != null) ? nonFilteredFileExtensions
        : new ArrayList<String>();
  }

  public boolean isBackslashesInFilePathEscaped() {
    return backslashesInFilePathEscaped;
  }

  public void setBackslashesInFilePathEscaped(boolean backslashesInFilePathEscaped) {
    this.backslashesInFilePathEscaped = backslashesInFilePathEscaped;
  }

  public List<String> getFilters() {
    return filters;
  }

  public void setFilters(List<String> filters) {
    this.filters = (filters != null) ? filters : new ArrayList<String>();
  }

  public List<String> getPackagingIncludes() {
    return packagingIncludes;
  }

  public void setPackagingIncludes(List<String> packagingIncludes) {
    this.packagingIncludes = (packagingIncludes != null) ? packagingIncludes : new ArrayList<String>();
  }

  public List<String> getPackagingExcludes() {
    return packagingExcludes;
  }

  public void setPackagingExcludes(List<String> packagingExcludes) {
    this.packagingExcludes = (packagingExcludes != null) ? packagingExcludes : new ArrayList<String>();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof WarConfiguration)) {
      return false;
    }
    WarConfiguration that = (WarConfiguration) obj;
    return eq(getResources(), that.getResources()) && eq(getOverlays(), that.getOverlays())
        && eq(getClassesDirectory(), that.getClassesDirectory()) && eq(getWorkDirectory(), that.getWorkDirectory())
        && eq(getWebXml(), that.getWebXml()) && isWebXmlFiltered() == that.isWebXmlFiltered()
        && isBackslashesInFilePathEscaped() == that.isBackslashesInFilePathEscaped()
        && eq(getEscapeString(), that.getEscapeString()) && eq(getPackagingIncludes(), that.getPackagingIncludes())
        && eq(getPackagingExcludes(), that.getPackagingExcludes()) && eq(getFilters(), that.getFilters())
        && eq(getFilenameMapping(), that.getFilenameMapping())
        && eq(getNonFilteredFileExtensions(), that.getNonFilteredFileExtensions());
  }

  private static <T> boolean eq(T s1, T s2) {
    return s1 != null ? s1.equals(s2) : s2 == null;
  }

  @Override
  public int hashCode() {
    int hash = 17;
    hash = hash * 31 + hash(getClassesDirectory());
    hash = hash * 31 + hash(getOverlays());
    hash = hash * 31 + hash(getResources());
    hash = hash * 31 + hash(getWebXml());
    hash = hash * 31 + (isWebXmlFiltered() ? 1 : 0);
    hash = hash * 31 + hash(getFilters());
    hash = hash * 31 + hash(getFilenameMapping());
    return hash;
  }

  private static int hash(Object obj) {
    return obj != null ? obj.hashCode() : 0;
  }

  public void save(File file) throws IOException {
    file.getAbsoluteFile().getParentFile().mkdirs();
    FileOutputStream fos = new FileOutputStream(file);
    try {
      ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(fos));
      oos.writeObject(this);
      oos.close();
    } finally {
      fos.close();
    }
  }

  public static WarConfiguration load(File file) throws IOException {
    FileInputStream fis = new FileInputStream(file);
    try {
      ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(fis));
      Object warConfig = ois.readObject();
      ois.close();
      if (warConfig instanceof WarConfiguration) {
        return (WarConfiguration) warConfig;
      } else {
        throw new IOException("Corrupted object stream");
      }
    } catch (ClassNotFoundException e) {
      throw (IOException) new IOException("Corrupted object stream").initCause(e);
    } finally {
      fis.close();
    }
  }

}
