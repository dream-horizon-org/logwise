package com.dream11.logcentralorchestrator.mysql.client.impl;

import com.dream11.logcentralorchestrator.config.utils.WatchUtils;
import com.dream11.logcentralorchestrator.mysql.client.MysqlClient;
import com.dream11.logcentralorchestrator.mysql.client.MysqlConfig;
import io.vertx.config.ConfigRetriever;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.consul.KeyValue;
import io.vertx.ext.consul.Watch;
import io.vertx.mysqlclient.MySQLPool;
import java.security.SecureRandom;
import java.util.Random;
import java.util.function.Function;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MysqlClientImpl implements MysqlClient {

  private final ConfigRetriever configRetriever;
  private final Vertx vertx;
  private @NonFinal MysqlConfig mysqlConfig;
  private @NonFinal Watch<KeyValue> reconnectWatch;
  @Setter private Function<JsonObject, MysqlConfig> jsonToConfigTransformer = MysqlConfig::new;

  private @NonFinal io.vertx.mysqlclient.MySQLPool mysqlMasterPool;
  private @NonFinal io.vertx.mysqlclient.MySQLPool mysqlSlavePool;
  private Random randomGenerator;

  public MysqlClientImpl(
      @NonNull final Vertx vertx, @NonNull final ConfigRetriever configRetriever) {
    this.vertx = vertx;
    this.configRetriever = configRetriever;
    this.randomGenerator = new SecureRandom();
  }

  @Override
  public io.vertx.mysqlclient.MySQLPool getMasterMysqlClient() {
    return this.mysqlMasterPool;
  }

  @Override
  public io.vertx.mysqlclient.MySQLPool getSlaveMysqlClient() {
    return this.mysqlSlavePool;
  }

  // Note: Vert.x MysqlClient is not actually connected until first query
  public void connect(Handler<AsyncResult<Void>> handler) {
    this.configRetriever.getConfig(
        configJson -> {
          if (configJson.succeeded()) {
            this.mysqlConfig = this.jsonToConfigTransformer.apply(configJson.result());
            createMasterSlavePool();
            if (this.mysqlConfig.getListenInterval() > 0) {
              this.vertx.setPeriodic(
                  this.mysqlConfig.getListenInterval(),
                  id -> {
                    if (this.mysqlMasterPool != null) {
                      this.mysqlMasterPool
                          .query("show variables where variable_name='innodb_read_only'") // this
                          // command is
                          // AWS Aurora
                          // specific
                          .execute(
                              ar -> {
                                if (ar.succeeded()) {
                                  if (ar.result().iterator().hasNext()) {
                                    String readOnly =
                                        ar.result().iterator().next().getString("Value");
                                    log.debug("Readonly mode on masterHost:{}", readOnly);
                                    if (readOnly.equals("ON")) {
                                      // current masterHost is a slave node, reconnect
                                      log.info("current masterHost is not a master, reconnecting");
                                      createMasterSlavePool();
                                    }
                                  }
                                } else {
                                  log.error(
                                      "Error in getting readonly information from master",
                                      ar.cause());
                                }
                              });
                    }
                  });
            }
            if (this.mysqlConfig.getMaxSlavePoolLifetime() != 0) {
              Integer delay =
                  randomGenerator.nextInt(this.mysqlConfig.getMaxSlavePoolDisconnectJitter());
              log.debug("creating new mysql slave pool");
              vertx.setPeriodic(
                  this.mysqlConfig
                          .getMaxLifetimeTimeUnit()
                          .toMillis(this.mysqlConfig.getMaxSlavePoolLifetime())
                      + this.mysqlConfig.getMaxSlavePoolDisconnectJitterTimeUnit().toMillis(delay),
                  id -> createSlavePool());
            }

            if (this.mysqlConfig.getUseResetConnection()) {
              registerConsulWatch();
            }
            handler.handle(Future.succeededFuture());
          } else {
            Throwable err = configJson.cause();
            log.error("Error loading MysqlConfig", err);
            handler.handle(Future.failedFuture(err));
          }
        });
    // TODO: Implement and test this (Using configStream() may make it simpler)
    this.configRetriever.listen(
        change -> {
          log.debug("{}", change);
          JsonObject previousConfigJson = change.getPreviousConfiguration();
          MysqlConfig previousConfig = this.jsonToConfigTransformer.apply(previousConfigJson);
          JsonObject configJson = change.getNewConfiguration();
          MysqlConfig config = this.jsonToConfigTransformer.apply(configJson);
          if (!config.equals(previousConfig)) {
            log.info("Detected a change in config from {} to {}", previousConfig, config);
            // TODO: Create a new MysqlClient by taking care that inflight requests are not lost
            // mysqlMasterPool = mysqlMasterPool;
            // mysqlSlavePool = mysqlSlavePool;
          }
        });
  }

  private void createMasterSlavePool() {
    this.closeConnection(); // close an older pool
    createMasterPool();
    createSlavePool();
  }

  private void createMasterPool() {
    this.mysqlMasterPool =
        MySQLPool.pool(
            vertx,
            this.mysqlConfig.getMysqlMasterConnectOptions(),
            this.mysqlConfig.getMysqlMasterPoolOptions());
    log.info("created mysql master pool");
  }

  private void createSlavePool() {
    if (this.mysqlSlavePool != null) {
      io.vertx.mysqlclient.MySQLPool oldSlavePool = this.mysqlSlavePool;
      this.mysqlSlavePool =
          MySQLPool.pool(
              vertx,
              this.mysqlConfig.getMysqlSlaveConnectOptions(),
              this.mysqlConfig.getMysqlSlavePoolOptions());
      vertx.setTimer(
          this.mysqlConfig
              .getSlavePoolDrainDelayTimeUnit()
              .toMillis(this.mysqlConfig.getSlavePoolDrainDelay()),
          id -> {
            oldSlavePool.close();
            log.debug("closed old mysql connections");
          });
    } else {
      this.mysqlSlavePool =
          MySQLPool.pool(
              vertx,
              this.mysqlConfig.getMysqlSlaveConnectOptions(),
              this.mysqlConfig.getMysqlSlavePoolOptions());
    }
    log.debug("created mysql slave pool");
  }

  @Deprecated
  public MysqlConfig getConfig() {
    return this.jsonToConfigTransformer.apply(configRetriever.getCachedConfig());
  }

  public void close() {
    closeConnection();
    closeWatch();
  }

  public void closeConnection() {
    if (this.mysqlMasterPool != null) {
      this.mysqlMasterPool.close();
      this.mysqlMasterPool = null;
    }
    if (this.mysqlSlavePool != null) {
      this.mysqlSlavePool.close();
      this.mysqlSlavePool = null;
    }
  }

  private void closeWatch() {
    if (this.reconnectWatch != null) {
      this.reconnectWatch.stop();
      this.reconnectWatch = null;
    }
  }

  private void registerConsulWatch() {
    this.reconnectWatch =
        WatchUtils.setConsulKeyWatch(
            this.vertx,
            this.mysqlConfig.getConsulKey(),
            this.mysqlConfig.getConsulWatchTimeout(),
            watchResult -> {
              if (watchResult.succeeded()) {
                if (watchResult.prevResult() != null) {
                  if (watchResult.nextResult().isPresent()) {
                    log.info("Change detected from consul");
                    JsonObject config = new JsonObject(watchResult.nextResult().getValue());
                    if (config.containsKey("maxDelaySeconds")) {
                      int delay =
                          randomGenerator.nextInt(config.getInteger("maxDelaySeconds")); // seconds
                      log.info("Will reconnect mysql after {} seconds (generated randomly)", delay);
                      vertx.setTimer(1L + delay * 1000, id -> createMasterSlavePool());
                    } else {
                      log.info("Reconnecting to mysql ...");
                      createMasterSlavePool();
                    }
                  } else {
                    log.info("Next result is not present");
                  }
                }
              } else {
                log.error("Unable to retrieve keys from consul", watchResult.cause());
              }
            });
  }
}
