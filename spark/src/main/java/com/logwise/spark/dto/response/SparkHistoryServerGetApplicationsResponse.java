package com.logwise.spark.dto.response;

import java.util.List;
import lombok.Data;

@Data
public class SparkHistoryServerGetApplicationsResponse {
  private List<SparkHistoryServerGetApplicationsResponseItem>
      sparkHistoryServerGetApplicationsResponse;

  @Data
  public static class SparkHistoryServerGetApplicationsResponseItem {
    private String name;
    private String id;
    private List<AttemptsItem> attempts;
  }

  @Data
  public static class AttemptsItem {
    private long duration;
    private String lastUpdated;
    private String appSparkVersion;
    private String startTime;
    private String sparkUser;
    private long startTimeEpoch;
    private String endTime;
    private boolean completed;
    private long lastUpdatedEpoch;
    private long endTimeEpoch;
  }
}
