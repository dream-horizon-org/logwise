package com.logwise.orchestrator.tests.unit.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.logwise.orchestrator.dto.entity.SparkStageHistory;
import com.logwise.orchestrator.dto.request.ScaleSparkClusterRequest;
import com.logwise.orchestrator.dto.response.DefaultSuccessResponse;
import com.logwise.orchestrator.enums.Tenant;
import com.logwise.orchestrator.rest.ScaleSparkCluster;
import com.logwise.orchestrator.rest.io.Response;
import com.logwise.orchestrator.service.SparkService;
import com.logwise.orchestrator.setup.BaseTest;
import io.reactivex.Completable;
import java.util.concurrent.CompletionStage;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Unit tests for ScaleSparkCluster REST endpoint. */
public class ScaleSparkClusterTest extends BaseTest {

  private SparkService mockSparkService;
  private ScaleSparkCluster scaleSparkCluster;

  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    mockSparkService = mock(SparkService.class);
    scaleSparkCluster = new ScaleSparkCluster(mockSparkService);
  }

  @AfterClass
  public static void tearDownClass() {
    BaseTest.cleanup();
  }

  @Test
  public void testHandle_WithValidRequest_ReturnsSuccessResponse() throws Exception {
    Tenant tenant = Tenant.ABC;
    String tenantName = tenant.getValue();
    ScaleSparkClusterRequest request = createValidRequest();

    when(mockSparkService.insertSparkStageHistory(any(SparkStageHistory.class)))
        .thenReturn(Completable.complete());
    when(mockSparkService.scaleSpark(any(Tenant.class), anyBoolean(), anyBoolean()))
        .thenReturn(Completable.complete());

    CompletionStage<Response<DefaultSuccessResponse>> future =
        scaleSparkCluster.handle(tenantName, request);

    Response<DefaultSuccessResponse> response = future.toCompletableFuture().get();

    Assert.assertNotNull(response);
    Assert.assertNotNull(response.getData());
    Assert.assertNotNull(response.getData().getMessage());
    Assert.assertTrue(response.getData().getMessage().contains(tenantName));
    Assert.assertEquals(response.getHttpStatusCode(), 200);
    verify(mockSparkService, times(1)).insertSparkStageHistory(any(SparkStageHistory.class));
    verify(mockSparkService, times(1)).scaleSpark(eq(tenant), eq(true), eq(true));
  }

  @Test
  public void testHandle_WithUpScaleDisabled_ReturnsSuccessResponse() throws Exception {
    Tenant tenant = Tenant.ABC;
    String tenantName = tenant.getValue();
    ScaleSparkClusterRequest request = createValidRequest();
    request.setEnableUpScale(false);
    request.setEnableDownScale(true);

    when(mockSparkService.insertSparkStageHistory(any(SparkStageHistory.class)))
        .thenReturn(Completable.complete());
    when(mockSparkService.scaleSpark(any(Tenant.class), anyBoolean(), anyBoolean()))
        .thenReturn(Completable.complete());

    CompletionStage<Response<DefaultSuccessResponse>> future =
        scaleSparkCluster.handle(tenantName, request);

    Response<DefaultSuccessResponse> response = future.toCompletableFuture().get();

    Assert.assertNotNull(response);
    Assert.assertEquals(response.getHttpStatusCode(), 200);
    verify(mockSparkService, times(1)).insertSparkStageHistory(any(SparkStageHistory.class));
    verify(mockSparkService, times(1)).scaleSpark(eq(tenant), eq(false), eq(true));
  }

  @Test
  public void testHandle_WithDownScaleDisabled_ReturnsSuccessResponse() throws Exception {
    Tenant tenant = Tenant.ABC;
    String tenantName = tenant.getValue();
    ScaleSparkClusterRequest request = createValidRequest();
    request.setEnableUpScale(true);
    request.setEnableDownScale(false);

    when(mockSparkService.insertSparkStageHistory(any(SparkStageHistory.class)))
        .thenReturn(Completable.complete());
    when(mockSparkService.scaleSpark(any(Tenant.class), anyBoolean(), anyBoolean()))
        .thenReturn(Completable.complete());

    CompletionStage<Response<DefaultSuccessResponse>> future =
        scaleSparkCluster.handle(tenantName, request);

    Response<DefaultSuccessResponse> response = future.toCompletableFuture().get();

    Assert.assertNotNull(response);
    Assert.assertEquals(response.getHttpStatusCode(), 200);
    verify(mockSparkService, times(1)).insertSparkStageHistory(any(SparkStageHistory.class));
    verify(mockSparkService, times(1)).scaleSpark(eq(tenant), eq(true), eq(false));
  }

  @Test
  public void testHandle_WithBothScalingDisabled_ReturnsSuccessResponse() throws Exception {
    Tenant tenant = Tenant.ABC;
    String tenantName = tenant.getValue();
    ScaleSparkClusterRequest request = createValidRequest();
    request.setEnableUpScale(false);
    request.setEnableDownScale(false);

    when(mockSparkService.insertSparkStageHistory(any(SparkStageHistory.class)))
        .thenReturn(Completable.complete());
    when(mockSparkService.scaleSpark(any(Tenant.class), anyBoolean(), anyBoolean()))
        .thenReturn(Completable.complete());

    CompletionStage<Response<DefaultSuccessResponse>> future =
        scaleSparkCluster.handle(tenantName, request);

    Response<DefaultSuccessResponse> response = future.toCompletableFuture().get();

    Assert.assertNotNull(response);
    Assert.assertEquals(response.getHttpStatusCode(), 200);
    verify(mockSparkService, times(1)).insertSparkStageHistory(any(SparkStageHistory.class));
    verify(mockSparkService, times(1)).scaleSpark(eq(tenant), eq(false), eq(false));
  }

  @Test
  public void testHandle_SetsTenantOnSparkStageHistory() throws Exception {
    Tenant tenant = Tenant.ABC;
    String tenantName = tenant.getValue();
    ScaleSparkClusterRequest request = createValidRequest();
    SparkStageHistory stageHistory = request.getSparkStageHistory();
    stageHistory.setTenant(null);

    when(mockSparkService.insertSparkStageHistory(any(SparkStageHistory.class)))
        .thenReturn(Completable.complete());
    when(mockSparkService.scaleSpark(any(Tenant.class), anyBoolean(), anyBoolean()))
        .thenReturn(Completable.complete());

    CompletionStage<Response<DefaultSuccessResponse>> future =
        scaleSparkCluster.handle(tenantName, request);

    future.toCompletableFuture().get();

    Assert.assertEquals(stageHistory.getTenant(), tenant.getValue());
    verify(mockSparkService, times(1)).insertSparkStageHistory(any(SparkStageHistory.class));
  }

  @Test
  public void testHandle_WithServiceError_StillReturnsSuccess() throws Exception {
    Tenant tenant = Tenant.ABC;
    String tenantName = tenant.getValue();
    ScaleSparkClusterRequest request = createValidRequest();
    RuntimeException error = new RuntimeException("Service error");

    when(mockSparkService.insertSparkStageHistory(any(SparkStageHistory.class)))
        .thenReturn(Completable.error(error));
    when(mockSparkService.scaleSpark(any(Tenant.class), anyBoolean(), anyBoolean()))
        .thenReturn(Completable.error(error));

    CompletionStage<Response<DefaultSuccessResponse>> future =
        scaleSparkCluster.handle(tenantName, request);

    Response<DefaultSuccessResponse> response = future.toCompletableFuture().get();

    Assert.assertNotNull(response);
    Assert.assertEquals(response.getHttpStatusCode(), 200);
  }

  @Test
  public void testHandle_WithComplexSparkStageHistory_ProcessesCorrectly() throws Exception {
    Tenant tenant = Tenant.ABC;
    String tenantName = tenant.getValue();
    ScaleSparkClusterRequest request = createValidRequest();
    SparkStageHistory stageHistory = request.getSparkStageHistory();
    stageHistory.setOutputBytes(1000000L);
    stageHistory.setInputRecords(5000L);
    stageHistory.setSubmissionTime(System.currentTimeMillis());
    stageHistory.setCompletionTime(System.currentTimeMillis() + 10000);
    stageHistory.setCoresUsed(8);
    stageHistory.setStatus("SUCCESS");

    when(mockSparkService.insertSparkStageHistory(any(SparkStageHistory.class)))
        .thenReturn(Completable.complete());
    when(mockSparkService.scaleSpark(any(Tenant.class), anyBoolean(), anyBoolean()))
        .thenReturn(Completable.complete());

    CompletionStage<Response<DefaultSuccessResponse>> future =
        scaleSparkCluster.handle(tenantName, request);

    Response<DefaultSuccessResponse> response = future.toCompletableFuture().get();

    Assert.assertNotNull(response);
    Assert.assertEquals(response.getHttpStatusCode(), 200);
    verify(mockSparkService, times(1)).insertSparkStageHistory(any(SparkStageHistory.class));
    verify(mockSparkService, times(1)).scaleSpark(eq(tenant), eq(true), eq(true));
  }

  /**
   * Helper method to create a valid ScaleSparkClusterRequest for testing.
   *
   * @return A valid ScaleSparkClusterRequest instance
   */
  private ScaleSparkClusterRequest createValidRequest() {
    ScaleSparkClusterRequest request = new ScaleSparkClusterRequest();
    request.setEnableUpScale(true);
    request.setEnableDownScale(true);

    SparkStageHistory stageHistory = new SparkStageHistory();
    stageHistory.setOutputBytes(500000L);
    stageHistory.setInputRecords(1000L);
    stageHistory.setSubmissionTime(System.currentTimeMillis());
    stageHistory.setCompletionTime(System.currentTimeMillis() + 5000);
    stageHistory.setCoresUsed(4);
    stageHistory.setStatus("SUCCESS");

    request.setSparkStageHistory(stageHistory);
    return request;
  }
}
