package org.sonatype.m2e.webby.internal.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.junit.jupiter.api.Test;

class FilenameMapperTest {

  private static final String DEFAULT_MAPPING = "@{artifactId}@-@{version}@@{dashClassifier?}@.@{extension}@";

  private static Artifact artifact(String scope, String type, String classifier) {
    DefaultArtifactHandler handler = new DefaultArtifactHandler(type);
    handler.setExtension("par".equals(type) ? "par" : type.startsWith("ejb") || type.equals("test-jar") ? "jar" : type);
    DefaultArtifact artifact = new DefaultArtifact("org.example", "lib", "1.0-SNAPSHOT", scope, type, classifier,
        handler);
    artifact.setFile(new File("lib.jar"));
    return artifact;
  }

  @Test
  void mapsWithDefaultMapping() {
    FilenameMapper mapper = new FilenameMapper(DEFAULT_MAPPING);
    assertEquals("lib-1.0-SNAPSHOT.jar", mapper.mapFilename(artifact("compile", "jar", null)));
    assertEquals("lib-1.0-SNAPSHOT-tests.jar", mapper.mapFilename(artifact("compile", "test-jar", "tests")));
    assertEquals("lib-1.0-SNAPSHOT.jar", mapper.mapFilename(artifact("compile", "par", null)));
  }

  @Test
  void mapsWithCustomMapping() {
    FilenameMapper mapper = new FilenameMapper(
        "@{groupId}@.@{artifactId}@-@{baseVersion}@@{dashClassifier}@-@{classifier}@.@{extension}@");
    assertEquals("org.example.lib-1.0-SNAPSHOT-jdk17-jdk17.jar",
        mapper.mapFilename(artifact("runtime", "jar", "jdk17")));
  }

  @Test
  void computesTargetDirectories() {
    assertEquals("WEB-INF/lib/", FilenameMapper.getTargetDir(artifact("compile", "jar", null)));
    assertEquals("WEB-INF/lib/", FilenameMapper.getTargetDir(artifact("runtime", "ejb", null)));
    assertEquals("WEB-INF/lib/", FilenameMapper.getTargetDir(artifact("runtime", "ejb-client", null)));
    assertEquals("WEB-INF/tld/", FilenameMapper.getTargetDir(artifact("compile", "tld", null)));
    assertEquals("WEB-INF/services/", FilenameMapper.getTargetDir(artifact("compile", "aar", null)));
    assertEquals("WEB-INF/modules/", FilenameMapper.getTargetDir(artifact("compile", "mar", null)));
    assertNull(FilenameMapper.getTargetDir(artifact("compile", "war", null)));
    assertNull(FilenameMapper.getTargetDir(artifact("provided", "jar", null)));
    assertNull(FilenameMapper.getTargetDir(artifact("test", "jar", null)));

    Artifact optional = artifact("compile", "jar", null);
    optional.setOptional(true);
    assertNull(FilenameMapper.getTargetDir(optional));
  }

  @Test
  void computesTargetPaths() {
    FilenameMapper mapper = new FilenameMapper(DEFAULT_MAPPING);
    Artifact jar = artifact("compile", "jar", null);
    Artifact provided = artifact("provided", "jar", null);
    assertEquals("WEB-INF/lib/lib-1.0-SNAPSHOT.jar", mapper.getTargetPath(jar));
    assertNull(mapper.getTargetPath(provided));

    Map<String, Artifact> paths = mapper.getTargetPaths(List.of(jar, provided));
    assertEquals(Map.of("WEB-INF/lib/lib-1.0-SNAPSHOT.jar", jar), paths);
  }

}
