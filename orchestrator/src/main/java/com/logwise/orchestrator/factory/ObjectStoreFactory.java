package com.logwise.orchestrator.factory;

import com.logwise.orchestrator.client.ObjectStoreClient;
import com.logwise.orchestrator.constant.ApplicationConstants;
import com.logwise.orchestrator.enums.Tenant;
import com.logwise.orchestrator.util.ApplicationUtils;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ObjectStoreFactory {
  public ObjectStoreClient getClient(Tenant tenant) {
    return ApplicationUtils.getGuiceInstance(
        ObjectStoreClient.class,
        ApplicationConstants.OBJECT_STORE_INJECTOR_NAME.apply(tenant.getValue()));
  }
}
