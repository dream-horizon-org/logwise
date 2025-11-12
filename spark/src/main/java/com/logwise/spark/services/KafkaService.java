package com.logwise.spark.services;

import com.google.inject.Inject;
import com.logwise.spark.utils.ApplicationUtils;
import com.typesafe.config.Config;
import java.util.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class KafkaService {
  private final Config config;

  @Inject
  public KafkaService(Config config) {
    this.config = config;
  }

  // Package-private constructor for testing
  KafkaService(Config config, boolean test) {
    this.config = config;
  }

  public String getKafkaBootstrapServerIp(String kafkaHostname) {
    List<String> ipAddressWithPort =
        ApplicationUtils.getIpAddresses(kafkaHostname).stream()
            .map(ip -> String.format("%s:%s", ip, config.getString("kafka.bootstrap.servers.port")))
            .collect(Collectors.toList());
    String kafkaBootstrapServerIp = String.join(",", ipAddressWithPort);
    log.info("Kafka Bootstrap Server IP: " + kafkaBootstrapServerIp);
    return kafkaBootstrapServerIp;
  }
}
