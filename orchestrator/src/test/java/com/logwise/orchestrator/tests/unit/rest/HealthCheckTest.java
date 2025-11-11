package com.logwise.orchestrator.tests.unit.rest;

import com.logwise.orchestrator.rest.healthcheck.HealthCheckException;
import com.logwise.orchestrator.rest.healthcheck.HealthCheckResponse;
import com.logwise.orchestrator.rest.healthcheck.HealthCheckUtil;
import io.reactivex.Single;
import io.vertx.core.json.JsonObject;
import java.util.HashMap;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Unit tests for HealthCheckUtil, HealthCheckResponse, and HealthCheckException. */
public class HealthCheckTest {

  @Test
  public void testHandler_WithAllUp_ReturnsUpStatus() {

    Map<String, Single<JsonObject>> healthChecks = new HashMap<>();
    healthChecks.put("db", Single.just(new JsonObject().put("status", "ok")));
    healthChecks.put("cache", Single.just(new JsonObject().put("status", "ok")));

    HealthCheckResponse response = HealthCheckUtil.handler(healthChecks).blockingGet();

    Assert.assertNotNull(response);
    Assert.assertNotNull(response.getStatus());
    Assert.assertEquals(response.getChecks().size(), 2);

    JsonObject json = response.toJson();
    Assert.assertEquals(json.getJsonObject("data").getString("status"), "UP");
  }

  @Test
  public void testHandler_WithOneDown_ThrowsHealthCheckException() {

    Map<String, Single<JsonObject>> healthChecks = new HashMap<>();
    healthChecks.put("db", Single.just(new JsonObject().put("status", "ok")));
    healthChecks.put("cache", Single.error(new RuntimeException("Cache error")));

    try {
      HealthCheckUtil.handler(healthChecks).blockingGet();
      Assert.fail("Should have thrown HealthCheckException");
    } catch (HealthCheckException e) {
      Assert.assertEquals(e.getHttpStatusCode(), 503);
      Assert.assertEquals(e.getError().getCode(), "HEALTHCHECK_FAILED");

      String message = e.getMessage();
      Assert.assertTrue(message.contains("DOWN"));
    }
  }

  @Test
  public void testHandler_WithAllDown_ThrowsHealthCheckException() {

    Map<String, Single<JsonObject>> healthChecks = new HashMap<>();
    healthChecks.put("db", Single.error(new RuntimeException("DB error")));
    healthChecks.put("cache", Single.error(new RuntimeException("Cache error")));

    try {
      HealthCheckUtil.handler(healthChecks).blockingGet();
      Assert.fail("Should have thrown HealthCheckException");
    } catch (HealthCheckException e) {
      Assert.assertEquals(e.getHttpStatusCode(), 503);
      Assert.assertEquals(e.getError().getCode(), "HEALTHCHECK_FAILED");
    }
  }

  @Test
  public void testHealthCheckResponse_WithUpChecks_ReturnsUpStatus() {

    Map<String, Single<JsonObject>> healthChecks = new HashMap<>();
    healthChecks.put("db", Single.just(new JsonObject().put("status", "ok")));
    healthChecks.put("cache", Single.just(new JsonObject().put("status", "ok")));

    HealthCheckResponse response = HealthCheckUtil.handler(healthChecks).blockingGet();

    Assert.assertNotNull(response.getStatus());
    Assert.assertEquals(response.getChecks().size(), 2);
    JsonObject json = response.toJson();
    Assert.assertEquals(json.getJsonObject("data").getString("status"), "UP");
  }

  @Test
  public void testHealthCheckResponse_WithDownCheck_ThrowsHealthCheckException() {

    Map<String, Single<JsonObject>> healthChecks = new HashMap<>();
    healthChecks.put("db", Single.just(new JsonObject().put("status", "ok")));
    healthChecks.put("cache", Single.error(new RuntimeException("Error")));

    try {
      HealthCheckUtil.handler(healthChecks).blockingGet();
      Assert.fail("Should have thrown HealthCheckException");
    } catch (HealthCheckException e) {
      Assert.assertEquals(e.getHttpStatusCode(), 503);
      Assert.assertEquals(e.getError().getCode(), "HEALTHCHECK_FAILED");

      String message = e.getMessage();
      Assert.assertTrue(message.contains("DOWN"));
    }
  }

  @Test
  public void testHealthCheckResponse_ToJson_ReturnsJsonObject() {

    Map<String, Single<JsonObject>> healthChecks = new HashMap<>();
    healthChecks.put("db", Single.just(new JsonObject().put("status", "ok")));

    HealthCheckResponse response = HealthCheckUtil.handler(healthChecks).blockingGet();
    JsonObject json = response.toJson();

    Assert.assertNotNull(json);
    Assert.assertTrue(json.containsKey("data"));
  }

  @Test
  public void testHealthCheckException_Constructor_SetsErrorAndStatusCode() {

    HealthCheckException exception = new HealthCheckException("Health check failed");

    Assert.assertEquals(exception.getHttpStatusCode(), 503);
    Assert.assertEquals(exception.getError().getCode(), "HEALTHCHECK_FAILED");
    Assert.assertEquals(exception.getError().getMessage(), "healthcheck failed");
    Assert.assertEquals(exception.getMessage(), "Health check failed");
  }

  @Test
  public void testHealthCheckException_ToString_ReturnsMessage() {

    HealthCheckException exception = new HealthCheckException("Test message");

    String result = exception.toString();

    Assert.assertEquals(result, "Test message");
  }
}
