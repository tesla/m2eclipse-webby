package org.sonatype.m2e.webby.internal.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

class PathSelectorTest {

  @Test
  void selectsEverythingWithoutPatterns() {
    PathSelector selector = new PathSelector(null, null);
    assertTrue(selector.isSelected("index.html"));
    assertTrue(selector.isSelected("WEB-INF/web.xml"));
    assertTrue(selector.isAncestorOfPotentiallySelected("WEB-INF"));
  }

  @Test
  void appliesIncludesAndExcludes() {
    PathSelector selector = new PathSelector(List.of("**/*.html", "WEB-INF/"), List.of("**/secret*"));
    assertTrue(selector.isSelected("index.html"));
    assertTrue(selector.isSelected("pages/about.html"));
    assertTrue(selector.isSelected("WEB-INF/web.xml"), "trailing slash includes the whole directory");
    assertFalse(selector.isSelected("style.css"));
    assertFalse(selector.isSelected("pages/secret.html"));
    assertFalse(selector.isSelected("WEB-INF/secret.properties"));
  }

  @Test
  void matchesWindowsAndUnixSeparators() {
    PathSelector selector = new PathSelector(List.of("WEB-INF/lib/*.jar"), null);
    assertTrue(selector.isSelected("WEB-INF/lib/a.jar"));
    assertTrue(selector.isSelected("WEB-INF\\lib\\a.jar"));
    assertFalse(selector.isSelected("WEB-INF/classes/a.jar"));
  }

  @Test
  void ignoresNullPatterns() {
    PathSelector selector = new PathSelector(Arrays.asList("*.txt", null), null);
    assertTrue(selector.isSelected("a.txt"));
  }

  @Test
  void detectsAncestors() {
    PathSelector selector = new PathSelector(List.of("WEB-INF/lib/*.jar"), null);
    assertTrue(selector.isAncestorOfPotentiallySelected("WEB-INF"));
    assertTrue(selector.isAncestorOfPotentiallySelected("WEB-INF/lib"));
    assertFalse(selector.isAncestorOfPotentiallySelected("META-INF"));
  }

  @Test
  void normalizesPaths() {
    String expected = "a" + File.separator + "b" + File.separator + "c";
    assertEquals(expected, PathSelector.normalizePath("a/b\\c"));
  }

  @Test
  void describesPatterns() {
    String description = new PathSelector(List.of("*.txt"), List.of("b.txt")).toString();
    assertTrue(description.contains("*.txt"));
    assertTrue(description.contains("b.txt"));
  }

}
