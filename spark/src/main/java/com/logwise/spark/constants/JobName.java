package com.logwise.spark.constants;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum JobName {
  PUSH_LOGS_TO_S3("push-logs-to-s3");
  private final String value;

  public static JobName fromValue(String value) {
    for (JobName jobName : JobName.values()) {
      if (jobName.getValue().equals(value)) {
        return jobName;
      }
    }
    throw new IllegalArgumentException("Invalid value: " + value);
  }
}
