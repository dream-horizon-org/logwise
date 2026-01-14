package com.logwise.orchestrator.tests.unit.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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
import io.reactivex.Single;
import java.util.concurrent.CompletionStage;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class UpdateSparkScaleOverrideTest extends BaseTest {

  private UpdateSparkScaleOverride updateSparkScaleOverride;
  private SparkService mockSparkService;

  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    TestResponseWrapper.init(vertx);
    mockSparkService = mock(SparkService.class);
    updateSparkScaleOverride = new UpdateSparkScaleOverride();
    // Use reflection to set the field since it's field-injected
    try {
      java.lang.reflect.Field field =
          UpdateSparkScaleOverride.class.getDeclaredField("sparkService");
      field.setAccessible(true);
      field.set(updateSparkScaleOverride, mockSparkService);
    } catch (Exception e) {
      // Ignore
    }
  }

  @Test
  public void testHandle_WithValidRequest_ReturnsSuccess() throws Exception {
    String tenantName = "ABC";
    UpdateSparkScaleOverrideRequest request = new UpdateSparkScaleOverrideRequest();
    request.setEnableUpScale(true);
    request.setEnableDownScale(false);

    when(mockSparkService.updateSparkScaleOverride(
            any(Tenant.class), any(UpdateSparkScaleOverrideRequest.class)))
        .thenReturn(Completable.complete());

    try (MockedStatic<ResponseWrapper> mockedWrapper = Mockito.mockStatic(ResponseWrapper.class)) {
      mockedWrapper
          .when(() -> ResponseWrapper.fromSingle(any(Single.class), anyInt()))
          .thenAnswer(
              invocation -> {
                Single<DefaultSuccessResponse> single = invocation.getArgument(0);
                int statusCode = invocation.getArgument(1);
                return TestResponseWrapper.fromSingle(single, statusCode);
              });

      CompletionStage<Response<DefaultSuccessResponse>> result =
          updateSparkScaleOverride.handle(tenantName, request);

      Thread.sleep(100); // Wait for async processing
      Response<DefaultSuccessResponse> response = result.toCompletableFuture().get();
      Assert.assertNotNull(response);
      Assert.assertNotNull(response.getData());
      Assert.assertEquals(response.getHttpStatusCode(), 200);
      verify(mockSparkService, times(1)).updateSparkScaleOverride(Tenant.ABC, request);
    }
  }

  @Test
  public void testHandle_WithInvalidTenant_ThrowsException() {
    String tenantName = "INVALID";
    UpdateSparkScaleOverrideRequest request = new UpdateSparkScaleOverrideRequest();

    try {
      updateSparkScaleOverride.handle(tenantName, request);
      Assert.fail("Should throw exception for invalid tenant");
    } catch (Exception e) {
      Assert.assertNotNull(e);
    }
  }
}
