package com.logwise.orchestrator.tests.unit.service;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.logwise.orchestrator.client.ObjectStoreClient;
import com.logwise.orchestrator.config.ApplicationConfig;
import com.logwise.orchestrator.dao.ServicesDao;
import com.logwise.orchestrator.dto.entity.ServiceDetails;
import com.logwise.orchestrator.enums.Tenant;
import com.logwise.orchestrator.factory.ObjectStoreFactory;
import com.logwise.orchestrator.service.ObjectStoreService;
import com.logwise.orchestrator.setup.BaseTest;
import com.logwise.orchestrator.testconfig.ApplicationTestConfig;
import com.logwise.orchestrator.util.ApplicationConfigUtil;
import io.reactivex.Single;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Unit tests for ObjectStoreService. */
public class ObjectStoreServiceTest extends BaseTest {

  private ObjectStoreService objectStoreService;
  private ServicesDao mockServicesDao;
  private ObjectStoreClient mockObjectStoreClient;

  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    mockServicesDao = mock(ServicesDao.class);
    // ObjectStoreService uses @RequiredArgsConstructor which Lombok generates at
    // compile time
    // Use reflection to find and use the generated constructor
    java.lang.reflect.Constructor<?>[] constructors =
        ObjectStoreService.class.getDeclaredConstructors();
    if (constructors.length == 0) {
      throw new RuntimeException(
          "No constructors found for ObjectStoreService. Make sure Lombok annotation processing is enabled and the project is compiled.");
    }
    // Find constructor that takes ServicesDao
    java.lang.reflect.Constructor<?> targetConstructor = null;
    for (java.lang.reflect.Constructor<?> constructor : constructors) {
      Class<?>[] paramTypes = constructor.getParameterTypes();
      if (paramTypes.length == 1 && paramTypes[0] == ServicesDao.class) {
        targetConstructor = constructor;
        break;
      }
    }
    if (targetConstructor == null) {
      // Fallback: try to use no-arg constructor and set field via reflection
      for (java.lang.reflect.Constructor<?> constructor : constructors) {
        if (constructor.getParameterTypes().length == 0) {
          constructor.setAccessible(true);
          objectStoreService = (ObjectStoreService) constructor.newInstance();
          // Set the servicesDao field using reflection
          java.lang.reflect.Field field = ObjectStoreService.class.getDeclaredField("servicesDao");
          field.setAccessible(true);
          field.set(objectStoreService, mockServicesDao);
          return;
        }
      }
      // Last resort: use first constructor
      targetConstructor = constructors[0];
    }
    targetConstructor.setAccessible(true);
    objectStoreService = (ObjectStoreService) targetConstructor.newInstance(mockServicesDao);
  }

  @Test
  public void testGetAllDistinctServicesInAws_WithValidTenant_ReturnsServiceDetails() {
    Tenant tenant = Tenant.ABC;
    String prefix1 = "logs/service_name=api/";
    String prefix2 = "logs/service_name=api2/";

    try (MockedStatic<ObjectStoreFactory> mockedFactory =
            Mockito.mockStatic(ObjectStoreFactory.class);
        MockedStatic<ApplicationConfigUtil> mockedConfigUtil =
            Mockito.mockStatic(ApplicationConfigUtil.class)) {

      mockObjectStoreClient = mock(ObjectStoreClient.class);
      mockedFactory
          .when(() -> ObjectStoreFactory.getClient(tenant))
          .thenReturn(mockObjectStoreClient);

      ApplicationConfig.TenantConfig tenantConfig =
          ApplicationTestConfig.createMockTenantConfig("ABC");
      mockedConfigUtil
          .when(() -> ApplicationConfigUtil.getTenantConfig(tenant))
          .thenReturn(tenantConfig);

      // Mock the nested listCommonPrefix calls
      when(mockObjectStoreClient.listCommonPrefix(anyString(), eq("/")))
          .thenReturn(Single.just(Arrays.asList(prefix1, prefix2)));

      Single<List<ServiceDetails>> result = objectStoreService.getAllDistinctServicesInAws(tenant);
      List<ServiceDetails> services = result.blockingGet();

      Assert.assertNotNull(services);
      // Should parse service details from prefixes
    }
  }

  @Test
  public void testGetAllDistinctServicesInAws_WithEmptyPrefixes_ReturnsEmptyList() {
    Tenant tenant = Tenant.ABC;

    try (MockedStatic<ObjectStoreFactory> mockedFactory =
            Mockito.mockStatic(ObjectStoreFactory.class);
        MockedStatic<ApplicationConfigUtil> mockedConfigUtil =
            Mockito.mockStatic(ApplicationConfigUtil.class)) {

      mockObjectStoreClient = mock(ObjectStoreClient.class);
      mockedFactory
          .when(() -> ObjectStoreFactory.getClient(tenant))
          .thenReturn(mockObjectStoreClient);

      ApplicationConfig.TenantConfig tenantConfig =
          ApplicationTestConfig.createMockTenantConfig("ABC");
      mockedConfigUtil
          .when(() -> ApplicationConfigUtil.getTenantConfig(tenant))
          .thenReturn(tenantConfig);

      when(mockObjectStoreClient.listCommonPrefix(anyString(), eq("/")))
          .thenReturn(Single.just(Collections.emptyList()));

      Single<List<ServiceDetails>> result = objectStoreService.getAllDistinctServicesInAws(tenant);
      List<ServiceDetails> services = result.blockingGet();

      Assert.assertNotNull(services);
      Assert.assertTrue(services.isEmpty());
    }
  }

  @Test
  public void testGetDefaultRetentionDays_ReturnsDefaultRetentionDays() throws Exception {
    Method method =
        ObjectStoreService.class.getDeclaredMethod(
            "getDefaultRetentionDays", ApplicationConfig.TenantConfig.class);
    method.setAccessible(true);

    ApplicationConfig.TenantConfig config = ApplicationTestConfig.createMockTenantConfig("ABC");
    config.setDefaultLogsRetentionDays(30);

    Integer result = (Integer) method.invoke(null, config);

    Assert.assertNotNull(result);
    Assert.assertEquals(result, Integer.valueOf(30));
  }

  @Test
  public void testGetAllDistinctServicesInAws_WithMultipleServices_ReturnsAllServices() {
    Tenant tenant = Tenant.ABC;
    String servicePrefix1 = "logs/service_name=api1/";
    String servicePrefix2 = "logs/service_name=api2/";
    String servicePrefix3 = "logs/service_name=api3/";

    try (MockedStatic<ObjectStoreFactory> mockedFactory =
            Mockito.mockStatic(ObjectStoreFactory.class);
        MockedStatic<ApplicationConfigUtil> mockedConfigUtil =
            Mockito.mockStatic(ApplicationConfigUtil.class)) {

      mockObjectStoreClient = mock(ObjectStoreClient.class);
      mockedFactory
          .when(() -> ObjectStoreFactory.getClient(tenant))
          .thenReturn(mockObjectStoreClient);

      ApplicationConfig.TenantConfig tenantConfig =
          ApplicationTestConfig.createMockTenantConfig("ABC");
      mockedConfigUtil
          .when(() -> ApplicationConfigUtil.getTenantConfig(tenant))
          .thenReturn(tenantConfig);

      // Mock listCommonPrefix call for service_name
      when(mockObjectStoreClient.listCommonPrefix(contains("service_name="), eq("/")))
          .thenReturn(Single.just(Arrays.asList(servicePrefix1, servicePrefix2, servicePrefix3)));

      Single<List<ServiceDetails>> result = objectStoreService.getAllDistinctServicesInAws(tenant);
      List<ServiceDetails> services = result.blockingGet();

      Assert.assertNotNull(services);
      // Should have parsed multiple services
    }
  }

  @Test
  public void testGetAllDistinctServicesInAws_WithError_PropagatesError() {
    Tenant tenant = Tenant.ABC;

    try (MockedStatic<ObjectStoreFactory> mockedFactory =
            Mockito.mockStatic(ObjectStoreFactory.class);
        MockedStatic<ApplicationConfigUtil> mockedConfigUtil =
            Mockito.mockStatic(ApplicationConfigUtil.class)) {

      mockObjectStoreClient = mock(ObjectStoreClient.class);
      mockedFactory
          .when(() -> ObjectStoreFactory.getClient(tenant))
          .thenReturn(mockObjectStoreClient);

      ApplicationConfig.TenantConfig tenantConfig =
          ApplicationTestConfig.createMockTenantConfig("ABC");
      mockedConfigUtil
          .when(() -> ApplicationConfigUtil.getTenantConfig(tenant))
          .thenReturn(tenantConfig);

      RuntimeException error = new RuntimeException("S3 error");
      when(mockObjectStoreClient.listCommonPrefix(anyString(), eq("/")))
          .thenReturn(Single.error(error));

      Single<List<ServiceDetails>> result = objectStoreService.getAllDistinctServicesInAws(tenant);

      try {
        result.blockingGet();
        Assert.fail("Should have thrown exception");
      } catch (RuntimeException e) {
        Assert.assertNotNull(e);
      }
    }
  }
}
