package com.logwise.spark.dto.response;

import com.logwise.spark.dto.entity.SparkStageHistory;
import lombok.Data;

import java.util.List;

@Data
public class GetSparkStageHistoryResponse {
  private ResponseData data;

  @Data
  public static class ResponseData {
    private List<SparkStageHistory> sparkStageHistory;
  }
}
