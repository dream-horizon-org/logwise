package com.logwise.orchestrator.tests.unit;

import com.google.inject.Module;
import com.logwise.orchestrator.MainApplication;
import com.logwise.orchestrator.common.app.Deployable;
import com.logwise.orchestrator.config.ApplicationConfig;
import com.logwise.orchestrator.config.ApplicationConfigProvider;
import com.logwise.orchestrator.module.ClientModule;
import com.logwise.orchestrator.module.MainModule;
import com.logwise.orchestrator.setup.BaseTest;
import com.logwise.orchestrator.verticle.RestVerticle;
import io.vertx.reactivex.core.Vertx;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Unit tests for MainApplication. */
public class MainApplicationTest extends BaseTest {

  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
  }

  @AfterClass
  public static void tearDownClass() {
    BaseTest.cleanup();
  }

  @Test
  public void testMainApplication_Constructor_CreatesInstance() {
    try (MockedStatic<ApplicationConfigProvider> mockedProvider =
        Mockito.mockStatic(ApplicationConfigProvider.class)) {
      ApplicationConfig mockConfig = Mockito.mock(ApplicationConfig.class);
      mockedProvider.when(ApplicationConfigProvider::getApplicationConfig).thenReturn(mockConfig);
      mockedProvider.when(ApplicationConfigProvider::initConfig).thenAnswer(invocation -> null);

      MainApplication app = new MainApplication();

      Assert.assertNotNull(app);
    }
  }

  @Test
  public void testGetGoogleGuiceModules_ReturnsModules() throws Exception {
    try (MockedStatic<ApplicationConfigProvider> mockedProvider =
        Mockito.mockStatic(ApplicationConfigProvider.class)) {
      ApplicationConfig mockConfig = Mockito.mock(ApplicationConfig.class);
      mockedProvider.when(ApplicationConfigProvider::getApplicationConfig).thenReturn(mockConfig);
      mockedProvider.when(ApplicationConfigProvider::initConfig).thenAnswer(invocation -> null);

      MainApplication app = new MainApplication();
      Vertx vertx = BaseTest.getReactiveVertx();

      // Use reflection to access protected method
      java.lang.reflect.Method method =
          MainApplication.class.getDeclaredMethod("getGoogleGuiceModules", Vertx.class);
      method.setAccessible(true);
      Module[] modules = (Module[]) method.invoke(app, vertx);

      Assert.assertNotNull(modules);
      Assert.assertEquals(modules.length, 2);
      Assert.assertTrue(modules[0] instanceof MainModule || modules[0] instanceof ClientModule);
      Assert.assertTrue(modules[1] instanceof MainModule || modules[1] instanceof ClientModule);
    }
  }

  @Test
  public void testGetVerticlesToDeploy_ReturnsDeployables() throws Exception {
    try (MockedStatic<ApplicationConfigProvider> mockedProvider =
        Mockito.mockStatic(ApplicationConfigProvider.class)) {
      ApplicationConfig mockConfig = Mockito.mock(ApplicationConfig.class);
      mockedProvider.when(ApplicationConfigProvider::getApplicationConfig).thenReturn(mockConfig);
      mockedProvider.when(ApplicationConfigProvider::initConfig).thenAnswer(invocation -> null);

      MainApplication app = new MainApplication();
      Vertx vertx = BaseTest.getReactiveVertx();

      // Use reflection to access protected method
      java.lang.reflect.Method method =
          MainApplication.class.getDeclaredMethod("getVerticlesToDeploy", Vertx.class);
      method.setAccessible(true);
      Deployable[] deployables = (Deployable[]) method.invoke(app, vertx);

      Assert.assertNotNull(deployables);
      Assert.assertEquals(deployables.length, 1);
      Assert.assertEquals(deployables[0].getVerticleClass(), RestVerticle.class);
    }
  }

  @Test
  public void testInitDDClient_DoesNothing() throws Exception {
    try (MockedStatic<ApplicationConfigProvider> mockedProvider =
        Mockito.mockStatic(ApplicationConfigProvider.class)) {
      ApplicationConfig mockConfig = Mockito.mock(ApplicationConfig.class);
      mockedProvider.when(ApplicationConfigProvider::getApplicationConfig).thenReturn(mockConfig);
      mockedProvider.when(ApplicationConfigProvider::initConfig).thenAnswer(invocation -> null);

      MainApplication app = new MainApplication();

      // Use reflection to access protected method
      java.lang.reflect.Method method = MainApplication.class.getDeclaredMethod("initDDClient");
      method.setAccessible(true);
      // Should not throw exception
      method.invoke(app);
    }
  }
}
