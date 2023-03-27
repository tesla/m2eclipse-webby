package org.sonatype.m2e.webby.internal.launch;

import org.eclipse.core.runtime.*;

public abstract class ClasspathContributor {

  protected final int ordinal;

  protected ClasspathContributor(int ordinal) {
    this.ordinal = ordinal;
  }

  public abstract void contribute(WarClasspath classpath, IProgressMonitor monitor) throws CoreException;

}
