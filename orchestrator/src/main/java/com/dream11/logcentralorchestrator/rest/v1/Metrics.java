package com.dream11.logcentralorchestrator.rest.v1;

import com.dream11.logcentralorchestrator.constant.ApplicationConstants;
import com.dream11.logcentralorchestrator.dto.response.DefaultErrorResponse;
import com.dream11.logcentralorchestrator.dto.response.LogSyncDelayResponse;
import com.dream11.logcentralorchestrator.enums.Tenant;
import com.dream11.logcentralorchestrator.rest.io.Response;
import com.dream11.logcentralorchestrator.service.MetricsService;
import com.dream11.logcentralorchestrator.util.ResponseWrapper;
import com.google.inject.Inject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.concurrent.CompletionStage;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({@Inject}))
@Path(ApplicationConstants.API_VERSION_V1 + "/metric")
@Tag(name = "Metrics", description = "Metrics operations")
public class Metrics {
  private final MetricsService metricsService;

  @GET
  @Path("/sync-delay")
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
      @Parameter(description = "Tenant name identifier", required = true, example = "dream11")
          @NotNull(message = ApplicationConstants.HEADER_TENANT_NAME + " header is missing")
          @HeaderParam(ApplicationConstants.HEADER_TENANT_NAME)
          String tenantName) {
    Tenant tenant = Tenant.fromValue(tenantName);
    return metricsService
        .computeLogSyncDelay(tenant)
        .to(response -> ResponseWrapper.fromSingle(response, HttpStatus.SC_OK));
  }
}
