package com.dream11.logcentralorchestrator.rest.exception;

import com.dream11.logcentralorchestrator.rest.io.Error;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WebApplicationExceptionMapper implements ExceptionMapper<WebApplicationException> {

  public Response toResponse(WebApplicationException webApplicationException) {
    log.error("Error occurred", webApplicationException);
    RestException restException =
        webApplicationException.getCause() instanceof RestException
            ? (RestException) webApplicationException.getCause()
            : new RestException(
                webApplicationException.getResponse().getStatusInfo().getReasonPhrase(),
                Error.of("UNKNOWN_EXCEPTION", webApplicationException.toString()),
                webApplicationException.getResponse().getStatus());
    return Response.status(restException.getHttpStatusCode())
        .entity(restException.toString())
        .build();
  }
}
