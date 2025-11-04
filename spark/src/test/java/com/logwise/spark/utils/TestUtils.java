package com.logwise.spark.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class TestUtils {

  public void setEnv(String key, String value) {
    getWritableEnv().put(key, value);
  }

  public void unsetEnv(String key) {
    getWritableEnv().remove(key);
  }

  @SneakyThrows
  @SuppressWarnings("unchecked")
  private Map<String, String> getWritableEnv() {
    Map<String, String> env = System.getenv();
    Class<?> cl = env.getClass();
    Field field = cl.getDeclaredField("m");
    field.setAccessible(true);
    return (Map<String, String>) field.get(env);
  }

  @SneakyThrows
  @SuppressWarnings("unchecked")
  public void setPrivateField(Object classObject, String fieldName, Object fieldValue) {
    Field field = classObject.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(classObject, fieldValue);
  }

  @SneakyThrows
  @SuppressWarnings("unchecked")
  public void setPrivateStaticField(Class<?> clazz, String fieldName, Object fieldValue) {
    Field field = clazz.getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(null, fieldValue);
  }

  @SneakyThrows
  @SuppressWarnings("unchecked")
  public Object getPrivateField(Object classObject, String fieldName) {
    Field field = classObject.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    return field.get(classObject);
  }

  @SneakyThrows
  @SuppressWarnings("unchecked")
  public Object getPrivateStaticField(Class<?> clazz, String fieldName) {
    Field field = clazz.getDeclaredField(fieldName);
    field.setAccessible(true);
    return field.get(null);
  }

  public Object invokePrivateMethod(Object obj, String methodName, Object... params) {
    int paramCount = params.length;
    Method method;
    Object requiredObj = null;
    try {
      Class<?>[] classArray = new Class<?>[paramCount];
      for (int i = 0; i < paramCount; i++) {
        classArray[i] = params[i].getClass();
      }
      method = obj.getClass().getDeclaredMethod(methodName, classArray);
      method.setAccessible(true);
      requiredObj = method.invoke(obj, params);
    } catch (Exception e) {
      log.error("Error in invoking private method {}: ", methodName, e);
    }
    return requiredObj;
  }

  public byte[] toBinary(String input) {
    return input.getBytes(StandardCharsets.UTF_8);
  }
}
