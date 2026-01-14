package com.logwise.orchestrator.tests.unit.service;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.logwise.orchestrator.client.ObjectStoreClient;
import com.logwise.orchestrator.config.ApplicationConfig;
import com.logwise.orchestrator.dao.ServicesDao;
import com.logwise.orchestrator.dto.entity.ServiceDetails;
import com.logwise.orchestrator.enums.Tenant;
import com.logwise.orchestrator.factory.ObjectStoreFactory;
import com.logwise.orchestrator.service.ObjectStoreService;
import com.logwise.orchestrator.util.ApplicationConfigUtil;
import com.logwise.orchestrator.util.ApplicationUtils;
import io.reactivex.Single;
import java.util.Arrays;
import java.util.List;
import org.mockito.MockedStatic;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ObjectStoreServiceTest {

  private ObjectStoreService objectStoreService;
  private ServicesDao mockServicesDao;
  private ObjectStoreClient mockObjectStoreClient;
  private ApplicationConfig.TenantConfig mockTenantConfig;
  private ApplicationConfig.SparkConfig mockSparkConfig;
  private ApplicationConfig.ObjectStoreConfig mockObjectStoreConfig;

  @BeforeMethod
  public void setUp() throws Exception {
    mockServicesDao = mock(ServicesDao.class);
    objectStoreService = new ObjectStoreService();
    java.lang.reflect.Field field = ObjectStoreService.class.getDeclaredField("servicesDao");
    field.setAccessible(true);
    field.set(objectStoreService, mockServicesDao);

    mockObjectStoreClient = mock(ObjectStoreClient.class);
    mockTenantConfig = mock(ApplicationConfig.TenantConfig.class);
    mockSparkConfig = mock(ApplicationConfig.SparkConfig.class);
    mockObjectStoreConfig = mock(ApplicationConfig.ObjectStoreConfig.class);
  }

  @Test
  public void testGetAllDistinctServicesInAws_WithValidTenant_ReturnsServices() {
    Tenant tenant = Tenant.ABC;
    String logsDir = "/logs";
    List<String> objectKeys =
        Arrays.asList("logs/service_name=service1/", "logs/service_name=service2/");

    when(mockTenantConfig.getSpark()).thenReturn(mockSparkConfig);
    when(mockSparkConfig.getLogsDir()).thenReturn(logsDir);
    when(mockTenantConfig.getDefaultLogsRetentionDays()).thenReturn(30);
    when(mockObjectStoreClient.listCommonPrefix(anyString(), anyString()))
        .thenReturn(Single.just(objectKeys));

    try (MockedStatic<ObjectStoreFactory> mockedFactory = mockStatic(ObjectStoreFactory.class);
        MockedStatic<ApplicationConfigUtil> mockedConfig = mockStatic(ApplicationConfigUtil.class);
        MockedStatic<ApplicationUtils> mockedUtils = mockStatic(ApplicationUtils.class)) {

      mockedFactory
          .when(() -> ObjectStoreFactory.getClient(tenant))
          .thenReturn(mockObjectStoreClient);
      mockedConfig
          .when(() -> ApplicationConfigUtil.getTenantConfig(tenant))
          .thenReturn(mockTenantConfig);

      ServiceDetails service1 = ServiceDetails.builder().serviceName("service1").build();
      ServiceDetails service2 = ServiceDetails.builder().serviceName("service2").build();

      mockedUtils
          .when(() -> ApplicationUtils.getServiceFromObjectKey("logs/service_name=service1/"))
          .thenReturn(service1);
      mockedUtils
          .when(() -> ApplicationUtils.getServiceFromObjectKey("logs/service_name=service2/"))
          .thenReturn(service2);

      Single<List<ServiceDetails>> result = objectStoreService.getAllDistinctServicesInAws(tenant);

      List<ServiceDetails> services = result.blockingGet();
      Assert.assertNotNull(services);
      Assert.assertEquals(services.size(), 2);
      Assert.assertEquals(services.get(0).getServiceName(), "service1");
      Assert.assertEquals(services.get(1).getServiceName(), "service2");
      Assert.assertEquals(services.get(0).getRetentionDays(), Integer.valueOf(30));
      Assert.assertEquals(services.get(0).getTenant(), "ABC");
    }
  }

  @Test
  public void testGetAllDistinctServicesInAws_WithEmptyList_ReturnsEmptyList() {
    Tenant tenant = Tenant.ABC;
    String logsDir = "/logs";

    when(mockTenantConfig.getSpark()).thenReturn(mockSparkConfig);
    when(mockSparkConfig.getLogsDir()).thenReturn(logsDir);
    when(mockObjectStoreClient.listCommonPrefix(anyString(), anyString()))
        .thenReturn(Single.just(Arrays.asList()));

    try (MockedStatic<ObjectStoreFactory> mockedFactory = mockStatic(ObjectStoreFactory.class);
        MockedStatic<ApplicationConfigUtil> mockedConfig =
            mockStatic(ApplicationConfigUtil.class)) {

      mockedFactory
          .when(() -> ObjectStoreFactory.getClient(tenant))
          .thenReturn(mockObjectStoreClient);
      mockedConfig
          .when(() -> ApplicationConfigUtil.getTenantConfig(tenant))
          .thenReturn(mockTenantConfig);

      Single<List<ServiceDetails>> result = objectStoreService.getAllDistinctServicesInAws(tenant);

      List<ServiceDetails> services = result.blockingGet();
      Assert.assertNotNull(services);
      Assert.assertTrue(services.isEmpty());
    }
  }
}
