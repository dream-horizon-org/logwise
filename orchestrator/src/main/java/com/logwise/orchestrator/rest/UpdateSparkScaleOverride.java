package com.logwise.orchestrator.rest;

import com.google.inject.Inject;
import com.logwise.orchestrator.constant.ApplicationConstants;
import com.logwise.orchestrator.dto.request.UpdateSparkScaleOverrideRequest;
import com.logwise.orchestrator.dto.response.DefaultErrorResponse;
import com.logwise.orchestrator.dto.response.DefaultSuccessResponse;
import com.logwise.orchestrator.enums.Tenant;
import com.logwise.orchestrator.rest.io.Response;
import com.logwise.orchestrator.service.SparkService;
import com.logwise.orchestrator.util.ResponseWrapper;
import io.reactivex.Single;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Path("/update-spark-scale-override")
@Tag(name = "Spark", description = "Spark job management operations")
public class UpdateSparkScaleOverride {
  SparkService sparkService;

  @POST
  @Consumes(MediaType.WILDCARD)
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Update Spark scale override",
      description = "Updates the scaling override configuration for Spark jobs")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully updated Spark scale override",
            content = @Content(schema = @Schema(implementation = DefaultSuccessResponse.class))),
        @ApiResponse(
            responseCode = "500",
            description = "Error occurred while updating Spark scale override",
            content = @Content(schema = @Schema(implementation = DefaultErrorResponse.class)))
      })
  public CompletionStage<Response<DefaultSuccessResponse>> handle(
      @Parameter(description = "Tenant name identifier", required = true, example = "ABC")
          @NotNull(message = ApplicationConstants.HEADER_TENANT_NAME + " header is missing")
          @HeaderParam(ApplicationConstants.HEADER_TENANT_NAME)
          String tenantName,
      @RequestBody(
              description = "Spark scale override configuration update request",
              required = true,
              content =
                  @Content(
                      schema = @Schema(implementation = UpdateSparkScaleOverrideRequest.class)))
          @Valid
          UpdateSparkScaleOverrideRequest request) {

    Tenant tenant = Tenant.fromValue(tenantName);

    return sparkService
        .updateSparkScaleOverride(tenant, request)
        .andThen(
            Single.just(
                DefaultSuccessResponse.builder()
                    .message(
                        "Successfully Updated Spark Scale Override Record for tenant: "
                            + tenantName)
                    .build()))
        .to(response -> ResponseWrapper.fromSingle(response, HttpStatus.SC_OK));
  }
}
