package com.dream11.logcentralorchestrator.module;

import com.dream11.logcentralorchestrator.client.ObjectStoreClient;
import com.dream11.logcentralorchestrator.client.impl.ObjectStoreAwsImpl;
import com.dream11.logcentralorchestrator.common.guice.VertxAbstractModule;
import com.dream11.logcentralorchestrator.config.ApplicationConfigProvider;
import com.dream11.logcentralorchestrator.constant.ApplicationConstants;
import com.dream11.logcentralorchestrator.util.ApplicationConfigUtil;
import com.google.inject.name.Names;
import io.vertx.reactivex.core.Vertx;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ClientModule extends VertxAbstractModule {
  public ClientModule(Vertx vertx) {
    super(vertx);
  }

  @Override
  protected void bindConfiguration() {
    log.info("Binding Client Modules Configuration...");
    bindObjectStoreClients();
  }

  private void bindObjectStoreClients() {
    log.info("Binding Object Stores Clients...");
    ApplicationConfigProvider.getApplicationConfig()
        .getTenants()
        .forEach(
            tenantConfig -> {
              String injectorName =
                  ApplicationConstants.OBJECT_STORE_INJECTOR_NAME.apply(tenantConfig.getName());
              if (ApplicationConfigUtil.isAwsObjectStore(tenantConfig)) {
                bind(ObjectStoreClient.class)
                    .annotatedWith(Names.named(injectorName))
                    .toInstance(new ObjectStoreAwsImpl());
              } else {
                log.warn(
                    "Only AWS object store is supported. Skipping tenant: {}",
                    tenantConfig.getName());
              }
            });
  }
}
