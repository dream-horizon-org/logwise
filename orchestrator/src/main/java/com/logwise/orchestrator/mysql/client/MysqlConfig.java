package com.logwise.orchestrator.mysql.client;

import com.logwise.orchestrator.config.constant.Constants;
import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.core.json.JsonObject;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.sqlclient.PoolOptions;
import java.util.concurrent.TimeUnit;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

@Data
@DataObject(generateConverter = true)
@Slf4j
public class MysqlConfig {

  static final Boolean DEFAULT_CACHE_PREPARED_STATEMENTS = true;
  static final String DEFAULT_CHARACTER_ENCODING = "UTF-8";
  static final String DEFAULT_CHARSET = "utf8mb4";
  static final String DEFAULT_COLLATION = "utf8mb4_unicode_520_ci";
  static final String DEFAULT_HOST = "127.0.0.1";
  static final int DEFAULT_MAX_POOL_SIZE = 5;
  static final int DEFAULT_PORT = 3306;
  static final int DEFAULT_LISTEN_INTERVAL = 30_000; // ms
  static final long DEFAULT_CONSUL_WATCH_TIMEOUT = 45; // seconds
  static final int DEFAULT_MAXIMUM_LIFETIME = 0; // minutes
  static final TimeUnit DEFAULT_MAXIMUM_LIFETIME_TIME_UNIT = TimeUnit.MINUTES;
  static final int DEFAULT_CONNECTION_TIMEOUT = 5; // seconds
  static final TimeUnit DEFAULT_CONNECTION_TIMEOUT_TIME_UNIT = TimeUnit.SECONDS;
  static final Integer DEFAULT_MAX_WAIT_QUEUE_SIZE = -1;
  static final String DEFAULT_CONSUL_KEY =
      Constants.NAMESPACE + "/" + Constants.SERVICE_NAME + "/" + Constants.ENV + "/mysql.json";
  static final Integer DEFAULT_MAX_SLAVE_POOL_DISCONNECT_JITTER = 7; // seconds
  static final TimeUnit DEFAULT_MAX_SLAVE_POOL_DISCONNECT_JITTER_TIME_UNIT = TimeUnit.SECONDS;
  static final Integer DEFAULT_SLAVE_POOL_DRAIN_DELAY = 5;
  static final TimeUnit DEFAULT_SLAVE_POOL_DRAIN_DELAY_TIME_UNIT = TimeUnit.SECONDS;

  /** Note: this only caches one-shot prepared queries i.e., via `client.preparedQuery` */
  @NonNull Boolean cachePreparedStatements = DEFAULT_CACHE_PREPARED_STATEMENTS;

  /** Java charset for encoding string value */
  @NonNull String characterEncoding = DEFAULT_CHARACTER_ENCODING;

  @NonNull String charset = DEFAULT_CHARSET;

  @NonNull String collation = DEFAULT_COLLATION;

  @NonNull String database;

  @NonNull String masterHost = DEFAULT_HOST;

  Integer maxMasterPoolSize;

  /** Note: A new pool of this size for each `.create` call. It is NOT shared across verticles. */
  @NonNull Integer maxPoolSize = DEFAULT_MAX_POOL_SIZE;

  Integer maxSlavePoolSize;

  @NonNull String password;

  @NonNull Integer port = DEFAULT_PORT;

  @NonNull String slaveHost = DEFAULT_HOST;

  @NonNull String username;

  @NonNull Integer listenInterval = DEFAULT_LISTEN_INTERVAL;

  @NonNull Boolean useResetConnection = false;

  @NonNull String consulKey = DEFAULT_CONSUL_KEY;

  @NonNull Long consulWatchTimeout = DEFAULT_CONSUL_WATCH_TIMEOUT;

  @NonNull Boolean useAffectedRows = Boolean.TRUE;

  @NonNull Integer maxSlavePoolLifetime = DEFAULT_MAXIMUM_LIFETIME;

  @NonNull TimeUnit maxLifetimeTimeUnit = DEFAULT_MAXIMUM_LIFETIME_TIME_UNIT;

  @NonNull Integer connectionTimeout = DEFAULT_CONNECTION_TIMEOUT;

  @NonNull TimeUnit connectionTimeoutTimeUnit = DEFAULT_CONNECTION_TIMEOUT_TIME_UNIT;

  @NonNull Integer maxSlavePoolDisconnectJitter = DEFAULT_MAX_SLAVE_POOL_DISCONNECT_JITTER;

