package org.sonatype.m2e.webby.tests;

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.utils.SWTBotPreferences;
import org.eclipse.swtbot.swt.finder.widgets.TimeoutException;

public final class UITestSupport {

  private UITestSupport() {
  }

  /**
   * @return a bot for the workbench, which got rid of the welcome page
   */
  public static SWTWorkbenchBot createBot() {
    SWTBotPreferences.TIMEOUT = 20_000;
    SWTBotPreferences.PLAYBACK_DELAY = 10;
    SWTBotPreferences.KEYBOARD_LAYOUT = "EN_US";
    SWTWorkbenchBot bot = new SWTWorkbenchBot();
    for (SWTBotView view : bot.views()) {
      if ("Welcome".equals(view.getTitle())) {
        try {
          view.close();
        } catch (TimeoutException e) {
          // not fatal
        }
      }
    }
    return bot;
  }

}
