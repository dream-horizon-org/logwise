package com.logwise.orchestrator.dto.kafka;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ClusterInfo {
  private String clusterId;
  private int brokerCount;
  private List<BrokerInfo> brokers;

  @Data
  @Builder
  public static class BrokerInfo {
    private int id;
    private String host;
    private int port;
  }
}
