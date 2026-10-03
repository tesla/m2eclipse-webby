package org.sonatype.m2e.webby.internal;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;

public final class WebbyImages {

  private static final String DEBUG_UI_PLUGIN_ID = "org.eclipse.debug.ui";

  public static final ImageDescriptor LAUNCH_CONFIG_DESC = createImageDescriptor("webby.gif");

  public static final Image LAUNCH_CONFIG = createImage("webby.gif", LAUNCH_CONFIG_DESC);

  public static final ImageDescriptor STOP_DESC = createImageDescriptor("stop.gif");

  public static final ImageDescriptor RESTART_DESC = createImageDescriptor(DEBUG_UI_PLUGIN_ID,
      "icons/full/etool16/term_restart.png");

  public static final ImageDescriptor BROWSE_DESC = createImageDescriptor("browse.gif");

  private WebbyImages() {
  }

  private static Image createImage(String key, ImageDescriptor imageDescriptor) {
    if (imageDescriptor == null) {
      return null;
    }
    ImageRegistry imageRegistry = WebbyPlugin.getDefault().getImageRegistry();
    if (imageRegistry.getDescriptor(key) == null) {
      imageRegistry.put(key, imageDescriptor);
    }
    return imageRegistry.get(key);
  }

  private static ImageDescriptor createImageDescriptor(String key) {
    return createImageDescriptor(WebbyPlugin.getPluginId(), "icons/" + key);
  }

  private static ImageDescriptor createImageDescriptor(String pluginId, String path) {
    ImageDescriptor imageDescriptor = AbstractUIPlugin.imageDescriptorFromPlugin(pluginId, path);
    if (imageDescriptor == null) {
      WebbyPlugin.log("Could not locate image " + pluginId + "/" + path, IStatus.ERROR);
    }
    return imageDescriptor;
  }

}
