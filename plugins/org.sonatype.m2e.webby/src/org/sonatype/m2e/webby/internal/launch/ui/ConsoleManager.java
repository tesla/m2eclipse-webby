package org.sonatype.m2e.webby.internal.launch.ui;

import org.eclipse.ui.console.*;
import org.sonatype.m2e.webby.internal.WebbyImages;

public class ConsoleManager {

  private static final String WEBBY = "Webby";

  public static MessageConsole getConsole() {
    return findConsole(WEBBY);
  }

  private static MessageConsole findConsole(String name) {
    ConsolePlugin plugin = ConsolePlugin.getDefault();
    IConsoleManager conMan = plugin.getConsoleManager();
    IConsole[] existing = conMan.getConsoles();
    for (int i = 0; i < existing.length; i++)
      if (name.equals(existing[i].getName()))
        return (MessageConsole) existing[i];
    // no console found, so create a new one
    MessageConsole myConsole = new MessageConsole(name, WebbyImages.LAUNCH_CONFIG_DESC);
    conMan.addConsoles(new IConsole[] { myConsole });
    return myConsole;
  }

}
