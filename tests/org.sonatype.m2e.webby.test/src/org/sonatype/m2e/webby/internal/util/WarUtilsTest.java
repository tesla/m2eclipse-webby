package org.sonatype.m2e.webby.internal.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;

class WarUtilsTest {

  @Test
  void computesTargetPath() {
    assertEquals("a/b.txt", WarUtils.getTargetPath(null, "a/b.txt"));
    assertEquals("a/b.txt", WarUtils.getTargetPath("", "a\\b.txt"));
    assertEquals("WEB-INF/a/b.txt", WarUtils.getTargetPath("WEB-INF", "a/b.txt"));
    assertEquals("WEB-INF/a/b.txt", WarUtils.getTargetPath("WEB-INF/", "a/b.txt"));
    assertEquals("WEB-INF/a/b.txt", WarUtils.getTargetPath("WEB-INF/", "/a/b.txt"));
    assertEquals("WEB-INF/a/b.txt", WarUtils.getTargetPath("WEB-INF", "/a/b.txt"));
    assertEquals("WEB-INF/a/b.txt", WarUtils.getTargetPath("WEB-INF\\", "a\\b.txt"));
  }

  @Test
  void computesSourcePath() {
    assertEquals("a/b.txt", WarUtils.getSourcePath(null, "a/b.txt"));
    assertEquals("a/b.txt", WarUtils.getSourcePath("WEB-INF", "WEB-INF/a/b.txt"));
    assertEquals("a/b.txt", WarUtils.getSourcePath("WEB-INF/", "WEB-INF\\a\\b.txt"));
    assertNull(WarUtils.getSourcePath("WEB-INF", "META-INF/a/b.txt"));
  }

  @Test
  void findsOverlayArtifacts() {
    MavenProject project = new MavenProject();
    Set<Artifact> artifacts = new LinkedHashSet<>();
    artifacts.add(artifact("g", "overlay", "war"));
    artifacts.add(artifact("g", "zipped", "zip"));
    artifacts.add(artifact("g", "lib", "jar"));
    project.setArtifacts(artifacts);

    Map<String, Artifact> overlays = WarUtils.getOverlayArtifacts(project);
    assertEquals(Set.of("g:overlay:war", "g:zipped:zip"), overlays.keySet());
  }

  static Artifact artifact(String groupId, String artifactId, String type) {
    return new DefaultArtifact(groupId, artifactId, "1.0", Artifact.SCOPE_COMPILE, type, null,
        new DefaultArtifactHandler(type));
  }

}
