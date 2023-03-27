package org.sonatype.m2e.webby.internal.config;

import java.lang.reflect.*;
import java.util.Locale;

class ReflectionUtils {

  public static <T> T getProperty(Object object, String property, Class<T> type, T defaultValue) {
    String getterName = property.substring(0, 1).toUpperCase(Locale.ENGLISH) + property.substring(1);
    if (Boolean.class.equals(type)) {
      getterName = "is" + getterName;
    } else {
      getterName = "get" + getterName;
    }
    try {
      Method method = object.getClass().getMethod(getterName);
      return type.cast(method.invoke(object));
    } catch (NoSuchMethodException e) {
      try {
        Field field = object.getClass().getDeclaredField(property);
        field.setAccessible(true);
        return type.cast(field.get(object));
      } catch (NoSuchFieldException e1) {
        return defaultValue;
      } catch (IllegalAccessException e1) {
        throw new IllegalStateException(e1);
      }
    } catch (IllegalAccessException e) {
      throw new IllegalStateException(e);
    } catch (InvocationTargetException e) {
      return defaultValue;
    }
  }

}
