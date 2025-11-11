package com.dream11.logcentralorchestrator.webclient.client;

import com.typesafe.config.Optional;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.net.ClientOptionsBase;
import io.vertx.core.net.ProxyType;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
@NoArgsConstructor
public class WebClientConfig {

  private static final Integer DEFAULT_KEEP_ALIVE_TIMEOUT = 6;
  private static final Integer DEFAULT_IDLE_TIMEOUT = 8;
  private static final Integer DEFAULT_MAX_POOL_SIZE = 32;
  private static final Integer DEFAULT_CONNECTION_SHUTDOWN_MILLIS = 30000;
  private static final Integer DEFAULT_RANDOM_SHUTDOWN_MILLIS = 30000;
  private static final Integer DEFAULT_FORCE_SHUTDOWN_MILLIS = 5000;

  @Optional @NonNull private Integer connectTimeout = ClientOptionsBase.DEFAULT_CONNECT_TIMEOUT;

  @Optional @NonNull private Integer idleTimeout = DEFAULT_IDLE_TIMEOUT;

  @Optional @NonNull private Integer maxPoolSize = DEFAULT_MAX_POOL_SIZE;

  @Optional private boolean logActivity = false;

  @Optional private boolean keepAlive = HttpClientOptions.DEFAULT_KEEP_ALIVE;

  @Optional @NonNull private Integer keepAliveTimeout = DEFAULT_KEEP_ALIVE_TIMEOUT;

  @Optional private boolean pipelining = HttpClientOptions.DEFAULT_PIPELINING;

  @Optional @NonNull private Integer pipeliningLimit = HttpClientOptions.DEFAULT_PIPELINING_LIMIT;

  @Optional private boolean tcpKeepAlive = true;

  @Optional @NonNull
  private String httpProtocolVersion = HttpClientOptions.DEFAULT_PROTOCOL_VERSION.name();

  @Optional @NonNull private Integer http2KeepAliveTimeout = DEFAULT_KEEP_ALIVE_TIMEOUT;

  @Optional @NonNull
  private Integer http2MaxPoolSize = HttpClientOptions.DEFAULT_HTTP2_MAX_POOL_SIZE;

  @Optional @NonNull
  private Integer http2MultiplexingLimit = HttpClientOptions.DEFAULT_HTTP2_MULTIPLEXING_LIMIT;

  @Optional private boolean useAlpn = true;

  @Optional @NonNull private Integer connectionShutdownMillis = DEFAULT_CONNECTION_SHUTDOWN_MILLIS;

  @Optional @NonNull private Integer forceShutdownMillis = DEFAULT_FORCE_SHUTDOWN_MILLIS;

  @Optional @NonNull private Integer randomShutdownMillis = DEFAULT_RANDOM_SHUTDOWN_MILLIS;

  @Optional @NonNull private Integer port = HttpClientOptions.DEFAULT_DEFAULT_PORT;

  @Optional private boolean useProxy = false;

  @Optional private String proxyHost;

  @Optional private Integer proxyPort;

  @Optional private ProxyType type = ProxyType.HTTP;

  @Optional private boolean useProxyAuthentication = false;

  @Optional private String userName;

  @Optional private String password;

  @Optional private Integer maxWaitQueueSize = HttpClientOptions.DEFAULT_MAX_WAIT_QUEUE_SIZE;
}
