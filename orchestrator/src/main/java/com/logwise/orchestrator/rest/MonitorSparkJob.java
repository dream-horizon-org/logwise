package com.logwise.orchestrator.rest;

import com.google.inject.Inject;
import com.logwise.orchestrator.constant.ApplicationConstants;
import com.logwise.orchestrator.dto.request.MonitorSparkJobRequest;
import com.logwise.orchestrator.dto.response.DefaultErrorResponse;
import com.logwise.orchestrator.dto.response.DefaultSuccessResponse;
import com.logwise.orchestrator.enums.Tenant;
import com.logwise.orchestrator.rest.io.Response;
import com.logwise.orchestrator.service.SparkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({@Inject}))
@Path("/monitor-spark-job")
@Tag(name = "Spark", description = "Spark job management operations")
public class MonitorSparkJob {
  private final SparkService sparkService;

  @POST
  @Consumes(MediaType.WILDCARD)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Monitor spark job",
      description = "Initiates monitoring of spark jobs with specified resource configurations")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully initiated spark job monitoring",
            content = @Content(schema = @Schema(implementation = DefaultSuccessResponse.class))),
        @ApiResponse(
            responseCode = "500",
            description = "Error occurred while processing the request",
            content = @Content(schema = @Schema(implementation = DefaultErrorResponse.class)))
      })
  public CompletionStage<Response<DefaultSuccessResponse>> handle(
      @Parameter(description = "Tenant name identifier", required = true)
          @NotNull(message = ApplicationConstants.HEADER_TENANT_NAME + " header is missing")
          @HeaderParam(ApplicationConstants.HEADER_TENANT_NAME)
          String tenantName,
      @RequestBody(
              description = "Spark job monitoring configuration",
              content = @Content(schema = @Schema(implementation = MonitorSparkJobRequest.class)))
          @Valid
          MonitorSparkJobRequest request) {
    log.info("Received request to monitor spark job for tenant: {}", tenantName);
    if (request == null) {
      request = new MonitorSparkJobRequest();
    }

    sparkService
        .monitorSparkJob(
            Tenant.fromValue(tenantName), request.getDriverCores(), request.getDriverMemoryInGb())
        .subscribe();
    CompletableFuture<Response<DefaultSuccessResponse>> future = new CompletableFuture<>();
    DefaultSuccessResponse response =
        DefaultSuccessResponse.builder()
            .message("Successfully monitored the spark job for tenant: " + tenantName)
            .build();
    future.complete(Response.successfulResponse(response, HttpStatus.SC_OK));
    return future;
  }
}
