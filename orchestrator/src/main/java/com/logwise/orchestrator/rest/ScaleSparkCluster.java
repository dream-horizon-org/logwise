package com.logwise.orchestrator.rest;

import com.google.inject.Inject;
import com.logwise.orchestrator.constant.ApplicationConstants;
import com.logwise.orchestrator.dto.request.ScaleSparkClusterRequest;
import com.logwise.orchestrator.dto.response.DefaultErrorResponse;
import com.logwise.orchestrator.dto.response.DefaultSuccessResponse;
import com.logwise.orchestrator.enums.Tenant;
import com.logwise.orchestrator.rest.io.Response;
import com.logwise.orchestrator.service.SparkService;
import io.reactivex.Completable;
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
@Path("/scale-spark-cluster")
@Tag(name = "Spark", description = "Spark job management operations")
public class ScaleSparkCluster {
  private final SparkService sparkService;

  @POST
  @Consumes(MediaType.WILDCARD)
  @Produces(MediaType.APPLICATION_JSON)
  @Timeout(60000)
  @Operation(
      summary = "Scale Spark cluster",
      description =
          "Scales the Spark cluster with specified up/down scale configuration and stage history")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully scaled spark cluster",
            content = @Content(schema = @Schema(implementation = DefaultSuccessResponse.class))),
        @ApiResponse(
            responseCode = "500",
            description = "Error occurred while processing the request",
            content = @Content(schema = @Schema(implementation = DefaultErrorResponse.class)))
      })
  public CompletionStage<Response<DefaultSuccessResponse>> handle(
      @Parameter(description = "Tenant name identifier", required = true, example = "ABC")
          @NotNull(message = ApplicationConstants.HEADER_TENANT_NAME + " header is missing")
          @HeaderParam(ApplicationConstants.HEADER_TENANT_NAME)
          String tenantName,
      @RequestBody(
              description =
                  "Spark cluster scaling configuration with up/down scale settings and stage history",
              required = true,
              content = @Content(schema = @Schema(implementation = ScaleSparkClusterRequest.class)))
          @Valid
          ScaleSparkClusterRequest request) {

    Tenant tenant = Tenant.fromValue(tenantName);
    request.getSparkStageHistory().setTenant(tenant.getValue());

    log.info("request here is {}", request);
    if (sparkService == null) {
      log.info("sparkService is null");
    }
    sparkService
        .insertSparkStageHistory(request.getSparkStageHistory())
        .andThen(
            Completable.defer(
                () ->
                    sparkService.scaleSpark(
                        tenant, request.getEnableUpScale(), request.getEnableDownScale())))
        .subscribe();

    CompletableFuture<Response<DefaultSuccessResponse>> future = new CompletableFuture<>();
    DefaultSuccessResponse response =
        DefaultSuccessResponse.builder()
            .message("Successfully scaled spark cluster for tenant: " + tenantName)
            .build();
    future.complete(Response.successfulResponse(response, HttpStatus.SC_OK));
    return future;
  }
}