  @NonNull
  TimeUnit maxSlavePoolDisconnectJitterTimeUnit =
      DEFAULT_MAX_SLAVE_POOL_DISCONNECT_JITTER_TIME_UNIT;

  @NonNull Integer slavePoolDrainDelay = DEFAULT_SLAVE_POOL_DRAIN_DELAY;
  @NonNull TimeUnit slavePoolDrainDelayTimeUnit = DEFAULT_SLAVE_POOL_DRAIN_DELAY_TIME_UNIT;

  @NonNull Integer maxWaitQueueSize = DEFAULT_MAX_WAIT_QUEUE_SIZE;

  Integer maxMasterWaitQueueSize;

  Integer maxSlaveWaitQueueSize;

  public MysqlConfig(JsonObject json) {
    log.debug("Creating MysqlConfig from {}", json);
    if (json != null) {
      if (json.containsKey("cachePreparedStatements"))
        this.cachePreparedStatements = json.getBoolean("cachePreparedStatements");
      if (json.containsKey("characterEncoding"))
        this.characterEncoding = json.getString("characterEncoding");
      if (json.containsKey("charset")) this.charset = json.getString("charset");
      if (json.containsKey("collation")) this.collation = json.getString("collation");
      if (json.containsKey("database")) this.database = json.getString("database");
      if (json.containsKey("masterHost")) this.masterHost = json.getString("masterHost");
      if (json.containsKey("maxMasterPoolSize"))
        this.maxMasterPoolSize = json.getInteger("maxMasterPoolSize");
      if (json.containsKey("maxPoolSize")) this.maxPoolSize = json.getInteger("maxPoolSize");
      if (json.containsKey("maxSlavePoolSize"))
        this.maxSlavePoolSize = json.getInteger("maxSlavePoolSize");
      if (json.containsKey("password")) this.password = json.getString("password");
      if (json.containsKey("port")) this.port = json.getInteger("port");
      if (json.containsKey("slaveHost")) this.slaveHost = json.getString("slaveHost");
      if (json.containsKey("username")) this.username = json.getString("username");
      if (json.containsKey("listenInterval"))
        this.listenInterval = json.getInteger("listenInterval");
      if (json.containsKey("useResetConnection"))
        this.useResetConnection = json.getBoolean("useResetConnection");
      if (json.containsKey("consulKey")) this.consulKey = json.getString("consulKey");
      if (json.containsKey("consulWatchTimeout"))
        this.consulWatchTimeout = json.getLong("consulWatchTimeout");
      if (json.containsKey("useAffectedRows"))
        this.useAffectedRows = json.getBoolean("useAffectedRows");
      if (json.containsKey("maxSlavePoolLifetime"))
        this.maxSlavePoolLifetime = json.getInteger("maxSlavePoolLifetime");
      if (json.containsKey("maxLifetimeTimeUnit"))
        this.maxLifetimeTimeUnit = TimeUnit.valueOf(json.getString("maxLifetimeTimeUnit"));
      if (json.containsKey("connectionTimeout"))
        this.connectionTimeout = json.getInteger("connectionTimeout");
      if (json.containsKey("connectionTimeoutTimeUnit"))
        this.connectionTimeoutTimeUnit =
            TimeUnit.valueOf(json.getString("connectionTimeoutTimeUnit"));
      if (json.containsKey("maxSlavePoolDisconnectJitter"))
        this.maxSlavePoolDisconnectJitter = json.getInteger("maxSlavePoolDisconnectJitter");
      if (json.containsKey("maxSlavePoolDisconnectJitterTimeUnit"))
        this.maxSlavePoolDisconnectJitterTimeUnit =
            TimeUnit.valueOf(json.getString("maxSlavePoolDisconnectJitterTimeUnit"));
      if (json.containsKey("slavePoolDrainDelay"))
        this.slavePoolDrainDelay = json.getInteger("slavePoolDrainDelay");
      if (json.containsKey("slavePoolDrainDelayTimeUnit"))
        this.slavePoolDrainDelayTimeUnit =
            TimeUnit.valueOf(json.getString("slavePoolDrainDelayTimeUnit"));
      if (json.containsKey("maxWaitQueueSize"))
        this.maxWaitQueueSize = json.getInteger("maxWaitQueueSize");
      if (json.containsKey("maxMasterWaitQueueSize"))
        this.maxMasterWaitQueueSize = json.getInteger("maxMasterWaitQueueSize");
      if (json.containsKey("maxSlaveWaitQueueSize"))
        this.maxSlaveWaitQueueSize = json.getInteger("maxSlaveWaitQueueSize");
    }
    log.debug("new MysqlConfig : {}", this);
  }

