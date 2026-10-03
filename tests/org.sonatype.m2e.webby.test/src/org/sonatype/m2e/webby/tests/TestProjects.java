package org.sonatype.m2e.webby.tests;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.apache.maven.model.Model;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.IMavenProjectImportResult;
import org.eclipse.m2e.core.project.MavenProjectInfo;
import org.eclipse.m2e.core.project.ProjectImportConfiguration;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 * Imports the Maven projects of the resources/projects folder of the test bundle into the workspace.
 */
public final class TestProjects {

  public static final String SAMPLE = "webby-sample";

  public static final String OVERLAY = "webby-overlay";

  public static final String OVERLAID = "webby-overlaid";

  private static final Duration JOBS_TIMEOUT = Duration.ofMinutes(5);

  private TestProjects() {
  }

  /**
   * @return the project, imported and built, reusing it when already in the workspace
   */
  public static IProject importProject(String name) throws Exception {
    return importProjects(name).get(0);
  }

  /**
   * @return the projects, imported together and built, reusing them when already in the workspace
   */
  public static List<IProject> importProjects(String... names) throws Exception {
    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    List<IProject> projects = new ArrayList<>();
    List<MavenProjectInfo> infos = new ArrayList<>();
    for (String name : names) {
      IProject project = workspace.getRoot().getProject(name);
      projects.add(project);
      if (project.exists() && MavenPlugin.getMavenProjectRegistry().getProject(project) != null) {
        continue;
      }
      Path target = workspace.getRoot().getLocation().toFile().toPath().resolve(name);
      copyDirectory(getProjectSource(name), target);
      File pom = target.resolve("pom.xml").toFile();
      Model model = MavenPlugin.getMavenModelManager().readMavenModel(pom);
      infos.add(new MavenProjectInfo(name, pom, model, null));
    }
    if (!infos.isEmpty()) {
      List<IMavenProjectImportResult> results = MavenPlugin.getProjectConfigurationManager().importProjects(infos,
          new ProjectImportConfiguration(), new NullProgressMonitor());
      for (IMavenProjectImportResult result : results) {
        if (result.getProject() == null) {
          throw new IllegalStateException("Project " + result.getMavenProjectInfo().getLabel()
              + " could not be imported");
        }
      }
      build();
    }
    return projects;
  }

  public static void build() throws CoreException, InterruptedException {
    waitForJobs();
    ResourcesPlugin.getWorkspace().build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
    waitForJobs();
  }

  public static void waitForJobs() throws InterruptedException {
    long deadline = System.currentTimeMillis() + JOBS_TIMEOUT.toMillis();
    while (System.currentTimeMillis() < deadline) {
      Job.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_BUILD, null);
      Job.getJobManager().join(ResourcesPlugin.FAMILY_MANUAL_BUILD, null);
      if (isIdle()) {
        // jobs may schedule follow-up jobs, make sure things stay quiet
        Thread.sleep(500);
        if (isIdle()) {
          return;
        }
      }
      Thread.sleep(200);
    }
  }

  private static boolean isIdle() {
    for (Job job : Job.getJobManager().find(null)) {
      if (job.getState() == Job.RUNNING && !job.isSystem()) {
        return false;
      }
    }
    return Job.getJobManager().find(ResourcesPlugin.FAMILY_AUTO_BUILD).length == 0;
  }

  public static Path getProjectSource(String name) throws IOException {
    return getResource("projects/" + name);
  }

  /**
   * @return a file of the resources folder, located through the system property webby.test.resources when running
   *         from the build, or else in the test bundle
   */
  public static Path getResource(String path) throws IOException {
    String resources = System.getProperty("webby.test.resources");
    if (resources != null) {
      return Path.of(resources, path);
    }
    Bundle bundle = FrameworkUtil.getBundle(TestProjects.class);
    URL entry = bundle.getEntry("resources/" + path);
    if (entry == null) {
      throw new IOException("Test resource " + path + " not found in " + bundle.getSymbolicName());
    }
    return new File(FileLocator.toFileURL(entry).getPath()).toPath();
  }

  private static void copyDirectory(Path source, Path target) throws IOException {
    try (Stream<Path> files = Files.walk(source)) {
      for (Path file : (Iterable<Path>) files::iterator) {
        Path dest = target.resolve(source.relativize(file).toString());
        if (Files.isDirectory(file)) {
          Files.createDirectories(dest);
        } else {
          Files.copy(file, dest, StandardCopyOption.REPLACE_EXISTING);
        }
      }
    }
  }

}
