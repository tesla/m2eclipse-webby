package org.sonatype.m2e.webby.internal.launch.ui;

import org.eclipse.debug.ui.*;
import org.eclipse.debug.ui.sourcelookup.SourceLookupTab;

public class WebbyLaunchConfigurationTabGroup extends AbstractLaunchConfigurationTabGroup {

  public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
    ILaunchConfigurationTab[] tabs = {
        //
        new WebbyTab(), //
        new WebbyJRETab(), //
        new SourceLookupTab(), //
        new EnvironmentTab(), //
        new CommonTab() };
    setTabs(tabs);
  }

}
