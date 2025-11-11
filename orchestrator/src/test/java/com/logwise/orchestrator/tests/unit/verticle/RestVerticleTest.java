package com.logwise.orchestrator.tests.unit.verticle;

import static org.mockito.Mockito.when;

import com.logwise.orchestrator.client.ObjectStoreClient;
import com.logwise.orchestrator.common.app.AppContext;
import com.logwise.orchestrator.config.ApplicationConfig;
import com.logwise.orchestrator.constant.ApplicationConstants;
import com.logwise.orchestrator.setup.BaseTest;
import com.logwise.orchestrator.verticle.RestVerticle;
import io.reactivex.Completable;
import io.vertx.reactivex.core.Vertx;
import java.util.ArrayList;
import java.util.List;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Unit tests for RestVerticle. */
public class RestVerticleTest extends BaseTest {

  private Vertx vertx;

  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    vertx = BaseTest.getReactiveVertx();
  }

  @Test
  public void testRestVerticle_Constructor_CreatesInstance() {

    RestVerticle verticle = new RestVerticle();

    Assert.assertNotNull(verticle);
  }

  @Test
  public void testStartObjectStores_WithTenants_ConnectsToObjectStores() {

    RestVerticle verticle = new RestVerticle();
    ApplicationConfig mockApplicationConfig = Mockito.mock(ApplicationConfig.class);
    ApplicationConfig.TenantConfig mockTenantConfig =
        Mockito.mock(ApplicationConfig.TenantConfig.class);
    ApplicationConfig.ObjectStoreConfig mockObjectStoreConfig =
        Mockito.mock(ApplicationConfig.ObjectStoreConfig.class);
    ObjectStoreClient mockObjectStoreClient = Mockito.mock(ObjectStoreClient.class);

    List<ApplicationConfig.TenantConfig> tenants = new ArrayList<>();
    tenants.add(mockTenantConfig);
    when(mockApplicationConfig.getTenants()).thenReturn(tenants);
    when(mockTenantConfig.getName()).thenReturn("test-tenant");
    when(mockTenantConfig.getObjectStore()).thenReturn(mockObjectStoreConfig);
    when(mockObjectStoreClient.rxConnect(mockObjectStoreConfig)).thenReturn(Completable.complete());

    try {
      java.lang.reflect.Field configField =
          RestVerticle.class.getDeclaredField("applicationConfig");
      configField.setAccessible(true);
      configField.set(verticle, mockApplicationConfig);
    } catch (Exception e) {
      Assert.fail("Failed to set applicationConfig", e);
    }

    try (MockedStatic<AppContext> mockedAppContext = Mockito.mockStatic(AppContext.class)) {
      mockedAppContext
          .when(
              () ->
                  AppContext.getInstance(
                      ObjectStoreClient.class,
                      ApplicationConstants.OBJECT_STORE_INJECTOR_NAME.apply("test-tenant")))
          .thenReturn(mockObjectStoreClient);

      java.lang.reflect.Method method = RestVerticle.class.getDeclaredMethod("startObjectStores");
      method.setAccessible(true);
      Completable result = (Completable) method.invoke(verticle);
      result.blockingAwait();

      Mockito.verify(mockObjectStoreClient).rxConnect(mockObjectStoreConfig);
    } catch (Exception e) {
      Assert.fail("Should not throw exception", e);
    }
  }
}
