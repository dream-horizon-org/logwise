package com.dream11.logcentralorchestrator.rest.healthcheck;

import com.dream11.logcentralorchestrator.rest.exception.RestException;
import com.dream11.logcentralorchestrator.rest.io.Error;

public class HealthCheckException extends RestException {

  public HealthCheckException(String responseMessage) {
    super(responseMessage, Error.of("HEALTHCHECK_FAILED", "healthcheck failed"), 503);
  }

  @Override
  public String toString() {
    return this.getMessage();
  }
}
