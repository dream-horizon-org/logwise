package com.logwise.orchestrator.util;

import static com.logwise.orchestrator.config.ApplicationConfig.TenantConfig;

import com.logwise.orchestrator.config.ApplicationConfigProvider;
import com.logwise.orchestrator.enums.Tenant;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class ApplicationConfigUtil {

  public TenantConfig getTenantConfig(Tenant tenant) {
    return ApplicationConfigProvider.getApplicationConfig().getTenants().stream()
        .filter(tenantConfig -> tenantConfig.getName().equals(tenant.getValue()))
        .findFirst()
        .orElse(null);
  }

  public boolean isAwsObjectStore(TenantConfig tenantConfig) {
    return tenantConfig.getObjectStore().getAws() != null;
  }
}
