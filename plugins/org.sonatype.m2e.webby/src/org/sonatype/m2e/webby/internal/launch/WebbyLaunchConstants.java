/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.sonatype.m2e.webby.internal.launch;

/**
 */
public abstract class WebbyLaunchConstants {

  public static final String TYPE_ID = "org.sonatype.m2e.webby.launchConfigType";

  private static final String ATTR_PREFIX = "org.sonatype.m2e.webby.";

  public static final String ATTR_CONTEXT_NAME = ATTR_PREFIX + "contextName";

  public static final String ATTR_OPEN_WHEN_STARTED = ATTR_PREFIX + "openWhenStarted";

  public static final String ATTR_LOG_LEVEL = ATTR_PREFIX + "logLevel";

  public static final String ATTR_CONTAINER_ID = ATTR_PREFIX + "containerId";

  public static final String ATTR_CONTAINER_TYPE = ATTR_PREFIX + "containerType";

  public static final String ATTR_CONTAINER_HOME = ATTR_PREFIX + "containerHome";

  public static final String ATTR_CONTAINER_PORT = ATTR_PREFIX + "containerPort";

  public static final String ATTR_CONTAINER_TIMEOUT = ATTR_PREFIX + "containerTimeout";

  public static final String ATTR_CONTAINER_DISABLE_WS_SCI = ATTR_PREFIX + "disableWsSci";

  public static final String ATTR_SYS_PROP_FILES = ATTR_PREFIX + "sysPropFiles";

}
