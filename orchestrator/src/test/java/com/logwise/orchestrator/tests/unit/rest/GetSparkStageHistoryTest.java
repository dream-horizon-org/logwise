package com.logwise.orchestrator.tests.unit.rest;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.logwise.orchestrator.dto.entity.SparkStageHistory;
import com.logwise.orchestrator.dto.response.GetSparkStageHistoryResponse;
import com.logwise.orchestrator.enums.Tenant;
import com.logwise.orchestrator.rest.GetSparkStageHistory;
import com.logwise.orchestrator.rest.io.Response;
import com.logwise.orchestrator.service.SparkService;
import com.logwise.orchestrator.setup.BaseTest;
import com.logwise.orchestrator.util.ResponseWrapper;
import com.logwise.orchestrator.util.TestResponseWrapper;
import io.reactivex.Single;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;
import org.mockito.MockedStatic;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class GetSparkStageHistoryTest extends BaseTest {

  private GetSparkStageHistory getSparkStageHistory;
  private SparkService mockSparkService;

  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    mockSparkService = mock(SparkService.class);
    getSparkStageHistory = new GetSparkStageHistory();
    java.lang.reflect.Field field = GetSparkStageHistory.class.getDeclaredField("sparkService");
    field.setAccessible(true);
    field.set(getSparkStageHistory, mockSparkService);
  }

  @AfterClass
  public static void tearDownClass() {
    BaseTest.cleanup();
  }

  @Test
  public void testHandle_WithValidRequest_ReturnsHistory() throws Exception {
    Tenant tenant = Tenant.ABC;
    String tenantName = tenant.getValue();
    int limit = 10;

    List<SparkStageHistory> historyList = new ArrayList<>();
    SparkStageHistory history1 = new SparkStageHistory();
    history1.setTenant("ABC");
    history1.setInputRecords(1000L);
    historyList.add(history1);

    GetSparkStageHistoryResponse response = GetSparkStageHistoryResponse.builder()
        .sparkStageHistory(historyList)
        .build();

    when(mockSparkService.getSparkStageHistory(eq(tenant), eq(limit)))
        .thenReturn(Single.just(response));

    CompletionStage<Response<GetSparkStageHistoryResponse>> future;
    try (MockedStatic<ResponseWrapper> mockedResponseWrapper =
        org.mockito.Mockito.mockStatic(ResponseWrapper.class)) {
      mockedResponseWrapper
          .when(() -> ResponseWrapper.fromSingle(any(Single.class), eq(200)))
          .thenAnswer(
              invocation -> {
                Single<GetSparkStageHistoryResponse> single = invocation.getArgument(0);
                int statusCode = invocation.getArgument(1);
                return TestResponseWrapper.fromSingle(single, statusCode);
              });

      future = getSparkStageHistory.handle(tenantName, limit);
    }

    Response<GetSparkStageHistoryResponse> result = future.toCompletableFuture().get();

    Assert.assertNotNull(result);
    Assert.assertNotNull(result.getData());
    Assert.assertNotNull(result.getData().getSparkStageHistory());
    Assert.assertEquals(result.getData().getSparkStageHistory().size(), 1);
    Assert.assertEquals(result.getHttpStatusCode(), 200);
    verify(mockSparkService, times(1)).getSparkStageHistory(eq(tenant), eq(limit));
  }

  @Test
  public void testHandle_WithEmptyHistory_ReturnsEmptyList() throws Exception {
    Tenant tenant = Tenant.ABC;
    String tenantName = tenant.getValue();
    int limit = 10;

    GetSparkStageHistoryResponse response = GetSparkStageHistoryResponse.builder()
        .sparkStageHistory(new ArrayList<>())
        .build();

    when(mockSparkService.getSparkStageHistory(eq(tenant), eq(limit)))
        .thenReturn(Single.just(response));

    CompletionStage<Response<GetSparkStageHistoryResponse>> future;
    try (MockedStatic<ResponseWrapper> mockedResponseWrapper =
        org.mockito.Mockito.mockStatic(ResponseWrapper.class)) {
      mockedResponseWrapper
          .when(() -> ResponseWrapper.fromSingle(any(Single.class), eq(200)))
          .thenAnswer(
              invocation -> {
                Single<GetSparkStageHistoryResponse> single = invocation.getArgument(0);
                int statusCode = invocation.getArgument(1);
                return TestResponseWrapper.fromSingle(single, statusCode);
              });

      future = getSparkStageHistory.handle(tenantName, limit);
    }

    Response<GetSparkStageHistoryResponse> result = future.toCompletableFuture().get();

    Assert.assertNotNull(result);
    Assert.assertNotNull(result.getData());
    Assert.assertNotNull(result.getData().getSparkStageHistory());
    Assert.assertTrue(result.getData().getSparkStageHistory().isEmpty());
    Assert.assertEquals(result.getHttpStatusCode(), 200);
  }

  @Test
  public void testHandle_WithServiceError_PropagatesError() throws Exception {
    Tenant tenant = Tenant.ABC;
    String tenantName = tenant.getValue();
    int limit = 10;

    RuntimeException serviceError = new RuntimeException("Service error");
    when(mockSparkService.getSparkStageHistory(eq(tenant), eq(limit)))
        .thenReturn(Single.error(serviceError));

    CompletionStage<Response<GetSparkStageHistoryResponse>> future;
    try (MockedStatic<ResponseWrapper> mockedResponseWrapper =
        org.mockito.Mockito.mockStatic(ResponseWrapper.class)) {
      mockedResponseWrapper
          .when(() -> ResponseWrapper.fromSingle(any(Single.class), eq(200)))
          .thenAnswer(
              invocation -> {
                Single<GetSparkStageHistoryResponse> single = invocation.getArgument(0);
                int statusCode = invocation.getArgument(1);
                return TestResponseWrapper.fromSingle(single, statusCode);
              });

      future = getSparkStageHistory.handle(tenantName, limit);
    }

    try {
      future.toCompletableFuture().get();
      Assert.fail("Should have thrown exception");
    } catch (Exception e) {
      Assert.assertNotNull(e.getCause());
    }
  }
}

