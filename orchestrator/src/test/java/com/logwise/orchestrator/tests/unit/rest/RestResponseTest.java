package com.logwise.orchestrator.tests.unit.rest;

import com.logwise.orchestrator.rest.exception.RestError;
import com.logwise.orchestrator.rest.exception.RestException;
import com.logwise.orchestrator.rest.io.Error;
import com.logwise.orchestrator.rest.io.Response;
import com.logwise.orchestrator.rest.io.RestResponse;
import com.logwise.orchestrator.setup.BaseTest;
import io.reactivex.Single;
import java.util.concurrent.CompletionStage;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Unit tests for RestResponse. */
public class RestResponseTest extends BaseTest {

  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
  }

  @Test
  public void testRestHandler_WithSuccess_ReturnsSuccessfulResponse() throws Exception {

    Single<Object> source = Single.just("test-data");

    Single<Response<Object>> result = RestResponse.restHandler().apply(source);
    Response<Object> response = result.blockingGet();

    Assert.assertNotNull(response);
    Assert.assertEquals(response.getData(), "test-data");
    Assert.assertNull(response.getError());
    Assert.assertEquals(response.getHttpStatusCode(), 200);
  }

  @Test
  public void testRestHandler_WithRestException_ReturnsErrorResponse() throws Exception {

    RestException restException =
        new RestException("Test error", Error.of("TEST_ERROR", "Test error message"), 400);
    Single<Object> source = Single.error(restException);

    Single<Response<Object>> result = RestResponse.restHandler().apply(source);
    Response<Object> response = result.blockingGet();

    Assert.assertNotNull(response);
    Assert.assertNull(response.getData());
    Assert.assertNotNull(response.getError());
    Assert.assertEquals(response.getError().getCode(), "TEST_ERROR");
    Assert.assertEquals(response.getError().getMessage(), "Test error message");
    Assert.assertEquals(response.getHttpStatusCode(), 400);
  }

  @Test
  public void testRestHandler_WithNonRestException_ReturnsErrorResponse() throws Exception {

    RuntimeException runtimeException = new RuntimeException("Runtime error");
    Single<Object> source = Single.error(runtimeException);

    Single<Response<Object>> result = RestResponse.restHandler().apply(source);
    Response<Object> response = result.blockingGet();

    Assert.assertNotNull(response);
    Assert.assertNull(response.getData());
    Assert.assertNotNull(response.getError());
    Assert.assertEquals(response.getError().getCode(), "UNKNOWN-EXCEPTION");
    Assert.assertEquals(response.getHttpStatusCode(), 500);
  }

  @Test
  public void testRestHandler_WithRestError_OnError_ReturnsRestException() throws Exception {

    RestError restError =
        new RestError() {
          @Override
          public String getErrorCode() {
            return "CUSTOM_ERROR";
          }

          @Override
          public String getErrorMessage() {
            return "Custom error";
          }

          @Override
          public int getHttpStatusCode() {
            return 500;
          }
        };
    RuntimeException runtimeException = new RuntimeException("Original error");
    Single<Object> source = Single.error(runtimeException);

    Single<Response<Object>> result = RestResponse.restHandler(restError).apply(source);
    Response<Object> response = result.blockingGet();

    Assert.assertNotNull(response);
    Assert.assertNull(response.getData());
    Assert.assertNotNull(response.getError());
    Assert.assertEquals(response.getError().getCode(), "CUSTOM_ERROR");
    Assert.assertEquals(response.getHttpStatusCode(), 500);
  }

  @Test
  public void testRestHandler_WithRestError_OnSuccess_ReturnsSuccessfulResponse() throws Exception {

    RestError restError =
        new RestError() {
          @Override
          public String getErrorCode() {
            return "CUSTOM_ERROR";
          }

          @Override
          public String getErrorMessage() {
            return "Custom error";
          }

          @Override
          public int getHttpStatusCode() {
            return 500;
          }
        };
    Single<Object> source = Single.just("success-data");

    Single<Response<Object>> result = RestResponse.restHandler(restError).apply(source);
    Response<Object> response = result.blockingGet();

    Assert.assertNotNull(response);
    Assert.assertEquals(response.getData(), "success-data");
    Assert.assertNull(response.getError());
    Assert.assertEquals(response.getHttpStatusCode(), 200);
  }

  @Test
  public void testJaxrsRestHandler_WithSuccess_ReturnsCompletionStage() throws Exception {

    Single<Object> source = Single.just("jaxrs-data");
    io.vertx.reactivex.core.Vertx reactiveVertx = BaseTest.getReactiveVertx();

    java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
    @SuppressWarnings("unchecked")
    final Response<Object>[] responseHolder = new Response[1];
    final Throwable[] exceptionHolder = new Throwable[1];

    reactiveVertx
        .getOrCreateContext()
        .runOnContext(
            v -> {
              try {
                CompletionStage<Response<Object>> stage =
                    RestResponse.jaxrsRestHandler().apply(source);
                Response<Object> response = stage.toCompletableFuture().get();
                responseHolder[0] = response;
              } catch (Exception e) {
                exceptionHolder[0] = e;
              } finally {
                latch.countDown();
              }
            });

    latch.await();

    if (exceptionHolder[0] != null) {
      if (exceptionHolder[0] instanceof Exception) {
        throw (Exception) exceptionHolder[0];
      } else {
        throw new Exception(exceptionHolder[0]);
      }
    }

    Response<Object> response = responseHolder[0];

    Assert.assertNotNull(response);
    Assert.assertEquals(response.getData(), "jaxrs-data");
    Assert.assertNull(response.getError());
    Assert.assertEquals(response.getHttpStatusCode(), 200);
  }

  @Test
  public void testJaxrsRestHandler_WithError_CompletesExceptionally() throws Exception {

    RuntimeException error = new RuntimeException("JAXRS error");
    Single<Object> source = Single.error(error);
    io.vertx.reactivex.core.Vertx reactiveVertx = BaseTest.getReactiveVertx();

    java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
    final Throwable[] exceptionHolder = new Throwable[1];
    final boolean[] exceptionThrown = new boolean[1];

    reactiveVertx
        .getOrCreateContext()
        .runOnContext(
            v -> {
              try {
                CompletionStage<Response<Object>> stage =
                    RestResponse.jaxrsRestHandler().apply(source);
                try {
                  stage.toCompletableFuture().get();
                  exceptionThrown[0] = false; // Should have thrown exception
                } catch (java.util.concurrent.ExecutionException e) {
                  Assert.assertNotNull(e.getCause());
                  exceptionThrown[0] = true; // Expected exception was thrown
                }
              } catch (Exception e) {
                exceptionHolder[0] = e;
              } finally {
                latch.countDown();
              }
            });

    latch.await();

    if (exceptionHolder[0] != null) {
      throw new Exception(exceptionHolder[0]);
    }

    Assert.assertTrue(exceptionThrown[0], "Should have thrown ExecutionException");
  }

  @Test
  public void testParseThrowable_WithRestException_ReturnsRestException() {

    RestException restException = new RestException("Test", Error.of("TEST", "Test message"), 400);

    Throwable result = RestResponse.parseThrowable(restException);

    Assert.assertSame(result, restException);
  }

  @Test
  public void testParseThrowable_WithNonRestException_ReturnsRestException() {

    RuntimeException runtimeException = new RuntimeException("Runtime error");

    Throwable result = RestResponse.parseThrowable(runtimeException);

    Assert.assertTrue(result instanceof RestException);
    RestException restException = (RestException) result;
    Assert.assertEquals(restException.getError().getCode(), "UNKNOWN-EXCEPTION");
    Assert.assertEquals(restException.getHttpStatusCode(), 500);
  }

  @Test
  public void testParseThrowable_WithOldErrorKeys_ReturnsRestExceptionWithOldErrorKeys() {

    RuntimeException runtimeException = new RuntimeException("Runtime error");

    Throwable result = RestResponse.parseThrowable(runtimeException, true);

    Assert.assertTrue(result instanceof RestException);
    RestException restException = (RestException) result;
    Assert.assertEquals(restException.getError().getCode(), "UNKNOWN-EXCEPTION");
    Assert.assertEquals(restException.getHttpStatusCode(), 500);
  }

  @Test
  public void testParseThrowable_WithRestExceptionAndOldErrorKeys_ReturnsRestException() {

    RestException restException = new RestException("Test", Error.of("TEST", "Test message"), 400);

    Throwable result = RestResponse.parseThrowable(restException, true);

    Assert.assertSame(result, restException);
  }
}
