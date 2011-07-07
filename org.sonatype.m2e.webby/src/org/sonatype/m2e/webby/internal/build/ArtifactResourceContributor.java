/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.sonatype.m2e.webby.internal.build;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.sonatype.m2e.webby.internal.config.OverlayConfiguration;
import org.sonatype.m2e.webby.internal.util.PathCollector;
import org.sonatype.m2e.webby.internal.util.PathSelector;



/**
 */
public class ArtifactResourceContributor extends ResourceContributor {

  private File path;

  private OverlayConfiguration overlayConfig;

  public ArtifactResourceContributor(int ordinal, File path, OverlayConfiguration overlayConfig) {
    super(ordinal);
    this.path = path;
    this.overlayConfig = overlayConfig;
  }

  public void contribute(WarAssembler assembler, IProgressMonitor monitor) {
    try {
      if(path.isDirectory()) {
        processDirectory(assembler, monitor);
      } else {
        processFile(assembler, monitor);
      }
    } finally {
      if(monitor != null) {
        monitor.done();
      }
    }
  }

  private void processFile(WarAssembler assembler, IProgressMonitor monitor) {
    boolean filtering = overlayConfig.isFiltering();
    String encoding = overlayConfig.getEncoding();

    PathSelector pathSelector = new PathSelector(overlayConfig.getIncludes(), overlayConfig.getExcludes());

    long lastModified = path.lastModified();

    try {
      ZipInputStream zis = new ZipInputStream(new FileInputStream(path));
      InputStream ncis = new NonClosingInputStream(zis);
      try {
        for(ZipEntry ze = zis.getNextEntry(); ze != null; ze = zis.getNextEntry()) {
          String path = ze.getName();
          if(ze.isDirectory() || !pathSelector.isSelected(path)) {
            continue;
          }
          String targetPath = overlayConfig.getTargetPath(path);
          if(assembler.registerTargetPath(targetPath, ordinal)) {
            try {
              assembler.copyResourceFile(ncis, targetPath, filtering, encoding, lastModified);
            } catch(IOException e) {
              assembler.addError(this.path.getAbsolutePath() + "!/" + path, targetPath, e);
            }
          }
        }
      } finally {
        zis.close();
      }
    } catch(IOException e) {
      assembler.addError(this.path.getAbsolutePath(), null, e);
    }
  }

  private void processDirectory(WarAssembler assembler, IProgressMonitor monitor) {
    boolean filtering = overlayConfig.isFiltering();
    String encoding = overlayConfig.getEncoding();

    PathCollector pathCollector = new PathCollector(overlayConfig.getIncludes(), overlayConfig.getExcludes());

    for(String file : pathCollector.collectFiles(path)) {
      String targetPath = overlayConfig.getTargetPath(file);
      if(!assembler.registerTargetPath(targetPath, ordinal)) {
        continue;
      }
      if(targetPath.startsWith("WEB-INF/lib/") || targetPath.startsWith("WEB-INF/classes/")) {
        continue;
      }
      File sourceFile = new File(path, file);
      try {
        InputStream is = new FileInputStream(sourceFile);
        try {
          assembler.copyResourceFile(is, targetPath, filtering, encoding, sourceFile.lastModified());
        } finally {
          is.close();
        }
      } catch(IOException e) {
        assembler.addError(sourceFile.getAbsolutePath(), targetPath, e);
      }
    }
  }

}
