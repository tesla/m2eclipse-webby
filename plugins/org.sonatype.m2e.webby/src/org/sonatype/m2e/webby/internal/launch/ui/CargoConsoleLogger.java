/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.sonatype.m2e.webby.internal.launch.ui;

import java.io.IOException;

import org.codehaus.cargo.util.internal.log.AbstractLogger;
import org.codehaus.cargo.util.log.LogLevel;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.sonatype.m2e.webby.internal.WebbyPlugin;



/**
 */
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
      } catch(IOException e) {
        WebbyPlugin.log(e, IStatus.WARNING);
      }
    }
  }

}
