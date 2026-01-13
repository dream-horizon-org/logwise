package com.logwise.orchestrator.factory;

import com.logwise.orchestrator.client.AsgClient;
import com.logwise.orchestrator.constant.ApplicationConstants;
import com.logwise.orchestrator.enums.Tenant;
import com.logwise.orchestrator.util.ApplicationUtils;
import lombok.experimental.UtilityClass;

@UtilityClass
public class AsgFactory {

  public AsgClient getSparkClient(Tenant tenant) {
    return ApplicationUtils.getGuiceInstance(
        AsgClient.class, ApplicationConstants.SPARK_ASG_INJECTOR_NAME.apply(tenant.getValue()));
  }
}
