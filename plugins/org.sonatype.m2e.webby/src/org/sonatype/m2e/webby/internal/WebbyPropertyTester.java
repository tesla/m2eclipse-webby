/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.sonatype.m2e.webby.internal;

import org.apache.maven.model.Model;
import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.m2e.core.MavenPlugin;


/**
 */
public class WebbyPropertyTester extends PropertyTester {

  private static final String IS_WEB_APP = "isWebApp";
  
  private static final String POM_FILE_NAME = "pom.xml";

  public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
    if(IS_WEB_APP.equals(property)) {
      IFile pomFile = getPomFile((IAdaptable) receiver);
      if(pomFile != null && pomFile.exists()) {
        try {
          Model pom = MavenPlugin.getMaven().readModel(pomFile.getContents(true));
          return "war".equals(pom.getPackaging());
        } catch(CoreException e) {
          WebbyPlugin.getDefault().getLog()
              .log(new Status(IStatus.ERROR, WebbyPlugin.getPluginId(), e.getMessage(), e));
        }
      }
    }
    return false;
  }

  private IFile getPomFile(IAdaptable adaptable) {
    IProject project = (IProject) adaptable.getAdapter(IProject.class);
    if(project != null) {
      return project.getFile(POM_FILE_NAME);
    }

    IFile file = (IFile) adaptable.getAdapter(IFile.class);
    if(file != null && POM_FILE_NAME.equals(file.getName()) && file.getFullPath().segmentCount() == 2) {
      return file;
    }

    return null;
  }

}
