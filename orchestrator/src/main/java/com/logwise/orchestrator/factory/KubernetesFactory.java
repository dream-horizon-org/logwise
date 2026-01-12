package com.logwise.orchestrator.factory;

import com.logwise.orchestrator.client.KubernetesClient;
import com.logwise.orchestrator.constant.ApplicationConstants;
import com.logwise.orchestrator.enums.Tenant;
import com.logwise.orchestrator.util.ApplicationUtils;
import lombok.experimental.UtilityClass;

@UtilityClass
public class KubernetesFactory {

  public KubernetesClient getSparkClient(Tenant tenant) {
    return ApplicationUtils.getGuiceInstance(
        KubernetesClient.class, ApplicationConstants.SPARK_KUBERNETES_INJECTOR_NAME.apply(tenant.getValue()));
  }
}

