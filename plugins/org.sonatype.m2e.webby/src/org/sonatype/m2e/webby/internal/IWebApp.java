package org.sonatype.m2e.webby.internal;

import org.eclipse.debug.core.ILaunch;

public interface IWebApp {

  ILaunch getLaunch();

  /**
   * @return the context path, without leading slash
   */
  String getContext();

  String getPort();

  String getContainerId();

  void stop() throws Exception;

  default String getUrl() {
    String context = getContext();
    if (context == null) {
      context = "";
    } else if (context.startsWith("/")) {
      context = context.substring(1);
    }
    return "http://localhost:" + getPort() + "/" + context;
  }

}
