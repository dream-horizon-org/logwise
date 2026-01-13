package com.logwise.orchestrator.enums;

import com.logwise.orchestrator.error.ServiceError;
import com.logwise.orchestrator.rest.exception.RestException;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum KafkaType {
  EC2("ec2", "Self-managed Kafka on EC2"),
  CONFLUENT("confluent", "Confluent Cloud/Platform"),
  MSK("msk", "AWS Managed Streaming for Kafka");

  private final String value;
  private final String description;

  public static KafkaType fromValue(String value) {
    if (value == null || value.isEmpty()) {
      return EC2; // Default to EC2 for backward compatibility
    }
    for (KafkaType type : KafkaType.values()) {
      if (type.getValue().equalsIgnoreCase(value)) {
        return type;
      }
    }
    throw new RestException(
        ServiceError.INVALID_REQUEST_ERROR.format("Invalid Kafka type: " + value));
  }
}
