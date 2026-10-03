package org.sonatype.m2e.webby.internal.build;

import java.io.FilterInputStream;
import java.io.InputStream;

/**
 * Protects a stream from being closed, e.g. the stream of a ZIP file while reading its entries.
 */
public class NonClosingInputStream extends FilterInputStream {

  public NonClosingInputStream(InputStream is) {
    super(is);
  }

  @Override
  public void close() {
    // the underlying stream is closed by its owner
  }

}
