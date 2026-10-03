package org.sonatype.m2e.webby.internal.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Tracks which overlays provide a resource of the WAR directory. Overlays are identified by their ordinal, the lowest
 * ordinal wins when several overlays provide the same resource.
 */
public class ResourceRegistry {

  private static final int[] EMPTY = {};

  private static final ObjectInputFilter SERIALIZATION_FILTER = ObjectInputFilter.Config
      .createFilter("maxdepth=5;java.util.*;java.lang.*;!*");

  /** Maps each resource to its overlay ordinal (an {@link Integer}) or to the sorted ordinals (an {@code int[]}). */
  private final Map<String, Object> resources;

  public ResourceRegistry() {
    this(new HashMap<>());
  }

  private ResourceRegistry(Map<String, Object> resources) {
    this.resources = resources;
  }

  private static String normalizePath(String path) {
    return path.replace('\\', '/');
  }

  /**
   * @return {@code true} if the given overlay provides the resource, i.e. no overlay with a lower ordinal does
   */
  public boolean register(String resourceName, int overlayOrdinal) {
    boolean accept = false;

    resourceName = normalizePath(resourceName);
    Object ordinals = resources.get(resourceName);

    if (ordinals == null) {
      resources.put(resourceName, overlayOrdinal);
      accept = true;
    } else if (ordinals instanceof Number) {
      int existing = ((Number) ordinals).intValue();
      if (overlayOrdinal == existing) {
        accept = true;
      } else if (overlayOrdinal < existing) {
        accept = true;
        resources.put(resourceName, new int[] { overlayOrdinal, existing });
      } else {
        accept = false;
        resources.put(resourceName, new int[] { existing, overlayOrdinal });
      }
    } else {
      int[] existing = (int[]) ordinals;
      int index = Arrays.binarySearch(existing, overlayOrdinal);
      if (index == 0) {
        accept = true;
      } else if (index > 0) {
        accept = false;
      } else {
        index = -index - 1;
        accept = index == 0;
        int[] tmp = new int[existing.length + 1];
        System.arraycopy(existing, 0, tmp, 0, index);
        System.arraycopy(existing, index, tmp, index + 1, existing.length - index);
        tmp[index] = overlayOrdinal;
        resources.put(resourceName, tmp);
      }
    }

    return accept;
  }

  /**
   * @return {@code null} if the overlay did not provide the resource, else the ordinals of the overlays still
   *         providing it
   */
  public int[] unregister(String resourceName, int overlayOrdinal) {
    resourceName = normalizePath(resourceName);
    Object ordinals = resources.get(resourceName);

    if (ordinals == null) {
      return null;
    } else if (ordinals instanceof Number) {
      int existing = ((Number) ordinals).intValue();
      if (existing == overlayOrdinal) {
        resources.remove(resourceName);
        return EMPTY;
      } else {
        return null;
      }
    } else {
      int[] existing = (int[]) ordinals;
      int index = Arrays.binarySearch(existing, overlayOrdinal);
      if (index < 0) {
        return null;
      }
      if (existing.length == 1) {
        resources.remove(resourceName);
        return EMPTY;
      } else if (existing.length == 2) {
        int remaining = existing[existing.length - index - 1];
        resources.put(resourceName, remaining);
        return new int[] { remaining };
      } else {
        int[] tmp = new int[existing.length - 1];
        System.arraycopy(existing, 0, tmp, 0, index);
        System.arraycopy(existing, index + 1, tmp, index, existing.length - index - 1);
        resources.put(resourceName, tmp);
        return tmp;
      }
    }
  }

  public void save(File file) throws IOException {
    file.getAbsoluteFile().getParentFile().mkdirs();
    try (OutputStream os = Files.newOutputStream(file.toPath());
        ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(os))) {
      oos.writeObject(new HashMap<>(resources));
    }
  }

  @SuppressWarnings("unchecked")
  public static ResourceRegistry load(File file) throws IOException {
    try (InputStream is = Files.newInputStream(file.toPath());
        ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(is))) {
      ois.setObjectInputFilter(SERIALIZATION_FILTER);
      Object resources = ois.readObject();
      if (resources instanceof Map) {
        return new ResourceRegistry((Map<String, Object>) resources);
      }
      throw new IOException("Corrupted object stream");
    } catch (ClassNotFoundException | RuntimeException e) {
      throw new IOException("Corrupted object stream", e);
    }
  }

}