  @GenIgnore(GenIgnore.PERMITTED_TYPE)
  public MySQLConnectOptions getMysqlMasterConnectOptions() {
    return getMysqlConnectOptions().setHost(this.getMasterHost());
  }

  @GenIgnore(GenIgnore.PERMITTED_TYPE)
  public MySQLConnectOptions getMysqlSlaveConnectOptions() {
    return getMysqlConnectOptions().setHost(this.getSlaveHost());
  }

  private MySQLConnectOptions getMysqlConnectOptions() {
    val connectOptions =
        new MySQLConnectOptions()
            .setPort(this.getPort())
            .setUser(this.getUsername())
            .setPassword(this.getPassword())
            .setCharset(this.getCharset())
            .setCollation(this.getCollation())
            .setCharacterEncoding(this.getCharacterEncoding())
            .setCachePreparedStatements(this.getCachePreparedStatements())
            .setUseAffectedRows(this.getUseAffectedRows());

    if (this.getDatabase() != null) {
      connectOptions.setDatabase(this.getDatabase());
    }

    return connectOptions;
  }

  @GenIgnore(GenIgnore.PERMITTED_TYPE)
  public PoolOptions getMysqlMasterPoolOptions() {
    return new PoolOptions()
        .setMaxWaitQueueSize(this.getMaxMasterWaitQueueSize())
        .setMaxSize(this.getMaxMasterPoolSize());
  }

  @GenIgnore(GenIgnore.PERMITTED_TYPE)
  public PoolOptions getMysqlSlavePoolOptions() {
    return new PoolOptions()
        .setMaxWaitQueueSize(this.getMaxSlaveWaitQueueSize())
        .setMaxSize(this.getMaxSlavePoolSize());
  }

  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    json.put("cachePreparedStatements", this.cachePreparedStatements);
    json.put("characterEncoding", this.characterEncoding);
    json.put("charset", this.charset);
    json.put("collation", this.collation);
    json.put("database", this.database);
    json.put("masterHost", this.masterHost);
    json.put("maxMasterPoolSize", this.maxMasterPoolSize);
    json.put("maxPoolSize", this.maxPoolSize);
    json.put("maxSlavePoolSize", this.maxSlavePoolSize);
    json.put("password", this.password);
    json.put("port", this.port);
    json.put("slaveHost", this.slaveHost);
    json.put("username", this.username);
    json.put("listenInterval", this.listenInterval);
    json.put("useResetConnection", this.useResetConnection);
    json.put("consulKey", this.consulKey);
    json.put("consulWatchTimeout", this.consulWatchTimeout);
    json.put("useAffectedRows", this.useAffectedRows);
    json.put("maxSlavePoolLifetime", this.maxSlavePoolLifetime);
    json.put("maxLifetimeTimeUnit", this.maxLifetimeTimeUnit.toString());
    json.put("connectionTimeout", this.connectionTimeout);
    json.put("connectionTimeoutTimeUnit", this.connectionTimeoutTimeUnit.toString());
    json.put("maxSlavePoolDisconnectJitter", this.maxSlavePoolDisconnectJitter);
    json.put(
        "maxSlavePoolDisconnectJitterTimeUnit",
        this.maxSlavePoolDisconnectJitterTimeUnit.toString());
    json.put("slavePoolDrainDelay", this.slavePoolDrainDelay);
    json.put("slavePoolDrainDelayTimeUnit", this.slavePoolDrainDelayTimeUnit.toString());
    json.put("maxWaitQueueSize", this.maxWaitQueueSize);
    json.put("maxMasterWaitQueueSize", this.maxMasterWaitQueueSize);
    json.put("maxSlaveWaitQueueSize", this.maxSlaveWaitQueueSize);
    return json;
  }

  public Integer getMaxMasterPoolSize() {
    return this.maxMasterPoolSize != null ? this.maxMasterPoolSize : this.maxPoolSize;
  }

  public Integer getMaxSlavePoolSize() {
    return this.maxSlavePoolSize != null ? this.maxSlavePoolSize : this.maxPoolSize;
  }

  public Integer getMaxSlaveWaitQueueSize() {
    return this.maxSlaveWaitQueueSize != null ? this.maxSlaveWaitQueueSize : this.maxWaitQueueSize;
  }

  public Integer getMaxMasterWaitQueueSize() {
    return this.maxMasterWaitQueueSize != null
        ? this.maxMasterWaitQueueSize
        : this.maxWaitQueueSize;
  }
}
