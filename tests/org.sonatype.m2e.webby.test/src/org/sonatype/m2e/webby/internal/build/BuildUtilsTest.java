package org.sonatype.m2e.webby.internal.build;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

class BuildUtilsTest {

  @Test
  void extractsLowerCaseExtension() {
    assertEquals("xml", FilteringHandler.getExtension("WEB-INF/web.XML"));
    assertEquals("properties", FilteringHandler.getExtension("a.b/c.properties"));
    assertEquals("", FilteringHandler.getExtension("WEB-INF/README"));
    assertEquals("gz", FilteringHandler.getExtension("a.tar.gz"));
  }

  @Test
  void doesNotCloseUnderlyingStream() throws IOException {
    AtomicBoolean closed = new AtomicBoolean();
    ByteArrayInputStream in = new ByteArrayInputStream(new byte[] { 1, 2 }) {
      @Override
      public void close() {
        closed.set(true);
      }
    };
    try (NonClosingInputStream ncis = new NonClosingInputStream(in)) {
      assertEquals(1, ncis.read());
    }
    assertEquals(false, closed.get());
    assertEquals(2, in.read());
  }

}
