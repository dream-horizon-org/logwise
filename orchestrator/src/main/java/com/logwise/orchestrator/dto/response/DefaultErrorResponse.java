package com.logwise.orchestrator.dto.response;

import lombok.Data;
import lombok.NonNull;

@Data
public class DefaultErrorResponse {
  @NonNull ErrorResponse error;

  @Data
  private static class ErrorResponse {
    @NonNull String message;
    @NonNull String cause;
    @NonNull String code;
  }
}
