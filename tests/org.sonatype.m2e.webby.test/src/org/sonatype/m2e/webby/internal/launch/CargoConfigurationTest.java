package org.sonatype.m2e.webby.internal.launch;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CargoConfigurationTest {

  @Test
  void stripsLeadingSlashesOfContext() {
    CargoConfiguration config = new CargoConfiguration();
    assertEquals("", config.getContextName());
    config.setContextName("/app");
    assertEquals("app", config.getContextName());
    config.setContextName("//nested/app");
    assertEquals("nested/app", config.getContextName());
    config.setContextName(null);
    assertEquals("", config.getContextName());
  }

}
