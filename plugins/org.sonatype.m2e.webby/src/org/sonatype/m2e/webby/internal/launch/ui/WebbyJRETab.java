package org.sonatype.m2e.webby.internal.launch.ui;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.*;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaJRETab;
import org.eclipse.jdt.internal.debug.ui.launcher.VMArgumentsBlock;
import org.eclipse.jdt.launching.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;

@SuppressWarnings("restriction")
public class WebbyJRETab extends JavaJRETab {

  private VMArgumentsBlock vmArgumentsBlock = new VMArgumentsBlock();

  private SystemPropertiesFileBlock sysPropsFile = new SystemPropertiesFileBlock();

  public void createControl(Composite parent) {
    super.createControl(parent);

    Composite comp = (Composite) fJREBlock.getControl();
    ((GridData) comp.getLayoutData()).grabExcessVerticalSpace = true;
    ((GridData) comp.getLayoutData()).verticalAlignment = SWT.FILL;

    vmArgumentsBlock.createControl(comp);
    ((GridData) vmArgumentsBlock.getControl().getLayoutData()).horizontalSpan = 2;

    sysPropsFile.createControl(comp);
    ((GridData) sysPropsFile.getControl().getLayoutData()).horizontalSpan = 2;
  }

  public void performApply(ILaunchConfigurationWorkingCopy configuration) {
    super.performApply(configuration);
    vmArgumentsBlock.performApply(configuration);
    sysPropsFile.performApply(configuration);
    setLaunchConfigurationWorkingCopy(configuration);
  }

  public void initializeFrom(ILaunchConfiguration configuration) {
    super.initializeFrom(configuration);
    vmArgumentsBlock.initializeFrom(configuration);
    sysPropsFile.initializeFrom(configuration);
  }

  public void setLaunchConfigurationDialog(ILaunchConfigurationDialog dialog) {
    super.setLaunchConfigurationDialog(dialog);
    vmArgumentsBlock.setLaunchConfigurationDialog(dialog);
    sysPropsFile.setLaunchConfigurationDialog(dialog);
  }

  public void activated(ILaunchConfigurationWorkingCopy workingCopy) {
    setLaunchConfigurationWorkingCopy(workingCopy);
  }

  public void setDefaults(ILaunchConfigurationWorkingCopy config) {
    super.setDefaults(config);
    IVMInstall defaultVMInstall = getDefaultVMInstall(config);
    if (defaultVMInstall != null) {
      setDefaultVMInstallAttributes(defaultVMInstall, config);
    }
  }

  private IVMInstall getDefaultVMInstall(ILaunchConfiguration config) {
    IVMInstall defaultVMInstall;
    try {
      defaultVMInstall = JavaRuntime.computeVMInstall(config);
    } catch (CoreException e) {
      // core exception thrown for non-Java project
      defaultVMInstall = JavaRuntime.getDefaultVMInstall();
    }
    return defaultVMInstall;
  }

  @SuppressWarnings("deprecation")
  private void setDefaultVMInstallAttributes(IVMInstall defaultVMInstall, ILaunchConfigurationWorkingCopy config) {
    String vmName = defaultVMInstall.getName();
    String vmTypeID = defaultVMInstall.getVMInstallType().getId();
    config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_INSTALL_NAME, vmName);
    config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_INSTALL_TYPE, vmTypeID);
  }

}
