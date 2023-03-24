/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.sonatype.m2e.webby.internal.build;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;


/**
 */
public class NonClosingInputStream extends FilterInputStream {

  public NonClosingInputStream(InputStream is) {
    super(is);
  }

  @Override
  public void close() throws IOException {
    // keep original stream open
  }

}
