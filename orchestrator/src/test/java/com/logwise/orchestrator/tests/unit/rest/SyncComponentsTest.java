package com.logwise.orchestrator.tests.unit.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Unit tests for SyncComponents REST endpoint. */
public class SyncComponentsTest extends BaseTest {

  private ServiceManagerService mockServiceManagerService;
  private SyncComponents syncComponents;

  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    mockServiceManagerService = mock(ServiceManagerService.class);
    syncComponents = new SyncComponents(mockServiceManagerService);
  }

  @AfterClass
  public static void tearDownClass() {
    BaseTest.cleanup();
  }

  @Test
  public void testSyncHandler_WithValidRequest_ReturnsSuccessResponse() throws Exception {
    Tenant tenant = Tenant.ABC;
    String tenantName = tenant.getValue();
    ComponentSyncRequest request = new ComponentSyncRequest();
    request.setComponentType("application");

    when(mockServiceManagerService.syncServices(any(Tenant.class)))
        .thenReturn(Completable.complete());

    try (MockedStatic<ResponseWrapper> mockedResponseWrapper =
        Mockito.mockStatic(ResponseWrapper.class)) {
      mockedResponseWrapper
          .when(() -> ResponseWrapper.fromSingle(any(Single.class), anyInt()))
          .thenAnswer(
              invocation -> {
                Single<DefaultSuccessResponse> single = invocation.getArgument(0);
                int statusCode = invocation.getArgument(1);
                return TestResponseWrapper.fromSingle(single, statusCode);
              });

      CompletionStage<Response<DefaultSuccessResponse>> future =
          syncComponents.syncHandler(tenantName, request);

      Response<DefaultSuccessResponse> response = future.toCompletableFuture().get();

      Assert.assertNotNull(response);
      Assert.assertNotNull(response.getData());
      Assert.assertTrue(response.getData().isSuccess());
      Assert.assertTrue(response.getData().getMessage().contains("application"));
      Assert.assertTrue(response.getData().getMessage().contains(tenantName));
      Assert.assertEquals(response.getHttpStatusCode(), 200);
      verify(mockServiceManagerService, times(1)).syncServices(eq(tenant));
    }
  }

  @Test
  public void testSyncHandler_WithServiceError_PropagatesError() throws Exception {
    Tenant tenant = Tenant.ABC;
    String tenantName = tenant.getValue();
    ComponentSyncRequest request = new ComponentSyncRequest();
    request.setComponentType("application");
    RuntimeException error = new RuntimeException("Service error");

    when(mockServiceManagerService.syncServices(any(Tenant.class)))
        .thenReturn(Completable.error(error));

    try (MockedStatic<ResponseWrapper> mockedResponseWrapper =
        Mockito.mockStatic(ResponseWrapper.class)) {
      mockedResponseWrapper
          .when(() -> ResponseWrapper.fromSingle(any(Single.class), anyInt()))
          .thenAnswer(
              invocation -> {
                Single<DefaultSuccessResponse> single = invocation.getArgument(0);
                int statusCode = invocation.getArgument(1);
                return TestResponseWrapper.fromSingle(single, statusCode);
              });

      CompletionStage<Response<DefaultSuccessResponse>> future =
          syncComponents.syncHandler(tenantName, request);

      try {
        future.toCompletableFuture().get();
        Assert.fail("Should have thrown exception");
      } catch (Exception e) {
        Assert.assertNotNull(e);
      }
    }
  }
}
