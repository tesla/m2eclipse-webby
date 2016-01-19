/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.sonatype.m2e.webby.internal.launch.ui;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.codehaus.cargo.container.ContainerType;
import org.codehaus.cargo.generic.DefaultContainerFactory;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.internal.ui.SWTFactory;
import org.eclipse.debug.ui.StringVariableSelectionDialog;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaLaunchTab;
import org.eclipse.jdt.internal.debug.ui.launcher.LauncherMessages;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.sonatype.m2e.webby.internal.WebbyImages;
import org.sonatype.m2e.webby.internal.WebbyPlugin;
import org.sonatype.m2e.webby.internal.launch.WebbyLaunchConstants;



/**
 */
@SuppressWarnings("restriction")
public class WebbyTab extends JavaLaunchTab {

  private Text projectName;

  private Button projectNameBrowse;

  private Text contextName;

  private Combo containerId;

  private Combo containerType;

  private Combo containerLogging;

  private Text containerHome;

  private Button containerHomeBrowse;

  private Button containerHomeVariables;

  private Spinner containerPort;

  private Spinner containerTimeout;

  private SortedMap<String, SortedSet<String>> containers;

  @Override
  public String getId() {
    return "org.sonatype.m2e.webby.ui.mainTab"; //$NON-NLS-1$
  }

  public void createControl(Composite parent) {
    Composite comp = SWTFactory.createComposite(parent, parent.getFont(), 1, 1, GridData.FILL_BOTH);
    createApplicationEditor(comp);
    createVerticalSpacer(comp, 1);
    createContainerEditor(comp);
    setControl(comp);
  }

