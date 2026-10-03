package org.sonatype.m2e.webby.internal.build;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sonatype.m2e.webby.internal.util.ResourceRegistry;

class WarAssemblerTest {

  @TempDir
  Path war;

  private static ByteArrayInputStream content(String text) {
    return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
  }

  @Test
  void validatesTargetPaths() {
    assertTrue(WarAssembler.isSafeTargetPath("index.html"));
    assertTrue(WarAssembler.isSafeTargetPath("WEB-INF/web.xml"));
    assertTrue(WarAssembler.isSafeTargetPath("a/../b.txt"));
    assertFalse(WarAssembler.isSafeTargetPath(null));
    assertFalse(WarAssembler.isSafeTargetPath(""));
    assertFalse(WarAssembler.isSafeTargetPath("../escape.txt"));
    assertFalse(WarAssembler.isSafeTargetPath("a/../../escape.txt"));
    assertFalse(WarAssembler.isSafeTargetPath("..\\escape.txt"));
    assertFalse(WarAssembler.isSafeTargetPath("/etc/passwd"));
    assertFalse(WarAssembler.isSafeTargetPath("C:/Windows/win.ini"));
    assertFalse(WarAssembler.isSafeTargetPath("."));
  }

  @Test
  void copiesResources() throws IOException {
    WarAssembler assembler = new WarAssembler(war.toFile(), null, null);
    assembler.copyResourceFile(content("hello"), "a/b/hello.txt", false, null, 0);
    assertEquals("hello", Files.readString(war.resolve("a/b/hello.txt")));

    assembler.copyResourceFile(content("again"), "a/b/hello.txt", false, "UTF-8", 0);
    assertEquals("again", Files.readString(war.resolve("a/b/hello.txt")));
  }

  @Test
  void unregistersAndDeletesResources() throws IOException {
    ResourceRegistry registry = new ResourceRegistry();
    WarAssembler assembler = new WarAssembler(war.toFile(), null, registry);
    assertTrue(assembler.registerTargetPath("index.html", 1));
    assembler.copyResourceFile(content("main"), "index.html", false, null, 0);

    assembler.unregisterTargetPath("missing.html", 1);
    assembler.unregisterTargetPath("index.html", 1);
    assertFalse(Files.exists(war.resolve("index.html")));
  }

  @Test
  void restoresShadowedResourcesOfOtherOverlays(@TempDir Path overlay) throws IOException {
    Files.writeString(overlay.resolve("index.html"), "overlay");
    Files.writeString(overlay.resolve("other.html"), "other");

    WarAssembler assembler = new WarAssembler(war.toFile(), null, new ResourceRegistry());
    assertTrue(assembler.registerTargetPath("index.html", 1));
    assertFalse(assembler.registerTargetPath("index.html", 2));
    assertTrue(assembler.registerTargetPath("gone.html", 1));
    assertFalse(assembler.registerTargetPath("gone.html", 2));

    String[] files = { "other.html" };
    assertSame(files, assembler.appendDirtyTargetPaths(files, 2, overlay.toString(), ""),
        "nothing to restore yet");

    // the main overlay deletes its resources, overlay 2 must provide them again
    assembler.unregisterTargetPath("index.html", 1);
    assembler.unregisterTargetPath("gone.html", 1);
    String[] dirty = assembler.appendDirtyTargetPaths(files, 2, overlay.toString(), "");
    assertEquals(Set.of("index.html", "other.html"), Arrays.stream(dirty).collect(Collectors.toSet()),
        "only resources still existing in the overlay are restored");

    assertArrayEquals(files, assembler.appendDirtyTargetPaths(files, 2, overlay.toString(), ""),
        "dirty paths are only reported once");
  }

  @Test
  void restoresOnlyResourcesOfTargetDirectory(@TempDir Path overlay) throws IOException {
    Files.createDirectories(overlay.resolve("css"));
    Files.writeString(overlay.resolve("css/site.css"), "body{}");

    WarAssembler assembler = new WarAssembler(war.toFile(), null, new ResourceRegistry());
    assembler.registerTargetPath("static/css/site.css", 1);
    assembler.registerTargetPath("static/css/site.css", 2);
    assembler.registerTargetPath("index.html", 1);
    assembler.registerTargetPath("index.html", 2);
    assembler.unregisterTargetPath("static/css/site.css", 1);
    assembler.unregisterTargetPath("index.html", 1);

    String[] dirty = assembler.appendDirtyTargetPaths(new String[0], 2, overlay.toString(), "static");
    assertEquals(Set.of("css/site.css"), Arrays.stream(dirty).map(p -> p.replace('\\', '/'))
        .collect(Collectors.toSet()));
  }

  @Test
  void logsErrors() {
    WarAssembler assembler = new WarAssembler(war.toFile(), null, null);
    // must not throw
    assembler.addError("source", "target", new IOException("expected by test"));
    assembler.addError("source", null, new IOException("expected by test"));
    assembler.addError(new CoreException(Status.error("expected by test")));
  }

}
