package com.logwise.orchestrator.config.client;

import com.logwise.orchestrator.config.constant.Constants;
import com.typesafe.config.Optional;
import io.vertx.ext.consul.ConsulClientOptions;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
public class ConfigOptions {

  static final String DEFAULT_CONSUL_HOST =
      Constants.ENV.equals("prod") || Constants.ENV.equals("uat")
          ? String.format("config-store-%s.dream11.com", Constants.ENV)
          : String.format("config-store-%s.d11dev.com", Constants.ENV);
  static final int DEFAULT_CONSUL_PORT = 80;

  @Optional private String consulHost = DEFAULT_CONSUL_HOST;
  @Optional private int consulPort = DEFAULT_CONSUL_PORT;

  public ConsulClientOptions retrieveConsulClientOptions() {
    return new ConsulClientOptions()
        .setHost(this.consulHost)
        .setPort(this.consulPort)
        .setAclToken(Constants.CONSUL_TOKEN);
  }
}
