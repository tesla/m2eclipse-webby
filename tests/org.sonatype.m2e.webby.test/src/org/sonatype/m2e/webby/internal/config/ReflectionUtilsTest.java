package org.sonatype.m2e.webby.internal.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ReflectionUtilsTest {

  public static class Base {

    @SuppressWarnings("unused")
    private String inherited = "from base";

  }

  public static class Bean extends Base {

    @SuppressWarnings("unused")
    private String hidden = "field value";

    private String nullValue;

    public String getName() {
      return "getter value";
    }

    public Boolean isEnabled() {
      return Boolean.TRUE;
    }

    public String getNullValue() {
      return nullValue;
    }

    public String getFailing() {
      throw new IllegalStateException("boom");
    }

    public Integer getNumber() {
      return 42;
    }

  }

  @Test
  void readsWithGetter() {
    assertEquals("getter value", ReflectionUtils.getProperty(new Bean(), "name", String.class, "default"));
    assertEquals(Boolean.TRUE, ReflectionUtils.getProperty(new Bean(), "enabled", Boolean.class, Boolean.FALSE));
  }

  @Test
  void readsFieldWithoutGetter() {
    assertEquals("field value", ReflectionUtils.getProperty(new Bean(), "hidden", String.class, "default"));
    assertEquals("from base", ReflectionUtils.getProperty(new Bean(), "inherited", String.class, "default"));
  }

  @Test
  void fallsBackToDefault() {
    assertEquals("default", ReflectionUtils.getProperty(new Bean(), "missing", String.class, "default"));
    assertEquals("default", ReflectionUtils.getProperty(new Bean(), "nullValue", String.class, "default"));
    assertEquals("default", ReflectionUtils.getProperty(new Bean(), "failing", String.class, "default"));
  }

  @Test
  void rejectsWrongType() {
    assertThrows(ClassCastException.class, () -> ReflectionUtils.getProperty(new Bean(), "number", String.class, ""));
  }

}
