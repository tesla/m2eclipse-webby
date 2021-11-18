/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.sonatype.m2e.webby.internal.launch.ui;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.StringVariableSelectionDialog;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaLaunchTab;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.sonatype.m2e.webby.internal.launch.WebbyLaunchConstants;



public class SystemPropertiesFileBlock extends JavaLaunchTab {

  private Text sysPropsFiles;

  private Button variableButton;

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
    sysPropsFiles.addTraverseListener(new TraverseListener() {
      public void keyTraversed(TraverseEvent e) {
        switch(e.detail) {
          case SWT.TRAVERSE_ESCAPE:
          case SWT.TRAVERSE_PAGE_NEXT:
          case SWT.TRAVERSE_PAGE_PREVIOUS:
            e.doit = true;
            break;
          case SWT.TRAVERSE_RETURN:
          case SWT.TRAVERSE_TAB_NEXT:
          case SWT.TRAVERSE_TAB_PREVIOUS:
            if((sysPropsFiles.getStyle() & SWT.SINGLE) != 0) {
              e.doit = true;
            } else {
              if(!sysPropsFiles.isEnabled() || (e.stateMask & SWT.MODIFIER_MASK) != 0) {
                e.doit = true;
              }
            }
            break;
        }
      }
    });
    sysPropsFiles.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        updateLaunchConfigurationDialog();
      }
    });
    gd = new GridData(GridData.FILL_BOTH);
    gd.heightHint = 40;
    gd.widthHint = 100;
    sysPropsFiles.setLayoutData(gd);
    sysPropsFiles.setFont(font);

    variableButton = createPushButton(group, "Variables...", null);
    variableButton.setFont(font);
    variableButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
    variableButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        StringVariableSelectionDialog dialog = new StringVariableSelectionDialog(getShell());
        dialog.open();
        String variable = dialog.getVariableExpression();
        if(variable != null) {
          sysPropsFiles.insert(variable);
        }
      }
    });
  }

  public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
    configuration.setAttribute(WebbyLaunchConstants.ATTR_SYS_PROP_FILES, (String) null);
  }

  @Override
  public void initializeFrom(ILaunchConfiguration config) {
    String sysPropFiles = "";
    try {
      sysPropFiles = config.getAttribute(WebbyLaunchConstants.ATTR_SYS_PROP_FILES, "");
    } catch(CoreException ce) {
      setErrorMessage(ce.getStatus().getMessage());
    }
    this.sysPropsFiles.setText(sysPropFiles);
  }

  public void performApply(ILaunchConfigurationWorkingCopy config) {
    String content = sysPropsFiles.getText().trim();
    if(content.length() <= 0) {
      content = null;
    }
    config.setAttribute(WebbyLaunchConstants.ATTR_SYS_PROP_FILES, content);
  }

  public String getName() {
    return "System Properties Files";
  }

}
