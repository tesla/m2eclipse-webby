package org.sonatype.m2e.webby.internal.launch;

import java.io.File;
import java.util.Map;

import org.codehaus.cargo.container.spi.jvm.DefaultJvmLauncherFactory;
import org.codehaus.cargo.container.spi.jvm.JvmLauncher;
import org.codehaus.cargo.container.spi.jvm.JvmLauncherFactory;
import org.codehaus.cargo.container.spi.jvm.JvmLauncherRequest;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.jdt.launching.IVMRunner;

public class EclipseJvmLauncherFactory implements JvmLauncherFactory {

  private final JvmLauncherFactory defaultJvmLauncherFactory = new DefaultJvmLauncherFactory();

  private final IVMRunner runner;

  private final ILaunch launch;

  private final File workingDirectory;

  private final String[] envVariables;

  private final IProgressMonitor monitor;

  private Map<String, Object> vmSpecificAttributesMap;

  public EclipseJvmLauncherFactory(IVMRunner runner, ILaunch launch, File workingDirectory, String[] envVariables, Map<String, Object> vmSpecificAttributesMap,
      IProgressMonitor monitor) {
    this.runner = runner;
    this.launch = launch;
    this.workingDirectory = workingDirectory;
    this.envVariables = envVariables;
    this.vmSpecificAttributesMap = vmSpecificAttributesMap;
    this.monitor = monitor;
  }

  public JvmLauncher createJvmLauncher(JvmLauncherRequest request) {
    if (!request.isServer()) {
      return defaultJvmLauncherFactory.createJvmLauncher(request);
    }

    return new EclipseJvmLauncher(runner, launch, workingDirectory, envVariables, vmSpecificAttributesMap, monitor);
  }

}
