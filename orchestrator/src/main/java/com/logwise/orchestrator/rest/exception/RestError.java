package com.logwise.orchestrator.rest.exception;

import com.logwise.orchestrator.rest.io.Error;

public interface RestError {

  String getErrorCode();

  String getErrorMessage();

  int getHttpStatusCode();

  default Error getError() {
    return Error.of(this.getErrorCode(), this.getErrorMessage());
  }
}
