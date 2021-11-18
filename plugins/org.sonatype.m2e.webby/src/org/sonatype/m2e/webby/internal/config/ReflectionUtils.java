/*******************************************************************************
 * Copyright (c) 2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.sonatype.m2e.webby.internal.config;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Locale;


/**
 */
class ReflectionUtils {

  public static <T> T getProperty(Object object, String property, Class<T> type, T defaultValue) {
    String getterName = property.substring(0, 1).toUpperCase(Locale.ENGLISH) + property.substring(1);
    if(Boolean.class.equals(type)) {
      getterName = "is" + getterName;
    } else {
      getterName = "get" + getterName;
    }
    try {
      Method method = object.getClass().getMethod(getterName);
      return type.cast(method.invoke(object));
    } catch(NoSuchMethodException e) {
      try {
        Field field = object.getClass().getDeclaredField(property);
        field.setAccessible(true);
        return type.cast(field.get(object));
      } catch(NoSuchFieldException e1) {
        return defaultValue;
      } catch(IllegalAccessException e1) {
        throw new IllegalStateException(e1);
      }
    } catch(IllegalAccessException e) {
      throw new IllegalStateException(e);
    } catch(InvocationTargetException e) {
      return defaultValue;
    }
  }

}
