package com.logwise.orchestrator.tests.unit;

import com.logwise.orchestrator.rest.exception.RestError;
import com.logwise.orchestrator.rest.exception.RestException;
import com.logwise.orchestrator.rest.io.Error;
import io.vertx.core.json.JsonObject;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Unit tests for RestException class. */
public class RestExceptionTest {

  @Test
  public void testConstructor_WithMessageAndError_CreatesException() {
    Error error = Error.of("CODE", "Message");
    RestException exception = new RestException("Test message", error);

    Assert.assertEquals(exception.getMessage(), "Test message");
    Assert.assertEquals(exception.getError(), error);
    Assert.assertEquals(exception.getHttpStatusCode(), 400); // Default
  }

  @Test
  public void testConstructor_WithMessageErrorAndStatusCode_CreatesException() {
    Error error = Error.of("CODE", "Message");
    RestException exception = new RestException("Test message", error, 404);

    Assert.assertEquals(exception.getMessage(), "Test message");
    Assert.assertEquals(exception.getError(), error);
    Assert.assertEquals(exception.getHttpStatusCode(), 404);
  }

  @Test
  public void testConstructor_WithCauseAndError_CreatesException() {
    Throwable cause = new RuntimeException("Cause");
    Error error = Error.of("CODE", "Message");
    RestException exception = new RestException(cause, error);

    Assert.assertEquals(exception.getCause(), cause);
    Assert.assertEquals(exception.getError(), error);
    Assert.assertEquals(exception.getHttpStatusCode(), 400); // Default
  }

  @Test
  public void testConstructor_WithCauseErrorAndStatusCode_CreatesException() {
    Throwable cause = new RuntimeException("Cause");
    Error error = Error.of("CODE", "Message");
    RestException exception = new RestException(cause, error, 500);

    Assert.assertEquals(exception.getCause(), cause);
    Assert.assertEquals(exception.getError(), error);
    Assert.assertEquals(exception.getHttpStatusCode(), 500);
  }

  @Test
  public void testConstructor_WithRestError_CreatesException() {
    RestError restError =
        new RestError() {
          @Override
          public String getErrorCode() {
            return "CODE";
          }

          @Override
          public String getErrorMessage() {
            return "Message";
          }

          @Override
          public int getHttpStatusCode() {
            return 400;
          }
        };
    RestException exception = new RestException(restError);

    Assert.assertEquals(exception.getError().getCode(), "CODE");
    Assert.assertEquals(exception.getError().getMessage(), "Message");
    Assert.assertEquals(exception.getHttpStatusCode(), 400);
  }

  @Test
  public void testConstructor_WithRestErrorAndCause_CreatesException() {
    Throwable cause = new RuntimeException("Cause");
    RestError restError =
        new RestError() {
          @Override
          public String getErrorCode() {
            return "CODE";
          }

          @Override
          public String getErrorMessage() {
            return "Message";
          }

          @Override
          public int getHttpStatusCode() {
            return 500;
          }
        };
    RestException exception = new RestException(restError, cause);

    Assert.assertEquals(exception.getCause(), cause);
    Assert.assertEquals(exception.getError().getCode(), "CODE");
    Assert.assertEquals(exception.getHttpStatusCode(), 500);
  }

  @Test
  public void testToJson_ReturnsValidJsonObject() {
    Error error = Error.of("CODE", "Message");
    RestException exception = new RestException("Test message", error, 400);

    JsonObject json = exception.toJson();

    Assert.assertNotNull(json);
    Assert.assertTrue(json.containsKey("error"));
    JsonObject errorJson = json.getJsonObject("error");
    Assert.assertEquals(errorJson.getString("code"), "CODE");
    Assert.assertEquals(errorJson.getString("message"), "Message");
    Assert.assertEquals(errorJson.getString("cause"), "Test message");
  }

  @Test
  public void testToJson_WithOldErrorKeys_ReturnsJsonWithOldKeys() {
    Error error = Error.of("CODE", "Message");
    RestException exception = new RestException("Test message", error, 400);
    exception.setOldErrorKeys(true);

    JsonObject json = exception.toJson();

    Assert.assertNotNull(json);
    JsonObject errorJson = json.getJsonObject("error");
    Assert.assertTrue(errorJson.containsKey("MsgCode"));
    Assert.assertTrue(errorJson.containsKey("MsgActionTitle"));
    Assert.assertTrue(errorJson.containsKey("MsgTitle"));
    Assert.assertTrue(errorJson.containsKey("MsgText"));
  }

  @Test
  public void testToString_ReturnsJsonString() {
    Error error = Error.of("CODE", "Message");
    RestException exception = new RestException("Test message", error, 400);

    String jsonString = exception.toString();

    Assert.assertNotNull(jsonString);
    Assert.assertTrue(jsonString.contains("error"));
    Assert.assertTrue(jsonString.contains("CODE"));
  }

  @Test
  public void testSetOldErrorKeys_SetsOldErrorKeys() {
    Error error = Error.of("CODE", "Message");
    RestException exception = new RestException("Test message", error);

    exception.setOldErrorKeys(true);

    Assert.assertNotNull(exception);
  }

  @Test
  public void testMergeIntoOldErrorMap_MergesMap() {
    java.util.Map<String, String> map = new java.util.HashMap<>();
    map.put("CODE1", "Message1");
    map.put("CODE2", "Message2");

    RestException.mergeIntoOldErrorMap(map);

    Assert.assertNotNull(map);
  }
}
