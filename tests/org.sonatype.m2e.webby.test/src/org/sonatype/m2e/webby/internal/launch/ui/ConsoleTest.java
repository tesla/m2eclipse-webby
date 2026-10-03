package org.sonatype.m2e.webby.internal.launch.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.codehaus.cargo.util.log.LogLevel;
import org.eclipse.ui.console.MessageConsole;
import org.junit.jupiter.api.Test;

class ConsoleTest {

  @Test
  void reusesWebbyConsole() {
    MessageConsole console = ConsoleManager.getConsole();
    assertEquals(ConsoleManager.WEBBY, console.getName());
    assertSame(console, ConsoleManager.getConsole());
  }

  @Test
  void logsCargoMessagesToConsole() throws InterruptedException {
    MessageConsole console = ConsoleManager.getConsole();
    console.clearConsole();
    CargoConsoleLogger logger = new CargoConsoleLogger(console);
    logger.setLevel(LogLevel.DEBUG);
    logger.info("Tomcat 11.x started on port [8080]", "test");

    long deadline = System.currentTimeMillis() + Duration.ofSeconds(10).toMillis();
    while (!console.getDocument().get().contains("started on port") && System.currentTimeMillis() < deadline) {
      Thread.sleep(50);
    }
    assertTrue(console.getDocument().get().contains("Tomcat 11.x started on port [8080]"));
  }

}
