package org.sonatype.m2e.webby.internal.launch;

public final class WebbyLaunchConstants {

  public static final String TYPE_ID = "org.sonatype.m2e.webby.launchConfigType";

  public static final String SOURCE_PATH_PROVIDER_ID = "org.sonatype.m2e.webby.sourcepathProvider";

  private static final String ATTR_PREFIX = "org.sonatype.m2e.webby.";

  public static final String ATTR_CONTEXT_NAME = ATTR_PREFIX + "contextName";

  public static final String ATTR_OPEN_WHEN_STARTED = ATTR_PREFIX + "openWhenStarted";

  public static final String ATTR_LOG_LEVEL = ATTR_PREFIX + "logLevel";

  public static final String ATTR_CONTAINER_ID = ATTR_PREFIX + "containerId";

  public static final String ATTR_CONTAINER_HOME = ATTR_PREFIX + "containerHome";

  public static final String ATTR_CONTAINER_PORT = ATTR_PREFIX + "containerPort";

  public static final String ATTR_CONTAINER_TIMEOUT = ATTR_PREFIX + "containerTimeout";

  public static final String ATTR_CONTAINER_DISABLE_WS_SCI = ATTR_PREFIX + "disableWsSci";

  public static final String ATTR_SYS_PROP_FILES = ATTR_PREFIX + "sysPropFiles";

  public static final String DEFAULT_CONTAINER_ID = "tomcat11x";

  public static final String DEFAULT_LOG_LEVEL = "medium";

  public static final int DEFAULT_PORT = 8080;

  /** Timeout in seconds. */
  public static final int DEFAULT_TIMEOUT = 60;

  public static final boolean DEFAULT_OPEN_WHEN_STARTED = true;

  public static final boolean DEFAULT_DISABLE_WS_SCI = true;

  private WebbyLaunchConstants() {
  }

}
