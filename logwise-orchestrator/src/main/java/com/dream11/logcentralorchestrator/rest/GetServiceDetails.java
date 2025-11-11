package com.dream11.logcentralorchestrator.rest;

import com.dream11.logcentralorchestrator.constant.ApplicationConstants;
import com.dream11.logcentralorchestrator.dto.response.DefaultErrorResponse;
import com.dream11.logcentralorchestrator.dto.response.GetServiceDetailsResponse;
import com.dream11.logcentralorchestrator.enums.Tenant;
import com.dream11.logcentralorchestrator.service.ServiceManagerService;
import com.dream11.logcentralorchestrator.util.ResponseWrapper;
import com.dream11.logcentralorchestrator.rest.io.Response;
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
@Path("/service-details")
@Tag(name = "Service Management", description = "Service management operations")
public class GetServiceDetails {
    final ServiceManagerService serviceManagerService;

    @GET
    @Consumes(MediaType.WILDCARD)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Get service details from DB",
            description = "Get service details from cache")
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully returned service details",
                            content = @Content(schema = @Schema(implementation = GetServiceDetailsResponse.class))),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Error occurred while processing the request",
                            content = @Content(schema = @Schema(implementation = DefaultErrorResponse.class)))
            })
    public CompletionStage<Response<GetServiceDetailsResponse>> handle(
            @Parameter(description = "Tenant name identifier", required = true, example = "dream11")
            @NotNull(message = ApplicationConstants.HEADER_TENANT_NAME + " header is missing")
            @HeaderParam(ApplicationConstants.HEADER_TENANT_NAME)
            String tenantName) {
        Tenant tenant = Tenant.fromValue(tenantName);
        return ResponseWrapper.fromSingle(
                serviceManagerService.getServiceDetailsFromCache(tenant), HttpStatus.SC_OK);
    }
}