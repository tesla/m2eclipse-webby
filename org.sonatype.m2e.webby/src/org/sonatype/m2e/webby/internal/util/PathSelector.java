/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.sonatype.m2e.webby.internal.util;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.codehaus.plexus.util.SelectorUtils;


/**
 */
public class PathSelector {

  private final String[] includes;

  private final String[] excludes;

  public PathSelector(List<String> includes, List<String> excludes) {
    this.includes = normalizePatterns(includes);
    this.excludes = normalizePatterns(excludes);
  }

  private static String[] normalizePatterns(List<String> patterns) {
    String[] normalized;

    if(patterns != null) {
      normalized = new String[patterns.size()];
      for(int i = patterns.size() - 1; i >= 0; i-- ) {
        normalized[i] = normalizePattern(patterns.get(i));
      }
    } else {
      normalized = new String[0];
    }

    return normalized;
  }

  private static String normalizePattern(String pattern) {
    if(pattern == null) {
      return "";
    }

    String normalized = normalizePath(pattern);

    if(normalized.endsWith(File.separator)) {
      normalized += "**";
    }

    return normalized;
  }

  public static String normalizePath(String path) {
    return path.replace((File.separatorChar == '/') ? '\\' : '/', File.separatorChar);
  }

  public boolean isSelected(String pathname) {
    String normalized = normalizePath(pathname);
    if(includes.length > 0 && !isMatched(normalized, includes)) {
      return false;
    }
    if(excludes.length > 0 && isMatched(normalized, excludes)) {
      return false;
    }
    return true;
  }

  private static boolean isMatched(String pathname, String[] patterns) {
    for(int i = patterns.length - 1; i >= 0; i-- ) {
      String pattern = patterns[i];
      if(SelectorUtils.matchPath(pattern, pathname)) {
        return true;
      }
    }
    return false;
  }

  public boolean isAncestorOfPotentiallySelected(String pathname) {
    if(includes.length <= 0) {
      return true;
    }
    for(String include : includes) {
      if(SelectorUtils.matchPatternStart(include, pathname)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public String toString() {
    return "includes = " + Arrays.asList(includes) + ", excludes = " + Arrays.asList(excludes);
  }

}
