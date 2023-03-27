package org.sonatype.m2e.webby.internal.build;

import org.eclipse.core.runtime.IProgressMonitor;

public abstract class ResourceContributor {

  protected final int ordinal;

  protected ResourceContributor(int ordinal) {
    this.ordinal = ordinal;
  }

  public abstract void contribute(WarAssembler assembler, IProgressMonitor monitor);

}
