package org.sonatype.m2e.webby.internal.launch.ui;

import java.io.IOException;

import org.codehaus.cargo.util.internal.log.AbstractLogger;
import org.codehaus.cargo.util.log.LogLevel;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.sonatype.m2e.webby.internal.WebbyPlugin;

/**
 * Forwards the Cargo log to the Webby console.
 */
public class CargoConsoleLogger extends AbstractLogger {

  private final MessageConsole console;

  public CargoConsoleLogger(MessageConsole console) {
    this.console = console;
  }

  @Override
  protected void doLog(LogLevel level, String message, String category) {
    try (MessageConsoleStream out = console.newMessageStream()) {
      out.println(message);
    } catch (IOException e) {
      WebbyPlugin.log(e, IStatus.WARNING);
    }
  }

}
