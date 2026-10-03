package org.sonatype.m2e.webby.internal.launch;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.codehaus.cargo.container.ContainerType;
import org.codehaus.cargo.generic.DefaultContainerFactory;

/**
 * The servlet containers Webby can launch, i.e. the Cargo containers that have been verified to work with an exploded
 * WAR directory and an extra classpath.
 */
public final class SupportedContainers {

  /** Cargo container ids, oldest first within each family. */
  public static final List<String> IDS = List.of(
      "tomcat8x", "tomcat9x", "tomcat10x", "tomcat11x",
      "tomee8x", "tomee9x", "tomee10x",
      "jetty9x", "jetty10x", "jetty11x", "jetty12x",
      "glassfish5x", "glassfish6x", "glassfish7x", "glassfish8x");

  private static final Set<String> LOCAL_TYPES = Set.of(ContainerType.INSTALLED.getType());

  private SupportedContainers() {
  }

  public static boolean isSupported(String containerId) {
    return IDS.contains(containerId);
  }

  /**
   * @return the supported container ids known to the embedded Cargo, mapped to the container types Webby can launch
   */
  public static SortedMap<String, SortedSet<String>> getAvailableContainers() {
    return filter(new DefaultContainerFactory().getContainerIds());
  }

  static SortedMap<String, SortedSet<String>> filter(Map<String, Set<ContainerType>> cargoContainers) {
    SortedMap<String, SortedSet<String>> containers = new TreeMap<>(SupportedContainers::compare);
    for (Map.Entry<String, Set<ContainerType>> entry : cargoContainers.entrySet()) {
      if (!isSupported(entry.getKey())) {
        continue;
      }
      SortedSet<String> types = new TreeSet<>();
      for (ContainerType type : entry.getValue()) {
        if (LOCAL_TYPES.contains(type.getType())) {
          types.add(type.getType());
        }
      }
      if (!types.isEmpty()) {
        containers.put(entry.getKey(), Collections.unmodifiableSortedSet(types));
      }
    }
    return containers;
  }

  /** Orders ids by family and then by version, so that "tomcat9x" comes before "tomcat10x". */
  static int compare(String id1, String id2) {
    String family1 = family(id1);
    String family2 = family(id2);
    int result = family1.compareTo(family2);
    if (result == 0) {
      result = Integer.compare(version(id1, family1), version(id2, family2));
    }
    return result != 0 ? result : id1.compareTo(id2);
  }

  private static String family(String id) {
    int end = 0;
    while (end < id.length() && !Character.isDigit(id.charAt(end))) {
      end++;
    }
    return id.substring(0, end);
  }

  private static int version(String id, String family) {
    int end = family.length();
    while (end < id.length() && Character.isDigit(id.charAt(end))) {
      end++;
    }
    return end > family.length() ? Integer.parseInt(id.substring(family.length(), end)) : 0;
  }

}
