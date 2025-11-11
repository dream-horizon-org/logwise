package com.logwise.orchestrator.tests.unit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.logwise.orchestrator.constants.TestConstants;
import com.logwise.orchestrator.dto.response.LogSyncDelayResponse;
import com.logwise.orchestrator.enums.Tenant;
import com.logwise.orchestrator.rest.MetricSyncDelay;
import com.logwise.orchestrator.rest.io.Response;
import com.logwise.orchestrator.service.MetricsService;
import com.logwise.orchestrator.setup.BaseTest;
import com.logwise.orchestrator.util.ResponseWrapper;
import com.logwise.orchestrator.util.TestResponseWrapper;
import io.reactivex.Single;
import java.util.concurrent.CompletionStage;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/** Unit tests for Metrics API - getObjectStoreSyncDelay endpoint. */
@Listeners(com.logwise.orchestrator.listeners.ExtentReportListener.class)
public class MetricsTest extends BaseTest {

  private static final int MAX_DELAY_MINUTES = 1440; // 24 hours * 60 minutes

  @Mock private MetricsService metricsService;

  private MetricSyncDelay metrics;

  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    MockitoAnnotations.openMocks(this);

    metrics = new MetricSyncDelay(metricsService);
  }

  @AfterClass
  public static void tearDownClass() {
    BaseTest.cleanup();
  }

  @Test
  public void testGetObjectStoreSyncDelay_WithValidTenant_ReturnsLogSyncDelayResponse()
      throws Exception {

    LogSyncDelayResponse expectedResponse =
        LogSyncDelayResponse.builder()
            .tenant(TestConstants.VALID_TENANT_NAME)
            .appLogsDelayMinutes(TestConstants.DELAY_MINUTES)
            .build();

    when(metricsService.computeLogSyncDelay(any(Tenant.class)))
        .thenReturn(Single.just(expectedResponse));

    try (MockedStatic<ResponseWrapper> mockedResponseWrapper =
        Mockito.mockStatic(ResponseWrapper.class)) {
      mockedResponseWrapper
          .when(() -> ResponseWrapper.fromSingle(any(Single.class), eq(200)))
          .thenAnswer(
              invocation -> {
                Single<LogSyncDelayResponse> single = invocation.getArgument(0);
                int statusCode = invocation.getArgument(1);
                return TestResponseWrapper.fromSingle(single, statusCode);
              });

      CompletionStage<Response<LogSyncDelayResponse>> result =
          metrics.getObjectStoreSyncDelay(TestConstants.VALID_TENANT_NAME);

      Response<LogSyncDelayResponse> response = result.toCompletableFuture().get();
      Assert.assertNotNull(response, "Response should not be null");
      Assert.assertEquals(response.getHttpStatusCode(), 200, "HTTP status code should be 200");
      Assert.assertNull(response.getError(), "Error should be null for successful response");

      LogSyncDelayResponse responseData = response.getData();
      Assert.assertNotNull(responseData, "Response data should not be null");

      Assert.assertNotNull(responseData.getTenant(), "Tenant should not be null");
      Assert.assertEquals(
          responseData.getTenant(),
          TestConstants.VALID_TENANT_NAME,
          "Tenant should match expected value");
      Assert.assertFalse(responseData.getTenant().isEmpty(), "Tenant should not be empty");

      Assert.assertNotNull(
          responseData.getAppLogsDelayMinutes(), "App logs delay minutes should not be null");
      Assert.assertEquals(
          responseData.getAppLogsDelayMinutes(),
          TestConstants.DELAY_MINUTES,
          "App logs delay minutes should match expected value");
      Assert.assertTrue(
          responseData.getAppLogsDelayMinutes() >= 0,
          "App logs delay minutes should be non-negative");

      verify(metricsService, times(1)).computeLogSyncDelay(eq(Tenant.ABC));

      mockedResponseWrapper.verify(
          () -> ResponseWrapper.fromSingle(any(Single.class), eq(200)), times(1));
    }
  }

  @Test(expectedExceptions = Exception.class)
  public void testGetObjectStoreSyncDelay_WhenMetricsComputationFails_PropagatesException()
      throws Exception {

    RuntimeException error = new RuntimeException("Metrics computation failed");

    when(metricsService.computeLogSyncDelay(any(Tenant.class))).thenReturn(Single.error(error));

    try (MockedStatic<ResponseWrapper> mockedResponseWrapper =
        Mockito.mockStatic(ResponseWrapper.class)) {
      mockedResponseWrapper
          .when(() -> ResponseWrapper.fromSingle(any(Single.class), anyInt()))
          .thenAnswer(
              invocation -> {
                Single<LogSyncDelayResponse> single = invocation.getArgument(0);
                int statusCode = invocation.getArgument(1);
                return TestResponseWrapper.fromSingle(single, statusCode);
              });

      CompletionStage<Response<LogSyncDelayResponse>> result =
          metrics.getObjectStoreSyncDelay(TestConstants.VALID_TENANT_NAME);

      result.toCompletableFuture().get();

      verify(metricsService, times(1)).computeLogSyncDelay(eq(Tenant.ABC));
    }
  }

  @Test
  public void testGetObjectStoreSyncDelay_WithNoLogsFound_ReturnsMaxDelay() throws Exception {

    LogSyncDelayResponse expectedResponse =
        LogSyncDelayResponse.builder()
            .tenant(TestConstants.VALID_TENANT_NAME)
            .appLogsDelayMinutes(MAX_DELAY_MINUTES)
            .build();

    when(metricsService.computeLogSyncDelay(any(Tenant.class)))
        .thenReturn(Single.just(expectedResponse));

    try (MockedStatic<ResponseWrapper> mockedResponseWrapper =
        Mockito.mockStatic(ResponseWrapper.class)) {
      mockedResponseWrapper
          .when(() -> ResponseWrapper.fromSingle(any(Single.class), anyInt()))
          .thenAnswer(
              invocation -> {
                Single<LogSyncDelayResponse> single = invocation.getArgument(0);
                int statusCode = invocation.getArgument(1);
                return TestResponseWrapper.fromSingle(single, statusCode);
              });

      CompletionStage<Response<LogSyncDelayResponse>> result =
          metrics.getObjectStoreSyncDelay(TestConstants.VALID_TENANT_NAME);

      Response<LogSyncDelayResponse> response = result.toCompletableFuture().get();
      Assert.assertNotNull(response, "Response should not be null");
      Assert.assertEquals(response.getHttpStatusCode(), 200, "HTTP status code should be 200");
      Assert.assertNull(response.getError(), "Error should be null for successful response");

      LogSyncDelayResponse responseData = response.getData();
      Assert.assertNotNull(responseData, "Response data should not be null");

      Assert.assertNotNull(responseData.getTenant(), "Tenant should not be null");
      Assert.assertEquals(
          responseData.getTenant(),
          TestConstants.VALID_TENANT_NAME,
          "Tenant should match expected value");
      Assert.assertFalse(responseData.getTenant().isEmpty(), "Tenant should not be empty");

      Assert.assertNotNull(
          responseData.getAppLogsDelayMinutes(), "App logs delay minutes should not be null");
      Assert.assertEquals(
          responseData.getAppLogsDelayMinutes(),
          MAX_DELAY_MINUTES,
          "App logs delay minutes should be max delay (1440 minutes) when no logs found");
      Assert.assertTrue(
          responseData.getAppLogsDelayMinutes() > 0, "App logs delay minutes should be positive");

      verify(metricsService, times(1)).computeLogSyncDelay(eq(Tenant.ABC));
    }
  }
}
