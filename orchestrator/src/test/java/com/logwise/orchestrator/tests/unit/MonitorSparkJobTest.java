package com.logwise.orchestrator.tests.unit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

import com.logwise.orchestrator.constants.TestConstants;
import com.logwise.orchestrator.dto.request.MonitorSparkJobRequest;
import com.logwise.orchestrator.dto.response.DefaultSuccessResponse;
import com.logwise.orchestrator.enums.Tenant;
import com.logwise.orchestrator.rest.MonitorSparkJob;
import com.logwise.orchestrator.rest.io.Response;
import com.logwise.orchestrator.service.SparkService;
import io.reactivex.Completable;
import java.util.concurrent.CompletionStage;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/** Unit tests for MonitorSparkJob API - handle endpoint. */
@Listeners(com.logwise.orchestrator.listeners.ExtentReportListener.class)
public class MonitorSparkJobTest {

  @Mock private SparkService sparkService;

  private MonitorSparkJob monitorSparkJob;

  @BeforeMethod
  public void setUp() throws Exception {
    MockitoAnnotations.openMocks(this);

    monitorSparkJob = new MonitorSparkJob(sparkService);
  }

  @Test
  public void testHandle_WithNullRequest_ReturnsSuccessResponse() throws Exception {
    MonitorSparkJobRequest request = null;

    when(sparkService.monitorSparkJob(any(), isNull(), isNull()))
        .thenReturn(Completable.complete());

    CompletionStage<Response<DefaultSuccessResponse>> result =
        monitorSparkJob.handle(TestConstants.VALID_TENANT_NAME, request);

    Response<DefaultSuccessResponse> response = result.toCompletableFuture().get();
    Assert.assertNotNull(response, "Response should not be null");
    Assert.assertEquals(response.getHttpStatusCode(), 200, "HTTP status code should be 200");
    Assert.assertNull(response.getError(), "Error should be null for successful response");

    DefaultSuccessResponse responseData = response.getData();
    Assert.assertNotNull(responseData, "Response data should not be null");

    Assert.assertTrue(responseData.isSuccess(), "Success field should be true");
    Assert.assertNotNull(responseData.getMessage(), "Response message should not be null");

    String message = responseData.getMessage();
    String expectedMessage =
        "Successfully monitored the spark job for tenant: " + TestConstants.VALID_TENANT_NAME;
    Assert.assertEquals(message, expectedMessage, "Response message should match expected format");
    Assert.assertTrue(
        message.contains(TestConstants.VALID_TENANT_NAME),
        "Response message should contain tenant name");
    Assert.assertTrue(
        message.startsWith("Successfully monitored the spark job for tenant: "),
        "Response message should start with expected prefix");

    verify(sparkService, times(1)).monitorSparkJob(eq(Tenant.ABC), isNull(), isNull());
  }

  @Test
  public void testHandle_WithCustomDriverCoresAndMemory_ReturnsSuccessResponse() throws Exception {
    MonitorSparkJobRequest request = new MonitorSparkJobRequest();
    request.setDriverCores(TestConstants.DEFAULT_DRIVER_CORES);
    request.setDriverMemoryInGb(TestConstants.DEFAULT_DRIVER_MEMORY_GB);

    when(sparkService.monitorSparkJob(any(), anyInt(), anyInt()))
        .thenReturn(Completable.complete());

    CompletionStage<Response<DefaultSuccessResponse>> result =
        monitorSparkJob.handle(TestConstants.VALID_TENANT_NAME, request);

    Response<DefaultSuccessResponse> response = result.toCompletableFuture().get();
    Assert.assertNotNull(response, "Response should not be null");
    Assert.assertEquals(response.getHttpStatusCode(), 200, "HTTP status code should be 200");
    Assert.assertNull(response.getError(), "Error should be null for successful response");

    DefaultSuccessResponse responseData = response.getData();
    Assert.assertNotNull(responseData, "Response data should not be null");

    Assert.assertTrue(responseData.isSuccess(), "Success field should be true");
    Assert.assertNotNull(responseData.getMessage(), "Response message should not be null");

    String message = responseData.getMessage();
    String expectedMessage =
        "Successfully monitored the spark job for tenant: " + TestConstants.VALID_TENANT_NAME;
    Assert.assertEquals(message, expectedMessage, "Response message should match expected format");
    Assert.assertTrue(
        message.contains(TestConstants.VALID_TENANT_NAME),
        "Response message should contain tenant name");

    verify(sparkService, times(1))
        .monitorSparkJob(
            eq(Tenant.ABC),
            eq(TestConstants.DEFAULT_DRIVER_CORES),
            eq(TestConstants.DEFAULT_DRIVER_MEMORY_GB));
  }

