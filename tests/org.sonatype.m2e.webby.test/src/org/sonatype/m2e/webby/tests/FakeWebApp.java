package org.sonatype.m2e.webby.tests;

import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.debug.core.ILaunch;
import org.sonatype.m2e.webby.internal.IWebApp;

public class FakeWebApp implements IWebApp {

  private final ILaunch launch;

  private final String context;

  private final String port;

  private final String containerId;

  public final AtomicInteger stopCount = new AtomicInteger();

  public FakeWebApp(ILaunch launch, String context, String port, String containerId) {
    this.launch = launch;
    this.context = context;
    this.port = port;
    this.containerId = containerId;
  }

  @Override
  public ILaunch getLaunch() {
    return launch;
  }

  @Override
  public String getContext() {
    return context;
  }

  @Override
  public String getPort() {
    return port;
  }

  @Override
  public String getContainerId() {
    return containerId;
  }

  @Override
  public void stop() {
    stopCount.incrementAndGet();
  }

}
