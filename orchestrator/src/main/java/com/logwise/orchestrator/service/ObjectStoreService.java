package com.logwise.orchestrator.service;

import com.google.inject.Inject;
import com.logwise.orchestrator.client.ObjectStoreClient;
import com.logwise.orchestrator.config.ApplicationConfig.EnvLogsRetentionDaysConfig;
import com.logwise.orchestrator.config.ApplicationConfig.TenantConfig;
import com.logwise.orchestrator.dao.ServicesDao;
import com.logwise.orchestrator.dto.entity.ServiceDetails;
import com.logwise.orchestrator.enums.Tenant;
import com.logwise.orchestrator.factory.ObjectStoreFactory;
import com.logwise.orchestrator.util.ApplicationConfigUtil;
import com.logwise.orchestrator.util.ApplicationUtils;
import io.reactivex.Observable;
import io.reactivex.Single;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class ObjectStoreService {
  ServicesDao servicesDao;

  public Single<List<ServiceDetails>> getAllDistinctServicesInAws(Tenant tenant) {
    ObjectStoreClient objectStoreClient = ObjectStoreFactory.getClient(tenant);
    TenantConfig tenantConfig = ApplicationConfigUtil.getTenantConfig(tenant);

    return objectStoreClient
        .listCommonPrefix(tenantConfig.getSpark().getLogsDir() + "/environment_name=", "/")
        .flatMapObservable(Observable::fromIterable)
        .flatMap(
            prefix ->
                objectStoreClient.listCommonPrefix(prefix + "component_type=", "/").toObservable())
        .flatMap(Observable::fromIterable)
        .flatMap(
            prefix ->
                objectStoreClient.listCommonPrefix(prefix + "service_name=", "/").toObservable())
        .flatMap(Observable::fromIterable)
        .toList()
        .map(
            logsObjectKeys ->
                logsObjectKeys.stream()
                    .map(ApplicationUtils::getServiceFromObjectKey)
                    .peek(
                        serviceDetails -> {
                          serviceDetails.setRetentionDays(
                              getEnvRetentionDays(tenantConfig, serviceDetails));
                          serviceDetails.setTenant(tenant.getValue());
                        })
                    .collect(Collectors.toList()));
  }

  private static Integer getEnvRetentionDays(TenantConfig config, ServiceDetails serviceDetails) {
    return config.getEnvLogsRetentionDays().stream()
        .filter(
            retentionDaysConfig ->
                retentionDaysConfig.getEnvs().contains(serviceDetails.getEnvironmentName()))
        .findFirst()
        .map(EnvLogsRetentionDaysConfig::getRetentionDays)
        .orElse(config.getDefaultLogsRetentionDays());
  }
}
