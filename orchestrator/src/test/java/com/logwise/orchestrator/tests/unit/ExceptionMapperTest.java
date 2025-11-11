package com.logwise.orchestrator.tests.unit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.logwise.orchestrator.rest.exception.GenericExceptionMapper;
import com.logwise.orchestrator.rest.exception.RestException;
import com.logwise.orchestrator.rest.exception.ValidationExceptionMapper;
import com.logwise.orchestrator.rest.exception.WebApplicationExceptionMapper;
import com.logwise.orchestrator.rest.io.Error;
import com.logwise.orchestrator.rest.io.RestResponse;
import javax.validation.ValidationException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import org.mockito.MockedStatic;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Unit tests for exception mapper package (GenericExceptionMapper, ValidationExceptionMapper,
 * WebApplicationExceptionMapper).
 */
public class ExceptionMapperTest {

  private GenericExceptionMapper genericMapper;
  private ValidationExceptionMapper validationMapper;
  private WebApplicationExceptionMapper webApplicationMapper;

  @BeforeMethod
  public void setUp() {
    genericMapper = new GenericExceptionMapper();
    validationMapper = new ValidationExceptionMapper();
    webApplicationMapper = new WebApplicationExceptionMapper();
  }

  @Test
  public void
      testGenericExceptionMapper_ToResponse_WithRestException_ReturnsResponseWithStatusCode() {

    RestException restException = new RestException("Test error", Error.of("CODE", "Message"), 400);

    try (MockedStatic<RestResponse> mockedRestResponse =
        org.mockito.Mockito.mockStatic(RestResponse.class)) {
      mockedRestResponse
          .when(() -> RestResponse.parseThrowable(any(Throwable.class)))
          .thenReturn(restException);

      Response response = genericMapper.toResponse(restException);

      Assert.assertNotNull(response);
      Assert.assertEquals(response.getStatus(), 400);
      Assert.assertNotNull(response.getEntity());
    }
  }

  @Test
  public void testGenericExceptionMapper_ToResponse_WithNonRestException_ReturnsResponseWith500() {

    RuntimeException runtimeException = new RuntimeException("Test error");
    RestException restException =
        new RestException(runtimeException, Error.of("UNKNOWN-EXCEPTION", "Test error"), 500);

    try (MockedStatic<RestResponse> mockedRestResponse =
        org.mockito.Mockito.mockStatic(RestResponse.class)) {
      mockedRestResponse
          .when(() -> RestResponse.parseThrowable(any(Throwable.class)))
          .thenReturn(restException);

      Response response = genericMapper.toResponse(runtimeException);

      Assert.assertNotNull(response);
      Assert.assertEquals(response.getStatus(), 500);
      Assert.assertNotNull(response.getEntity());
    }
  }

  @Test
  public void testGenericExceptionMapper_ToResponse_WithNullThrowable_HandlesGracefully() {

    RestException restException =
        new RestException((Throwable) null, Error.of("UNKNOWN-EXCEPTION", "null"), 500);

    try (MockedStatic<RestResponse> mockedRestResponse =
        org.mockito.Mockito.mockStatic(RestResponse.class)) {
      mockedRestResponse
          .when(() -> RestResponse.parseThrowable(org.mockito.ArgumentMatchers.isNull()))
          .thenReturn(restException);

      mockedRestResponse
          .when(() -> RestResponse.parseThrowable(any(Throwable.class)))
          .thenAnswer(
              invocation -> {
                Throwable t = invocation.getArgument(0);
                if (t == null) {
                  return restException;
                }
                return RestResponse.parseThrowable(t);
              });

      Response response = genericMapper.toResponse((Throwable) null);

      Assert.assertNotNull(response);
      Assert.assertEquals(response.getStatus(), 500);
    }
  }

  @Test
  public void testValidationExceptionMapper_ToResponse_WithValidationException_ReturnsBadRequest() {

    ValidationException validationException = new ValidationException("Constraint violation");

    Response response = validationMapper.toResponse(validationException);

    Assert.assertNotNull(response);
    Assert.assertEquals(response.getStatus(), Response.Status.BAD_REQUEST.getStatusCode());
    Assert.assertNotNull(response.getEntity());

    String entity = (String) response.getEntity();
    Assert.assertTrue(entity.contains("INVALID_REQUEST"));
    Assert.assertTrue(entity.contains("Constraint Violation"));
  }

  @Test
  public void testValidationExceptionMapper_ToResponse_WithNullMessage_HandlesGracefully() {

    ValidationException validationException = new ValidationException((String) null);

    Response response = validationMapper.toResponse(validationException);

    Assert.assertNotNull(response);
    Assert.assertEquals(response.getStatus(), Response.Status.BAD_REQUEST.getStatusCode());
    Assert.assertNotNull(response.getEntity());
  }

  @Test
  public void testValidationExceptionMapper_ToResponse_WithEmptyMessage_HandlesGracefully() {

    ValidationException validationException = new ValidationException("");

    Response response = validationMapper.toResponse(validationException);

    Assert.assertNotNull(response);
    Assert.assertEquals(response.getStatus(), Response.Status.BAD_REQUEST.getStatusCode());
    Assert.assertNotNull(response.getEntity());
  }

  @Test
  public void
      testWebApplicationExceptionMapper_ToResponse_WithRestExceptionAsCause_ReturnsRestExceptionResponse() {

    RestException restException = new RestException("Test error", Error.of("CODE", "Message"), 400);
    WebApplicationException webException = new WebApplicationException(restException);

    Response response = webApplicationMapper.toResponse(webException);

    Assert.assertNotNull(response);
    Assert.assertEquals(response.getStatus(), 400);
    Assert.assertNotNull(response.getEntity());
  }

  @Test
  public void
      testWebApplicationExceptionMapper_ToResponse_WithNonRestExceptionCause_ReturnsGenericErrorResponse() {

    RuntimeException runtimeException = new RuntimeException("Test error");
    WebApplicationException webException =
        new WebApplicationException(runtimeException, Response.Status.INTERNAL_SERVER_ERROR);

    Response response = webApplicationMapper.toResponse(webException);

    Assert.assertNotNull(response);
    Assert.assertEquals(
        response.getStatus(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    Assert.assertNotNull(response.getEntity());

    String entity = (String) response.getEntity();
    Assert.assertTrue(entity.contains("UNKNOWN_EXCEPTION"));
  }

  @Test
  public void
      testWebApplicationExceptionMapper_ToResponse_WithNoCause_ReturnsGenericErrorResponse() {

    WebApplicationException webException = new WebApplicationException(Response.Status.BAD_REQUEST);

    Response response = webApplicationMapper.toResponse(webException);

    Assert.assertNotNull(response);
    Assert.assertEquals(response.getStatus(), Response.Status.BAD_REQUEST.getStatusCode());
    Assert.assertNotNull(response.getEntity());
  }

  @Test
  public void testWebApplicationExceptionMapper_ToResponse_WithNullCause_HandlesGracefully() {

    WebApplicationException webException =
        new WebApplicationException((Throwable) null, Response.Status.INTERNAL_SERVER_ERROR);

    Response response = webApplicationMapper.toResponse(webException);

    Assert.assertNotNull(response);
    Assert.assertEquals(
        response.getStatus(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    Assert.assertNotNull(response.getEntity());
  }
}