  @Test
  public void testHandle_WithOnlyDriverCores_ReturnsSuccessResponse() throws Exception {
    MonitorSparkJobRequest request = new MonitorSparkJobRequest();
    request.setDriverCores(TestConstants.DEFAULT_DRIVER_CORES);

    when(sparkService.monitorSparkJob(any(), anyInt(), isNull()))
        .thenReturn(Completable.complete());

    CompletionStage<Response<DefaultSuccessResponse>> result =
        monitorSparkJob.handle(TestConstants.VALID_TENANT_NAME, request);

    Response<DefaultSuccessResponse> response = result.toCompletableFuture().get();
    Assert.assertNotNull(response, "Response should not be null");
    Assert.assertEquals(response.getHttpStatusCode(), 200, "HTTP status code should be 200");
    Assert.assertNull(response.getError(), "Error should be null for successful response");

    DefaultSuccessResponse responseData = response.getData();
    Assert.assertNotNull(responseData, "Response data should not be null");

    Assert.assertTrue(responseData.isSuccess(), "Success field should be true");
    Assert.assertNotNull(responseData.getMessage(), "Response message should not be null");

    String message = responseData.getMessage();
    String expectedMessage =
        "Successfully monitored the spark job for tenant: " + TestConstants.VALID_TENANT_NAME;
    Assert.assertEquals(message, expectedMessage, "Response message should match expected format");
    Assert.assertTrue(
        message.contains(TestConstants.VALID_TENANT_NAME),
        "Response message should contain tenant name");

    verify(sparkService, times(1))
        .monitorSparkJob(eq(Tenant.ABC), eq(TestConstants.DEFAULT_DRIVER_CORES), isNull());
  }

  @Test
  public void testHandle_WithOnlyDriverMemory_ReturnsSuccessResponse() throws Exception {
    MonitorSparkJobRequest request = new MonitorSparkJobRequest();
    request.setDriverMemoryInGb(TestConstants.DEFAULT_DRIVER_MEMORY_GB);

    when(sparkService.monitorSparkJob(any(), isNull(), anyInt()))
        .thenReturn(Completable.complete());

    CompletionStage<Response<DefaultSuccessResponse>> result =
        monitorSparkJob.handle(TestConstants.VALID_TENANT_NAME, request);

    Response<DefaultSuccessResponse> response = result.toCompletableFuture().get();
    Assert.assertNotNull(response, "Response should not be null");
    Assert.assertEquals(response.getHttpStatusCode(), 200, "HTTP status code should be 200");
    Assert.assertNull(response.getError(), "Error should be null for successful response");

    DefaultSuccessResponse responseData = response.getData();
    Assert.assertNotNull(responseData, "Response data should not be null");

    Assert.assertTrue(responseData.isSuccess(), "Success field should be true");
    Assert.assertNotNull(responseData.getMessage(), "Response message should not be null");

    String message = responseData.getMessage();
    String expectedMessage =
        "Successfully monitored the spark job for tenant: " + TestConstants.VALID_TENANT_NAME;
    Assert.assertEquals(message, expectedMessage, "Response message should match expected format");
    Assert.assertTrue(
        message.contains(TestConstants.VALID_TENANT_NAME),
        "Response message should contain tenant name");

    verify(sparkService, times(1))
        .monitorSparkJob(eq(Tenant.ABC), isNull(), eq(TestConstants.DEFAULT_DRIVER_MEMORY_GB));
  }
}
