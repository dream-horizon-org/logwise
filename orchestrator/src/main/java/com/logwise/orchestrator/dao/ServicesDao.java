package com.logwise.orchestrator.dao;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.logwise.orchestrator.dao.query.Query;
import com.logwise.orchestrator.dto.entity.ServiceDetails;
import com.logwise.orchestrator.enums.Tenant;
import com.logwise.orchestrator.error.ServiceError;
import com.logwise.orchestrator.mysql.reactivex.client.MysqlClient;
import com.logwise.orchestrator.rest.exception.RestException;
import com.logwise.orchestrator.util.ApplicationUtils;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.reactivex.sqlclient.Tuple;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({@Inject}))
public class ServicesDao {
  final MysqlClient mysqlClient;
  final ObjectMapper objectMapper;

  public Single<List<ServiceDetails>> getAllServiceDetails(Tenant tenant) {
    log.info("Getting all service details for tenant: {} from DB", tenant);
    String query = Query.GET_SERVICES;
    return mysqlClient
        .getSlaveMysqlClient()
        .preparedQuery(query)
        .rxExecute(Tuple.of(tenant.getValue()))
        .map(
            rows ->
                ApplicationUtils.rowSetToMapList(rows).stream()
                    .map(row -> objectMapper.convertValue(row, ServiceDetails.class))
                    .collect(Collectors.toList()))
        .doOnError(error -> log.error("Error in getting all service details: ", error));
  }

  public Completable insertServiceDetails(List<ServiceDetails> serviceDetails) {
    log.info("Inserting service details in DB: {}", serviceDetails);
    List<Tuple> tuples =
        serviceDetails.stream()
            .map(s -> Tuple.of(s.getServiceName(), s.getRetentionDays(), s.getTenant()))
            .collect(Collectors.toList());
    String query = Query.INSERT_SERVICE_DETAILS;
    return mysqlClient
        .getMasterMysqlClient()
        .preparedQuery(query)
        .rxExecuteBatch(tuples)
        .doOnError(error -> log.error("Error in inserting service details: ", error))
        .onErrorResumeNext(
            __ ->
                Single.error(new RestException(ServiceError.QUERY_EXECUTION_FAILED.format(query))))
        .ignoreElement();
  }

  public Completable deleteServiceDetails(List<ServiceDetails> serviceDetails) {
    log.info("Deleting service details: {}", serviceDetails);
    List<Tuple> tuples =
        serviceDetails.stream()
            .map(s -> Tuple.of(s.getServiceName(), s.getTenant()))
            .collect(Collectors.toList());
    String query = Query.DELETE_SERVICE_DETAILS;
    return mysqlClient
        .getMasterMysqlClient()
        .preparedQuery(query)
        .rxExecuteBatch(tuples)
        .doOnError(error -> log.error("Error in deleting service details: ", error))
        .onErrorResumeNext(
            __ ->
                Single.error(new RestException(ServiceError.QUERY_EXECUTION_FAILED.format(query))))
        .ignoreElement();
  }

  public Completable deleteServiceDetailsOlderThan(Tenant tenant, Duration duration) {
    log.info(
        "Deleting service details for tenant: {} and duration older than: {}", tenant, duration);
    LocalDateTime cutoff = LocalDateTime.now().minus(duration);
    String query = Query.DELETE_SERVICE_DETAILS_BEFORE_INTERVAL;
    return mysqlClient
        .getMasterMysqlClient()
        .preparedQuery(query)
        .rxExecute(Tuple.of(tenant.getValue(), cutoff))
        .doOnError(error -> log.error("Error in deleting service details: ", error))
        .onErrorResumeNext(
            __ ->
                Single.error(new RestException(ServiceError.QUERY_EXECUTION_FAILED.format(query))))
        .ignoreElement();
  }
}
