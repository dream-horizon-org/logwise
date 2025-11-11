package com.dream11.logcentralorchestrator.rest.exception;

import com.dream11.logcentralorchestrator.rest.io.Error;

public interface RestError {

  String getErrorCode();

  String getErrorMessage();

  int getHttpStatusCode();

  default Error getError() {
    return Error.of(this.getErrorCode(), this.getErrorMessage());
  }
}
