package org.sonatype.m2e.webby.internal.build;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.filtering.DefaultMavenFileFilter;
import org.apache.maven.shared.filtering.MavenFilteringException;
import org.apache.maven.shared.filtering.MavenResourcesExecution;
import org.apache.maven.shared.utils.io.FileUtils;
import org.codehaus.plexus.MutablePlexusContainer;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.xml.XmlStreamReader;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.embedder.MavenImpl;
import org.sonatype.m2e.webby.internal.config.WarConfiguration;

/**
 * Filters web resources with the Maven properties and filter files, like the maven-war-plugin does.
 */
@SuppressWarnings("restriction")
public class FilteringHandler {

  private final WarConfiguration warConfig;

  private final MavenProject mvnProject;

  private final MavenSession mvnSession;

  private static final Set<String> DEFAULT_NON_FILTERED_EXTENSIONS = Set.of("jpg", "jpeg", "gif", "bmp", "png");

  private static final Set<String> XML_EXTENSIONS = Set.of("xml", "jspx");

  private final Set<String> nonFilteredExtensions;

  private FileUtils.FilterWrapper[] filterWrappers;

  public FilteringHandler(WarConfiguration warConfig, MavenProject mvnProject, MavenSession mvnSession) {
    this.warConfig = warConfig;
    this.mvnProject = mvnProject;
    this.mvnSession = mvnSession;

    nonFilteredExtensions = new HashSet<>(DEFAULT_NON_FILTERED_EXTENSIONS);
    for (String ext : warConfig.getNonFilteredFileExtensions()) {
      nonFilteredExtensions.add(ext.toLowerCase(Locale.ENGLISH));
    }
  }

  private FileUtils.FilterWrapper[] getFilterWrappers() throws IOException {
    if (filterWrappers == null) {
      try {
        MavenResourcesExecution mavenResourcesExecution = new MavenResourcesExecution();
        mavenResourcesExecution.setEscapeString(warConfig.getEscapeString());
        DefaultMavenFileFilter fileFilter = new DefaultMavenFileFilter();
        fileFilter.enableLogging(
            ((MutablePlexusContainer) ((MavenImpl) MavenPlugin.getMaven()).getPlexusContainer()).getLogger());
        List<FileUtils.FilterWrapper> wrappers = fileFilter.getDefaultFilterWrappers(mvnProject,
            warConfig.getFilters(), warConfig.isBackslashesInFilePathEscaped(), mvnSession, mavenResourcesExecution);
        filterWrappers = wrappers.toArray(FileUtils.FilterWrapper[]::new);
      } catch (MavenFilteringException | CoreException e) {
        throw new IOException("Failed to setup web resource filtering for " + mvnProject, e);
      }
    }
    return filterWrappers;
  }

  public FilteringInput getReader(InputStream is, String path, String encoding) throws IOException {
    String ext = getExtension(path);
    if (nonFilteredExtensions.contains(ext)) {
      return null;
    }

    Reader reader;
    if (isXml(ext)) {
      XmlStreamReader xsr = ReaderFactory.newXmlReader(is);
      reader = xsr;
      encoding = xsr.getEncoding();
    } else if (isProperties(ext)) {
      encoding = "ISO-8859-1";
      reader = new InputStreamReader(is, encoding);
    } else if (encoding != null && !encoding.isEmpty()) {
      reader = new InputStreamReader(is, encoding);
    } else {
      InputStreamReader isr = new InputStreamReader(is);
      reader = isr;
      encoding = isr.getEncoding();
    }

    for (FileUtils.FilterWrapper filterWrapper : getFilterWrappers()) {
      reader = filterWrapper.getReader(reader);
    }

    return new FilteringInput(reader, encoding);
  }

  static String getExtension(String path) {
    String name = new File(path).getName();
    int dot = name.lastIndexOf('.');
    return (dot < 0) ? "" : name.substring(dot + 1).toLowerCase(Locale.ENGLISH);
  }

  private static boolean isXml(String extension) {
    return XML_EXTENSIONS.contains(extension);
  }

  private static boolean isProperties(String extension) {
    return "properties".equals(extension);
  }

  public static class FilteringInput {

    public final Reader reader;

    public final String encoding;

    FilteringInput(Reader reader, String encoding) {
      this.reader = reader;
      this.encoding = encoding;
    }

  }

}
