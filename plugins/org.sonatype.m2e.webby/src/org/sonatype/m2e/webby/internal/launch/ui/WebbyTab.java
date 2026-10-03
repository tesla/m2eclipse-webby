package org.sonatype.m2e.webby.internal.launch.ui;

import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;

import org.codehaus.cargo.container.ContainerType;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.internal.ui.SWTFactory;
import org.eclipse.debug.ui.StringVariableSelectionDialog;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaLaunchTab;
import org.eclipse.jdt.internal.debug.ui.launcher.LauncherMessages;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
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
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.sonatype.m2e.webby.internal.WebbyImages;
import org.sonatype.m2e.webby.internal.WebbyPlugin;
import org.sonatype.m2e.webby.internal.launch.SupportedContainers;
import org.sonatype.m2e.webby.internal.launch.WebbyLaunchConstants;

/**
 * Main tab of the Webby launch configuration: the web application and the servlet container to run it in.
 */
@SuppressWarnings("restriction")
public class WebbyTab extends JavaLaunchTab {

  /** Key under which the widgets are tagged for UI tests, it is the default key of SWTBot. */
  static final String WIDGET_ID_KEY = "org.eclipse.swtbot.widget.key";

  static final String[] LOG_LEVELS = { "low", "medium", "high" };

  private Text projectName;

  private Text contextName;

  private Button openWhenStarted;

  private Combo containerId;

  private Combo containerType;

  private Combo containerLogging;

  private Text containerHome;

  private Button containerHomeBrowse;

  private Button containerHomeVariables;

  private Spinner containerPort;

  private Spinner containerTimeout;

  private Button containerDisableWsSci;

  private SortedMap<String, SortedSet<String>> containers;

  @Override
  public String getId() {
    return "org.sonatype.m2e.webby.ui.mainTab"; //$NON-NLS-1$
  }

  @Override
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
    projectName.addModifyListener(e -> updateLaunchConfigurationDialog());
    projectName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    projectName.setFont(font);
    tag(projectName, "projectName");

