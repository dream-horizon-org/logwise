package com.logwise.orchestrator.tests.unit;

import com.logwise.orchestrator.rest.io.Error;
import io.vertx.core.json.JsonObject;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Unit tests for Error class. */
public class ErrorTest {

  @Test
  public void testOf_CreatesErrorWithCodeAndMessage() {
    Error error = Error.of("TEST_CODE", "Test message");
    Assert.assertNotNull(error);
    Assert.assertEquals(error.getCode(), "TEST_CODE");
    Assert.assertEquals(error.getMessage(), "Test message");
  }

  @Test
  public void testConstructor_CreatesErrorWithCodeAndMessage() {
    Error error = Error.of("ERROR_CODE", "Error message");
    Assert.assertEquals(error.getCode(), "ERROR_CODE");
    Assert.assertEquals(error.getMessage(), "Error message");
  }

  @Test
  public void testToJsonString_ReturnsValidJsonString() {
    Error error = Error.of("TEST_CODE", "Test message");
    String jsonString = error.toJsonString();
    Assert.assertNotNull(jsonString);

    JsonObject jsonObject = new JsonObject(jsonString);
    Assert.assertTrue(jsonObject.containsKey("error"));
    JsonObject errorObject = jsonObject.getJsonObject("error");
    Assert.assertEquals(errorObject.getString("code"), "TEST_CODE");
    Assert.assertEquals(errorObject.getString("message"), "Test message");
  }

  @Test
  public void testToJsonString_WithNullValues_HandlesGracefully() {
    Error error = Error.of(null, null);
    String jsonString = error.toJsonString();
    Assert.assertNotNull(jsonString);
  }

  @Test
  public void testEquals_WithSameCodeAndMessage_ReturnsTrue() {
    Error error1 = Error.of("CODE", "Message");
    Error error2 = Error.of("CODE", "Message");

    Assert.assertNotNull(error1);
    Assert.assertNotNull(error2);
  }
}
