package com.logwise.orchestrator.rest.healthcheck;

import com.logwise.orchestrator.rest.exception.RestException;
import com.logwise.orchestrator.rest.io.Error;

public class HealthCheckException extends RestException {

  public HealthCheckException(String responseMessage) {
    super(responseMessage, Error.of("HEALTHCHECK_FAILED", "healthcheck failed"), 503);
  }

  @Override
  public String toString() {
    return this.getMessage();
  }
}
