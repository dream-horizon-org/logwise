package com.logwise.orchestrator.rest;

import com.google.inject.Inject;
import com.logwise.orchestrator.constant.ApplicationConstants;
import com.logwise.orchestrator.dto.request.ComponentSyncRequest;
import com.logwise.orchestrator.dto.response.DefaultErrorResponse;
import com.logwise.orchestrator.dto.response.DefaultSuccessResponse;
import com.logwise.orchestrator.enums.Tenant;
import com.logwise.orchestrator.rest.io.Response;
import com.logwise.orchestrator.service.ServiceManagerService;
import com.logwise.orchestrator.util.ResponseWrapper;
import io.reactivex.Completable;
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
@Path("/component/sync")
@Tag(
    name = "Component Management",
    description = "Component management operations including sync and data analytics")
public class SyncComponents {
  private final ServiceManagerService serviceManagerService;

  @POST
  @Consumes(MediaType.WILDCARD)
  @Produces(MediaType.APPLICATION_JSON)
  @Timeout(60000)
  @Operation(
      summary = "Sync components",
      description = "Sync application services between object store and database")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully synced component",
            content = @Content(schema = @Schema(implementation = DefaultSuccessResponse.class))),
        @ApiResponse(
            responseCode = "500",
            description = "Error occurred while processing the request",
            content = @Content(schema = @Schema(implementation = DefaultErrorResponse.class)))
      })
  public CompletionStage<Response<DefaultSuccessResponse>> syncHandler(
      @Parameter(description = "Tenant name identifier", required = true)
          @NotNull(message = ApplicationConstants.HEADER_TENANT_NAME + " header is missing")
          @HeaderParam(ApplicationConstants.HEADER_TENANT_NAME)
          String tenantName,
      @RequestBody(
              description = "Component sync configuration",
              required = true,
              content = @Content(schema = @Schema(implementation = ComponentSyncRequest.class)))
          @Valid
          @BeanParam
          ComponentSyncRequest request) {

    Tenant tenant = Tenant.fromValue(tenantName);
    request.validateParam();

    Completable completable = serviceManagerService.syncServices(tenant);
    return completable
        .andThen(
            Single.just(
                DefaultSuccessResponse.builder()
                    .success(true)
                    .message(
                        String.format(
                            "Successfully synced componentType: %s for tenant: %s",
                            request.getComponentType(), tenant.getValue()))
                    .build()))
        .to(response -> ResponseWrapper.fromSingle(response, HttpStatus.SC_OK));
  }
}
