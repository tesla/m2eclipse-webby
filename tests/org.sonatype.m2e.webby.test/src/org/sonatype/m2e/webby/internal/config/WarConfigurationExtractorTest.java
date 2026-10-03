package org.sonatype.m2e.webby.internal.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.File;
import java.util.List;

import org.apache.maven.model.Build;
import org.apache.maven.model.InputLocation;
import org.apache.maven.model.InputSource;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WarConfigurationExtractorTest {

  @Test
  void resolveKeepsPathWithoutBasedir() {
    assertEquals("test/path", WarConfigurationExtractor.resolve(null, "test/path"));
    assertNull(WarConfigurationExtractor.resolve(null, null));
    assertNull(WarConfigurationExtractor.resolve("basedir", null));
  }

  @Test
  void resolveAlignsRelativePathWithBasedir() {
    String expected = new File(System.getProperty("user.dir"),
        "basedir" + File.separator + "test" + File.separator + "path").getAbsolutePath();
    assertEquals(expected, WarConfigurationExtractor.resolve("./basedir", "test/path"));
    assertEquals(expected, WarConfigurationExtractor.resolve("basedir", "test/path"));
    assertEquals(expected, WarConfigurationExtractor.resolve("basedir", "./test/path"));
    assertEquals(expected, WarConfigurationExtractor.resolve("basedir", "other/../test/path"));
  }

  @Test
  void resolveKeepsAbsolutePath(@TempDir File tmp) {
    String absolute = new File(tmp, "abs").getAbsolutePath();
    assertEquals(absolute, WarConfigurationExtractor.resolve("basedir", absolute));
  }

  @Test
  void splitsPatternLists() {
    assertEquals(List.of(), WarConfigurationExtractor.split(null));
    assertEquals(List.of(), WarConfigurationExtractor.split(""));
    assertEquals(List.of("a/**", "b.txt"), WarConfigurationExtractor.split("a/**, b.txt,,"));
  }

  @Test
  void computesWorkDirectory(@TempDir File basedir) {
    MavenProject project = new MavenProject(new Model());
    project.setFile(new File(basedir, "pom.xml"));
    Build build = new Build();
    build.setDirectory("target");
    project.getModel().setBuild(build);

    assertEquals(new File(basedir, "target" + File.separator + "m2e-webby").getAbsolutePath(),
        new WarConfigurationExtractor().getWorkDirectory(project));
  }

  @Test
  void locatesWarPluginConfiguration() {
    Model model = new Model();
    model.setBuild(new Build());
    MavenProject project = new MavenProject(model);
    WarConfigurationExtractor extractor = new WarConfigurationExtractor();
    assertNull(extractor.getConfigurationLocation(project));

    Plugin other = new Plugin();
    other.setArtifactId("maven-compiler-plugin");
    model.getBuild().addPlugin(other);
    Plugin war = new Plugin();
    war.setArtifactId("maven-war-plugin");
    InputLocation location = new InputLocation(12, 7, new InputSource());
    war.setLocation("artifactId", location);
    model.getBuild().addPlugin(war);

    assertEquals(location, extractor.getConfigurationLocation(project));
  }

}
