package com.logwise.orchestrator.tests.unit.service;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.logwise.orchestrator.client.ObjectStoreClient;
import com.logwise.orchestrator.service.MetricsService;
import com.logwise.orchestrator.setup.BaseTest;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Unit tests for MetricsService. */
public class MetricsServiceTest extends BaseTest {

  private MetricsService metricsService;
  private ObjectStoreClient mockObjectStoreClient;

  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    // MetricsService uses @RequiredArgsConstructor
    metricsService = new MetricsService();
    mockObjectStoreClient = mock(ObjectStoreClient.class);
  }

  //  @Test
  //  public void testComputeLogSyncDelay_WithValidObjects_ReturnsDelay() {
  //    Tenant tenant = Tenant.ABC;
  //
  //    try (MockedStatic<ApplicationConfigUtil> mockedConfigUtil =
  //            Mockito.mockStatic(ApplicationConfigUtil.class);
  //        MockedStatic<ObjectStoreFactory> mockedFactory =
  //            Mockito.mockStatic(ObjectStoreFactory.class)) {
  //
  //      ApplicationConfig.TenantConfig tenantConfig =
  //          ApplicationTestConfig.createMockTenantConfig("ABC");
  //      mockedConfigUtil
  //          .when(() -> ApplicationConfigUtil.getTenantConfig(tenant))
  //          .thenReturn(tenantConfig);
  //
  //      mockedFactory
  //          .when(() -> ObjectStoreFactory.getClient(tenant))
  //          .thenReturn(mockObjectStoreClient);
  //
  //      // Mock listObjects to return objects with hour/minute pattern
  //      List<String> objectKeys =
  //          Arrays.asList(
  //
  // "logs/environment_name=prod/component_type=web/service_name=api/year=2024/month=01/day=15/hour=10/minute=30/file.log");
  //      when(mockObjectStoreClient.listObjects(anyString()))
  //          .thenReturn(Single.just(objectKeys));
  //
  //      Single<LogSyncDelayResponse> result = metricsService.computeLogSyncDelay(tenant);
  //      LogSyncDelayResponse response = result.blockingGet();
  //
  //      Assert.assertNotNull(response);
  //      Assert.assertNotNull(response.getAppLogsDelayMinutes());
  //      Assert.assertEquals(response.getTenant(), tenant.getValue());
  //    }
  //  }
  //
  //  @Test
  //  public void testComputeLogSyncDelay_WithNoObjects_ReturnsMaxDelay() {
  //    Tenant tenant = Tenant.ABC;
  //
  //    try (MockedStatic<ApplicationConfigUtil> mockedConfigUtil =
  //            Mockito.mockStatic(ApplicationConfigUtil.class);
  //        MockedStatic<ObjectStoreFactory> mockedFactory =
  //            Mockito.mockStatic(ObjectStoreFactory.class)) {
  //
  //      ApplicationConfig.TenantConfig tenantConfig =
  //          ApplicationTestConfig.createMockTenantConfig("ABC");
  //      mockedConfigUtil
  //          .when(() -> ApplicationConfigUtil.getTenantConfig(tenant))
  //          .thenReturn(tenantConfig);
  //
  //      mockedFactory
  //          .when(() -> ObjectStoreFactory.getClient(tenant))
  //          .thenReturn(mockObjectStoreClient);
  //
  //      // Mock listObjects to return empty lists
  //      when(mockObjectStoreClient.listObjects(anyString()))
  //          .thenReturn(Single.just(Collections.emptyList()));
  //
  //      Single<LogSyncDelayResponse> result = metricsService.computeLogSyncDelay(tenant);
  //      LogSyncDelayResponse response = result.blockingGet();
  //
  //      Assert.assertNotNull(response);
  //      // Should return max delay when no objects found
  //      Assert.assertNotNull(response.getAppLogsDelayMinutes());
  //    }
  //  }
  //
  //  @Test
  //  public void testComputeLogSyncDelay_WithObjectsInDifferentHours_ReturnsCorrectDelay() {
  //    Tenant tenant = Tenant.ABC;
  //
  //    try (MockedStatic<ApplicationConfigUtil> mockedConfigUtil =
  //            Mockito.mockStatic(ApplicationConfigUtil.class);
  //        MockedStatic<ObjectStoreFactory> mockedFactory =
  //            Mockito.mockStatic(ObjectStoreFactory.class)) {
  //
  //      ApplicationConfig.TenantConfig tenantConfig =
  //          ApplicationTestConfig.createMockTenantConfig("ABC");
  //      mockedConfigUtil
  //          .when(() -> ApplicationConfigUtil.getTenantConfig(tenant))
  //          .thenReturn(tenantConfig);
  //
  //      mockedFactory
  //          .when(() -> ObjectStoreFactory.getClient(tenant))
  //          .thenReturn(mockObjectStoreClient);
  //
  //      // Mock first prefix to return empty, second to return objects
  //      when(mockObjectStoreClient.listObjects(anyString()))
  //          .thenReturn(Single.just(Collections.emptyList()))
  //          .thenReturn(
  //              Single.just(
  //                  Arrays.asList(
  //
  // "logs/environment_name=prod/component_type=web/service_name=api/year=2024/month=01/day=15/hour=09/minute=45/file.log")));
  //
  //      Single<LogSyncDelayResponse> result = metricsService.computeLogSyncDelay(tenant);
  //      LogSyncDelayResponse response = result.blockingGet();
  //
  //      Assert.assertNotNull(response);
  //      Assert.assertNotNull(response.getAppLogsDelayMinutes());
  //    }
  //  }

  @Test
  public void testGetPrefixList_WithValidInputs_ReturnsPrefixList() throws Exception {
    Method method =
        MetricsService.class.getDeclaredMethod(
            "getPrefixList",
            LocalDateTime.class,
            String.class,
            String.class,
            String.class,
            String.class);
    method.setAccessible(true);

    LocalDateTime nowTime = LocalDateTime.of(2024, 1, 15, 12, 0);
    String dir = "logs";
    String environmentName = "prod";
    String componentType = "web";
    String serviceName = "api";

    @SuppressWarnings("unchecked")
    List<String> result =
        (List<String>)
            method.invoke(null, nowTime, dir, environmentName, componentType, serviceName);

    Assert.assertNotNull(result);
    Assert.assertFalse(result.isEmpty());
    // Should generate prefixes for multiple hours
    Assert.assertTrue(result.size() > 1);
    // All prefixes should contain the expected pattern
    result.forEach(
        prefix -> {
          Assert.assertTrue(prefix.contains(environmentName));
          Assert.assertTrue(prefix.contains(componentType));
          Assert.assertTrue(prefix.contains(serviceName));
        });
  }

  @Test
  public void testGetPrefixList_WithDifferentTimes_GeneratesDifferentPrefixes() throws Exception {
    Method method =
        MetricsService.class.getDeclaredMethod(
            "getPrefixList",
            LocalDateTime.class,
            String.class,
            String.class,
            String.class,
            String.class);
    method.setAccessible(true);

    LocalDateTime time1 = LocalDateTime.of(2024, 1, 15, 12, 0);
    LocalDateTime time2 = LocalDateTime.of(2024, 2, 20, 15, 30);

    @SuppressWarnings("unchecked")
    List<String> result1 = (List<String>) method.invoke(null, time1, "logs", "prod", "web", "api");
    @SuppressWarnings("unchecked")
    List<String> result2 = (List<String>) method.invoke(null, time2, "logs", "prod", "web", "api");

    Assert.assertNotNull(result1);
    Assert.assertNotNull(result2);
    // Should have different prefixes for different times
    Assert.assertNotEquals(result1, result2);
  }
}
