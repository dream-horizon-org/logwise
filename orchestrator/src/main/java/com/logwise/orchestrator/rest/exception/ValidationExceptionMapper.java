package com.logwise.orchestrator.rest.exception;

import com.logwise.orchestrator.rest.io.Error;
import javax.validation.ValidationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ValidationExceptionMapper implements ExceptionMapper<ValidationException> {

  public Response toResponse(ValidationException validationException) {
    log.error("Constraint violation", validationException);
    RestException restException =
        new RestException(
            "Constraint Violation", Error.of("INVALID_REQUEST", validationException.getMessage()));
    return Response.status(Response.Status.BAD_REQUEST).entity(restException.toString()).build();
  }
}