    Button projectNameBrowse = createPushButton(group, LauncherMessages.AbstractJavaMainTab_1, null);
    projectNameBrowse.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
      IJavaProject project = chooseJavaProject();
      if (project != null) {
        projectName.setText(project.getElementName());
      }
    }));
    tag(projectNameBrowse, "projectNameBrowse");

    new Label(group, SWT.LEFT).setText("Context:");

    contextName = new Text(group, SWT.SINGLE | SWT.BORDER);
    contextName.addModifyListener(e -> updateLaunchConfigurationDialog());
    contextName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    contextName.setFont(font);
    tag(contextName, "contextName");

    new Label(group, SWT.NONE).setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));

    new Label(group, SWT.LEFT).setText("Open when started:");

    openWhenStarted = new Button(group, SWT.CHECK);
    openWhenStarted.setFont(font);
    openWhenStarted.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    openWhenStarted.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> updateLaunchConfigurationDialog()));
    tag(openWhenStarted, "openWhenStarted");
  }

  private void createContainerEditor(Composite parent) {
    containers = SupportedContainers.getAvailableContainers();

    Font font = parent.getFont();

    Group group = new Group(parent, SWT.NONE);
    group.setFont(font);
    group.setText("Container");
    group.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    group.setLayout(new GridLayout(5, false));

    new Label(group, SWT.LEFT).setText("Provider:");

    containerId = new Combo(group, SWT.DROP_DOWN | SWT.BORDER | SWT.READ_ONLY);
    containerId.setFont(font);
    containerId.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
    containers.keySet().forEach(containerId::add);
    containerId.setVisibleItemCount(Math.min(containerId.getItemCount(), 20));
    containerId.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
      updateContainerTypes();
      updateLaunchConfigurationDialog();
    }));
    tag(containerId, "containerId");

    new Label(group, SWT.LEFT).setText("Type:");

    containerType = new Combo(group, SWT.DROP_DOWN | SWT.BORDER | SWT.READ_ONLY);
    containerType.setFont(font);
    containerType.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
    containerType.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
      updateContainerHome();
      updateLaunchConfigurationDialog();
    }));
    tag(containerType, "containerType");

    SWTFactory.createHorizontalSpacer(group, 3);

    new Label(group, SWT.LEFT).setText("Home:");

    containerHome = new Text(group, SWT.SINGLE | SWT.BORDER);
    containerHome.setFont(font);
    containerHome.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
    containerHome.addModifyListener(e -> updateLaunchConfigurationDialog());
    tag(containerHome, "containerHome");

    containerHomeVariables = createPushButton(group, "Variables...", null);
    containerHomeVariables.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
      StringVariableSelectionDialog dialog = new StringVariableSelectionDialog(getShell());
      dialog.open();
      String variable = dialog.getVariableExpression();
      if (variable != null) {
        containerHome.insert(variable);
      }
    }));
    tag(containerHomeVariables, "containerHomeVariables");

    containerHomeBrowse = createPushButton(group, LauncherMessages.AbstractJavaMainTab_1, null);
    containerHomeBrowse.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
      String path = chooseContainerHome();
      if (path != null) {
        containerHome.setText(path);
      }
    }));
    tag(containerHomeBrowse, "containerHomeBrowse");

    new Label(group, SWT.LEFT).setText("Logging:");

    containerLogging = new Combo(group, SWT.DROP_DOWN | SWT.BORDER | SWT.READ_ONLY);
    containerLogging.setFont(font);
    containerLogging.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
    containerLogging.setItems(LOG_LEVELS);
    select(containerLogging, WebbyLaunchConstants.DEFAULT_LOG_LEVEL, null);
    containerLogging.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> updateLaunchConfigurationDialog()));
    tag(containerLogging, "containerLogging");

    SWTFactory.createHorizontalSpacer(group, 3);

    new Label(group, SWT.LEFT).setText("Port:");

    containerPort = createSpinner(group, 1, 65535);
    tag(containerPort, "containerPort");

    SWTFactory.createHorizontalSpacer(group, 3);

    new Label(group, SWT.LEFT).setText("Timeout:");

    containerTimeout = createSpinner(group, 1, Integer.MAX_VALUE);
    containerTimeout.setToolTipText("Seconds to wait for the container to start");
    tag(containerTimeout, "containerTimeout");

    SWTFactory.createHorizontalSpacer(group, 3);

    new Label(group, SWT.LEFT).setText("Disable WsSci:");

    containerDisableWsSci = new Button(group, SWT.CHECK);
    containerDisableWsSci.setFont(font);
    containerDisableWsSci.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
    containerDisableWsSci.setToolTipText("Skips the WebSocket initializer of Tomcat/TomEE to speed up the startup");
    containerDisableWsSci.addSelectionListener(
        SelectionListener.widgetSelectedAdapter(e -> updateLaunchConfigurationDialog()));
    tag(containerDisableWsSci, "containerDisableWsSci");
  }

  private Spinner createSpinner(Composite parent, int min, int max) {
    Spinner spinner = new Spinner(parent, SWT.SINGLE | SWT.BORDER);
    spinner.setFont(parent.getFont());
    spinner.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
    spinner.setMinimum(min);
    spinner.setMaximum(max);
    spinner.setPageIncrement(10);
    spinner.addModifyListener(e -> updateLaunchConfigurationDialog());
    return spinner;
  }

  private static void tag(Widget widget, String id) {
    widget.setData(WIDGET_ID_KEY, "webby." + id);
  }

  private void updateContainerTypes() {
    Set<String> types = containers.get(containerId.getText());
    String sel = containerType.getText();
    containerType.removeAll();
    if (types != null) {
      for (String type : types) {
        containerType.add(type);
        if (type.equals(sel)) {
          containerType.select(containerType.getItemCount() - 1);
        }
      }
    }
    if (containerType.getSelectionIndex() < 0 && containerType.getItemCount() > 0) {
      containerType.select(0);
    }
    updateContainerHome();
  }

  private void updateContainerHome() {
    boolean homeEnabled = ContainerType.INSTALLED.getType().equals(containerType.getText());
    containerHome.setEnabled(homeEnabled);
    containerHomeBrowse.setEnabled(homeEnabled);
    containerHomeVariables.setEnabled(homeEnabled);
    containerDisableWsSci.setEnabled(homeEnabled && isCatalina(containerId.getText()));
  }

  static boolean isCatalina(String containerId) {
    return containerId.startsWith("tomcat") || containerId.startsWith("tomee");
  }

  private IJavaProject chooseJavaProject() {
    ElementListSelectionDialog dialog = new ElementListSelectionDialog(getShell(),
        new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_DEFAULT));
    dialog.setTitle(LauncherMessages.AbstractJavaMainTab_4);
    dialog.setMessage(LauncherMessages.AbstractJavaMainTab_3);
    try {
      dialog.setElements(JavaCore.create(getWorkspaceRoot()).getJavaProjects());
    } catch (JavaModelException jme) {
      WebbyPlugin.log(jme);
    }
    IJavaProject javaProject = getJavaProject();
    if (javaProject != null) {
      dialog.setInitialSelections(javaProject);
    }
    if (dialog.open() == Window.OK) {
      return (IJavaProject) dialog.getFirstResult();
    }
    return null;
  }

  private IJavaProject getJavaProject() {
    String name = projectName.getText().trim();
    if (name.isEmpty()) {
      return null;
    }
    return JavaCore.create(getWorkspaceRoot()).getJavaProject(name);
  }

  private static IWorkspaceRoot getWorkspaceRoot() {
    return ResourcesPlugin.getWorkspace().getRoot();
  }

  private String chooseContainerHome() {
    DirectoryDialog dialog = new DirectoryDialog(getShell());
    dialog.setText("Container Home Directory");
    dialog.setMessage("Choose the home directory where the container is installed:");
    dialog.setFilterPath(containerHome.getText());
    return dialog.open();
  }

  @Override
  public void setDefaults(ILaunchConfigurationWorkingCopy config) {
    IJavaElement javaElement = getContext();
    if (javaElement != null) {
      initializeJavaProject(javaElement, config);
    } else {
      config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, "");
    }
    config.setAttribute(WebbyLaunchConstants.ATTR_CONTEXT_NAME, "");
    config.setAttribute(WebbyLaunchConstants.ATTR_CONTAINER_ID, WebbyLaunchConstants.DEFAULT_CONTAINER_ID);
    config.setAttribute(WebbyLaunchConstants.ATTR_CONTAINER_HOME, "");
    config.setAttribute(WebbyLaunchConstants.ATTR_LOG_LEVEL, WebbyLaunchConstants.DEFAULT_LOG_LEVEL);
  }

  @Override
  public void initializeFrom(ILaunchConfiguration config) {
    projectName.setText(getAttribute(config, IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, ""));
    contextName.setText(getAttribute(config, WebbyLaunchConstants.ATTR_CONTEXT_NAME, ""));

    select(containerId, getAttribute(config, WebbyLaunchConstants.ATTR_CONTAINER_ID, ""),
        WebbyLaunchConstants.DEFAULT_CONTAINER_ID);
    updateContainerTypes();

    containerHome.setText(getAttribute(config, WebbyLaunchConstants.ATTR_CONTAINER_HOME, ""));
    select(containerLogging, getAttribute(config, WebbyLaunchConstants.ATTR_LOG_LEVEL, ""),
        WebbyLaunchConstants.DEFAULT_LOG_LEVEL);

    containerPort.setSelection(getIntAttribute(config, WebbyLaunchConstants.ATTR_CONTAINER_PORT,
        WebbyLaunchConstants.DEFAULT_PORT));
    containerTimeout.setSelection(getIntAttribute(config, WebbyLaunchConstants.ATTR_CONTAINER_TIMEOUT,
        WebbyLaunchConstants.DEFAULT_TIMEOUT));

    containerDisableWsSci.setSelection(getAttribute(config, WebbyLaunchConstants.ATTR_CONTAINER_DISABLE_WS_SCI,
        WebbyLaunchConstants.DEFAULT_DISABLE_WS_SCI));
    openWhenStarted.setSelection(getAttribute(config, WebbyLaunchConstants.ATTR_OPEN_WHEN_STARTED,
        WebbyLaunchConstants.DEFAULT_OPEN_WHEN_STARTED));

    super.initializeFrom(config);
  }

  private String getAttribute(ILaunchConfiguration config, String name, String defaultValue) {
    try {
      return config.getAttribute(name, defaultValue);
    } catch (CoreException e) {
      setErrorMessage(e.getStatus().getMessage());
      return defaultValue;
    }
  }

  private boolean getAttribute(ILaunchConfiguration config, String name, boolean defaultValue) {
    try {
      return config.getAttribute(name, defaultValue);
    } catch (CoreException e) {
      setErrorMessage(e.getStatus().getMessage());
      return defaultValue;
    }
  }

  private int getIntAttribute(ILaunchConfiguration config, String name, int defaultValue) {
    try {
      // older versions stored some numbers as strings
      Object value = config.getAttributes().get(name);
      if (value instanceof Integer intValue) {
        return intValue;
      }
      if (value instanceof String strValue) {
        return Integer.parseInt(strValue.trim());
      }
    } catch (CoreException e) {
      setErrorMessage(e.getStatus().getMessage());
    } catch (NumberFormatException e) {
      // just stick to the default value
    }
    return defaultValue;
  }

  private static void select(Combo combo, String value, String fallback) {
    int index = combo.indexOf(value);
    if (index < 0 && fallback != null) {
      index = combo.indexOf(fallback);
    }
    if (index < 0 && combo.getItemCount() > 0) {
      index = combo.getItemCount() - 1;
    }
    if (index >= 0) {
      combo.select(index);
    }
  }

  @Override
  public boolean isValid(ILaunchConfiguration config) {
    setErrorMessage(null);
    setMessage(null);

    String name = projectName.getText().trim();
    if (name.isEmpty()) {
      setErrorMessage("No project specified");
      return false;
    }
    IStatus status = ResourcesPlugin.getWorkspace().validateName(name, IResource.PROJECT);
    if (!status.isOK()) {
      setErrorMessage("Invalid project name: " + status.getMessage());
      return false;
    }
    IProject project = getWorkspaceRoot().getProject(name);
    if (!project.exists()) {
      setErrorMessage("Project " + name + " does not exist");
      return false;
    }
    if (!project.isOpen()) {
      setErrorMessage("Project " + name + " is closed");
      return false;
    }
    if (containerId.getSelectionIndex() < 0) {
      setErrorMessage("No container provider selected");
      return false;
    }
    if (containerHome.isEnabled() && containerHome.getText().isBlank()) {
      setErrorMessage("No container home directory specified");
      return false;
    }
    return true;
  }

  @Override
  public void performApply(ILaunchConfigurationWorkingCopy config) {
    String name = projectName.getText().trim();
    config.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, name);
    config.setAttribute(WebbyLaunchConstants.ATTR_CONTEXT_NAME, contextName.getText().trim());
    config.setAttribute(WebbyLaunchConstants.ATTR_OPEN_WHEN_STARTED, openWhenStarted.getSelection());
    config.setAttribute(WebbyLaunchConstants.ATTR_CONTAINER_ID, containerId.getText().trim());
    config.setAttribute(WebbyLaunchConstants.ATTR_CONTAINER_HOME, containerHome.getText().trim());
    config.setAttribute(WebbyLaunchConstants.ATTR_LOG_LEVEL, containerLogging.getText().trim());
    config.setAttribute(WebbyLaunchConstants.ATTR_CONTAINER_PORT, containerPort.getSelection());
    config.setAttribute(WebbyLaunchConstants.ATTR_CONTAINER_TIMEOUT, containerTimeout.getSelection());
    config.setAttribute(WebbyLaunchConstants.ATTR_CONTAINER_DISABLE_WS_SCI, containerDisableWsSci.getSelection());

    if (!name.isEmpty() && ResourcesPlugin.getWorkspace().validateName(name, IResource.PROJECT).isOK()) {
      config.setMappedResources(new IResource[] { getWorkspaceRoot().getProject(name) });
    } else {
      config.setMappedResources(null);
    }
  }

  @Override
  public String getName() {
    return LauncherMessages.JavaMainTab__Main_19;
  }

  @Override
  public Image getImage() {
    return WebbyImages.LAUNCH_CONFIG;
  }

}
