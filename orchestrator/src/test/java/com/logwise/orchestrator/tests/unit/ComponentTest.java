package com.logwise.orchestrator.tests.unit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.logwise.orchestrator.constants.TestConstants;
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
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/** Unit tests for Component API - syncHandler endpoint. */
@Listeners(com.logwise.orchestrator.listeners.ExtentReportListener.class)
public class ComponentTest extends BaseTest {

  @Mock private ServiceManagerService serviceManagerService;

  private SyncComponents component;

  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    MockitoAnnotations.openMocks(this);
    component = new SyncComponents(serviceManagerService);
  }

  @AfterClass
  public static void tearDownClass() {
    BaseTest.cleanup();
  }

  @Test
  public void testSyncHandler_WithApplicationComponentType_ReturnsSuccessResponse()
      throws Exception {
    ComponentSyncRequest request = new ComponentSyncRequest();
    request.setComponentType(TestConstants.APPLICATION_COMPONENT_TYPE);

    when(serviceManagerService.syncServices(any(Tenant.class))).thenReturn(Completable.complete());

    try (MockedStatic<ResponseWrapper> mockedResponseWrapper =
        Mockito.mockStatic(ResponseWrapper.class)) {
      mockedResponseWrapper
          .when(() -> ResponseWrapper.fromSingle(any(Single.class), eq(200)))
          .thenAnswer(
              invocation -> {
                Single<DefaultSuccessResponse> single = invocation.getArgument(0);
                int statusCode = invocation.getArgument(1);
                return TestResponseWrapper.fromSingle(single, statusCode);
              });

      CompletionStage<Response<DefaultSuccessResponse>> result =
          component.syncHandler(TestConstants.VALID_TENANT_NAME, request);

      Response<DefaultSuccessResponse> response = result.toCompletableFuture().get();
      Assert.assertNotNull(response, "Response should not be null");
      Assert.assertEquals(response.getHttpStatusCode(), 200, "HTTP status code should be 200");
      Assert.assertNull(response.getError(), "Error should be null for successful response");

      DefaultSuccessResponse responseData = response.getData();
      Assert.assertNotNull(responseData, "Response data should not be null");

      Assert.assertTrue(responseData.isSuccess(), "Success field should be true");
      Assert.assertNotNull(responseData.getMessage(), "Response message should not be null");

      String message = responseData.getMessage();
      String expectedMessageFormat =
          "Successfully synced componentType: "
              + TestConstants.APPLICATION_COMPONENT_TYPE
              + " for tenant: "
              + TestConstants.VALID_TENANT_NAME;
      Assert.assertEquals(
          message, expectedMessageFormat, "Response message should match expected format");
      Assert.assertTrue(
          message.contains(TestConstants.APPLICATION_COMPONENT_TYPE),
          "Response message should contain component type");
      Assert.assertTrue(
          message.contains(TestConstants.VALID_TENANT_NAME),
          "Response message should contain tenant name");

      verify(serviceManagerService, times(1)).syncServices(eq(Tenant.ABC));

      mockedResponseWrapper.verify(
          () -> ResponseWrapper.fromSingle(any(Single.class), eq(200)), times(1));
    }
  }

  @Test(expectedExceptions = Exception.class)
  public void testSyncHandler_WhenServiceSyncFails_PropagatesException() throws Exception {
    ComponentSyncRequest request = new ComponentSyncRequest();
    request.setComponentType(TestConstants.APPLICATION_COMPONENT_TYPE);

    RuntimeException error = new RuntimeException("Service sync failed");
    when(serviceManagerService.syncServices(any(Tenant.class)))
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

      CompletionStage<Response<DefaultSuccessResponse>> result =
          component.syncHandler(TestConstants.VALID_TENANT_NAME, request);

      result.toCompletableFuture().get();

      verify(serviceManagerService, times(1)).syncServices(eq(Tenant.ABC));
    }
  }
}
