/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.sonatype.m2e.webby.internal.launch;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;


/**
 */
public abstract class ClasspathContributor {

  protected final int ordinal;

  protected ClasspathContributor(int ordinal) {
    this.ordinal = ordinal;
  }

  public abstract void contribute(WarClasspath classpath, IProgressMonitor monitor) throws CoreException;

}
