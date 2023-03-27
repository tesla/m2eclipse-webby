package org.sonatype.m2e.webby.internal.build;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Arrays;
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

@SuppressWarnings("restriction")
public class FilteringHandler {

  private final WarConfiguration warConfig;

  private final MavenProject mvnProject;

  private final MavenSession mvnSession;

  private String[] DEFAULT_NON_FILTERED_EXTENSIONS = {"jpg", "jpeg", "gif", "bmp", "png"};

  private Set<String> nonFilteredExtensions;

  private final Set<String> xmlExtensions = new HashSet<String>(Arrays.asList("xml", "jspx"));

  private FileUtils.FilterWrapper[] filterWrappers;

  public FilteringHandler(WarConfiguration warConfig, MavenProject mvnProject, MavenSession mvnSession) {
    this.warConfig = warConfig;
    this.mvnProject = mvnProject;
    this.mvnSession = mvnSession;

    nonFilteredExtensions = new HashSet<String>();
    for(String ext : warConfig.getNonFilteredFileExtensions()) {
      nonFilteredExtensions.add(ext.toLowerCase(Locale.ENGLISH));
    }
    for(String ext : DEFAULT_NON_FILTERED_EXTENSIONS) {
      nonFilteredExtensions.add(ext);
    }
  }

  private FileUtils.FilterWrapper[] getFilterWrappers() throws IOException {
    if(filterWrappers == null) {
      try {
        MavenResourcesExecution mavenResourcesExecution = new MavenResourcesExecution();
        mavenResourcesExecution.setEscapeString(warConfig.getEscapeString());
        DefaultMavenFileFilter fileFilter = new DefaultMavenFileFilter();
        fileFilter.enableLogging(((MutablePlexusContainer) ((MavenImpl)MavenPlugin.getMaven()).getPlexusContainer()).getLogger());
        @SuppressWarnings("unchecked")
        List<FileUtils.FilterWrapper> wrappers = fileFilter.getDefaultFilterWrappers(mvnProject,
            warConfig.getFilters(), warConfig.isBackslashesInFilePathEscaped(), mvnSession, mavenResourcesExecution);
        filterWrappers = wrappers.toArray(new FileUtils.FilterWrapper[wrappers.size()]);
      } catch(MavenFilteringException e) {
        throw (IOException) new IOException("Failed to setup web resource filtering for " + mvnProject).initCause(e);
      } catch(CoreException e) {
		throw new RuntimeException(e.getMessage(), e);
	  }
    }
    return filterWrappers;
  }

  @SuppressWarnings("deprecation")
  public FilteringInput getReader(InputStream is, String path, String encoding) throws IOException {
    String ext = getExtension(path);
    if(nonFilteredExtensions.contains(ext)) {
      return null;
    }

    Reader reader;
    if(isXml(ext)) {
      XmlStreamReader xsr = ReaderFactory.newXmlReader(is);
      reader = xsr;
      encoding = xsr.getEncoding();
    } else if(isProperties(ext)) {
      encoding = "ISO-8859-1";
      reader = new InputStreamReader(is, encoding);
    } else if(encoding != null && encoding.length() > 0) {
      reader = new InputStreamReader(is, encoding);
    } else {
      InputStreamReader isr = new InputStreamReader(is);
      reader = isr;
      encoding = isr.getEncoding();
    }

    for(FileUtils.FilterWrapper filterWrapper : getFilterWrappers()) {
      reader = filterWrapper.getReader(reader);
    }

    return new FilteringInput(reader, encoding);
  }

  private String getExtension(String path) {
    String name = new File(path).getName();
    int dot = name.lastIndexOf('.');
    String ext = (dot < 0) ? "" : name.substring(dot + 1).toLowerCase(Locale.ENGLISH);
    return ext;
  }

  private boolean isXml(String extension) {
    return xmlExtensions.contains(extension);
  }

  private boolean isProperties(String extension) {
    return "properties".equals(extension);
  }

  public class FilteringInput {

    public final Reader reader;

    public final String encoding;

    FilteringInput(Reader reader, String encoding) {
      this.reader = reader;
      this.encoding = encoding;
    }

  }

}
