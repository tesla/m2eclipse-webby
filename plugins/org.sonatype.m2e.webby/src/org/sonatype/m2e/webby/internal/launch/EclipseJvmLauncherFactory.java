/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.sonatype.m2e.webby.internal.launch;

import java.io.File;

import org.codehaus.cargo.container.spi.jvm.DefaultJvmLauncherFactory;
import org.codehaus.cargo.container.spi.jvm.JvmLauncher;
import org.codehaus.cargo.container.spi.jvm.JvmLauncherFactory;
import org.codehaus.cargo.container.spi.jvm.JvmLauncherRequest;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.jdt.launching.IVMRunner;


/**
 */
public class EclipseJvmLauncherFactory implements JvmLauncherFactory {

  private final JvmLauncherFactory defaultJvmLauncherFactory = new DefaultJvmLauncherFactory();

  private final IVMRunner runner;

  private final ILaunch launch;

  private final File workingDirectory;

  private final String[] envVariables;

  private final IProgressMonitor monitor;

  public EclipseJvmLauncherFactory(IVMRunner runner, ILaunch launch, File workingDirectory, String[] envVariables,
      IProgressMonitor monitor) {
    this.runner = runner;
    this.launch = launch;
    this.workingDirectory = workingDirectory;
    this.envVariables = envVariables;
    this.monitor = monitor;
  }

  public JvmLauncher createJvmLauncher(JvmLauncherRequest request) {
    if(!request.isServer()) {
      return defaultJvmLauncherFactory.createJvmLauncher(request);
    }

    return new EclipseJvmLauncher(runner, launch, workingDirectory, envVariables, monitor);
  }

}
