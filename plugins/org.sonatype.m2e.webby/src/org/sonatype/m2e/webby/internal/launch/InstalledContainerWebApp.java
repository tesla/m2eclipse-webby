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

  @Override
  public ILaunch getLaunch() {
    return launch;
  }

  @Override
  public String getContext() {
    return cargoConfig.getContextName();
  }

  @Override
  public String getPort() {
    return cargoConfig.getPort();
  }

  @Override
  public String getContainerId() {
    return cargoConfig.getContainerId();
  }

  @Override
  public void stop() {
    container.stop();
  }

}
