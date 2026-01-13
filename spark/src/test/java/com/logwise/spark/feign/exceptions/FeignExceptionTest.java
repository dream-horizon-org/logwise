package com.logwise.spark.feign.exceptions;

import static org.testng.Assert.*;

import org.testng.annotations.Test;

public class FeignExceptionTest {

  @Test
  public void testFeignClientException_WithValidParams_CreatesException() {
    FeignClientException exception = new FeignClientException(500, "Error body", "Error reason");

    assertNotNull(exception);
    assertTrue(exception.getMessage().contains("500"));
    assertTrue(exception.getMessage().contains("Error body"));
    assertTrue(exception.getMessage().contains("Error reason"));
  }

  @Test
  public void testFeignClientException_WithNullBody_HandlesGracefully() {
    FeignClientException exception = new FeignClientException(500, null, "Error reason");

    assertNotNull(exception);
    assertTrue(exception.getMessage().contains("500"));
    assertTrue(exception.getMessage().contains("null"));
  }

  @Test
  public void testFeignClientException_WithNullReason_HandlesGracefully() {
    FeignClientException exception = new FeignClientException(500, "Error body", null);

    assertNotNull(exception);
    assertTrue(exception.getMessage().contains("500"));
    assertTrue(exception.getMessage().contains("Error body"));
  }

  @Test
  public void testClientErrorException_WithValidParams_CreatesException() {
    ClientErrorException exception = new ClientErrorException(400, "Bad request", "Invalid input");

    assertNotNull(exception);
    assertTrue(exception.getMessage().contains("400"));
    assertTrue(exception.getMessage().contains("Bad request"));
    assertTrue(exception.getMessage().contains("Invalid input"));
  }

  @Test
  public void testClientErrorException_WithNullBody_HandlesGracefully() {
    ClientErrorException exception = new ClientErrorException(401, null, "Unauthorized");

    assertNotNull(exception);
    assertTrue(exception.getMessage().contains("401"));
    assertTrue(exception.getMessage().contains("null"));
  }

  @Test
  public void testServerErrorException_WithValidParams_CreatesException() {
    ServerErrorException exception = new ServerErrorException(500, "Server error", "Internal error");

    assertNotNull(exception);
    assertTrue(exception.getMessage().contains("500"));
    assertTrue(exception.getMessage().contains("Server error"));
    assertTrue(exception.getMessage().contains("Internal error"));
  }

  @Test
  public void testServerErrorException_WithNullBody_HandlesGracefully() {
    ServerErrorException exception = new ServerErrorException(503, null, "Service unavailable");

    assertNotNull(exception);
    assertTrue(exception.getMessage().contains("503"));
    assertTrue(exception.getMessage().contains("null"));
  }

  @Test
  public void testClientErrorException_WithDifferentStatusCodes_FormatsCorrectly() {
    ClientErrorException exception403 = new ClientErrorException(403, "body", "reason");
    ClientErrorException exception404 = new ClientErrorException(404, "body", "reason");

    assertTrue(exception403.getMessage().contains("403"));
    assertTrue(exception404.getMessage().contains("404"));
  }

  @Test
  public void testServerErrorException_WithDifferentStatusCodes_FormatsCorrectly() {
    ServerErrorException exception501 = new ServerErrorException(501, "body", "reason");
    ServerErrorException exception504 = new ServerErrorException(504, "body", "reason");

    assertTrue(exception501.getMessage().contains("501"));
    assertTrue(exception504.getMessage().contains("504"));
  }
}

