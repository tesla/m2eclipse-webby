package org.sonatype.m2e.webby.internal.util;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.codehaus.plexus.util.SelectorUtils;

/**
 * Matches paths against Ant-style include/exclude patterns.
 */
public class PathSelector {

  private static final Pattern SEPARATORS = Pattern.compile("[\\\\/]");

  private final String[] includes;

  private final String[] excludes;

  public PathSelector(List<String> includes, List<String> excludes) {
    this.includes = normalizePatterns(includes);
    this.excludes = normalizePatterns(excludes);
  }

  private static String[] normalizePatterns(List<String> patterns) {
    String[] normalized;

    if (patterns != null) {
      normalized = new String[patterns.size()];
      for (int i = patterns.size() - 1; i >= 0; i--) {
        normalized[i] = normalizePattern(patterns.get(i));
      }
    } else {
      normalized = new String[0];
    }

    return normalized;
  }

  private static String normalizePattern(String pattern) {
    if (pattern == null) {
      return "";
    }

    String normalized = normalizePath(pattern);

    if (normalized.endsWith(File.separator)) {
      normalized += "**";
    }

    return normalized;
  }

  /**
   * @return the path using the platform file separator
   */
  public static String normalizePath(String path) {
    return SEPARATORS.matcher(path).replaceAll(Matcher.quoteReplacement(File.separator));
  }

  public boolean isSelected(String pathname) {
    String normalized = normalizePath(pathname);
    if (includes.length > 0 && !isMatched(normalized, includes)) {
      return false;
    }
    return excludes.length == 0 || !isMatched(normalized, excludes);
  }

  private static boolean isMatched(String pathname, String[] patterns) {
    for (int i = patterns.length - 1; i >= 0; i--) {
      String pattern = patterns[i];
      if (SelectorUtils.matchPath(pattern, pathname)) {
        return true;
      }
    }
    return false;
  }

  public boolean isAncestorOfPotentiallySelected(String pathname) {
    if (includes.length == 0) {
      return true;
    }
    String normalized = normalizePath(pathname);
    for (String include : includes) {
      if (SelectorUtils.matchPatternStart(include, normalized)) {
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
