package org.sonatype.m2e.webby.internal.config;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Locale;

/**
 * Reads properties of objects created by the maven-war-plugin, whose classes are not visible to this bundle.
 */
final class ReflectionUtils {

  private ReflectionUtils() {
  }

  /**
   * @return the value of the property, read with its getter or else directly from the field, or the default value if
   *         the property does not exist or is {@code null}
   */
  public static <T> T getProperty(Object object, String property, Class<T> type, T defaultValue) {
    String getterName = property.substring(0, 1).toUpperCase(Locale.ENGLISH) + property.substring(1);
    if (Boolean.class.equals(type)) {
      getterName = "is" + getterName;
    } else {
      getterName = "get" + getterName;
    }
    Object value;
    try {
      Method method = object.getClass().getMethod(getterName);
      value = method.invoke(object);
    } catch (NoSuchMethodException e) {
      Field field = findField(object.getClass(), property);
      if (field == null) {
        return defaultValue;
      }
      try {
        field.setAccessible(true);
        value = field.get(object);
      } catch (IllegalAccessException | RuntimeException e1) {
        throw new IllegalStateException("Cannot read " + property + " of " + object.getClass().getName(), e1);
      }
    } catch (IllegalAccessException e) {
      throw new IllegalStateException(e);
    } catch (InvocationTargetException e) {
      return defaultValue;
    }
    return value != null ? type.cast(value) : defaultValue;
  }

  private static Field findField(Class<?> type, String name) {
    for (Class<?> c = type; c != null; c = c.getSuperclass()) {
      try {
        return c.getDeclaredField(name);
      } catch (NoSuchFieldException e) {
        // continue with the superclass
      }
    }
    return null;
  }

}
