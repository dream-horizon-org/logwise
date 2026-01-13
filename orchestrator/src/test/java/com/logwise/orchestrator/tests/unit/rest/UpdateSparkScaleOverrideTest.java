package com.logwise.orchestrator.tests.unit.rest;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.logwise.orchestrator.dto.request.UpdateSparkScaleOverrideRequest;
import com.logwise.orchestrator.dto.response.DefaultSuccessResponse;
import com.logwise.orchestrator.enums.Tenant;
import com.logwise.orchestrator.rest.UpdateSparkScaleOverride;
import com.logwise.orchestrator.rest.io.Response;
import com.logwise.orchestrator.service.SparkService;
import com.logwise.orchestrator.setup.BaseTest;
import com.logwise.orchestrator.util.ResponseWrapper;
import com.logwise.orchestrator.util.TestResponseWrapper;
import io.reactivex.Completable;
import java.util.concurrent.CompletionStage;
import org.mockito.MockedStatic;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class UpdateSparkScaleOverrideTest extends BaseTest {

  private UpdateSparkScaleOverride updateSparkScaleOverride;
  private SparkService mockSparkService;

  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    mockSparkService = mock(SparkService.class);
    updateSparkScaleOverride = new UpdateSparkScaleOverride();
    java.lang.reflect.Field field = UpdateSparkScaleOverride.class.getDeclaredField("sparkService");
    field.setAccessible(true);
    field.set(updateSparkScaleOverride, mockSparkService);
  }

  @AfterClass
  public static void tearDownClass() {
    BaseTest.cleanup();
  }

  @Test
  public void testHandle_WithValidRequest_ReturnsSuccess() throws Exception {
    Tenant tenant = Tenant.ABC;
    String tenantName = tenant.getValue();
    UpdateSparkScaleOverrideRequest request = new UpdateSparkScaleOverrideRequest();
    request.setEnableUpScale(true);
    request.setEnableDownScale(false);

    when(mockSparkService.updateSparkScaleOverride(eq(tenant), eq(request)))
        .thenReturn(Completable.complete());

    CompletionStage<Response<DefaultSuccessResponse>> future;
    try (MockedStatic<ResponseWrapper> mockedResponseWrapper =
        org.mockito.Mockito.mockStatic(ResponseWrapper.class)) {
      mockedResponseWrapper
          .when(() -> ResponseWrapper.fromSingle(any(io.reactivex.Single.class), eq(200)))
          .thenAnswer(
              invocation -> {
                io.reactivex.Single<DefaultSuccessResponse> single = invocation.getArgument(0);
                int statusCode = invocation.getArgument(1);
                return TestResponseWrapper.fromSingle(single, statusCode);
              });

      future = updateSparkScaleOverride.handle(tenantName, request);
    }

    Response<DefaultSuccessResponse> response = future.toCompletableFuture().get();

    Assert.assertNotNull(response);
    Assert.assertNotNull(response.getData());
    Assert.assertNotNull(response.getData().getMessage());
    Assert.assertTrue(response.getData().getMessage().contains(tenantName));
    Assert.assertEquals(response.getHttpStatusCode(), 200);
    verify(mockSparkService, times(1)).updateSparkScaleOverride(eq(tenant), eq(request));
  }

  @Test
  public void testHandle_WithBothFlagsEnabled_UpdatesSuccessfully() throws Exception {
    Tenant tenant = Tenant.ABC;
    String tenantName = tenant.getValue();
    UpdateSparkScaleOverrideRequest request = new UpdateSparkScaleOverrideRequest();
    request.setEnableUpScale(true);
    request.setEnableDownScale(true);

    when(mockSparkService.updateSparkScaleOverride(eq(tenant), eq(request)))
        .thenReturn(Completable.complete());

    CompletionStage<Response<DefaultSuccessResponse>> future;
    try (MockedStatic<ResponseWrapper> mockedResponseWrapper =
        org.mockito.Mockito.mockStatic(ResponseWrapper.class)) {
      mockedResponseWrapper
          .when(() -> ResponseWrapper.fromSingle(any(io.reactivex.Single.class), eq(200)))
          .thenAnswer(
              invocation -> {
                io.reactivex.Single<DefaultSuccessResponse> single = invocation.getArgument(0);
                int statusCode = invocation.getArgument(1);
                return TestResponseWrapper.fromSingle(single, statusCode);
              });

      future = updateSparkScaleOverride.handle(tenantName, request);
    }

    Response<DefaultSuccessResponse> response = future.toCompletableFuture().get();

    Assert.assertNotNull(response);
    Assert.assertEquals(response.getHttpStatusCode(), 200);
    verify(mockSparkService, times(1)).updateSparkScaleOverride(eq(tenant), eq(request));
  }

  @Test
  public void testHandle_WithBothFlagsDisabled_UpdatesSuccessfully() throws Exception {
    Tenant tenant = Tenant.ABC;
    String tenantName = tenant.getValue();
    UpdateSparkScaleOverrideRequest request = new UpdateSparkScaleOverrideRequest();
    request.setEnableUpScale(false);
    request.setEnableDownScale(false);

    when(mockSparkService.updateSparkScaleOverride(eq(tenant), eq(request)))
        .thenReturn(Completable.complete());

    CompletionStage<Response<DefaultSuccessResponse>> future;
    try (MockedStatic<ResponseWrapper> mockedResponseWrapper =
        org.mockito.Mockito.mockStatic(ResponseWrapper.class)) {
      mockedResponseWrapper
          .when(() -> ResponseWrapper.fromSingle(any(io.reactivex.Single.class), eq(200)))
          .thenAnswer(
              invocation -> {
                io.reactivex.Single<DefaultSuccessResponse> single = invocation.getArgument(0);
                int statusCode = invocation.getArgument(1);
                return TestResponseWrapper.fromSingle(single, statusCode);
              });

      future = updateSparkScaleOverride.handle(tenantName, request);
    }

    Response<DefaultSuccessResponse> response = future.toCompletableFuture().get();

    Assert.assertNotNull(response);
    Assert.assertEquals(response.getHttpStatusCode(), 200);
    verify(mockSparkService, times(1)).updateSparkScaleOverride(eq(tenant), eq(request));
  }

  @Test
  public void testHandle_WithServiceError_PropagatesError() throws Exception {
    Tenant tenant = Tenant.ABC;
    String tenantName = tenant.getValue();
    UpdateSparkScaleOverrideRequest request = new UpdateSparkScaleOverrideRequest();

    RuntimeException serviceError = new RuntimeException("Service error");
    when(mockSparkService.updateSparkScaleOverride(eq(tenant), eq(request)))
        .thenReturn(Completable.error(serviceError));

    CompletionStage<Response<DefaultSuccessResponse>> future;
    try (MockedStatic<ResponseWrapper> mockedResponseWrapper =
        org.mockito.Mockito.mockStatic(ResponseWrapper.class)) {
      mockedResponseWrapper
          .when(() -> ResponseWrapper.fromSingle(any(io.reactivex.Single.class), eq(200)))
          .thenAnswer(
              invocation -> {
                io.reactivex.Single<DefaultSuccessResponse> single = invocation.getArgument(0);
                int statusCode = invocation.getArgument(1);
                return TestResponseWrapper.fromSingle(single, statusCode);
              });

      future = updateSparkScaleOverride.handle(tenantName, request);
    }

    try {
      future.toCompletableFuture().get();
      Assert.fail("Should have thrown exception");
    } catch (Exception e) {
      Assert.assertNotNull(e.getCause());
    }
  }
}
