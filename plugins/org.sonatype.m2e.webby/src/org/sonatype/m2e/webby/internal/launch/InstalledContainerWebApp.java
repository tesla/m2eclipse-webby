package org.sonatype.m2e.webby.internal.launch;

import org.codehaus.cargo.container.LocalContainer;
import org.eclipse.debug.core.ILaunch;
import org.sonatype.m2e.webby.internal.IWebApp;

public class InstalledContainerWebApp implements IWebApp {

  private final ILaunch launch;

  private final CargoConfiguration cargoConfig;

  private final LocalContainer container;

  public InstalledContainerWebApp(ILaunch launch, CargoConfiguration cargoConfig, LocalContainer container) {
    this.launch = launch;
    this.cargoConfig = cargoConfig;
    this.container = container;
  }

  public ILaunch getLaunch() {
    return launch;
  }

  public String getContext() {
    return cargoConfig.getContextName();
  }

  public String getPort() {
    return cargoConfig.getPort();
  }

  public String getContainerId() {
    return cargoConfig.getContainerId();
  }

  public void stop() {
    container.stop();
  }

}
