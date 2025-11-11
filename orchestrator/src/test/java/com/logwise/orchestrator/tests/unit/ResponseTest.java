package com.logwise.orchestrator.tests.unit;

import com.logwise.orchestrator.rest.io.Error;
import com.logwise.orchestrator.rest.io.Response;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Unit tests for Response class. */
public class ResponseTest {

  @Test
  public void testSuccessfulResponse_WithData_ReturnsResponseWithData() {
    String testData = "test data";
    Response<String> response = Response.successfulResponse(testData);

    Assert.assertNotNull(response);
    Assert.assertEquals(response.getData(), testData);
    Assert.assertNull(response.getError());
    Assert.assertEquals(response.getHttpStatusCode(), 200);
  }

  @Test
  public void testSuccessfulResponse_WithDataAndStatusCode_ReturnsResponseWithStatusCode() {
    String testData = "test data";
    int statusCode = 201;
    Response<String> response = Response.successfulResponse(testData, statusCode);

    Assert.assertNotNull(response);
    Assert.assertEquals(response.getData(), testData);
    Assert.assertNull(response.getError());
    Assert.assertEquals(response.getHttpStatusCode(), statusCode);
  }

  @Test
  public void testErrorResponse_WithError_ReturnsResponseWithError() {
    Error error = Error.of("ERROR_CODE", "Error message");
    Response<?> response = Response.errorResponse(error);

    Assert.assertNotNull(response);
    Assert.assertNull(response.getData());
    Assert.assertEquals(response.getError(), error);
    Assert.assertEquals(response.getHttpStatusCode(), 200); // Default
  }

  @Test
  public void testErrorResponse_WithErrorAndStatusCode_ReturnsResponseWithStatusCode() {
    Error error = Error.of("ERROR_CODE", "Error message");
    int statusCode = 400;
    Response<?> response = Response.errorResponse(error, statusCode);

    Assert.assertNotNull(response);
    Assert.assertNull(response.getData());
    Assert.assertEquals(response.getError(), error);
    Assert.assertEquals(response.getHttpStatusCode(), statusCode);
  }

  @Test
  public void testResponse_WithNullData_HandlesGracefully() {
    Response<String> response = Response.successfulResponse(null);
    Assert.assertNotNull(response);
    Assert.assertNull(response.getData());
  }

  @Test
  public void testResponse_WithComplexObject_WorksCorrectly() {
    TestObject testObject = new TestObject("test", 123);
    Response<TestObject> response = Response.successfulResponse(testObject);

    Assert.assertNotNull(response);
    Assert.assertEquals(response.getData(), testObject);
    Assert.assertEquals(response.getData().getName(), "test");
    Assert.assertEquals(response.getData().getValue(), Integer.valueOf(123));
  }

  private static class TestObject {
    private String name;
    private Integer value;

    public TestObject(String name, Integer value) {
      this.name = name;
      this.value = value;
    }

    public String getName() {
      return name;
    }

    public Integer getValue() {
      return value;
    }
  }
}
