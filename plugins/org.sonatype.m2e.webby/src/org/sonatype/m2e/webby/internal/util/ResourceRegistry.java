package org.sonatype.m2e.webby.internal.util;

import java.io.*;
import java.util.*;

public class ResourceRegistry {

  private static int[] EMPTY = {};

  private Map<String, Object> resources;

  public ResourceRegistry() {
    this(new HashMap<>());
  }

  private ResourceRegistry(Map<String, Object> resources) {
    this.resources = resources;
  }

  private String normalizePath(String path) {
    String result = path;
    result = result.replace('\\', '/');
    return result;
  }

  public boolean register(String resourceName, int overlayOrdinal) {
    boolean accept = false;

    resourceName = normalizePath(resourceName);
    Object ordinals = resources.get(resourceName);

    if (ordinals == null) {
      resources.put(resourceName, Integer.valueOf(overlayOrdinal));
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
        resources.put(resourceName, Integer.valueOf(remaining));
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
    FileOutputStream fos = new FileOutputStream(file);
    try {
      ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(fos));
      oos.writeObject(resources);
      oos.close();
    } finally {
      fos.close();
    }
  }

  @SuppressWarnings("unchecked")
  public static ResourceRegistry load(File file) throws IOException {
    FileInputStream fis = new FileInputStream(file);
    try {
      ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(fis));
      Object resources = ois.readObject();
      ois.close();
      if (resources instanceof Map) {
        return new ResourceRegistry((Map<String, Object>) resources);
      } else {
        throw new IOException("Corrupted object stream");
      }
    } catch (ClassNotFoundException e) {
      throw (IOException) new IOException("Corrupted object stream").initCause(e);
    } finally {
      fis.close();
    }
  }

}
