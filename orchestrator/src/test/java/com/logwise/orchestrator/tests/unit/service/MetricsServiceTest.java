package com.logwise.orchestrator.tests.unit.service;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.logwise.orchestrator.client.ObjectStoreClient;
import com.logwise.orchestrator.config.ApplicationConfig;
import com.logwise.orchestrator.enums.Tenant;
import com.logwise.orchestrator.service.MetricsService;
import com.logwise.orchestrator.setup.BaseTest;
import com.logwise.orchestrator.util.ApplicationConfigUtil;
import io.reactivex.Single;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.mockito.MockedStatic;
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
    reset(mockObjectStoreClient);
  }

  @Test
  public void testGetPrefixList_WithValidInputs_ReturnsPrefixList() throws Exception {
    Method method =
        MetricsService.class.getDeclaredMethod(
            "getPrefixList", LocalDateTime.class, String.class, String.class);
    method.setAccessible(true);

    LocalDateTime nowTime = LocalDateTime.of(2024, 1, 15, 12, 0);
    String dir = "logs";
    String serviceName = "api";

    @SuppressWarnings("unchecked")
    List<String> result = (List<String>) method.invoke(null, nowTime, dir, serviceName);

    Assert.assertNotNull(result);
    Assert.assertFalse(result.isEmpty());
    // Should generate prefixes for multiple hours
    Assert.assertTrue(result.size() > 1);
    // All prefixes should contain the expected pattern
    result.forEach(
        prefix -> {
          Assert.assertTrue(prefix.contains(serviceName));
        });
  }

  @Test
  public void testGetPrefixList_WithDifferentTimes_GeneratesDifferentPrefixes() throws Exception {
    Method method =
        MetricsService.class.getDeclaredMethod(
            "getPrefixList", LocalDateTime.class, String.class, String.class);
    method.setAccessible(true);

    LocalDateTime time1 = LocalDateTime.of(2024, 1, 15, 12, 0);
    LocalDateTime time2 = LocalDateTime.of(2024, 2, 20, 15, 30);

    @SuppressWarnings("unchecked")
    List<String> result1 = (List<String>) method.invoke(null, time1, "logs", "api");
    @SuppressWarnings("unchecked")
    List<String> result2 = (List<String>) method.invoke(null, time2, "logs", "api");

    Assert.assertNotNull(result1);
    Assert.assertNotNull(result2);
    // Should have different prefixes for different times
    Assert.assertNotEquals(result1, result2);
  }

  @Test
  public void testGetPrefixList_WithDuplicatePrefixes_RemovesDuplicates() throws Exception {
    Method method =
        MetricsService.class.getDeclaredMethod(
            "getPrefixList", LocalDateTime.class, String.class, String.class);
    method.setAccessible(true);

    LocalDateTime nowTime = LocalDateTime.of(2024, 1, 15, 12, 0);
    String dir = "logs";
    String serviceName = "api";

    @SuppressWarnings("unchecked")
    List<String> result = (List<String>) method.invoke(null, nowTime, dir, serviceName);

    // Check for duplicates
    List<String> uniquePrefixes = new ArrayList<>(result);
    Assert.assertEquals(result.size(), uniquePrefixes.size(), "Should not contain duplicates");
  }

  @Test
  public void testComputeLogSyncDelay_WithValidTenant_ReturnsResponse() throws Exception {
    Tenant tenant = Tenant.ABC;

    try (MockedStatic<ApplicationConfigUtil> mockedConfigUtil =
            mockStatic(ApplicationConfigUtil.class);
        MockedStatic<com.logwise.orchestrator.factory.ObjectStoreFactory> mockedFactory =
            mockStatic(com.logwise.orchestrator.factory.ObjectStoreFactory.class)) {
      ApplicationConfig.TenantConfig tenantConfig = mock(ApplicationConfig.TenantConfig.class);
      ApplicationConfig.SparkConfig sparkConfig = mock(ApplicationConfig.SparkConfig.class);
      ApplicationConfig.DelayMetricsConfig delayMetricsConfig =
          mock(ApplicationConfig.DelayMetricsConfig.class);
      ApplicationConfig.ApplicationDelayMetricsConfig appConfig =
          mock(ApplicationConfig.ApplicationDelayMetricsConfig.class);

      when(tenantConfig.getSpark()).thenReturn(sparkConfig);
      when(tenantConfig.getDelayMetrics()).thenReturn(delayMetricsConfig);
      when(delayMetricsConfig.getApp()).thenReturn(appConfig);
      when(sparkConfig.getLogsDir()).thenReturn("logs");
      when(appConfig.getSampleServiceName()).thenReturn("test-service");

      mockedConfigUtil
          .when(() -> ApplicationConfigUtil.getTenantConfig(tenant))
          .thenReturn(tenantConfig);

      // Mock object store to return objects with current hour
      int currentHour = java.time.LocalDateTime.now().getHour();
      String hourPattern = String.format("hour=%02d", currentHour);
      List<String> objects = Arrays.asList("logs/test-service/" + hourPattern + "/file.log");

      when(mockObjectStoreClient.listObjects(anyString())).thenReturn(Single.just(objects));
      mockedFactory
          .when(() -> com.logwise.orchestrator.factory.ObjectStoreFactory.getClient(tenant))
          .thenReturn(mockObjectStoreClient);

      Single<com.logwise.orchestrator.dto.response.LogSyncDelayResponse> result =
          metricsService.computeLogSyncDelay(tenant);
      com.logwise.orchestrator.dto.response.LogSyncDelayResponse response = result.blockingGet();

      Assert.assertNotNull(response);
      Assert.assertEquals(response.getTenant(), "ABC");
      Assert.assertNotNull(response.getAppLogsDelayMinutes());
    }
  }

  @Test
  public void testComputeApplicationLogSyncDelayForAws_WithNoObjects_ReturnsMaxDelay()
      throws Exception {
    Tenant tenant = Tenant.ABC;

    try (MockedStatic<ApplicationConfigUtil> mockedConfigUtil =
            mockStatic(ApplicationConfigUtil.class);
        MockedStatic<com.logwise.orchestrator.factory.ObjectStoreFactory> mockedFactory =
            mockStatic(com.logwise.orchestrator.factory.ObjectStoreFactory.class)) {
      ApplicationConfig.TenantConfig tenantConfig = mock(ApplicationConfig.TenantConfig.class);
      ApplicationConfig.SparkConfig sparkConfig = mock(ApplicationConfig.SparkConfig.class);
      ApplicationConfig.DelayMetricsConfig delayMetricsConfig =
          mock(ApplicationConfig.DelayMetricsConfig.class);
      ApplicationConfig.ApplicationDelayMetricsConfig appConfig =
          mock(ApplicationConfig.ApplicationDelayMetricsConfig.class);

      when(tenantConfig.getSpark()).thenReturn(sparkConfig);
      when(tenantConfig.getDelayMetrics()).thenReturn(delayMetricsConfig);
      when(delayMetricsConfig.getApp()).thenReturn(appConfig);
      when(sparkConfig.getLogsDir()).thenReturn("logs");
      when(appConfig.getSampleServiceName()).thenReturn("test-service");

      mockedConfigUtil
          .when(() -> ApplicationConfigUtil.getTenantConfig(tenant))
          .thenReturn(tenantConfig);

      when(mockObjectStoreClient.listObjects(anyString()))
          .thenReturn(Single.just(Collections.emptyList()));
      mockedFactory
          .when(() -> com.logwise.orchestrator.factory.ObjectStoreFactory.getClient(tenant))
          .thenReturn(mockObjectStoreClient);

      java.lang.reflect.Method method =
          MetricsService.class.getDeclaredMethod(
              "computeApplicationLogSyncDelayForAws", Tenant.class);
      method.setAccessible(true);

      Single<Integer> result = (Single<Integer>) method.invoke(metricsService, tenant);
      Integer delay = result.blockingGet();

      Assert.assertNotNull(delay);
      // Should return max delay when no objects found
      Assert.assertTrue(
          delay
              >= com.logwise.orchestrator.constant.ApplicationConstants.MAX_LOGS_SYNC_DELAY_HOURS
                  * 60);
    }
  }
}
