package com.dream11.logcentralorchestrator.rest;

import com.dream11.logcentralorchestrator.common.util.CompletableFutureUtils;
import com.dream11.logcentralorchestrator.dao.HealthCheckDao;
import com.dream11.logcentralorchestrator.rest.healthcheck.HealthCheckResponse;
import com.dream11.logcentralorchestrator.rest.healthcheck.HealthCheckUtil;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import io.reactivex.Single;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.vertx.core.json.JsonObject;
import java.util.concurrent.CompletionStage;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({@Inject}))
@Path("/healthcheck")
@Tag(name = "Health Check", description = "Health check operations")
public class HealthCheck {

  final HealthCheckDao healthCheckDao;

  @GET
  @Consumes(MediaType.WILDCARD)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Health check endpoint",
      description = "Checks the health of the application and its dependencies")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Health check successful",
            content = @Content(schema = @Schema(implementation = HealthCheckResponse.class))),
        @ApiResponse(
            responseCode = "500",
            description = "Health check failed",
            content = @Content(schema = @Schema(implementation = HealthCheckResponse.class)))
      })
  public CompletionStage<HealthCheckResponse> healthcheck() {
    val map =
        ImmutableMap.<String, Single<JsonObject>>builder()
            .put("mysql", healthCheckDao.mysqlHealthCheck())
            .build();
    return HealthCheckUtil.handler(map).to(CompletableFutureUtils::fromSingle);
  }
}
