package com.logwise.orchestrator.rest;

import com.google.inject.Inject;
import com.logwise.orchestrator.constant.ApplicationConstants;
import com.logwise.orchestrator.dto.response.DefaultErrorResponse;
import com.logwise.orchestrator.dto.response.GetSparkStageHistoryResponse;
import com.logwise.orchestrator.enums.Tenant;
import com.logwise.orchestrator.rest.io.Response;
import com.logwise.orchestrator.service.SparkService;
import com.logwise.orchestrator.util.ResponseWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.concurrent.CompletionStage;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({@Inject}))
@Path("/spark-stage-history")
public class GetSparkStageHistory {
  SparkService sparkService;

  @GET
  @Consumes(MediaType.WILDCARD)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(summary = "Get spark stage history from DB", description = "Get spark stage history")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully returned spark stage history",
            content =
                @Content(schema = @Schema(implementation = GetSparkStageHistoryResponse.class))),
        @ApiResponse(
            responseCode = "500",
            description = "Error occurred while processing the request",
            content = @Content(schema = @Schema(implementation = DefaultErrorResponse.class)))
      })
  public CompletionStage<Response<GetSparkStageHistoryResponse>> handle(
      @NotNull(message = ApplicationConstants.HEADER_TENANT_NAME + " header is missing")
          @HeaderParam(ApplicationConstants.HEADER_TENANT_NAME)
          String tenantName,
      @NotNull(message = "limit query param is missing") @QueryParam("limit") Integer limit) {

    Tenant tenant = Tenant.fromValue(tenantName);

    return ResponseWrapper.fromSingle(
        sparkService.getSparkStageHistory(tenant, limit), HttpStatus.SC_OK);
  }
}
