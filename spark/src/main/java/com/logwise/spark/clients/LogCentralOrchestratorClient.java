package com.logwise.spark.clients;

import com.logwise.spark.dto.request.ScaleSparkClusterRequest;
import feign.HeaderMap;
import feign.RequestLine;
import java.util.Map;

public interface LogCentralOrchestratorClient {
  @RequestLine("POST /scale-spark-cluster")
  Map<String, Object> postScaleSparkCluster(
      @HeaderMap Map<String, String> headers, ScaleSparkClusterRequest request);
}
