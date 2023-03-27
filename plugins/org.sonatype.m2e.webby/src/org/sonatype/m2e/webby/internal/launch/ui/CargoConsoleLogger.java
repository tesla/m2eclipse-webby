package org.sonatype.m2e.webby.internal.launch.ui;

import java.io.IOException;

import org.codehaus.cargo.util.internal.log.AbstractLogger;
import org.codehaus.cargo.util.log.LogLevel;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.ui.console.*;
import org.sonatype.m2e.webby.internal.WebbyPlugin;

public class CargoConsoleLogger extends AbstractLogger {

  private final MessageConsole console;

  public CargoConsoleLogger(MessageConsole console) {
    this.console = console;
  }

  @Override
  protected void doLog(LogLevel level, String message, String category) {
    MessageConsoleStream out = console.newMessageStream();
    try {
      out.println(message);
    } finally {
      try {
        out.close();
      } catch (IOException e) {
        WebbyPlugin.log(e, IStatus.WARNING);
      }
    }
  }

}
