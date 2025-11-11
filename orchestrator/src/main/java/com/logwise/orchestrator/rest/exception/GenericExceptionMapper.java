package com.logwise.orchestrator.rest.exception;

import com.logwise.orchestrator.rest.io.RestResponse;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GenericExceptionMapper implements ExceptionMapper<Throwable> {
  @Override
  public Response toResponse(Throwable throwable) {
    RestException restException = (RestException) RestResponse.parseThrowable(throwable);
    log.error(
        "Error: "
            + restException.getError().getCode()
            + " "
            + restException.getHttpStatusCode()
            + " "
            + restException.getError().getMessage(),
        throwable);
    return Response.status(restException.getHttpStatusCode())
        .entity(restException.toString())
        .build();
  }
}
