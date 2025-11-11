package com.logwise.orchestrator.service;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.logwise.orchestrator.CaffeineCacheFactory;
import com.logwise.orchestrator.common.util.CompletableFutureUtils;
import com.logwise.orchestrator.constant.ApplicationConstants;
import com.logwise.orchestrator.dao.ServicesDao;
import com.logwise.orchestrator.dto.entity.ServiceDetails;
import com.logwise.orchestrator.dto.response.GetServiceDetailsResponse;
import com.logwise.orchestrator.enums.Tenant;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.vertx.reactivex.core.Vertx;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ServiceManagerService {
  private final ServicesDao servicesDao;
  private final ObjectStoreService objectStoreService;
  private final AsyncLoadingCache<Tenant, GetServiceDetailsResponse> getServiceDetailsFromCache;

  @Inject
  public ServiceManagerService(
      Vertx vertx, ServicesDao servicesDao, ObjectStoreService objectStoreService) {
    this.servicesDao = servicesDao;
    this.objectStoreService = objectStoreService;
    this.getServiceDetailsFromCache =
        CaffeineCacheFactory.createAsyncLoadingCache(
            vertx,
            ApplicationConstants.GET_SERVICE_DETAILS_CACHE,
            this::getServiceDetailsFromDB,
            ApplicationConstants.GET_SERVICE_DETAILS_CACHE);
  }

  public Single<GetServiceDetailsResponse> getServiceDetailsFromCache(Tenant tenant) {
    return CompletableFutureUtils.toSingle(getServiceDetailsFromCache.get(tenant));
  }

  public Single<GetServiceDetailsResponse> getServiceDetailsFromDB(Tenant tenant) {
    return servicesDao
        .getAllServiceDetails(tenant)
        .map(
            serviceDetailsList ->
                GetServiceDetailsResponse.builder().serviceDetails(serviceDetailsList).build());
  }

  public Completable syncServices(Tenant tenant) {
    return syncServicesInAws(tenant);
  }

  private Completable syncServicesInAws(Tenant tenant) {
    log.info("Syncing services in AWS for tenant: {}", tenant.getValue());
    Single<GetServiceDetailsResponse> serviceDetailsFromDBSingle = getServiceDetailsFromDB(tenant);
    Single<List<ServiceDetails>> serviceDetailsFromAnalyticsStorageSingle =
        objectStoreService.getAllDistinctServicesInAws(tenant);
    return Single.zip(
            serviceDetailsFromDBSingle,
            serviceDetailsFromAnalyticsStorageSingle,
            (serviceDetailsFromDB, serviceDetailsFromAnalyticsStorage) -> {
              List<ServiceDetails> dbServiceDetails = serviceDetailsFromDB.getServiceDetails();

              log.info("Services in db: {} for tenant: {}", dbServiceDetails, tenant);
              log.info(
                  "Services in analytics storage: {} for tenant: {}",
                  serviceDetailsFromAnalyticsStorage,
                  tenant);

              List<Completable> syncCompletables = Lists.newArrayList();
              List<ServiceDetails> servicesNotInDb =
                  getServicesNotInDb(dbServiceDetails, serviceDetailsFromAnalyticsStorage);

              log.info("Services not in db: {}", servicesNotInDb);
              if (!servicesNotInDb.isEmpty()) {
                syncCompletables.add(onBoardNewServices(servicesNotInDb, tenant));
              }
              List<ServiceDetails> servicesNotInObjectStore =
                  getServicesNotInObjectStore(dbServiceDetails, serviceDetailsFromAnalyticsStorage);

              log.info("Services not in object store: {}", servicesNotInObjectStore);
              if (!servicesNotInObjectStore.isEmpty()) {
                syncCompletables.add(removeServices(servicesNotInObjectStore, tenant));
              }
              return Completable.mergeDelayError(syncCompletables)
                  .doOnError(throwable -> log.error("Error syncing services", throwable));
            })
        .flatMapCompletable(completable -> completable);
  }

  private Completable onBoardNewServices(List<ServiceDetails> servicesNotInDb, Tenant tenant) {
    log.info("Onboarding new services: {} for tenant: {}", servicesNotInDb, tenant.getValue());
    return servicesDao
        .insertServiceDetails(servicesNotInDb)
        .onErrorResumeNext(
            throwable -> {
              log.error("Error on-boarding new services {}", servicesNotInDb, throwable);
              return Completable.error(throwable);
            });
  }

  private Completable removeServices(List<ServiceDetails> servicesNotInObjectStore, Tenant tenant) {
    log.info("Removing services: {} for tenant: {}", servicesNotInObjectStore, tenant.getValue());
    return Flowable.fromIterable(servicesNotInObjectStore)
        .flatMapCompletable(
            serviceDetails ->
                servicesDao
                    .deleteServiceDetails(Collections.singletonList(serviceDetails))
                    .doOnError(
                        throwable ->
                            log.error("Error removing services {}", serviceDetails, throwable))
                    .onErrorComplete());
  }

  private static List<ServiceDetails> getServicesNotInDb(
      List<ServiceDetails> dbServiceDetails, List<ServiceDetails> objectStoreServiceDetails) {
    return objectStoreServiceDetails.stream()
        .filter(element -> !dbServiceDetails.contains(element))
        .collect(Collectors.toList());
  }

  private static List<ServiceDetails> getServicesNotInObjectStore(
      List<ServiceDetails> dbServiceDetails, List<ServiceDetails> objectStoreServiceDetails) {
    return dbServiceDetails.stream()
        .filter(element -> !objectStoreServiceDetails.contains(element))
        .collect(Collectors.toList());
  }
}
