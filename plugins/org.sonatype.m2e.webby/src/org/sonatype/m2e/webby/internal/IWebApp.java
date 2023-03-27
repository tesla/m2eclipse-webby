package org.sonatype.m2e.webby.internal;

import org.eclipse.debug.core.ILaunch;

public interface IWebApp {

  ILaunch getLaunch();

  String getContext();

  String getPort();

  String getContainerId();

  void stop() throws Exception;

}
