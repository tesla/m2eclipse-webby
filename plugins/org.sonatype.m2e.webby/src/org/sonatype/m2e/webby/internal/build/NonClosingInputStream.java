package org.sonatype.m2e.webby.internal.build;

import java.io.*;

public class NonClosingInputStream extends FilterInputStream {

  public NonClosingInputStream(InputStream is) {
    super(is);
  }

  @Override
  public void close() throws IOException {
  }

}
