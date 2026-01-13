package com.logwise.orchestrator.dto.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.NonFinal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SparkStageHistory implements Comparable<SparkStageHistory> {
  @NonFinal Long outputBytes;
  @NonFinal Long inputRecords;
  @NonFinal Long submissionTime;
  @NonFinal Long completionTime;
  @NonFinal Integer coresUsed;
  @NonFinal String status;
  @NonFinal String tenant;

  @Override
  public int compareTo(SparkStageHistory o) {
    return o.submissionTime.compareTo(this.submissionTime);
  }
}
