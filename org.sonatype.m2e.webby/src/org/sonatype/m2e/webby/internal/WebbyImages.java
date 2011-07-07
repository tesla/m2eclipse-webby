/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.sonatype.m2e.webby.internal;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;


/**
 *
 */
public class WebbyImages {

  public static final ImageDescriptor LAUNCH_CONFIG_DESC = createImageDescriptor("webby.gif");

  public static final Image LAUNCH_CONFIG = createImage("webby.gif", LAUNCH_CONFIG_DESC);

  public static final ImageDescriptor STOP_DESC = createImageDescriptor("stop.gif");

  public static final ImageDescriptor BROWSE_DESC = createImageDescriptor("browse.gif");

  private static Image createImage(String key, ImageDescriptor imageDescriptor) {
    if(imageDescriptor == null) {
      return null;
    }
    WebbyPlugin plugin = WebbyPlugin.getDefault();
    ImageRegistry imageRegistry = plugin.getImageRegistry();
    imageRegistry.put(key, imageDescriptor);
    return imageRegistry.get(key);
  }

  private static ImageDescriptor createImageDescriptor(String key) {
    WebbyPlugin plugin = WebbyPlugin.getDefault();
    String pluginId = WebbyPlugin.getPluginId();
    ImageDescriptor imageDescriptor = AbstractUIPlugin.imageDescriptorFromPlugin(pluginId, "icons/" + key);
    if(imageDescriptor == null) {
      plugin.getLog().log(new Status(IStatus.ERROR, pluginId, IStatus.ERROR, "Could not locate image " + key, null));
      return null;
    }
    return imageDescriptor;
  }

}
