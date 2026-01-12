package com.logwise.orchestrator.util;

import static com.logwise.orchestrator.config.ApplicationConfig.TenantConfig;

import com.logwise.orchestrator.config.ApplicationConfig;
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

  public boolean isAwsSparkCluster(TenantConfig tenantConfig) {
    String clusterType = tenantConfig.getSpark().getCluster().getClusterType();
    if (clusterType != null) {
      return "asg".equalsIgnoreCase(clusterType);
    }
    // Fallback to checking if ASG config exists (for backward compatibility)
    return tenantConfig.getSpark().getCluster().getAsg() != null
            && tenantConfig.getSpark().getCluster().getAsg().getAws() != null;
  }

  public boolean isKubernetesSparkCluster(TenantConfig tenantConfig) {
    String clusterType = tenantConfig.getSpark().getCluster().getClusterType();
    if (clusterType != null) {
      return "kubernetes".equalsIgnoreCase(clusterType) || "k8s".equalsIgnoreCase(clusterType);
    }
    // Fallback to checking if Kubernetes config exists (for backward compatibility)
    return tenantConfig.getSpark().getCluster().getKubernetes() != null;
  }

  public ApplicationConfig.VMConfig getVmConfigFromAsgConfig(
          ApplicationConfig.AsgConfig asgConfig) {
    ApplicationConfig.VMConfig.VMConfigBuilder vmConfig = ApplicationConfig.VMConfig.builder();
    if (asgConfig.getAws() != null) {
      vmConfig.aws(
              ApplicationConfig.EC2Config.builder().region(asgConfig.getAws().getRegion()).build());
    }
    return vmConfig.build();
  }
}
