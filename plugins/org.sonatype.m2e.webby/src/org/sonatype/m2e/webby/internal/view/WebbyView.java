/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.sonatype.m2e.webby.internal.view;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.internal.ui.actions.RelaunchActionDelegate;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;
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
 */
public class WebbyView extends ViewPart implements IWebAppListener {

  private static final int EXTERNAL_BROWSER_MOD_MASK = SWT.MOD1;

  private WebAppRegistry webAppRegistry;

  private Table table;

  private Action browse;

  private Action stop;

  private Action restart;

  public WebbyView() {
    webAppRegistry = WebbyPlugin.getDefault().getWebAppRegistry();
  }

  public void webAppStarted(final IWebApp webApp) {
    Display display = table.getDisplay();
    if(display == null) {
      return;
    }
    display.asyncExec(new Runnable() {
      public void run() {
        add(webApp);
      }
    });
  }

  public void webAppStopped(final IWebApp webApp) {
    Display display = table.getDisplay();
    if(display == null) {
      return;
    }
    display.asyncExec(new Runnable() {
      public void run() {
        remove(webApp);
      }
    });
  }

  private void add(IWebApp webApp) {
    TableItem item = new TableItem(table, SWT.LEFT);
    if(webApp.getContext().startsWith("/")) {
      item.setText(0, webApp.getContext());
    } else {
      item.setText(0, "/" + webApp.getContext());
    }
    item.setText(1, webApp.getPort());
    item.setText(2, webApp.getContainerId());
    item.setData(webApp);
  }

  private void remove(IWebApp webApp) {
    for(int i = table.getItemCount() - 1; i >= 0; i-- ) {
      TableItem item = table.getItem(i);
      if(item.getData() == webApp) {
        table.remove(i);
      }
    }
    updateActions();
  }

  private void stop() {
    for(TableItem item : table.getSelection()) {
      final IWebApp webApp = (IWebApp) item.getData();
      Job job = new Job("Stopping " + webApp.getContainerId() + ":" + webApp.getPort() + "/" + webApp.getContext()) {
        @Override
        protected IStatus run(IProgressMonitor monitor) {
          try {
            webApp.stop();
            return Status.OK_STATUS;
          } catch(Exception e) {
            return WebbyPlugin.newStatus(e.getMessage(), e);
          }
        }
      };
      job.schedule();
    }
  }

  private void restart() {
    for(TableItem item : table.getSelection()) {
      final IWebApp webApp = (IWebApp) item.getData();
      Job job = new Job("Restarting " + webApp.getContainerId() + ":" + webApp.getPort() + "/" + webApp.getContext()) {
        @Override
        protected IStatus run(IProgressMonitor monitor) {
          try {
            RelaunchActionDelegate.relaunch(webApp.getLaunch().getLaunchConfiguration(), webApp.getLaunch().getLaunchMode(), true);
            return Status.OK_STATUS;
          } catch(Exception e) {
            return WebbyPlugin.newStatus(e.getMessage(), e);
          }
        }
      };
      job.schedule();
    }
  }
  private void browse(boolean external) {
    if(table.getSelectionCount() != 1) {
      return;
    }
    TableItem item = table.getSelection()[0];
    IWebApp webApp = (IWebApp) item.getData();
    String url = "http://localhost:" + webApp.getPort() + "/" + webApp.getContext();

    if(external) {
      Program.launch(url);
    } else {
      String id = "webby-" + webApp.getContext();
      try {
        PlatformUI.getWorkbench().getBrowserSupport().createBrowser(id).openURL(new URL(url));
      } catch(PartInitException e) {
        WebbyPlugin.log(e);
      } catch(MalformedURLException e) {
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
    TableColumn col = new TableColumn(table, SWT.LEFT);
    col.setText("Context");
    col.setMoveable(true);
    col.setWidth(200);
    col = new TableColumn(table, SWT.RIGHT);
    col.setText("Port");
    col.setMoveable(true);
    col.setWidth(100);
    col = new TableColumn(table, SWT.LEFT);
    col.setText("Container");
    col.setMoveable(true);
    col.setWidth(200);

    table.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        updateActions();
      }

      @Override
      public void widgetDefaultSelected(SelectionEvent e) {
        browse((e.stateMask & EXTERNAL_BROWSER_MOD_MASK) != 0);
      }
    });

    for(IWebApp webApp : webAppRegistry.getWebApps()) {
      add(webApp);
    }

    webAppRegistry.addListener(this);

    createActions();
    createToolbar();
    hookContextMenu();
  }

  private void updateActions() {
    stop.setEnabled(table.getSelectionCount() > 0);
    restart.setEnabled(table.getSelectionCount() > 0);
    browse.setEnabled(table.getSelectionCount() == 1);
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
    restart.setToolTipText("Restart the selected web applications");
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
    menuMgr.addMenuListener(new IMenuListener() {
      public void menuAboutToShow(IMenuManager manager) {
        manager.add(browse);
        manager.add(new Separator());
        manager.add(stop);
      }
    });
    Menu menu = menuMgr.createContextMenu(table);
    table.setMenu(menu);
  }

  @Override
  public void dispose() {
    super.dispose();

    if(webAppRegistry != null) {
      webAppRegistry.removeListener(this);
    }
  }

  @Override
  public void setFocus() {
    table.setFocus();
  }

}
