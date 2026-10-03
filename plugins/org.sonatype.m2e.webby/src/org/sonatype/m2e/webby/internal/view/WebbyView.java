package org.sonatype.m2e.webby.internal.view;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.sonatype.m2e.webby.internal.IWebApp;
import org.sonatype.m2e.webby.internal.IWebAppListener;
import org.sonatype.m2e.webby.internal.WebAppRegistry;
import org.sonatype.m2e.webby.internal.WebbyImages;
import org.sonatype.m2e.webby.internal.WebbyPlugin;

/**
 * The "Web Apps" view, listing the running web applications and allowing to open, stop or restart them.
 */
public class WebbyView extends ViewPart implements IWebAppListener {

  public static final String ID = "org.sonatype.m2e.webby.webApps";

  private static final int EXTERNAL_BROWSER_MOD_MASK = SWT.MOD1;

  private static final long RESTART_TIMEOUT_MILLIS = 60_000;

  private final WebAppRegistry webAppRegistry;

  private Table table;

  private Action browse;

  private Action stop;

  private Action restart;

  public WebbyView() {
    webAppRegistry = WebbyPlugin.getDefault().getWebAppRegistry();
  }

  @Override
  public void webAppStarted(IWebApp webApp) {
    asyncExec(() -> add(webApp));
  }

  @Override
  public void webAppStopped(IWebApp webApp) {
    asyncExec(() -> remove(webApp));
  }

  private void asyncExec(Runnable runnable) {
    if (table == null || table.isDisposed()) {
      return;
    }
    table.getDisplay().asyncExec(() -> {
      if (!table.isDisposed()) {
        runnable.run();
      }
    });
  }

  private void add(IWebApp webApp) {
    TableItem item = new TableItem(table, SWT.LEFT);
    String context = webApp.getContext();
    item.setText(0, context.startsWith("/") ? context : "/" + context);
    item.setText(1, webApp.getPort());
    item.setText(2, webApp.getContainerId());
    item.setData(webApp);
    updateActions();
  }

  private void remove(IWebApp webApp) {
    for (int i = table.getItemCount() - 1; i >= 0; i--) {
      if (table.getItem(i).getData() == webApp) {
        table.remove(i);
      }
    }
    updateActions();
  }

  private List<IWebApp> getSelectedWebApps() {
    List<IWebApp> webApps = new ArrayList<>();
    for (TableItem item : table.getSelection()) {
      webApps.add((IWebApp) item.getData());
    }
    return webApps;
  }

  private void stop() {
    for (IWebApp webApp : getSelectedWebApps()) {
      Job.create("Stopping " + describe(webApp), monitor -> {
        return stop(webApp);
      }).schedule();
    }
  }

  private static IStatus stop(IWebApp webApp) {
    try {
      webApp.stop();
      return Status.OK_STATUS;
    } catch (Exception e) {
      return WebbyPlugin.newStatus("Failed to stop " + describe(webApp) + ": " + e.getMessage(), e);
    }
  }

  private void restart() {
    for (IWebApp webApp : getSelectedWebApps()) {
      Job.create("Restarting " + describe(webApp), monitor -> {
        return restart(webApp, monitor);
      }).schedule();
    }
  }

  private static IStatus restart(IWebApp webApp, IProgressMonitor monitor) {
    ILaunch launch = webApp.getLaunch();
    ILaunchConfiguration configuration = launch.getLaunchConfiguration();
    String mode = launch.getLaunchMode();

    IStatus status = stop(webApp);
    if (!status.isOK()) {
      return status;
    }

    // the container must be gone before it can be started again on the same ports
    long deadline = System.currentTimeMillis() + RESTART_TIMEOUT_MILLIS;
    while (!launch.isTerminated()) {
      if (monitor.isCanceled()) {
        return Status.CANCEL_STATUS;
      }
      if (System.currentTimeMillis() > deadline) {
        return WebbyPlugin.newStatus(describe(webApp) + " did not stop in time, not restarting it", null);
      }
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return Status.CANCEL_STATUS;
      }
    }

    PlatformUI.getWorkbench().getDisplay().asyncExec(() -> DebugUITools.launch(configuration, mode));
    return Status.OK_STATUS;
  }

  private static String describe(IWebApp webApp) {
    return webApp.getContainerId() + ":" + webApp.getPort() + "/" + webApp.getContext();
  }

  private void browse(boolean external) {
    if (table.getSelectionCount() != 1) {
      return;
    }
    IWebApp webApp = (IWebApp) table.getSelection()[0].getData();
    String url = webApp.getUrl();

    if (external) {
      Program.launch(url);
    } else {
      try {
        PlatformUI.getWorkbench().getBrowserSupport().createBrowser("webby-" + webApp.getContext())
            .openURL(URI.create(url).toURL());
      } catch (PartInitException | MalformedURLException e) {
        WebbyPlugin.log(e);
      }
    }
  }

  @Override
  public void createPartControl(Composite parent) {
    parent.setLayout(new GridLayout());

    table = new Table(parent, SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION);
    table.setFont(parent.getFont());
    table.setLinesVisible(true);
    table.setHeaderVisible(true);
    table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    createColumn("Context", SWT.LEFT, 200);
    createColumn("Port", SWT.RIGHT, 100);
    createColumn("Container", SWT.LEFT, 200);

    table.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> updateActions()));
    table.addSelectionListener(SelectionListener.widgetDefaultSelectedAdapter(
        e -> browse((e.stateMask & EXTERNAL_BROWSER_MOD_MASK) != 0)));

    createActions();
    createToolbar();
    hookContextMenu();

    for (IWebApp webApp : webAppRegistry.getWebApps()) {
      add(webApp);
    }
    webAppRegistry.addListener(this);
  }

  private void createColumn(String text, int style, int width) {
    TableColumn col = new TableColumn(table, style);
    col.setText(text);
    col.setMoveable(true);
    col.setWidth(width);
  }

  private void updateActions() {
    int count = table.getSelectionCount();
    stop.setEnabled(count > 0);
    restart.setEnabled(count > 0);
    browse.setEnabled(count == 1);
  }

  private void createActions() {
    stop = new Action("Stop", WebbyImages.STOP_DESC) {
      @Override
      public void run() {
        stop();
      }
    };
    stop.setToolTipText("Stops the selected web applications");

    restart = new Action("Restart", WebbyImages.RESTART_DESC) {
      @Override
      public void run() {
        restart();
      }
    };
    restart.setToolTipText("Restarts the selected web applications");

    browse = new Action("Browse", WebbyImages.BROWSE_DESC) {
      @Override
      public void runWithEvent(Event event) {
        browse((event.stateMask & EXTERNAL_BROWSER_MOD_MASK) != 0);
      }
    };
    browse.setToolTipText("Opens the selected web application, hold modifier key to open in external browser");

    updateActions();
  }

  private void createToolbar() {
    IToolBarManager mgr = getViewSite().getActionBars().getToolBarManager();
    mgr.add(browse);
    mgr.add(new Separator());
    mgr.add(stop);
    mgr.add(restart);
  }

  private void hookContextMenu() {
    MenuManager menuMgr = new MenuManager(null);
    menuMgr.setRemoveAllWhenShown(true);
    menuMgr.addMenuListener(manager -> {
      manager.add(browse);
      manager.add(new Separator());
      manager.add(stop);
      manager.add(restart);
    });
    table.setMenu(menuMgr.createContextMenu(table));
  }

  @Override
  public void dispose() {
    webAppRegistry.removeListener(this);
    super.dispose();
  }

  @Override
  public void setFocus() {
    table.setFocus();
  }

}
