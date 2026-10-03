package org.sonatype.m2e.webby.internal.build;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.sonatype.m2e.webby.internal.config.OverlayConfiguration;
import org.sonatype.m2e.webby.internal.config.WarConfiguration;
import org.sonatype.m2e.webby.internal.config.WarConfigurationExtractor;
import org.sonatype.m2e.webby.internal.launch.WarClasspath;
import org.sonatype.m2e.webby.internal.launch.WarClasspathPopulator;
import org.sonatype.m2e.webby.tests.TestProjects;

/**
 * Builds a WAR project overlaid with another WAR project of the workspace.
 */
class WebbyOverlayTest {

  private static IProject overlay;

  private static IProject overlaid;

  private static Path war;

  @BeforeAll
  static void importProjects() throws Exception {
    List<IProject> projects = TestProjects.importProjects(TestProjects.OVERLAY, TestProjects.OVERLAID);
    overlay = projects.get(0);
    overlaid = projects.get(1);
    war = overlaid.getLocation().toFile().toPath().resolve("target/m2e-webby/war");
  }

  @Test
  void readsOverlayConfiguration() throws Exception {
    IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().getProject(overlaid);
    MavenProject mvnProject = facade.getMavenProject(new NullProgressMonitor());
    WarConfiguration config = new WarConfigurationExtractor().getConfiguration(facade, mvnProject,
        new NullProgressMonitor());

    List<OverlayConfiguration> overlays = config.getOverlays();
    assertEquals(2, overlays.size());
    assertTrue(overlays.get(0).isMain(), "the project itself comes first");
    assertEquals("org.example:webby-overlay:war", overlays.get(1).getArtifactKey());
    assertEquals(List.of("excluded.txt"), overlays.get(1).getExcludes());

    WarClasspath classpath = new WarClasspath();
    new WarClasspathPopulator().populate(classpath, mvnProject, config, true, new NullProgressMonitor());
    File overlayClasses = new File(overlay.getLocation().toFile(), "target/classes");
    assertTrue(classpath.getRuntimeClasspath().contains(overlayClasses),
        "classes of the overlay project are on the classpath: " + classpath.getRuntimeClasspath());
  }

  @Test
  void mergesOverlayIntoWarDirectory() throws Exception {
    assertEquals("<html><body>index of the overlaid project</body></html>",
        Files.readString(war.resolve("index.html")).trim(), "the project wins over its overlays");
    assertEquals("<html><body>page of the overlay</body></html>",
        Files.readString(war.resolve("overlay.html")).trim());
    assertFalse(Files.exists(war.resolve("excluded.txt")), "excludes of the overlay are honored");

    // the overlay resource shows up again once the project does not shadow it anymore
    IFile index = overlaid.getFile("src/main/webapp/index.html");
    index.delete(true, null);
    overlaid.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, new NullProgressMonitor());
    assertEquals("<html><body>index of the overlay</body></html>", Files.readString(war.resolve("index.html")).trim());

    index.create(new ByteArrayInputStream("restored".getBytes(StandardCharsets.UTF_8)), true, null);
    overlaid.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, new NullProgressMonitor());
    assertEquals("restored", Files.readString(war.resolve("index.html")));
  }

  @Test
  void findsProjectsUsingOverlay() {
    assertEquals(List.of(overlaid),
        OverlayDependents.findDependents(MavenPlugin.getMavenProjectRegistry().getProject(overlay)));
    assertEquals(List.of(), OverlayDependents.findDependents(MavenPlugin.getMavenProjectRegistry().getProject(overlaid)));
  }

  @Test
  void propagatesChangesOfOverlayProject() throws Exception {
    IFile page = overlay.getFile("src/main/webapp/overlay.html");
    page.setContents(new ByteArrayInputStream("updated".getBytes(StandardCharsets.UTF_8)), true, false, null);
    overlay.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, new NullProgressMonitor());
    TestProjects.waitForJobs();
    assertEquals("updated", Files.readString(overlay.getLocation().toFile().toPath()
        .resolve("target/m2e-webby/war/overlay.html")));
    assertEquals("updated", Files.readString(war.resolve("overlay.html")),
        "the project using the overlay is updated without changes of its own");
  }

}
