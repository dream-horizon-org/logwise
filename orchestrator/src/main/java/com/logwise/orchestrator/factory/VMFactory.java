package com.logwise.orchestrator.factory;

import com.logwise.orchestrator.client.VMClient;
import com.logwise.orchestrator.constant.ApplicationConstants;
import com.logwise.orchestrator.enums.Tenant;
import com.logwise.orchestrator.util.ApplicationUtils;
import lombok.experimental.UtilityClass;

@UtilityClass
public class VMFactory {

  public VMClient getSparkClient(Tenant tenant) {
    return ApplicationUtils.getGuiceInstance(
        VMClient.class, ApplicationConstants.SPARK_VM_INJECTOR_NAME.apply(tenant.getValue()));
  }
}
