package org.sonatype.m2e.webby.internal.build;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sonatype.m2e.webby.internal.config.OverlayConfiguration;
import org.sonatype.m2e.webby.internal.util.ResourceRegistry;

/**
 * Overlays that are not projects of the workspace, i.e. WAR archives or exploded WAR directories.
 */
class ArtifactResourceContributorTest {

  @TempDir
  Path tmp;

  private static void addEntry(ZipOutputStream zos, String name, String content) throws IOException {
    zos.putNextEntry(new ZipEntry(name));
    if (content != null) {
      zos.write(content.getBytes(StandardCharsets.UTF_8));
    }
    zos.closeEntry();
  }

  @Test
  void extractsWarArchive() throws IOException {
    Path archive = tmp.resolve("overlay.war");
    try (OutputStream os = Files.newOutputStream(archive); ZipOutputStream zos = new ZipOutputStream(os)) {
      addEntry(zos, "META-INF/", null);
      addEntry(zos, "META-INF/MANIFEST.MF", "Manifest-Version: 1.0");
      addEntry(zos, "index.html", "overlay index");
      addEntry(zos, "css/site.css", "body{}");
      addEntry(zos, "WEB-INF/lib/lib.jar", "jar");
      addEntry(zos, "../../evil.txt", "outside");
    }
    Path war = tmp.resolve("war");
    ResourceRegistry registry = new ResourceRegistry();
    registry.register("index.html", 1);
    WarAssembler assembler = new WarAssembler(war.toFile(), null, registry);

    OverlayConfiguration overlay = new OverlayConfiguration("g", "overlay", null, "war");
    overlay.setTargetPath("static");
    new ArtifactResourceContributor(2, archive.toFile(), overlay).contribute(assembler, new NullProgressMonitor());

    assertEquals("body{}", Files.readString(war.resolve("static/css/site.css")));
    assertEquals("overlay index", Files.readString(war.resolve("static/index.html")));
    assertTrue(Files.exists(war.resolve("static/WEB-INF/lib/lib.jar")), "libraries of archives are extracted");
    assertFalse(Files.exists(war.resolve("static/META-INF/MANIFEST.MF")), "manifest is excluded by default");
    assertFalse(Files.exists(tmp.resolve("evil.txt")), "entries cannot escape the WAR directory");
  }

  @Test
  void copiesExplodedWar() throws IOException {
    Path exploded = tmp.resolve("exploded");
    Files.createDirectories(exploded.resolve("WEB-INF/classes"));
    Files.writeString(exploded.resolve("index.html"), "overlay index");
    Files.writeString(exploded.resolve("page.html"), "overlay page");
    Files.writeString(exploded.resolve("WEB-INF/classes/A.class"), "class");
    Path war = tmp.resolve("war");
    ResourceRegistry registry = new ResourceRegistry();
    registry.register("index.html", 1);
    WarAssembler assembler = new WarAssembler(war.toFile(), null, registry);

    OverlayConfiguration overlay = new OverlayConfiguration("g", "overlay", null, "war");
    overlay.setIncludes(List.of("**"));
    new ArtifactResourceContributor(2, exploded.toFile(), overlay).contribute(assembler, null);

    assertEquals("overlay page", Files.readString(war.resolve("page.html")));
    assertFalse(Files.exists(war.resolve("index.html")), "the project wins over the overlay");
    assertFalse(Files.exists(war.resolve("WEB-INF/classes/A.class")), "classes of directories are not copied");
  }

  @Test
  void reportsUnreadableArchive() throws IOException {
    Path archive = tmp.resolve("broken.war");
    Files.writeString(archive, "not a zip");
    WarAssembler assembler = new WarAssembler(tmp.resolve("war").toFile(), null, null);
    OverlayConfiguration overlay = new OverlayConfiguration("g", "overlay", null, "war");
    new ArtifactResourceContributor(2, tmp.resolve("missing.war").toFile(), overlay).contribute(assembler, null);
    new ArtifactResourceContributor(2, archive.toFile(), overlay).contribute(assembler, null);
    assertFalse(Files.exists(tmp.resolve("war")));
  }

}
