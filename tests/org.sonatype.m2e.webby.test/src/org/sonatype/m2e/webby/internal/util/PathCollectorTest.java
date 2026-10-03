package org.sonatype.m2e.webby.internal.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PathCollectorTest {

  @TempDir
  Path dir;

  private void touch(String path) throws IOException {
    Path file = dir.resolve(path);
    Files.createDirectories(file.getParent());
    Files.writeString(file, path);
  }

  private static Set<String> normalized(String[] files) {
    return Arrays.stream(files).map(f -> f.replace(File.separatorChar, '/')).collect(Collectors.toSet());
  }

  @Test
  void collectsAllFilesByDefault() throws IOException {
    touch("index.html");
    touch("WEB-INF/web.xml");
    touch(".git/config");

    PathCollector collector = new PathCollector(null, null);
    assertEquals(Set.of("index.html", "WEB-INF/web.xml"), normalized(collector.collectFiles(dir.toString())),
        "default excludes apply");
  }

  @Test
  void appliesPatterns() throws IOException {
    touch("index.html");
    touch("a/page.html");
    touch("a/secret.html");
    touch("a/style.css");

    PathCollector collector = new PathCollector(List.of("**/*.html"), List.of("**/secret*"));
    assertEquals(Set.of("index.html", "a/page.html"), normalized(collector.collectFiles(dir.toFile())));
    // the collector can be reused
    assertEquals(Set.of("index.html", "a/page.html"), normalized(collector.collectFiles(dir.toFile())));
    assertTrue(collector.toString().contains("**/*.html"));
  }

  @Test
  void handlesMissingDirectory() {
    assertEquals(0, new PathCollector(null, null).collectFiles(dir.resolve("missing").toFile()).length);
  }

  @Test
  void handlesMissingDelta() {
    String[][] files = new PathCollector(null, null).collectFiles((org.eclipse.core.resources.IResourceDelta) null);
    assertEquals(0, files[0].length);
    assertEquals(0, files[1].length);
  }

}
