package org.sonatype.m2e.webby.internal.build;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.sonatype.m2e.webby.internal.WebbyPropertyTester;
import org.sonatype.m2e.webby.internal.config.OverlayConfiguration;
import org.sonatype.m2e.webby.internal.config.ResourceConfiguration;
import org.sonatype.m2e.webby.internal.config.WarConfiguration;
import org.sonatype.m2e.webby.internal.config.WarConfigurationExtractor;
import org.sonatype.m2e.webby.internal.launch.WarClasspath;
import org.sonatype.m2e.webby.internal.launch.WarClasspathPopulator;
import org.sonatype.m2e.webby.tests.TestProjects;

/**
 * Builds a real WAR project of the workspace and checks the WAR directory assembled by Webby.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class WebbyBuildParticipantTest {

  private static IProject project;

  private static Path war;

  @BeforeAll
  static void importProject() throws Exception {
    project = TestProjects.importProject(TestProjects.SAMPLE);
    war = project.getLocation().toFile().toPath().resolve("target/m2e-webby/war");
  }

  @Test
  @Order(1)
  void projectIsAWebApp() throws Exception {
    IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().getProject(project);
    assertNotNull(facade, "project is a Maven project");
    assertEquals("war", facade.getPackaging());

    IMarker[] errors = project.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
    for (IMarker marker : errors) {
      assertFalse(marker.getAttribute(IMarker.SEVERITY, 0) == IMarker.SEVERITY_ERROR,
          "unexpected error: " + marker.getAttribute(IMarker.MESSAGE, ""));
    }

    WebbyPropertyTester tester = new WebbyPropertyTester();
    assertTrue(tester.test(project, "isWebApp", null, null));
    assertTrue(tester.test(project.getFile("pom.xml"), "isWebApp", null, null));
    assertFalse(tester.test(project.getFile("src/main/webapp/index.html"), "isWebApp", null, null));
    assertFalse(tester.test(project, "other", null, null));
    assertFalse(tester.test("not a resource", "isWebApp", null, null));
  }

  @Test
  @Order(2)
  void extractsWarConfiguration() throws Exception {
    IMavenProjectFacade facade = MavenPlugin.getMavenProjectRegistry().getProject(project);
    MavenProject mvnProject = facade.getMavenProject(new NullProgressMonitor());
    WarConfiguration config = new WarConfigurationExtractor().getConfiguration(facade, mvnProject,
        new NullProgressMonitor());

    File basedir = project.getLocation().toFile();
    assertEquals(new File(basedir, "target/classes").getAbsolutePath(), config.getClassesDirectory());
    assertEquals(new File(basedir, "target/m2e-webby").getAbsolutePath(), config.getWorkDirectory());
    assertEquals(1, config.getOverlays().size());
    OverlayConfiguration main = config.getOverlays().get(0);
    assertTrue(main.isMain());
    assertEquals("UTF-8", main.getEncoding());

    List<ResourceConfiguration> resources = config.getResources();
    assertEquals(2, resources.size());
    assertEquals(new File(basedir, "src/main/webapp").getAbsolutePath(), resources.get(0).getDirectory());
    assertEquals(List.of("excluded.txt"), resources.get(0).getExcludes());
    assertEquals(new File(basedir, "src/main/filtered").getAbsolutePath(), resources.get(1).getDirectory());
    assertTrue(resources.get(1).isFiltering());
    assertEquals("WEB-INF/info", resources.get(1).getTargetPath());
    assertFalse(config.isWebXmlFiltered());

    WarClasspath classpath = new WarClasspath();
    new WarClasspathPopulator().populate(classpath, mvnProject, config, true, new NullProgressMonitor());
    assertEquals(List.of(new File(config.getClassesDirectory())), List.copyOf(classpath.getRuntimeClasspath()));
    assertEquals(1, classpath.getProvidedClasspath().size());
    assertTrue(classpath.getProvidedClasspath().iterator().next().getName().startsWith("jakarta.servlet-api"));
  }

  @Test
  @Order(3)
  void assemblesWarDirectory() throws IOException {
    assertEquals("<!DOCTYPE html><html><body><h1>Webby sample</h1></body></html>",
        Files.readString(war.resolve("index.html")).trim());
    assertTrue(Files.isRegularFile(war.resolve("WEB-INF/web.xml")));
    assertEquals("Hello from Webby", Files.readString(war.resolve("WEB-INF/info/greeting.txt")).trim(),
        "web resource is filtered");
    assertFalse(Files.exists(war.resolve("excluded.txt")), "warSourceExcludes is honored");
    assertTrue(Files.isRegularFile(war.resolve("../config.ser")));
    assertTrue(Files.isRegularFile(war.resolve("../resources.ser")));
    assertTrue(Files.isRegularFile(project.getLocation().toFile().toPath()
        .resolve("target/classes/org/example/HelloServlet.class")), "servlet is compiled by JDT");
  }

  @Test
  @Order(4)
  void updatesWarDirectoryIncrementally() throws Exception {
    IFile page = project.getFile("src/main/webapp/new.html");
    page.create(new ByteArrayInputStream("new page".getBytes(StandardCharsets.UTF_8)), true, null);
    TestProjects.waitForJobs();
    project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, new NullProgressMonitor());
    assertEquals("new page", Files.readString(war.resolve("new.html")));

    page.setContents(new ByteArrayInputStream("changed".getBytes(StandardCharsets.UTF_8)), true, false, null);
    project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, new NullProgressMonitor());
    assertEquals("changed", Files.readString(war.resolve("new.html")));

    page.delete(true, null);
    project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, new NullProgressMonitor());
    assertFalse(Files.exists(war.resolve("new.html")));
  }

  @Test
  @Order(5)
  void rebuildsWhenCleaned() throws Exception {
    project.build(IncrementalProjectBuilder.CLEAN_BUILD, new NullProgressMonitor());
    assertFalse(Files.exists(war), "clean removes the WAR directory");

    project.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
    TestProjects.waitForJobs();
    assertTrue(Files.isRegularFile(war.resolve("index.html")));
    assertTrue(Files.isRegularFile(war.resolve("WEB-INF/info/greeting.txt")));
  }

}
