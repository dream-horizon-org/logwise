package com.logwise.orchestrator.error;

import com.logwise.orchestrator.rest.exception.RestError;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@AllArgsConstructor
public enum ServiceError implements RestError {
  INVALID_TENANT("LogCentralOrchestrator:INVALID_TENANT", "Tenant: %s is invalid", 500),
  INVALID_COMPONENT_TYPE(
      "LogCentralOrchestrator:INVALID_COMPONENT_TYPE", "ComponentType: %s is invalid", 500),
  SPARK_MASTER_ERROR(
      "LogCentralOrchestrator:SPARK_MASTER_ERROR", "Spark Master Rest Error: %s", 500),
  SPARK_SUBMIT_ERROR(
      "LogCentralOrchestrator:SPARK_SUBMIT_ERROR", "Spark Submit Rest Error: %s", 500),
  QUERY_EXECUTION_FAILED(
      "LogCentralOrchestrator:QUERY_EXECUTION_FAILED", "Failed to execute query: %s", 500),
  INVALID_REQUEST_ERROR("LogCentralOrchestrator:INVALID_REQUEST_ERROR", "Invalid Request: %s", 400),
  INVALID_KAFKA_TYPE("LogCentralOrchestrator:INVALID_KAFKA_TYPE", "Invalid Kafka type: %s", 400);

  String errorCode;
  String errorMessage;
  int httpStatusCode;

  public RestError format(Object... params) {
    return new ServiceErrorWithDynamicMessage(
        this.errorCode, String.format(this.getErrorMessage(), params), this.getHttpStatusCode());
  }

  @Getter
  @AllArgsConstructor
  private static class ServiceErrorWithDynamicMessage implements RestError {
    String errorCode;
    String errorMessage;
    int httpStatusCode;
  }
}
