package com.dream11.logcentralorchestrator.factory;

import com.dream11.logcentralorchestrator.client.ObjectStoreClient;
import com.dream11.logcentralorchestrator.constant.ApplicationConstants;
import com.dream11.logcentralorchestrator.enums.Tenant;
import com.dream11.logcentralorchestrator.util.ApplicationUtils;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ObjectStoreFactory {
  public ObjectStoreClient getClient(Tenant tenant) {
    return ApplicationUtils.getGuiceInstance(
        ObjectStoreClient.class,
        ApplicationConstants.OBJECT_STORE_INJECTOR_NAME.apply(tenant.getValue()));
  }
}
