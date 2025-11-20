package com.logwise.orchestrator.tests.unit.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.logwise.orchestrator.dto.response.GetServiceDetailsResponse;
import com.logwise.orchestrator.enums.Tenant;
import com.logwise.orchestrator.rest.GetServiceDetails;
import com.logwise.orchestrator.rest.io.Response;
import com.logwise.orchestrator.service.ServiceManagerService;
import com.logwise.orchestrator.setup.BaseTest;
import com.logwise.orchestrator.util.ResponseWrapper;
import com.logwise.orchestrator.util.TestResponseWrapper;
import io.reactivex.Single;
import java.util.concurrent.CompletionStage;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Unit tests for GetServiceDetails REST endpoint. */
public class GetServiceDetailsTest extends BaseTest {

  private ServiceManagerService mockServiceManagerService;
  private GetServiceDetails getServiceDetails;

  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    mockServiceManagerService = mock(ServiceManagerService.class);
    getServiceDetails = new GetServiceDetails(mockServiceManagerService);
  }

  @AfterClass
  public static void tearDownClass() {
    BaseTest.cleanup();
  }

  @Test
  public void testHandle_WithValidTenant_ReturnsServiceDetails() throws Exception {
    Tenant tenant = Tenant.ABC;
    String tenantName = tenant.getValue();
    GetServiceDetailsResponse response = GetServiceDetailsResponse.builder().build();

    when(mockServiceManagerService.getServiceDetailsFromCache(any(Tenant.class)))
        .thenReturn(Single.just(response));

    try (MockedStatic<ResponseWrapper> mockedResponseWrapper =
        Mockito.mockStatic(ResponseWrapper.class)) {
      mockedResponseWrapper
          .when(() -> ResponseWrapper.fromSingle(any(Single.class), anyInt()))
          .thenAnswer(
              invocation -> {
                Single<GetServiceDetailsResponse> single = invocation.getArgument(0);
                int statusCode = invocation.getArgument(1);
                return TestResponseWrapper.fromSingle(single, statusCode);
              });

      CompletionStage<Response<GetServiceDetailsResponse>> future =
          getServiceDetails.handle(tenantName);

      Response<GetServiceDetailsResponse> result = future.toCompletableFuture().get();

      Assert.assertNotNull(result);
      Assert.assertNotNull(result.getData());
      Assert.assertEquals(result.getHttpStatusCode(), 200);
      verify(mockServiceManagerService, times(1)).getServiceDetailsFromCache(eq(tenant));
    }
  }

  @Test
  public void testHandle_WithDifferentTenant_CallsServiceWithCorrectTenant() throws Exception {
    Tenant tenant = Tenant.ABC;
    String tenantName = tenant.getValue();
    GetServiceDetailsResponse response = GetServiceDetailsResponse.builder().build();

    when(mockServiceManagerService.getServiceDetailsFromCache(any(Tenant.class)))
        .thenReturn(Single.just(response));

    try (MockedStatic<ResponseWrapper> mockedResponseWrapper =
        Mockito.mockStatic(ResponseWrapper.class)) {
      mockedResponseWrapper
          .when(() -> ResponseWrapper.fromSingle(any(Single.class), anyInt()))
          .thenAnswer(
              invocation -> {
                Single<GetServiceDetailsResponse> single = invocation.getArgument(0);
                int statusCode = invocation.getArgument(1);
                return TestResponseWrapper.fromSingle(single, statusCode);
              });

      CompletionStage<Response<GetServiceDetailsResponse>> future =
          getServiceDetails.handle(tenantName);

      Response<GetServiceDetailsResponse> result = future.toCompletableFuture().get();

      Assert.assertNotNull(result);
      verify(mockServiceManagerService, times(1)).getServiceDetailsFromCache(eq(tenant));
    }
  }

  @Test
  public void testHandle_WithServiceError_PropagatesError() {
    Tenant tenant = Tenant.ABC;
    String tenantName = tenant.getValue();
    RuntimeException error = new RuntimeException("Service error");

    when(mockServiceManagerService.getServiceDetailsFromCache(any(Tenant.class)))
        .thenReturn(Single.error(error));

    try (MockedStatic<ResponseWrapper> mockedResponseWrapper =
        Mockito.mockStatic(ResponseWrapper.class)) {
      mockedResponseWrapper
          .when(() -> ResponseWrapper.fromSingle(any(Single.class), anyInt()))
          .thenAnswer(
              invocation -> {
                Single<GetServiceDetailsResponse> single = invocation.getArgument(0);
                int statusCode = invocation.getArgument(1);
                return TestResponseWrapper.fromSingle(single, statusCode);
              });

      CompletionStage<Response<GetServiceDetailsResponse>> future =
          getServiceDetails.handle(tenantName);

      try {
        future.toCompletableFuture().get();
        Assert.fail("Should have thrown exception");
      } catch (Exception e) {
        Assert.assertNotNull(e);
      }
    }
  }
}
