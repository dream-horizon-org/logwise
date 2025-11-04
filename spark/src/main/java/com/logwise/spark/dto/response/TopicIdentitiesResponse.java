package com.logwise.spark.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Data;

@Data
public class TopicIdentitiesResponse {
    private List<TopicIdentitiesItem> topicIdentities;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TopicIdentitiesItem {
        private int readVersion;
        private int partitions;
        private Object clusterContext;
        private int replicationFactor;
        private int underReplicatedPercentage;
        private int brokersSpreadPercentage;
        private int preferredReplicasPercentage;
        private int topicBrokers;
        private int brokersSkewPercentage;
        private List<Object> partitionsByBroker;
        private String producerRate;
        private int numBrokers;
        private Object size;
        private long summedTopicOffsets;
        private String topic;
        private int configReadVersion;
        private List<PartitionsIdentityItem> partitionsIdentity;
        private List<List<String>> config;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PartitionsIdentityItem {
        private Integer partNum;
        private Integer leader;
        private Long latestOffset;
        private List<Integer> isr;
        private List<Integer> replicas;
        private Boolean isPreferredLeader;
        private Boolean isUnderReplicated;
    }
}