package com.logwise.orchestrator.webclient.client.impl;

import com.logwise.orchestrator.webclient.client.WebClient;
import com.logwise.orchestrator.webclient.client.WebClientConfig;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.net.ProxyOptions;
import io.vertx.ext.web.client.WebClientOptions;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WebClientImpl implements WebClient, AutoCloseable {

  @Getter private io.vertx.ext.web.client.WebClient webClient;

  public WebClientImpl(Vertx vertx, WebClientConfig webClientConfig) {
    WebClientOptions webClientOptions =
        new WebClientOptions()
            .setConnectTimeout(webClientConfig.getConnectTimeout())
            .setLogActivity(webClientConfig.isLogActivity())
            .setMaxPoolSize(webClientConfig.getMaxPoolSize())
            .setKeepAlive(webClientConfig.isKeepAlive())
            .setKeepAliveTimeout(webClientConfig.getKeepAliveTimeout())
            .setPipelining(webClientConfig.isPipelining())
            .setPipeliningLimit(webClientConfig.getPipeliningLimit())
            .setTcpKeepAlive(webClientConfig.isTcpKeepAlive())
            .setProtocolVersion(HttpVersion.valueOf(webClientConfig.getHttpProtocolVersion()))
            .setHttp2KeepAliveTimeout(webClientConfig.getHttp2KeepAliveTimeout())
            .setHttp2MaxPoolSize(webClientConfig.getHttp2MaxPoolSize())
            .setIdleTimeout(webClientConfig.getIdleTimeout())
            .setHttp2MultiplexingLimit(webClientConfig.getHttp2MultiplexingLimit())
            .setUseAlpn(webClientConfig.isUseAlpn())
            .setDefaultPort(webClientConfig.getPort())
            .setMaxWaitQueueSize(webClientConfig.getMaxWaitQueueSize());

    if (webClientConfig.isUseProxy()) {
      ProxyOptions proxyOptions =
          (new ProxyOptions())
              .setHost(webClientConfig.getProxyHost())
              .setPort(webClientConfig.getProxyPort())
              .setType(webClientConfig.getType());

      if (webClientConfig.isUseProxyAuthentication()) {
        proxyOptions.setUsername(webClientConfig.getUserName());
        proxyOptions.setPassword(webClientConfig.getPassword());
      }
      webClientOptions.setProxyOptions(proxyOptions);
    }

    HttpClient httpClient = vertx.createHttpClient(webClientOptions);
    httpClient.connectionHandler(
        conn -> {
          log.debug("connection created");
          if (webClientConfig.getConnectionShutdownMillis() != -1) {
            vertx.setTimer(
                webClientConfig.getConnectionShutdownMillis()
                    + (long) (Math.random() * webClientConfig.getRandomShutdownMillis()),
                id -> conn.shutdown(webClientConfig.getForceShutdownMillis()));
          }
          conn.closeHandler(v -> log.debug("connection closed"));
        });
    this.webClient = io.vertx.ext.web.client.WebClient.wrap(httpClient, webClientOptions);
  }

  @Override
  public void close() {
    if (this.webClient != null) {
      this.webClient.close();
      this.webClient = null;
    }
  }
}
