package org.sonatype.m2e.webby.internal.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WarConfigurationTest {

  static WarConfiguration sample() {
    WarConfiguration config = new WarConfiguration();
    config.setClassesDirectory("/p/target/classes");
    config.setWorkDirectory("/p/target/m2e-webby");
    config.setWebXml("/p/src/main/webapp/WEB-INF/web.xml");
    config.setWebXmlFiltered(true);
    config.setBackslashesInFilePathEscaped(true);
    config.setEscapeString("\\");
    config.setFilenameMapping("@{artifactId}@.@{extension}@");
    config.setFilters(List.of("/p/filter.properties"));
    config.setNonFilteredFileExtensions(List.of("pdf"));
    config.setPackagingIncludes(List.of("**"));
    config.setPackagingExcludes(List.of("WEB-INF/lib/bad.jar"));
    ResourceConfiguration resource = new ResourceConfiguration("/p/src/main/webapp", List.of(), List.of("x"));
    resource.setTargetPath("WEB-INF/");
    resource.setFiltering(true);
    resource.setEncoding("UTF-8");
    config.setResources(List.of(resource));
    OverlayConfiguration overlay = new OverlayConfiguration("g", "a", "c", "war");
    overlay.setEncoding("UTF-8");
    config.setOverlays(List.of(new OverlayConfiguration(null, null, null, null), overlay));
    return config;
  }

  @Test
  void hasDefaults() {
    WarConfiguration config = new WarConfiguration();
    assertNull(config.getWarDirectory());
    assertEquals("@{artifactId}@-@{version}@@{dashClassifier?}@.@{extension}@", config.getFilenameMapping());
    config.setFilenameMapping("");
    assertEquals("@{artifactId}@-@{version}@@{dashClassifier?}@.@{extension}@", config.getFilenameMapping());

    config.setOverlays(null);
    config.setResources(null);
    config.setFilters(null);
    config.setNonFilteredFileExtensions(null);
    config.setPackagingIncludes(null);
    config.setPackagingExcludes(null);
    assertTrue(config.getOverlays().isEmpty());
    assertTrue(config.getResources().isEmpty());
    assertTrue(config.getFilters().isEmpty());
    assertTrue(config.getNonFilteredFileExtensions().isEmpty());
    assertTrue(config.getPackagingIncludes().isEmpty());
    assertTrue(config.getPackagingExcludes().isEmpty());
  }

  @Test
  void locatesWarDirectory() {
    WarConfiguration config = new WarConfiguration();
    config.setWorkDirectory("work");
    assertEquals("work" + File.separator + "war", config.getWarDirectory());
  }

  @Test
  void savesAndLoads(@TempDir Path tmp) throws IOException {
    WarConfiguration config = sample();
    File file = tmp.resolve("a/b/config.ser").toFile();
    config.save(file);

    WarConfiguration loaded = WarConfiguration.load(file);
    assertEquals(config, loaded);
    assertEquals(config.hashCode(), loaded.hashCode());
    assertEquals("WEB-INF", loaded.getResources().get(0).getTargetPath());
  }

  @Test
  void rejectsUnexpectedContent(@TempDir Path tmp) throws IOException {
    Path other = tmp.resolve("other.ser");
    try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(other))) {
      oos.writeObject("a string");
    }
    assertThrows(IOException.class, () -> WarConfiguration.load(other.toFile()));

    Path unexpectedClass = tmp.resolve("file.ser");
    try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(unexpectedClass))) {
      oos.writeObject(new File("x"));
    }
    assertThrows(IOException.class, () -> WarConfiguration.load(unexpectedClass.toFile()));
  }

  @Test
  void detectsChanges() {
    WarConfiguration a = sample();
    assertEquals(a, a);
    assertNotEquals(a, null);
    assertNotEquals(a, "config");

    WarConfiguration b = sample();
    b.setWebXmlFiltered(false);
    assertNotEquals(a, b);

    b = sample();
    b.setBackslashesInFilePathEscaped(false);
    assertNotEquals(a, b);

    b = sample();
    b.setEscapeString(null);
    assertNotEquals(a, b);

    b = sample();
    b.setClassesDirectory("other");
    assertNotEquals(a, b);

    b = sample();
    b.setWorkDirectory("other");
    assertNotEquals(a, b);

    b = sample();
    b.setWebXml(null);
    assertNotEquals(a, b);

    b = sample();
    b.setPackagingExcludes(List.of());
    assertNotEquals(a, b);

    b = sample();
    b.setPackagingIncludes(List.of());
    assertNotEquals(a, b);

    b = sample();
    b.setFilters(List.of());
    assertNotEquals(a, b);

    b = sample();
    b.setNonFilteredFileExtensions(List.of());
    assertNotEquals(a, b);

    b = sample();
    b.setFilenameMapping(null);
    assertNotEquals(a, b);

    b = sample();
    b.setOverlays(List.of());
    assertNotEquals(a, b);

    b = sample();
    b.setResources(List.of());
    assertNotEquals(a, b);
  }

}
