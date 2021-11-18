/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.sonatype.m2e.webby.internal.launch.ui;

import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;
import org.sonatype.m2e.webby.internal.WebbyImages;



/**
 */
public class ConsoleManager {

  private static final String WEBBY = "Webby";

  public static MessageConsole getConsole() {
    return findConsole(WEBBY);
  }

  private static MessageConsole findConsole(String name) {
    ConsolePlugin plugin = ConsolePlugin.getDefault();
    IConsoleManager conMan = plugin.getConsoleManager();
    IConsole[] existing = conMan.getConsoles();
    for(int i = 0; i < existing.length; i++ )
      if(name.equals(existing[i].getName()))
        return (MessageConsole) existing[i];
    //no console found, so create a new one
    MessageConsole myConsole = new MessageConsole(name, WebbyImages.LAUNCH_CONFIG_DESC);
    conMan.addConsoles(new IConsole[] {myConsole});
    return myConsole;
  }

}
