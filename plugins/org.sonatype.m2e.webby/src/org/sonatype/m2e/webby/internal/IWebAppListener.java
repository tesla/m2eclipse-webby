package org.sonatype.m2e.webby.internal;

public interface IWebAppListener {

  void webAppStarted(IWebApp webApp);

  void webAppStopped(IWebApp webApp);

}
