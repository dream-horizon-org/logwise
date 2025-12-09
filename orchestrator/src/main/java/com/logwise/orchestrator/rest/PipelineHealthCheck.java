package com.logwise.orchestrator.rest;

import com.google.inject.Inject;
import com.logwise.orchestrator.common.util.CompletableFutureUtils;
import com.logwise.orchestrator.enums.Tenant;
import com.logwise.orchestrator.rest.exception.RestException;
import com.logwise.orchestrator.service.PipelineHealthCheckService;
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
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({@Inject}))
@Path("/pipeline/health")
@Tag(name = "Pipeline Health Check", description = "Complete pipeline health check operations")
public class PipelineHealthCheck {

  final PipelineHealthCheckService pipelineHealthCheckService;

  @GET
  @Consumes(MediaType.WILDCARD)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Complete pipeline health check",
      description =
          "Checks the health of the complete log processing pipeline: Vector → Kafka → Spark → S3. "
              + "Returns detailed status for each component and identifies where the pipeline might be broken.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Pipeline health check completed",
            content = @Content(schema = @Schema(implementation = JsonObject.class))),
        @ApiResponse(responseCode = "400", description = "Missing or invalid tenant name"),
        @ApiResponse(responseCode = "500", description = "Pipeline health check failed")
      })
  public CompletionStage<JsonObject> checkPipelineHealth(
      @HeaderParam("X-Tenant-Name") String tenantName) {
    log.info("Received pipeline health check request for tenant: {}", tenantName);

    if (tenantName == null || tenantName.isEmpty()) {
      return CompletableFutureUtils.fromSingle(
          Single.just(
              new JsonObject()
                  .put("status", "ERROR")
                  .put("message", "X-Tenant-Name header is required")));
    }

    try {
      Tenant tenant = Tenant.fromValue(tenantName);
      return pipelineHealthCheckService
          .checkCompletePipeline(tenant)
          .to(CompletableFutureUtils::fromSingle);
    } catch (RestException e) {
      log.error("Invalid tenant name: {}", tenantName, e);
      return CompletableFutureUtils.fromSingle(
          Single.just(
              new JsonObject()
                  .put("status", "ERROR")
                  .put("message", "Invalid tenant name: " + tenantName)));
    }
  }
}
