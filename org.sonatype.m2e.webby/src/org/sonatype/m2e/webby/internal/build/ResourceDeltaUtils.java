/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.sonatype.m2e.webby.internal.build;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.IPath;
import org.eclipse.m2e.core.project.MavenProjectUtils;


/**
 */
class ResourceDeltaUtils {

  public static IResourceDelta findChildDelta(IResourceDelta resDelta, IProject project, String path) {
    IResourceDelta childDelta = null;
    if(resDelta != null) {
      IPath p = MavenProjectUtils.getProjectRelativePath(project, path);
      if(p != null) {
        childDelta = resDelta.findMember(p);
      }
    }
    return childDelta;
  }

  public static IResourceDelta findChildDelta(IResourceDelta resDelta, IProject project, File path) {
    return findChildDelta(resDelta, project, path.getAbsolutePath());
  }

}
