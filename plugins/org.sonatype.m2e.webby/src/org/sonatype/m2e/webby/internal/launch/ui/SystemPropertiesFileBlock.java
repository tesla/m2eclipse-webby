package org.sonatype.m2e.webby.internal.launch.ui;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.StringVariableSelectionDialog;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaLaunchTab;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.sonatype.m2e.webby.internal.launch.WebbyLaunchConstants;

/**
 * Lists the properties files, one per line, whose content is passed as system properties to the container JVM.
 */
public class SystemPropertiesFileBlock extends JavaLaunchTab {

  private Text sysPropsFiles;

  @Override
  public void createControl(Composite parent) {
    Font font = parent.getFont();

    Group group = new Group(parent, SWT.NONE);
    setControl(group);

    GridLayout topLayout = new GridLayout();
    group.setLayout(topLayout);
    GridData gd = new GridData(GridData.FILL_BOTH);
    group.setLayoutData(gd);
    group.setFont(font);
    group.setText("System Properties Files:");

    sysPropsFiles = new Text(group, SWT.MULTI | SWT.WRAP | SWT.BORDER | SWT.V_SCROLL);
    sysPropsFiles.addTraverseListener(e -> {
      switch (e.detail) {
        case SWT.TRAVERSE_ESCAPE, SWT.TRAVERSE_PAGE_NEXT, SWT.TRAVERSE_PAGE_PREVIOUS -> e.doit = true;
        case SWT.TRAVERSE_RETURN, SWT.TRAVERSE_TAB_NEXT, SWT.TRAVERSE_TAB_PREVIOUS -> {
          if (!sysPropsFiles.isEnabled() || (e.stateMask & SWT.MODIFIER_MASK) != 0) {
            e.doit = true;
          }
        }
        default -> {
          // keep the default behavior
        }
      }
    });
    sysPropsFiles.addModifyListener(e -> updateLaunchConfigurationDialog());
    sysPropsFiles.setData(WebbyTab.WIDGET_ID_KEY, "webby.sysPropsFiles");
    gd = new GridData(GridData.FILL_BOTH);
    gd.heightHint = 40;
    gd.widthHint = 100;
    sysPropsFiles.setLayoutData(gd);
    sysPropsFiles.setFont(font);

    Button variableButton = createPushButton(group, "Variables...", null);
    variableButton.setFont(font);
    variableButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
    variableButton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
      StringVariableSelectionDialog dialog = new StringVariableSelectionDialog(getShell());
      dialog.open();
      String variable = dialog.getVariableExpression();
      if (variable != null) {
        sysPropsFiles.insert(variable);
      }
    }));
  }

  @Override
  public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
    configuration.setAttribute(WebbyLaunchConstants.ATTR_SYS_PROP_FILES, (String) null);
  }

  @Override
  public void initializeFrom(ILaunchConfiguration config) {
    String sysPropFiles = "";
    try {
      sysPropFiles = config.getAttribute(WebbyLaunchConstants.ATTR_SYS_PROP_FILES, "");
    } catch (CoreException ce) {
      setErrorMessage(ce.getStatus().getMessage());
    }
    this.sysPropsFiles.setText(sysPropFiles);
  }

  @Override
  public void performApply(ILaunchConfigurationWorkingCopy config) {
    String content = sysPropsFiles.getText().trim();
    if (content.isEmpty()) {
      content = null;
    }
    config.setAttribute(WebbyLaunchConstants.ATTR_SYS_PROP_FILES, content);
  }

  @Override
  public String getName() {
    return "System Properties Files";
  }

}
