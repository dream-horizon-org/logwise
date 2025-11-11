package com.dream11.logcentralorchestrator.service;

import com.dream11.logcentralorchestrator.client.ObjectStoreClient;
import com.dream11.logcentralorchestrator.config.ApplicationConfig.EnvLogsRetentionDaysConfig;
import com.dream11.logcentralorchestrator.config.ApplicationConfig.TenantConfig;
import com.dream11.logcentralorchestrator.dao.ServicesDao;
import com.dream11.logcentralorchestrator.dto.entity.ServiceDetails;
import com.dream11.logcentralorchestrator.enums.Tenant;
import com.dream11.logcentralorchestrator.factory.ObjectStoreFactory;
import com.dream11.logcentralorchestrator.util.ApplicationConfigUtil;
import com.dream11.logcentralorchestrator.util.ApplicationUtils;
import com.google.inject.Inject;
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
        .listCommonPrefix(tenantConfig.getSpark().getLogsDir() + "/env=", "/")
        .flatMapObservable(Observable::fromIterable)
        .flatMap(
            prefix ->
                objectStoreClient.listCommonPrefix(prefix + "service_name=", "/").toObservable())
        .flatMap(Observable::fromIterable)
        .flatMap(
            prefix ->
                objectStoreClient.listCommonPrefix(prefix + "component_name=", "/").toObservable())
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
            retentionDaysConfig -> retentionDaysConfig.getEnvs().contains(serviceDetails.getEnv()))
        .findFirst()
        .map(EnvLogsRetentionDaysConfig::getRetentionDays)
        .orElse(config.getDefaultLogsRetentionDays());
  }
}
