package org.sonatype.m2e.webby.internal.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class OverlayConfigurationTest {

  /** Mimics org.apache.maven.plugins.war.Overlay, which is not visible to Webby. */
  public static class Overlay {

    private String id;

    private String groupId;

    private String artifactId;

    private String classifier;

    private String[] includes = { "**/*.jsp" };

    private String[] excludes;

    private boolean filtered = true;

    private boolean skip;

    private String targetPath = "sub/";

    private String type = "war";

    public String getId() {
      return id;
    }

    public String getGroupId() {
      return groupId;
    }

    public String getArtifactId() {
      return artifactId;
    }

    public String getClassifier() {
      return classifier;
    }

    public String[] getIncludes() {
      return includes;
    }

    public boolean isFiltered() {
      return filtered;
    }

    public boolean isSkip() {
      return skip;
    }

    public String getTargetPath() {
      return targetPath;
    }

    public String getType() {
      return type;
    }

  }

  @Test
  void readsMavenWarPluginOverlay() {
    Overlay overlay = new Overlay();
    overlay.groupId = "org.example";
    overlay.artifactId = "base";
    overlay.classifier = "web";
    overlay.excludes = new String[] { "WEB-INF/lib/**" };

    OverlayConfiguration config = new OverlayConfiguration(overlay);
    assertEquals("org.example:base:war:web", config.getId());
    assertEquals("org.example:base:war:web", config.getArtifactKey());
    assertEquals(List.of("**/*.jsp"), config.getIncludes());
    assertEquals(List.of("WEB-INF/lib/**"), config.getExcludes(), "private field without getter is read too");
    assertTrue(config.isFiltering());
    assertFalse(config.isSkip());
    assertFalse(config.isMain());
    assertEquals("sub", config.getTargetPath());
    assertEquals("sub/index.jsp", config.getTargetPath("index.jsp"));
  }

  @Test
  void recognizesMainOverlay() {
    Overlay overlay = new Overlay();
    overlay.id = "null:null";
    overlay.targetPath = ".";
    overlay.type = null;

    OverlayConfiguration config = new OverlayConfiguration(overlay);
    assertTrue(config.isMain());
    assertEquals("(web project)", config.getId());
    assertEquals("(web project)", config.toString());
    assertEquals("", config.getTargetPath());
    assertEquals("war", config.getType());
  }

  @Test
  void usesExplicitId() {
    Overlay overlay = new Overlay();
    overlay.id = "my-overlay";
    assertEquals("my-overlay", new OverlayConfiguration(overlay).getId());
  }

  @Test
  void normalizesNullValues() {
    OverlayConfiguration config = new OverlayConfiguration(null, null, null, null);
    assertEquals("", config.getGroupId());
    assertEquals("", config.getArtifactId());
    assertEquals("", config.getClassifier());
    assertEquals("war", config.getType());
    assertTrue(config.isMain());
    assertEquals(":" + ":war", config.getArtifactKey());
    assertEquals(List.of("**/**"), config.getIncludes());
    assertEquals(List.of("META-INF/MANIFEST.MF"), config.getExcludes());

    config.setTargetPath(null);
    assertEquals("", config.getTargetPath());
  }

  @Test
  void implementsEquality() {
    OverlayConfiguration a = new OverlayConfiguration("g", "a", null, "zip");
    OverlayConfiguration b = new OverlayConfiguration("g", "a", "", "zip");
    assertEquals(a, a);
    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
    assertNotEquals(a, null);
    assertNotEquals(a, "g:a");

    b.setFiltering(true);
    assertNotEquals(a, b);
    b.setFiltering(false);
    b.setEncoding("UTF-8");
    assertNotEquals(a, b);
    a.setEncoding("UTF-8");
    assertEquals(a, b);
    b.setSkip(true);
    assertNotEquals(a, b);
    b.setSkip(false);
    b.setIncludes(List.of("x"));
    assertNotEquals(a, b);
    b.setIncludes(a.getIncludes());
    b.setExcludes(List.of());
    assertNotEquals(a, b);
    b.setExcludes(a.getExcludes());
    b.setTargetPath("x");
    assertNotEquals(a, b);
    b.setTargetPath("");
    b.setId("other id");
    assertEquals(a, b, "the id is not relevant");
  }

}
