package org.sonatype.m2e.webby.internal.build;

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Contributes the resources of an overlay to the WAR directory.
 */
public abstract class ResourceContributor {

  protected final int ordinal;

  protected ResourceContributor(int ordinal) {
    this.ordinal = ordinal;
  }

  public abstract void contribute(WarAssembler assembler, IProgressMonitor monitor);

}
