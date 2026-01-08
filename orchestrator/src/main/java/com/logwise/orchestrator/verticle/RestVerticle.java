package com.logwise.orchestrator.verticle;

import com.logwise.orchestrator.MainApplication;
import com.logwise.orchestrator.client.AsgClient;
import com.logwise.orchestrator.client.ObjectStoreClient;
import com.logwise.orchestrator.client.VMClient;
import com.logwise.orchestrator.common.app.AppContext;
import com.logwise.orchestrator.common.util.ContextUtils;
import com.logwise.orchestrator.config.ApplicationConfig;
import com.logwise.orchestrator.config.ApplicationConfigProvider;
import com.logwise.orchestrator.constant.ApplicationConstants;
import com.logwise.orchestrator.mysql.reactivex.client.MysqlClient;
import com.logwise.orchestrator.rest.AbstractRestVerticle;
import com.logwise.orchestrator.util.ApplicationConfigUtil;
import com.logwise.orchestrator.webclient.reactivex.client.WebClient;
import io.reactivex.Completable;
import java.util.stream.Collectors;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RestVerticle extends AbstractRestVerticle {

  @NonFinal MysqlClient mysqlClient;
  @NonFinal WebClient webClient;
  ApplicationConfig applicationConfig;

  public RestVerticle() {
    super(MainApplication.class.getPackageName());
    applicationConfig = ApplicationConfigProvider.getApplicationConfig();
  }

  @Override
  public Completable rxStart() {
    this.mysqlClient = MysqlClient.create(vertx);
    ContextUtils.setInstance(mysqlClient);

    this.webClient = WebClient.create(vertx);
    ContextUtils.setInstance(webClient);

    return mysqlClient
        .rxConnect()
        .andThen(Completable.defer(this::startObjectStores))
        .andThen(Completable.defer(this::startSparkAsgClients))
        .andThen(Completable.defer(this::startSparkVMClients))
        .andThen(Completable.defer(super::rxStart));
  }

  private Completable startObjectStores() {
    log.info("Starting Object Stores Clients...");
    return Completable.merge(
        applicationConfig.getTenants().stream()
            .map(
                tenantConfig -> {
                  ObjectStoreClient objectStoreClient =
                      AppContext.getInstance(
                          ObjectStoreClient.class,
                          ApplicationConstants.OBJECT_STORE_INJECTOR_NAME.apply(
                              tenantConfig.getName()));
                  return objectStoreClient.rxConnect(tenantConfig.getObjectStore());
                })
            .collect(Collectors.toList()));
  }

  private Completable startSparkAsgClients() {
    log.info("Starting Spark Asg Clients...");
    return Completable.merge(
        applicationConfig.getTenants().stream()
            .map(
                tenantConfig -> {
                  AsgClient sparkAsgClient =
                      AppContext.getInstance(
                          AsgClient.class,
                          ApplicationConstants.SPARK_ASG_INJECTOR_NAME.apply(
                              tenantConfig.getName()));
                  return sparkAsgClient != null
                      ? sparkAsgClient.rxConnect(tenantConfig.getSpark().getCluster().getAsg())
                      : Completable.complete();
                })
            .collect(Collectors.toList()));
  }

  private Completable startSparkVMClients() {
    log.info("Starting Spark VM Clients...");
    return Completable.merge(
        applicationConfig.getTenants().stream()
            .map(
                tenantConfig -> {
                  VMClient sparkVmClient =
                      AppContext.getInstance(
                          VMClient.class,
                          ApplicationConstants.SPARK_VM_INJECTOR_NAME.apply(
                              tenantConfig.getName()));
                  return sparkVmClient != null
                      ? sparkVmClient.rxConnect(
                          ApplicationConfigUtil.getVmConfigFromAsgConfig(
                              tenantConfig.getSpark().getCluster().getAsg()))
                      : Completable.complete();
                })
            .collect(Collectors.toList()));
  }

  @Override
  public Completable rxStop() {
    mysqlClient.close();
    webClient.close();
    return super.rxStop();
  }
}
