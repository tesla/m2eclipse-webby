package org.sonatype.m2e.webby.internal;

import java.net.*;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.resource.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;

public class WebbyImages {

  public static final ImageDescriptor LAUNCH_CONFIG_DESC = createImageDescriptor("webby.gif");

  public static final Image LAUNCH_CONFIG = createImage("webby.gif", LAUNCH_CONFIG_DESC);

  public static final ImageDescriptor STOP_DESC = createImageDescriptor("stop.gif");

  public static final ImageDescriptor RESTART_DESC = createImageFromURL("platform:/plugin/org.eclipse.debug.ui.launchview/icons/term_restart.png");

  public static final ImageDescriptor BROWSE_DESC = createImageDescriptor("browse.gif");

  private static Image createImage(String key, ImageDescriptor imageDescriptor) {
    if (imageDescriptor == null) {
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
    if (imageDescriptor == null) {
      plugin.getLog().log(new Status(IStatus.ERROR, pluginId, IStatus.ERROR, "Could not locate image " + key, null));
      return null;
    }
    return imageDescriptor;
  }

  protected static ImageDescriptor createImageFromURL(String url) {
    try {
      return ImageDescriptor.createFromURL(new URL(url));
    } catch (MalformedURLException e) {
      // Rien � faire.
    }
    return null;
  }
}
