/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.sonatype.m2e.webby.internal.util;

import org.eclipse.core.resources.IProject;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.IMavenProjectFacade;


/**
 */
public class MavenUtils {
  public static IMavenProjectFacade getFacade(IProject project) {
    return MavenPlugin.getMavenProjectRegistry().getProject(project);
  }

  public static IMavenProjectFacade getFacade(String groupId, String artifactId, String baseVersion) {
    return MavenPlugin.getMavenProjectRegistry().getMavenProject(groupId, artifactId, baseVersion);
  }
}
