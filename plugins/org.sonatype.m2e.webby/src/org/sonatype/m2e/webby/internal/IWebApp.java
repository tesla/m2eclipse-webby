/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.sonatype.m2e.webby.internal;

import org.eclipse.debug.core.ILaunch;


/**
 */
public interface IWebApp {

  ILaunch getLaunch();

  String getContext();

  String getPort();

  String getContainerId();

  void stop() throws Exception;

}
