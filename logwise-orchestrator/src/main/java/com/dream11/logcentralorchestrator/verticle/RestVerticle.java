package com.dream11.logcentralorchestrator.verticle;

import com.dream11.logcentralorchestrator.MainApplication;
import com.dream11.logcentralorchestrator.client.ObjectStoreClient;
import com.dream11.logcentralorchestrator.common.app.AppContext;
import com.dream11.logcentralorchestrator.common.util.ContextUtils;
import com.dream11.logcentralorchestrator.config.ApplicationConfig;
import com.dream11.logcentralorchestrator.config.ApplicationConfigProvider;
import com.dream11.logcentralorchestrator.constant.ApplicationConstants;
import com.dream11.logcentralorchestrator.mysql.reactivex.client.MysqlClient;
import com.dream11.logcentralorchestrator.rest.AbstractRestVerticle;
import com.dream11.logcentralorchestrator.webclient.reactivex.client.WebClient;
import io.reactivex.Completable;
import java.util.stream.Collectors;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RestVerticle extends AbstractRestVerticle {

  @NonFinal MysqlClient d11MysqlClient;
  @NonFinal WebClient d11WebClient;
  ApplicationConfig applicationConfig;

  public RestVerticle() {
    super(MainApplication.class.getPackageName());
    applicationConfig = ApplicationConfigProvider.getApplicationConfig();
  }

  @Override
  public Completable rxStart() {
    this.d11MysqlClient = MysqlClient.create(vertx);
    ContextUtils.setInstance(d11MysqlClient);

    this.d11WebClient = WebClient.create(vertx);
    ContextUtils.setInstance(d11WebClient);

    return d11MysqlClient
        .rxConnect()
        .andThen(Completable.defer(this::startObjectStores))
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

  @Override
  public Completable rxStop() {
    d11MysqlClient.close();
    d11WebClient.close();
    return super.rxStop();
  }
}
