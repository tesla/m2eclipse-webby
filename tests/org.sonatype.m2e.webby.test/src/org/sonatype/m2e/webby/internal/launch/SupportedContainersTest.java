package org.sonatype.m2e.webby.internal.launch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;

import org.codehaus.cargo.container.ContainerType;
import org.junit.jupiter.api.Test;

class SupportedContainersTest {

  @Test
  void embeddedCargoKnowsAllSupportedContainers() {
    SortedMap<String, SortedSet<String>> containers = SupportedContainers.getAvailableContainers();
    assertEquals(SupportedContainers.IDS.size(), containers.size(),
        "Cargo does not provide " + SupportedContainers.IDS.stream().filter(id -> !containers.containsKey(id)).toList());
    for (SortedSet<String> types : containers.values()) {
      assertEquals(Set.of(ContainerType.INSTALLED.getType()), types);
    }
  }

  @Test
  void supportsLatestContainers() {
    for (String id : List.of("tomcat11x", "tomcat10x", "jetty12x", "tomee10x", "glassfish8x")) {
      assertTrue(SupportedContainers.isSupported(id), id);
    }
    assertTrue(SupportedContainers.isSupported(WebbyLaunchConstants.DEFAULT_CONTAINER_ID));
    assertFalse(SupportedContainers.isSupported("tomcat5x"));
    assertFalse(SupportedContainers.isSupported("wildfly30x"));
  }

  @Test
  void filtersUnsupportedContainersAndTypes() {
    SortedMap<String, SortedSet<String>> containers = SupportedContainers.filter(Map.of(
        "tomcat11x", Set.of(ContainerType.INSTALLED, ContainerType.REMOTE, ContainerType.EMBEDDED),
        "jetty12x", Set.of(ContainerType.EMBEDDED),
        "wildfly30x", Set.of(ContainerType.INSTALLED)));
    assertEquals(Set.of("tomcat11x"), containers.keySet());
    assertEquals(Set.of("installed"), containers.get("tomcat11x"));
  }

  @Test
  void ordersContainersByFamilyAndVersion() {
    List<String> ids = new ArrayList<>(List.of("tomcat9x", "jetty12x", "tomcat11x", "jetty9x", "tomcat10x",
        "glassfish7x", "tomee10x", "tomee8x", "liberty"));
    ids.sort(SupportedContainers::compare);
    assertEquals(List.of("glassfish7x", "jetty9x", "jetty12x", "liberty", "tomcat9x", "tomcat10x", "tomcat11x",
        "tomee8x", "tomee10x"), ids);
    assertEquals(0, SupportedContainers.compare("tomcat9x", "tomcat9x"));
  }

}
