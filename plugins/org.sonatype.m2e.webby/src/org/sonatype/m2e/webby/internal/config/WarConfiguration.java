package org.sonatype.m2e.webby.internal.config;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The configuration of the maven-war-plugin relevant to assemble the WAR directory. It is persisted between builds to
 * detect configuration changes that require a full rebuild.
 */
public class WarConfiguration implements Serializable {

  private static final long serialVersionUID = -3252093653638950999L;

  private static final String DEFAULT_FILENAME_MAPPING = "@{artifactId}@-@{version}@@{dashClassifier?}@.@{extension}@";

  private String classesDirectory;

  private String workDirectory;

  private List<OverlayConfiguration> overlays = new ArrayList<>();

  private List<ResourceConfiguration> resources = new ArrayList<>();

  private String filenameMapping = DEFAULT_FILENAME_MAPPING;

  private String webXml;

  private boolean webXmlFiltered;

  private boolean backslashesInFilePathEscaped;

  private String escapeString;

  private List<String> filters = new ArrayList<>();

  private List<String> nonFilteredFileExtensions = new ArrayList<>();

  private List<String> packagingIncludes = new ArrayList<>();

  private List<String> packagingExcludes = new ArrayList<>();

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
    this.overlays = (overlays != null) ? overlays : new ArrayList<>();
  }

  public List<ResourceConfiguration> getResources() {
    return resources;
  }

  public void setResources(List<ResourceConfiguration> resources) {
    this.resources = (resources != null) ? resources : new ArrayList<>();
  }

  public String getFilenameMapping() {
    return filenameMapping;
  }

  public void setFilenameMapping(String filenameMapping) {
    this.filenameMapping = (filenameMapping != null && !filenameMapping.isEmpty()) ? filenameMapping
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
        : new ArrayList<>();
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
    this.filters = (filters != null) ? filters : new ArrayList<>();
  }

  public List<String> getPackagingIncludes() {
    return packagingIncludes;
  }

  public void setPackagingIncludes(List<String> packagingIncludes) {
    this.packagingIncludes = (packagingIncludes != null) ? packagingIncludes : new ArrayList<>();
  }

  public List<String> getPackagingExcludes() {
    return packagingExcludes;
  }

  public void setPackagingExcludes(List<String> packagingExcludes) {
    this.packagingExcludes = (packagingExcludes != null) ? packagingExcludes : new ArrayList<>();
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
    return Objects.equals(getResources(), that.getResources()) && Objects.equals(getOverlays(), that.getOverlays())
        && Objects.equals(getClassesDirectory(), that.getClassesDirectory())
        && Objects.equals(getWorkDirectory(), that.getWorkDirectory()) && Objects.equals(getWebXml(), that.getWebXml())
        && isWebXmlFiltered() == that.isWebXmlFiltered()
        && isBackslashesInFilePathEscaped() == that.isBackslashesInFilePathEscaped()
        && Objects.equals(getEscapeString(), that.getEscapeString())
        && Objects.equals(getPackagingIncludes(), that.getPackagingIncludes())
        && Objects.equals(getPackagingExcludes(), that.getPackagingExcludes())
        && Objects.equals(getFilters(), that.getFilters())
        && Objects.equals(getFilenameMapping(), that.getFilenameMapping())
        && Objects.equals(getNonFilteredFileExtensions(), that.getNonFilteredFileExtensions());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getClassesDirectory(), getOverlays(), getResources(), getWebXml(), isWebXmlFiltered(),
        getFilters(), getFilenameMapping());
  }

  public void save(File file) throws IOException {
    file.getAbsoluteFile().getParentFile().mkdirs();
    try (OutputStream os = Files.newOutputStream(file.toPath());
        ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(os))) {
      oos.writeObject(this);
    }
  }

  public static WarConfiguration load(File file) throws IOException {
    try (InputStream is = Files.newInputStream(file.toPath());
        ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(is))) {
      ois.setObjectInputFilter(SERIALIZATION_FILTER);
      Object warConfig = ois.readObject();
      if (warConfig instanceof WarConfiguration result) {
        return result;
      }
      throw new IOException("Corrupted object stream");
    } catch (ClassNotFoundException | RuntimeException e) {
      throw new IOException("Corrupted object stream", e);
    }
  }

  /** Only the classes of the WAR configuration may be deserialized. */
  private static final ObjectInputFilter SERIALIZATION_FILTER = ObjectInputFilter.Config.createFilter(
      "maxdepth=20;" + WarConfiguration.class.getName() + ";" + OverlayConfiguration.class.getName() + ";"
          + ResourceConfiguration.class.getName() + ";org.apache.maven.model.*;java.util.*;java.lang.*;!*");

}
