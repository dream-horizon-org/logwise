package com.logwise.orchestrator.tests.unit.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

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
import java.util.concurrent.CompletionStage;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class GetSparkStageHistoryTest extends BaseTest {

  private GetSparkStageHistory getSparkStageHistory;
  private SparkService mockSparkService;

  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    TestResponseWrapper.init(vertx);
    mockSparkService = mock(SparkService.class);
    getSparkStageHistory = new GetSparkStageHistory();
    // Use reflection to set the field since it's field-injected
    try {
      java.lang.reflect.Field field = GetSparkStageHistory.class.getDeclaredField("sparkService");
      field.setAccessible(true);
      field.set(getSparkStageHistory, mockSparkService);
    } catch (Exception e) {
      // Ignore
    }
  }

  @Test
  public void testHandle_WithValidTenantAndLimit_ReturnsHistory() throws Exception {
    String tenantName = "ABC";
    Integer limit = 10;
    GetSparkStageHistoryResponse mockResponse =
        GetSparkStageHistoryResponse.builder().sparkStageHistory(new ArrayList<>()).build();

    when(mockSparkService.getSparkStageHistory(any(Tenant.class), eq(limit)))
        .thenReturn(Single.just(mockResponse));

    try (MockedStatic<ResponseWrapper> mockedWrapper = Mockito.mockStatic(ResponseWrapper.class)) {
      mockedWrapper
          .when(() -> ResponseWrapper.fromSingle(any(Single.class), anyInt()))
          .thenAnswer(
              invocation -> {
                Single<GetSparkStageHistoryResponse> single = invocation.getArgument(0);
                int statusCode = invocation.getArgument(1);
                return TestResponseWrapper.fromSingle(single, statusCode);
              });

      CompletionStage<Response<GetSparkStageHistoryResponse>> result =
          getSparkStageHistory.handle(tenantName, limit);

      Thread.sleep(100); // Wait for async processing
      Response<GetSparkStageHistoryResponse> response = result.toCompletableFuture().get();
      Assert.assertNotNull(response);
      Assert.assertNotNull(response.getData());
      verify(mockSparkService, times(1)).getSparkStageHistory(Tenant.ABC, limit);
    }
  }

  @Test
  public void testHandle_WithNullLimit_ThrowsException() {
    String tenantName = "ABC";

    try {
      getSparkStageHistory.handle(tenantName, null);
      Assert.fail("Should throw exception for null limit");
    } catch (Exception e) {
      Assert.assertNotNull(e);
    }
  }
}
