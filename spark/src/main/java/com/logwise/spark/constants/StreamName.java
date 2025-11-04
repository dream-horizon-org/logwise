package com.logwise.spark.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum StreamName {
  APPLICATION_LOGS_STREAM_TO_S3("application-logs-stream-to-s3");

  private final String value;

  public static StreamName fromValue(String value) {
    for (StreamName streamName : StreamName.values()) {
      if (streamName.getValue().equals(value)) {
        return streamName;
      }
    }
    throw new IllegalArgumentException("Invalid value: " + value);
  }
}
