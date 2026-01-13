package com.logwise.orchestrator.rest;

import com.google.inject.Inject;
import com.logwise.orchestrator.constant.ApplicationConstants;
import com.logwise.orchestrator.dto.request.ScaleKafkaPartitionsRequest;
import com.logwise.orchestrator.dto.response.DefaultErrorResponse;
import com.logwise.orchestrator.dto.response.ScaleKafkaPartitionsResponse;
import com.logwise.orchestrator.enums.Tenant;
import com.logwise.orchestrator.rest.io.Response;
import com.logwise.orchestrator.service.KafkaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
@RequiredArgsConstructor(onConstructor = @__(@Inject))
@Path("/kafka/scale-partitions")
@Tag(name = "Kafka", description = "Kafka partition scaling operations")
public class ScaleKafkaPartitions {
  private final KafkaService kafkaService;

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Scale Kafka partitions",
      description = "Scales Kafka topic partitions based on metrics, lag, and size thresholds")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully scaled Kafka partitions",
            content =
                @Content(schema = @Schema(implementation = ScaleKafkaPartitionsResponse.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request",
            content = @Content(schema = @Schema(implementation = DefaultErrorResponse.class))),
        @ApiResponse(
            responseCode = "500",
            description = "Error occurred while processing the request",
            content = @Content(schema = @Schema(implementation = DefaultErrorResponse.class)))
      })
  public CompletionStage<Response<ScaleKafkaPartitionsResponse>> scalePartitions(
      @Parameter(description = "Tenant name identifier", required = true)
          @NotNull(message = ApplicationConstants.HEADER_TENANT_NAME + " header is missing")
          @HeaderParam(ApplicationConstants.HEADER_TENANT_NAME)
          String tenantName,
      @RequestBody(
              description = "Kafka partition scaling configuration (optional)",
              content =
                  @Content(schema = @Schema(implementation = ScaleKafkaPartitionsRequest.class)))
          @Valid
          ScaleKafkaPartitionsRequest request) {
    log.info("Received request to scale Kafka partitions for tenant: {}", tenantName);

    if (request == null) {
      request = new ScaleKafkaPartitionsRequest();
    }

    CompletableFuture<Response<ScaleKafkaPartitionsResponse>> future = new CompletableFuture<>();
    List<String> warnings = new ArrayList<>();
    List<String> errors = new ArrayList<>();

    try {
      Tenant tenant = Tenant.fromValue(tenantName);

      kafkaService
          .scaleKafkaPartitions(tenant)
          .subscribe(
              scalingDecisions -> {
                ScaleKafkaPartitionsResponse response =
                    ScaleKafkaPartitionsResponse.builder()
                        .success(true)
                        .message("Successfully scaled Kafka partitions for tenant: " + tenantName)
                        .topicsScaled(scalingDecisions.size())
                        .scalingDecisions(
                            scalingDecisions != null ? scalingDecisions : Collections.emptyList())
                        .warnings(warnings)
                        .errors(errors)
                        .build();
                future.complete(Response.successfulResponse(response, HttpStatus.SC_OK));
              },
              throwable -> {
                log.error("Error scaling Kafka partitions for tenant: {}", tenantName, throwable);
                errors.add(throwable.getMessage());
                ScaleKafkaPartitionsResponse response =
                    ScaleKafkaPartitionsResponse.builder()
                        .success(false)
                        .message("Failed to scale Kafka partitions: " + throwable.getMessage())
                        .topicsScaled(0)
                        .scalingDecisions(Collections.emptyList())
                        .warnings(warnings)
                        .errors(errors)
                        .build();
                future.complete(
                    Response.successfulResponse(response, HttpStatus.SC_INTERNAL_SERVER_ERROR));
              });
    } catch (Exception e) {
      log.error("Error processing scale request for tenant: {}", tenantName, e);
      errors.add(e.getMessage());
      ScaleKafkaPartitionsResponse response =
          ScaleKafkaPartitionsResponse.builder()
              .success(false)
              .message("Error processing request: " + e.getMessage())
              .topicsScaled(0)
              .scalingDecisions(Collections.emptyList())
              .warnings(warnings)
              .errors(errors)
              .build();
      future.complete(Response.successfulResponse(response, HttpStatus.SC_BAD_REQUEST));
    }

    return future;
  }
}
