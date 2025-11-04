package com.logwise.spark.clients;

import com.logwise.spark.dto.response.TopicIdentitiesResponse;
import feign.Param;
import feign.RequestLine;

public interface KafkaManagerClient {

    @RequestLine("GET /api/status/{clusterName}/topicIdentities")
    TopicIdentitiesResponse topicIdentities(@Param("clusterName") String clusterName);
}