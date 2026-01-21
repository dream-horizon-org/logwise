package com.logwise.spark.dto.entity;

import lombok.Data;

@Data
public class SparkStageHistory implements Comparable<SparkStageHistory> {
  private Long outputBytes;
  private Long inputRecords;
  private Long submissionTime;
  private Long completionTime;
  private Integer coresUsed;
  private String status;
  private String tenant;

  @Override
  public int compareTo(SparkStageHistory sparkStageHistory) {
    return Long.compare(this.submissionTime, sparkStageHistory.submissionTime);
  }
}
