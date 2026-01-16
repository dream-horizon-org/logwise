package com.logwise.orchestrator.tests.unit.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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
import java.util.ArrayList;
import java.util.concurrent.CompletionStage;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class GetServiceDetailsTest extends BaseTest {

  private GetServiceDetails getServiceDetails;
  private ServiceManagerService mockServiceManagerService;

  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    TestResponseWrapper.init(vertx);
    mockServiceManagerService = mock(ServiceManagerService.class);
    getServiceDetails = new GetServiceDetails(mockServiceManagerService);
  }

  @Test
  public void testHandle_WithValidTenant_ReturnsServiceDetails() throws Exception {
    String tenantName = "ABC";
    GetServiceDetailsResponse mockResponse =
        GetServiceDetailsResponse.builder().serviceDetails(new ArrayList<>()).build();

    when(mockServiceManagerService.getServiceDetailsFromCache(any(Tenant.class)))
        .thenReturn(Single.just(mockResponse));

    try (MockedStatic<ResponseWrapper> mockedWrapper = Mockito.mockStatic(ResponseWrapper.class)) {
      mockedWrapper
          .when(() -> ResponseWrapper.fromSingle(any(Single.class), anyInt()))
          .thenAnswer(
              invocation -> {
                Single<GetServiceDetailsResponse> single = invocation.getArgument(0);
                int statusCode = invocation.getArgument(1);
                return TestResponseWrapper.fromSingle(single, statusCode);
              });

      CompletionStage<Response<GetServiceDetailsResponse>> result =
          getServiceDetails.handle(tenantName);

      Thread.sleep(100); // Wait for async processing
      Response<GetServiceDetailsResponse> response = result.toCompletableFuture().get();
      Assert.assertNotNull(response);
      Assert.assertNotNull(response.getData());
      verify(mockServiceManagerService, times(1)).getServiceDetailsFromCache(Tenant.ABC);
    }
  }

  @Test
  public void testHandle_WithInvalidTenant_ThrowsException() {
    String tenantName = "INVALID";

    try {
      getServiceDetails.handle(tenantName);
      Assert.fail("Should throw exception for invalid tenant");
    } catch (Exception e) {
      Assert.assertNotNull(e);
    }
  }
}
