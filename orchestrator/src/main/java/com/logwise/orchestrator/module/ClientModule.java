package com.logwise.orchestrator.module;

import com.google.inject.name.Names;
import com.logwise.orchestrator.client.ObjectStoreClient;
import com.logwise.orchestrator.client.impl.ObjectStoreAwsImpl;
import com.logwise.orchestrator.common.guice.VertxAbstractModule;
import com.logwise.orchestrator.config.ApplicationConfigProvider;
import com.logwise.orchestrator.constant.ApplicationConstants;
import com.logwise.orchestrator.util.ApplicationConfigUtil;
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
