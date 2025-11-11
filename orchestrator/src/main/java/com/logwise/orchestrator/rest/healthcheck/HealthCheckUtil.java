package com.logwise.orchestrator.rest.healthcheck;

import com.logwise.orchestrator.common.util.MapUtils;
import io.reactivex.Single;
import io.vertx.core.json.JsonObject;
import java.util.Arrays;
import java.util.Map;
import java.util.function.BiFunction;
import lombok.val;

public class HealthCheckUtil {
  // TODO: Add git revision, serving host name, app name

  /** Helper for implementing /healthCheck route. */
  public static Single<HealthCheckResponse> handler(Map<String, Single<JsonObject>> healthChecks) {
    BiFunction<String, Single<JsonObject>, Single<HealthCheckResponse.Check>> listWithType =
        (type, single) ->
            single
                .map(result -> new HealthCheckResponse.Check(type, result))
                .onErrorReturn(err -> new HealthCheckResponse.Check(type, err));
    val healthChecksWithType = MapUtils.mapToList(listWithType, healthChecks);

    Single<HealthCheckResponse> finalHealthChecks =
        Single.zip(
            healthChecksWithType,
            checks ->
                new HealthCheckResponse(
                    Arrays.asList(
                        Arrays.copyOf(checks, checks.length, HealthCheckResponse.Check[].class))));

    return finalHealthChecks.map(
        response -> {
          if (response.getStatus().equals(HealthCheckResponse.Status.DOWN)) {
            // Given the limitation of out current d11-rest implementation, a hacky way to make it
            // throw 500 error with given body
            throw new HealthCheckException(String.valueOf(response.toJson()));
          } else {
            return response;
          }
        });
  }
}