  private void createApplicationEditor(Composite parent) {
    Font font = parent.getFont();

    Group group = new Group(parent, SWT.NONE);
    group.setText("Web Application");
    group.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    group.setLayout(new GridLayout(3, false));
    group.setFont(font);

    new Label(group, SWT.LEFT).setText("Project:");

    projectName = new Text(group, SWT.SINGLE | SWT.BORDER);
    projectName.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        updateLaunchConfigurationDialog();
      }
    });
    projectName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    projectName.setFont(font);

    projectNameBrowse = createPushButton(group, LauncherMessages.AbstractJavaMainTab_1, null);
    projectNameBrowse.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        IJavaProject project = chooseJavaProject();
        if(project != null) {
          projectName.setText(project.getElementName());
        }
      }
    });

    new Label(group, SWT.LEFT).setText("Context:");

    contextName = new Text(group, SWT.SINGLE | SWT.BORDER);
    contextName.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        updateLaunchConfigurationDialog();
      }
    });
    contextName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    contextName.setFont(font);

    return;
  }

  private boolean isEmbeddedContainerInstalled(String containerId) {
    return WebbyPlugin.getDefault().isEmbeddedContainerInstalled(containerId);
  }

  private void createContainerEditor(Composite parent) {
    Set<String> supported = new HashSet<String>(Arrays.asList("jetty6x", "jetty7x", "jetty8x", "tomcat5x", "tomcat6x",
        "tomcat7x", "tomcat8x", "tomee1x"));
    containers = new TreeMap<String, SortedSet<String>>();
    for(Map.Entry<String, Set<ContainerType>> entry : new DefaultContainerFactory().getContainerIds().entrySet()) {
      if(!supported.contains(entry.getKey())) {
        continue;
      }
      SortedSet<String> types = new TreeSet<String>();
      for(ContainerType type : entry.getValue()) {
        if(ContainerType.REMOTE.equals(type)) {
          continue;
        } else if(ContainerType.EMBEDDED.equals(type) && !isEmbeddedContainerInstalled(entry.getKey())) {
          continue;
        }
        types.add(type.getType());
      }
      if(!types.isEmpty()) {
        containers.put(entry.getKey(), types);
      }
    }

    Font font = parent.getFont();

    Group group = new Group(parent, SWT.NONE);
    group.setFont(font);
    group.setText("Container");
    group.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    group.setLayout(new GridLayout(5, false));

    new Label(group, SWT.LEFT).setText("Provider:");

    containerId = new Combo(group, SWT.VERTICAL | SWT.DROP_DOWN | SWT.BORDER | SWT.READ_ONLY);
    containerId.setFont(font);
    containerId.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
    for(String id : containers.keySet()) {
      containerId.add(id);
    }
    containerId.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        updateContainerTypes();
        updateLaunchConfigurationDialog();
      }
    });

    new Label(group, SWT.LEFT).setText("Type:");

    containerType = new Combo(group, SWT.VERTICAL | SWT.DROP_DOWN | SWT.BORDER | SWT.READ_ONLY);
    containerType.setFont(font);
    containerType.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
    containerType.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        updateContainerHome();
        updateLaunchConfigurationDialog();
      }
    });

    SWTFactory.createHorizontalSpacer(group, 3);

    new Label(group, SWT.LEFT).setText("Home:");

    containerHome = new Text(group, SWT.SINGLE | SWT.BORDER);
    containerHome.setFont(font);
    containerHome.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
    containerHome.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        updateLaunchConfigurationDialog();
      }
    });

    containerHomeVariables = createPushButton(group, "Variables...", null);
    containerHomeVariables.addSelectionListener(new SelectionAdapter() {

      public void widgetSelected(SelectionEvent e) {
        StringVariableSelectionDialog dialog = new StringVariableSelectionDialog(getShell());
        dialog.open();
        String variable = dialog.getVariableExpression();
        if(variable != null) {
          containerHome.insert(variable);
        }
      }

    });

    containerHomeBrowse = createPushButton(group, LauncherMessages.AbstractJavaMainTab_1, null);
    containerHomeBrowse.addSelectionListener(new SelectionAdapter() {

      public void widgetSelected(SelectionEvent e) {
        String path = chooseContainerHome();
        if(path != null) {
          containerHome.setText(path);
        }
      }

    });

    new Label(group, SWT.LEFT).setText("Logging:");

    containerLogging = new Combo(group, SWT.VERTICAL | SWT.DROP_DOWN | SWT.BORDER | SWT.READ_ONLY);
    containerLogging.setFont(font);
    containerLogging.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
    containerLogging.add("low");
    containerLogging.add("medium");
    containerLogging.add("high");
    containerLogging.select(1);
    containerLogging.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        updateLaunchConfigurationDialog();
      }
    });

    SWTFactory.createHorizontalSpacer(group, 3);

    new Label(group, SWT.LEFT).setText("Port:");

    containerPort = new Spinner(group, SWT.SINGLE | SWT.BORDER);
    containerPort.setFont(font);
    containerPort.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
    containerPort.setMinimum(1);
    containerPort.setMaximum(65535);
    containerPort.setPageIncrement(10);
    containerPort.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        updateLaunchConfigurationDialog();
      }
    });
    containerPort.addMouseWheelListener(new MouseWheelListener() {
      public void mouseScrolled(MouseEvent e) {
        int delta = (e.count + (e.count < 0 ? -2 : 2)) / 3;
        int value = containerPort.getSelection() + delta;
        value = Math.min(Math.max(value, containerPort.getMinimum()), containerPort.getMaximum());
        containerPort.setSelection(value);
      }
    });

    SWTFactory.createHorizontalSpacer(group, 3);

    new Label(group, SWT.LEFT).setText("Timeout:");

    containerTimeout = new Spinner(group, SWT.SINGLE | SWT.BORDER);
    containerTimeout.setFont(font);
    containerTimeout.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
    containerTimeout.setMinimum(1);
    containerTimeout.setMaximum(Integer.MAX_VALUE);
    containerTimeout.setPageIncrement(10);
    containerTimeout.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        updateLaunchConfigurationDialog();
      }
    });
    containerTimeout.addMouseWheelListener(new MouseWheelListener() {
      public void mouseScrolled(MouseEvent e) {
        int delta = (e.count + (e.count < 0 ? -2 : 2)) / 3;
        int value = containerTimeout.getSelection() + delta;
        containerTimeout.setSelection(value);
      }
    });

    return;
  }

  private void updateContainerTypes() {
    Set<String> types = containers.get(containerId.getText());
    String sel = containerType.getText();
    containerType.removeAll();
    if(types != null) {
      for(String type : types) {
        containerType.add(type);
        if(type.equals(sel)) {
          containerType.select(containerType.getItemCount() - 1);
        }
      }
    }
    if(containerType.getSelectionIndex() < 0) {
      containerType.select(0);
    }
    updateContainerHome();
  }

  private void updateContainerHome() {
    String type = containerType.getText();
    boolean homeEnabled = ContainerType.INSTALLED.getType().equals(type);
    containerHome.setEnabled(homeEnabled);
    containerHomeBrowse.setEnabled(homeEnabled);
    containerHomeVariables.setEnabled(homeEnabled);
  }

  private IJavaProject chooseJavaProject() {
    ILabelProvider labelProvider = new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_DEFAULT);
    ElementListSelectionDialog dialog = new ElementListSelectionDialog(getShell(), labelProvider);
    dialog.setTitle(LauncherMessages.AbstractJavaMainTab_4);
    dialog.setMessage(LauncherMessages.AbstractJavaMainTab_3);
    try {
      dialog.setElements(JavaCore.create(getWorkspaceRoot()).getJavaProjects());
    } catch(JavaModelException jme) {
      WebbyPlugin.log(jme);
    }
    IJavaProject javaProject = getJavaProject();
    if(javaProject != null) {
      dialog.setInitialSelections(new Object[] {javaProject});
    }
    if(dialog.open() == Window.OK) {
      return (IJavaProject) dialog.getFirstResult();
    }
    return null;
  }

  private IJavaProject getJavaProject() {
    String projectName = this.projectName.getText().trim();
    if(projectName.length() <= 0) {
      return null;
    }
    return getJavaModel().getJavaProject(projectName);
  }

  private IJavaModel getJavaModel() {
    return JavaCore.create(getWorkspaceRoot());
  }

  private IWorkspaceRoot getWorkspaceRoot() {
    return ResourcesPlugin.getWorkspace().getRoot();
  }

  private String chooseContainerHome() {
    DirectoryDialog dialog = new DirectoryDialog(getShell());
    dialog.setText("Container Home Directory");
    dialog.setMessage("Choose the home directory where the container is installed:");
    dialog.setFilterPath(containerHome.getText());
    return dialog.open();
  }

  public void setDefaults(ILaunchConfigurationWorkingCopy config) {
    IJavaElement javaElement = getContext();
    if(javaElement != null) {
      initializeJavaProject(javaElement, config);
    } else {
      config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, "");
    }
    config.setAttribute(WebbyLaunchConstants.ATTR_CONTEXT_NAME, "");
    config.setAttribute(WebbyLaunchConstants.ATTR_CONTAINER_ID, "jetty7x");
    config.setAttribute(WebbyLaunchConstants.ATTR_CONTAINER_TYPE, ContainerType.EMBEDDED.getType());
    config.setAttribute(WebbyLaunchConstants.ATTR_CONTAINER_HOME, "");
    config.setAttribute(WebbyLaunchConstants.ATTR_LOG_LEVEL, "medium");
  }

  @Override
  public void initializeFrom(ILaunchConfiguration config) {
    String projectName = "";
    try {
      projectName = config.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, "");
    } catch(CoreException ce) {
      setErrorMessage(ce.getStatus().getMessage());
    }
    this.projectName.setText(projectName);

    String contextName = "";
    try {
      contextName = config.getAttribute(WebbyLaunchConstants.ATTR_CONTEXT_NAME, "");
    } catch(CoreException ce) {
      setErrorMessage(ce.getStatus().getMessage());
    }
    this.contextName.setText(contextName);

    String containerId = "";
    try {
      containerId = config.getAttribute(WebbyLaunchConstants.ATTR_CONTAINER_ID, "");
    } catch(CoreException ce) {
      setErrorMessage(ce.getStatus().getMessage());
    }
    select(this.containerId, containerId, "jetty7x");
    updateContainerTypes();

    String containerType = "";
    try {
      containerType = config.getAttribute(WebbyLaunchConstants.ATTR_CONTAINER_TYPE, "");
    } catch(CoreException ce) {
      setErrorMessage(ce.getStatus().getMessage());
    }
    select(this.containerType, containerType, "installed");
    updateContainerHome();

    String containerHome = "";
    try {
      containerHome = config.getAttribute(WebbyLaunchConstants.ATTR_CONTAINER_HOME, "");
    } catch(CoreException ce) {
      setErrorMessage(ce.getStatus().getMessage());
    }
    this.containerHome.setText(containerHome);

    String logLevel = "";
    try {
      logLevel = config.getAttribute(WebbyLaunchConstants.ATTR_LOG_LEVEL, "");
    } catch(CoreException ce) {
      setErrorMessage(ce.getStatus().getMessage());
    }
    select(this.containerLogging, logLevel, "medium");

    int containerPort = 8080;
    try {
      containerPort = config.getAttribute(WebbyLaunchConstants.ATTR_CONTAINER_PORT, 8080);
    } catch(CoreException ce) {
      try {
        containerPort = Integer.parseInt(config.getAttribute(WebbyLaunchConstants.ATTR_CONTAINER_PORT, "8080"));
      } catch(CoreException nce) {
        setErrorMessage(ce.getStatus().getMessage());
      } catch(NumberFormatException e) {
        // just stick to the default value
      }
    }
    this.containerPort.setSelection(containerPort);

    int containerTimeout = 60;
    try {
      containerTimeout = config.getAttribute(WebbyLaunchConstants.ATTR_CONTAINER_TIMEOUT, 60);
    } catch(CoreException ce) {
      try {
      containerTimeout = Integer.parseInt(config.getAttribute(WebbyLaunchConstants.ATTR_CONTAINER_TIMEOUT, "60"));
      } catch(CoreException nce) {
        setErrorMessage(ce.getStatus().getMessage());
      } catch(NumberFormatException e) {
        // just stick to the default value
      }
    }
    this.containerTimeout.setSelection(containerTimeout);
    
    super.initializeFrom(config);
  }

  private void select(Combo combo, String value, String fallback) {
    int index = combo.indexOf(value);
    if(index < 0 && fallback != null) {
      index = combo.indexOf(fallback);
    }
    if(index >= 0) {
      combo.select(index);
    }
  }

  public void performApply(ILaunchConfigurationWorkingCopy config) {
    String projectName = this.projectName.getText().trim();
    config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, projectName);
    config.setAttribute(WebbyLaunchConstants.ATTR_CONTEXT_NAME, contextName.getText().trim());
    config.setAttribute(WebbyLaunchConstants.ATTR_CONTAINER_ID, containerId.getText().trim());
    config.setAttribute(WebbyLaunchConstants.ATTR_CONTAINER_TYPE, containerType.getText().trim());
    config.setAttribute(WebbyLaunchConstants.ATTR_CONTAINER_HOME, containerHome.getText().trim());
    config.setAttribute(WebbyLaunchConstants.ATTR_LOG_LEVEL, containerLogging.getText().trim());
    config.setAttribute(WebbyLaunchConstants.ATTR_CONTAINER_PORT, containerPort.getSelection());
    config.setAttribute(WebbyLaunchConstants.ATTR_CONTAINER_TIMEOUT, containerTimeout.getSelection());

    IProject project = null;
    if(projectName.length() > 0) {
      project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
    }
    if(project != null) {
      config.setMappedResources(new IResource[] {project});
    } else {
      config.setMappedResources(null);
    }
  }

  public String getName() {
    return LauncherMessages.JavaMainTab__Main_19;
  }

  public Image getImage() {
    return WebbyImages.LAUNCH_CONFIG;
  }

}
