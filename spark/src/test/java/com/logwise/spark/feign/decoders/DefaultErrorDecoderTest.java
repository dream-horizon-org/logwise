package com.logwise.spark.feign.decoders;

import static org.testng.Assert.*;

import com.logwise.spark.feign.exceptions.ClientErrorException;
import com.logwise.spark.feign.exceptions.FeignClientException;
import com.logwise.spark.feign.exceptions.ServerErrorException;
import feign.Request;
import feign.Response;
import java.util.HashMap;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class DefaultErrorDecoderTest {

  private DefaultErrorDecoder errorDecoder;

  @BeforeMethod
  public void setUp() {
    errorDecoder = new DefaultErrorDecoder();
  }

  @Test
  public void testDecode_WithClientError400_ReturnsClientErrorException() {
    Response response = createResponse(400, "Bad Request", "Invalid input");
    Exception exception = errorDecoder.decode("testMethod", response);

    assertNotNull(exception);
    assertTrue(exception instanceof ClientErrorException);
    assertTrue(exception.getMessage().contains("400"));
  }

  @Test
  public void testDecode_WithClientError401_ReturnsClientErrorException() {
    Response response = createResponse(401, "Unauthorized", "Authentication required");
    Exception exception = errorDecoder.decode("testMethod", response);

    assertNotNull(exception);
    assertTrue(exception instanceof ClientErrorException);
    assertTrue(exception.getMessage().contains("401"));
  }

  @Test
  public void testDecode_WithClientError403_ReturnsClientErrorException() {
    Response response = createResponse(403, "Forbidden", "Access denied");
    Exception exception = errorDecoder.decode("testMethod", response);

    assertNotNull(exception);
    assertTrue(exception instanceof ClientErrorException);
    assertTrue(exception.getMessage().contains("403"));
  }

  @Test
  public void testDecode_WithClientError404_ReturnsClientErrorException() {
    Response response = createResponse(404, "Not Found", "Resource not found");
    Exception exception = errorDecoder.decode("testMethod", response);

    assertNotNull(exception);
    assertTrue(exception instanceof ClientErrorException);
    assertTrue(exception.getMessage().contains("404"));
  }

  @Test
  public void testDecode_WithServerError500_ReturnsServerErrorException() {
    Response response = createResponse(500, "Internal Server Error", "Server error");
    Exception exception = errorDecoder.decode("testMethod", response);

    assertNotNull(exception);
    assertTrue(exception instanceof ServerErrorException);
    assertTrue(exception.getMessage().contains("500"));
  }

  @Test
  public void testDecode_WithServerError502_ReturnsServerErrorException() {
    Response response = createResponse(502, "Bad Gateway", "Gateway error");
    Exception exception = errorDecoder.decode("testMethod", response);

    assertNotNull(exception);
    assertTrue(exception instanceof ServerErrorException);
    assertTrue(exception.getMessage().contains("502"));
  }

  @Test
  public void testDecode_WithServerError503_ReturnsServerErrorException() {
    Response response = createResponse(503, "Service Unavailable", "Service unavailable");
    Exception exception = errorDecoder.decode("testMethod", response);

    assertNotNull(exception);
    assertTrue(exception instanceof ServerErrorException);
    assertTrue(exception.getMessage().contains("503"));
  }

  @Test
  public void testDecode_WithUnknownError_ReturnsFeignClientException() {
    Response response = createResponse(418, "I'm a teapot", "Unknown error");
    Exception exception = errorDecoder.decode("testMethod", response);

    assertNotNull(exception);
    assertTrue(exception instanceof FeignClientException);
    assertTrue(exception.getMessage().contains("418"));
  }

  @Test
  public void testDecode_WithNullBody_HandlesGracefully() {
    Response response = createResponse(500, "Error", null);
    Exception exception = errorDecoder.decode("testMethod", response);

    assertNotNull(exception);
    assertTrue(exception instanceof ServerErrorException);
  }

  @Test
  public void testDecode_WithNullReason_HandlesGracefully() {
    Response response = createResponse(500, null, "Error body");
    Exception exception = errorDecoder.decode("testMethod", response);

    assertNotNull(exception);
    assertTrue(exception instanceof ServerErrorException);
  }

  private Response createResponse(int status, String reason, String body) {
    return Response.builder()
        .status(status)
        .reason(reason != null ? reason : "")
        .headers(new HashMap<>())
        .body(body != null ? body.getBytes() : new byte[0])
        .request(
            Request.create(
                Request.HttpMethod.GET, "http://example.com", new HashMap<>(), null, null, null))
        .build();
  }
}
