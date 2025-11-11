package com.dream11.logcentralorchestrator.common.app;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.name.Names;
import java.util.List;

/**
 * Singleton class which holds a Guice injector instance. And delegates getInstance method to it
 *
 * <p><i>Note: Avoid using AppContext when possible</i> - using getInstance is <a
 * href="https://google.github.io/guice/api-docs/latest/javadoc/index.html?com/google/inject/Injector.html">not
 * recommended by guice</a> - singleton AppContext instance is volatile. Avoid using it extensively
 * as it can be slow. - This AppContext class is an <a
 * href="https://en.wikipedia.org/wiki/Service_locator_pattern">anti-pattern</a>
 */
public class AppContext {

  private static volatile AppContext instance = null;

  private Injector injector;

  private AppContext(List<Module> modules) {
    injector = Guice.createInjector(modules);
  }

  public static synchronized void initialize(List<Module> modules) {
    if (instance != null) {
      throw new RuntimeException("Application context is already initialized.");
    } else {
      instance = new AppContext(modules);
    }
  }

  public static synchronized void reset() {
    instance = null;
  }

  private static AppContext instance() {
    if (instance != null) {
      return instance;
    }
    throw new RuntimeException("Application context not initialized.");
  }

  /**
   * Used to get the singleton AppContext instance.
   *
   * @deprecated Use {@link #getInstance(Class)} or {@link #getInstance(Class, String)} directly
   *     instead
   */
  @Deprecated
  public static AppContext getInstance() {
    return instance();
  }

  public static <T> T getInstance(Class<T> klazz) {
    return instance().injector.getInstance(klazz);
  }

  public static <T> T getInstance(Class<T> klazz, String name) {
    return instance().injector.getInstance(Key.get(klazz, Names.named(name)));
  }
}
