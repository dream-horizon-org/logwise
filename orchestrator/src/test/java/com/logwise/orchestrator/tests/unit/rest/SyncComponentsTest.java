package com.logwise.orchestrator.tests.unit.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.logwise.orchestrator.dto.request.ComponentSyncRequest;
import com.logwise.orchestrator.dto.response.DefaultSuccessResponse;
import com.logwise.orchestrator.enums.Tenant;
import com.logwise.orchestrator.rest.SyncComponents;
import com.logwise.orchestrator.rest.io.Response;
import com.logwise.orchestrator.service.ServiceManagerService;
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

/** Unit tests for SyncComponents REST endpoint. */
public class SyncComponentsTest extends BaseTest {

  private SyncComponents syncComponents;
  private ServiceManagerService mockServiceManagerService;

  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    TestResponseWrapper.init(vertx);
    mockServiceManagerService = mock(ServiceManagerService.class);
    syncComponents = new SyncComponents(mockServiceManagerService);
  }

  @Test
  public void testSyncHandler_WithValidRequest_ReturnsSuccess() throws Exception {
    String tenantName = "ABC";
    ComponentSyncRequest request = new ComponentSyncRequest();
    request.setComponentType("application");

    when(mockServiceManagerService.syncServices(any(Tenant.class)))
        .thenReturn(Completable.complete());

    try (MockedStatic<ResponseWrapper> mockedWrapper = Mockito.mockStatic(ResponseWrapper.class)) {
      mockedWrapper
          .when(() -> ResponseWrapper.fromSingle(any(Single.class), eq(200)))
          .thenAnswer(
              invocation -> {
                Single<DefaultSuccessResponse> single = invocation.getArgument(0);
                int statusCode = invocation.getArgument(1);
                return TestResponseWrapper.fromSingle(single, statusCode);
              });

      CompletionStage<Response<DefaultSuccessResponse>> result =
          syncComponents.syncHandler(tenantName, request);

      Response<DefaultSuccessResponse> response = result.toCompletableFuture().get();
      Assert.assertNotNull(response);
      Assert.assertNotNull(response.getData());
      Assert.assertTrue(response.getData().isSuccess());
      verify(mockServiceManagerService, times(1)).syncServices(Tenant.ABC);
    }
  }

  @Test
  public void testSyncHandler_WithInvalidTenant_ThrowsException() {
    String tenantName = "INVALID";
    ComponentSyncRequest request = new ComponentSyncRequest();
    request.setComponentType("application");

    try {
      syncComponents.syncHandler(tenantName, request);
      Assert.fail("Should throw exception for invalid tenant");
    } catch (Exception e) {
      Assert.assertNotNull(e);
    }
  }

  @Test(expectedExceptions = Exception.class)
  public void testSyncHandler_WithServiceError_PropagatesError() throws Exception {
    String tenantName = "ABC";
    ComponentSyncRequest request = new ComponentSyncRequest();
    request.setComponentType("application");

    RuntimeException error = new RuntimeException("Sync error");
    when(mockServiceManagerService.syncServices(any(Tenant.class)))
        .thenReturn(Completable.error(error));

    // When there's an error, ResponseWrapper.fromSingle won't be called
    // The error propagates through the Completable chain
    CompletionStage<Response<DefaultSuccessResponse>> result =
        syncComponents.syncHandler(tenantName, request);

    result.toCompletableFuture().get();
    verify(mockServiceManagerService, times(1)).syncServices(Tenant.ABC);
  }
}
