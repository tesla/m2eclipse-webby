package org.sonatype.m2e.webby.internal.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class ResourceConfigurationTest {

  @Test
  void normalizesTargetPath() {
    ResourceConfiguration resource = new ResourceConfiguration();
    resource.setTargetPath("WEB-INF/");
    assertEquals("WEB-INF", resource.getTargetPath());
    assertEquals("WEB-INF/a.txt", resource.getTargetPath("a.txt"));

    resource.setTargetPath(".");
    assertEquals("", resource.getTargetPath());
    assertEquals("a.txt", resource.getTargetPath("a.txt"));

    resource.setTargetPath(null);
    assertEquals("", resource.getTargetPath());
  }

  @Test
  void implementsEquality() {
    ResourceConfiguration a = new ResourceConfiguration("src/main/webapp", List.of("**"), List.of("x"));
    ResourceConfiguration b = new ResourceConfiguration("src/main/webapp", List.of("**"), List.of("x"));
    assertEquals(a, a);
    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
    assertNotEquals(a, null);
    assertNotEquals(a, "src/main/webapp");

    b.setEncoding("UTF-8");
    assertNotEquals(a, b);
    a.setEncoding("UTF-8");
    assertEquals(a, b);
    assertEquals("UTF-8", a.getEncoding());

    b.setFiltering(true);
    assertNotEquals(a, b);
    b.setFiltering(false);
    b.setTargetPath("WEB-INF");
    assertNotEquals(a, b);
    b.setTargetPath(null);
    b.setDirectory("other");
    assertNotEquals(a, b);
  }

}
