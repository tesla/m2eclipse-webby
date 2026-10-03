package org.sonatype.m2e.webby.internal.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ResourceRegistryTest {

  @Test
  void lowestOrdinalWins() {
    ResourceRegistry registry = new ResourceRegistry();
    assertTrue(registry.register("index.html", 2));
    assertTrue(registry.register("index.html", 2), "re-registering the same overlay is accepted");
    assertTrue(registry.register("index.html", 1), "lower ordinal overrides");
    assertFalse(registry.register("index.html", 3), "higher ordinal is shadowed");
    assertTrue(registry.register("index.html", 0));
    assertTrue(registry.register("index.html", 0));
    assertFalse(registry.register("index.html", 1));
  }

  @Test
  void normalizesSeparators() {
    ResourceRegistry registry = new ResourceRegistry();
    assertTrue(registry.register("WEB-INF\\web.xml", 1));
    assertFalse(registry.register("WEB-INF/web.xml", 2));
  }

  @Test
  void unregistersSingleOwner() {
    ResourceRegistry registry = new ResourceRegistry();
    assertNull(registry.unregister("missing", 1));
    registry.register("a", 1);
    assertNull(registry.unregister("a", 2), "not registered by that overlay");
    assertArrayEquals(new int[0], registry.unregister("a", 1));
    assertNull(registry.unregister("a", 1));
  }

  @Test
  void unregistersAmongSeveralOwners() {
    ResourceRegistry registry = new ResourceRegistry();
    registry.register("a", 1);
    registry.register("a", 3);
    registry.register("a", 5);
    assertNull(registry.unregister("a", 4));
    assertArrayEquals(new int[] { 1, 5 }, registry.unregister("a", 3));
    assertArrayEquals(new int[] { 5 }, registry.unregister("a", 1));
    assertTrue(registry.register("a", 5), "remaining overlay now provides the resource");
    assertArrayEquals(new int[0], registry.unregister("a", 5));
  }

  @Test
  void unregistersFromPair() {
    ResourceRegistry registry = new ResourceRegistry();
    registry.register("a", 1);
    registry.register("a", 2);
    assertArrayEquals(new int[] { 1 }, registry.unregister("a", 2));
  }

  @Test
  void savesAndLoads(@TempDir Path tmp) throws IOException {
    ResourceRegistry registry = new ResourceRegistry();
    registry.register("a", 1);
    registry.register("b", 1);
    registry.register("b", 2);
    File file = tmp.resolve("sub/resources.ser").toFile();
    registry.save(file);

    ResourceRegistry loaded = ResourceRegistry.load(file);
    assertFalse(loaded.register("a", 2));
    assertArrayEquals(new int[] { 2 }, loaded.unregister("b", 1));
  }

  @Test
  void rejectsCorruptedFiles(@TempDir Path tmp) throws IOException {
    Path garbage = tmp.resolve("garbage.ser");
    Files.writeString(garbage, "not serialized");
    assertThrows(IOException.class, () -> ResourceRegistry.load(garbage.toFile()));

    Path wrongType = tmp.resolve("list.ser");
    try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(wrongType))) {
      oos.writeObject(new ArrayList<String>());
    }
    assertThrows(IOException.class, () -> ResourceRegistry.load(wrongType.toFile()));

    Path unexpectedClass = tmp.resolve("file.ser");
    try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(unexpectedClass))) {
      oos.writeObject(new java.util.HashMap<>(java.util.Map.of("a", new File("x"))));
    }
    assertThrows(IOException.class, () -> ResourceRegistry.load(unexpectedClass.toFile()),
        "classes outside of the allow list are rejected");
  }

}
