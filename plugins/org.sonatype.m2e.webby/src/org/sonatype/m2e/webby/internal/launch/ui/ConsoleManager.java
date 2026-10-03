package org.sonatype.m2e.webby.internal.launch.ui;

import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;
import org.sonatype.m2e.webby.internal.WebbyImages;

public final class ConsoleManager {

  static final String WEBBY = "Webby";

  private ConsoleManager() {
  }

  public static synchronized MessageConsole getConsole() {
    IConsoleManager conMan = ConsolePlugin.getDefault().getConsoleManager();
    for (IConsole console : conMan.getConsoles()) {
      if (WEBBY.equals(console.getName()) && console instanceof MessageConsole messageConsole) {
        return messageConsole;
      }
    }
    MessageConsole console = new MessageConsole(WEBBY, WebbyImages.LAUNCH_CONFIG_DESC);
    conMan.addConsoles(new IConsole[] { console });
    return console;
  }

}
