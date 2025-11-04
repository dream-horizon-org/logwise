package com.logwise.spark.guice.injectors;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

public class  ApplicationInjector {
  private static ApplicationInjector applicationInjector;
  private Injector injector;

  private ApplicationInjector() {}

  public static void initInjection(Module... modules) {
    if (applicationInjector == null) {
      applicationInjector = new ApplicationInjector();
      applicationInjector.injector = Guice.createInjector(modules);
    } else {
      throw new IllegalStateException("ApplicationInjector already initialized");
    }
  }

  public static <T> T getInstance(Class<T> clazz) {
    if (applicationInjector == null) {
      throw new IllegalStateException("ApplicationInjector not initialized");
    }
    return applicationInjector.injector.getInstance(clazz);
  }
}
