package com.logwise.orchestrator.rest;

import com.google.inject.Inject;
import com.logwise.orchestrator.constant.ApplicationConstants;
import com.logwise.orchestrator.dto.response.DefaultErrorResponse;
import com.logwise.orchestrator.dto.response.LogSyncDelayResponse;
import com.logwise.orchestrator.enums.Tenant;
import com.logwise.orchestrator.rest.io.Response;
import com.logwise.orchestrator.service.MetricsService;
import com.logwise.orchestrator.util.ResponseWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.concurrent.CompletionStage;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({@Inject}))
@Path("/metric/sync-delay")
@Tag(name = "Metrics", description = "Metrics operations")
public class MetricSyncDelay {
  private final MetricsService metricsService;

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Get object-store log sync delay",
      description = "Returns delay (in minutes) for app logs.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully computed delay metrics",
            content = @Content(schema = @Schema(implementation = LogSyncDelayResponse.class))),
        @ApiResponse(
            responseCode = "500",
            description = "Error occurred while computing delay metrics",
            content = @Content(schema = @Schema(implementation = DefaultErrorResponse.class)))
      })
  public CompletionStage<Response<LogSyncDelayResponse>> getObjectStoreSyncDelay(
      @Parameter(description = "Tenant name identifier", required = true)
          @NotNull(message = ApplicationConstants.HEADER_TENANT_NAME + " header is missing")
          @HeaderParam(ApplicationConstants.HEADER_TENANT_NAME)
          String tenantName) {
    Tenant tenant = Tenant.fromValue(tenantName);
    return metricsService
        .computeLogSyncDelay(tenant)
        .to(response -> ResponseWrapper.fromSingle(response, HttpStatus.SC_OK));
  }
}
