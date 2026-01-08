package com.logwise.orchestrator.client.impl;

import com.logwise.orchestrator.client.AsgClient;
import com.logwise.orchestrator.common.util.CompletableFutureUtils;
import com.logwise.orchestrator.config.ApplicationConfig;
import com.logwise.orchestrator.util.AwsClientUtils;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import java.util.List;
import java.util.stream.Collectors;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.autoscaling.AutoScalingAsyncClient;
import software.amazon.awssdk.services.autoscaling.model.*;

@Slf4j
public class AsgClientAwsImpl implements AsgClient {
  @NonFinal ApplicationConfig.AsgAwsConfig asgAwsConfig;
  @NonFinal AutoScalingAsyncClient asgClient;

  @Override
  public Completable rxConnect(ApplicationConfig.AsgConfig config) {
    this.asgAwsConfig = config.getAws();
    if (asgAwsConfig != null) {
      log.debug("Connecting to asg with config: {}", config.getAws());
      return Completable.fromAction(this::createAsgClient);
    } else {
      log.info("Aws Asg config is not present in {}", config);
    }
    return Completable.complete();
  }

  @Override
  public Completable updateDesiredCapacity(String asgName, int desiredCapacity) {
    return describe(asgName)
        .flatMap(
            resp -> {
              if (resp.autoScalingGroups().isEmpty()) {
                log.error("No ASG found with name {}", asgName);
                return Single.error(new RuntimeException("No ASG found with name " + asgName));
              }
              Integer maxCapacity = resp.autoScalingGroups().get(0).maxSize();
              Integer minCapacity = resp.autoScalingGroups().get(0).minSize();
              int newCapacity =
                  desiredCapacity > maxCapacity
                      ? maxCapacity
                      : desiredCapacity < minCapacity ? minCapacity : desiredCapacity;
              return CompletableFutureUtils.toSingle(
                  asgClient.setDesiredCapacity(
                      SetDesiredCapacityRequest.builder()
                          .autoScalingGroupName(asgName)
                          .desiredCapacity(newCapacity)
                          .build()));
            })
        .ignoreElement();
  }

  @Override
  public Completable removeInstances(
      String asgName, List<String> instanceIds, boolean decrementCount) {
    log.info(
        "Detaching instanceIds: {} from ASG: {} decrementCount: {}",
        instanceIds,
        asgName,
        decrementCount);
    int maxAsgBatchSize = 19;
    return getInstanceIdsInAsg(asgName)
        .map(
            asgInstanceIds ->
                asgInstanceIds.stream().filter(instanceIds::contains).collect(Collectors.toList()))
        .flatMapCompletable(
            asgInstanceIds ->
                Flowable.fromIterable(asgInstanceIds)
                    .buffer(maxAsgBatchSize)
                    .flatMapSingle(
                        batch -> {
                          log.info("Detaching batch: {} from ASG: {}", batch, asgName);
                          return CompletableFutureUtils.toSingle(
                              asgClient.detachInstances(
                                  DetachInstancesRequest.builder()
                                      .autoScalingGroupName(asgName)
                                      .instanceIds(batch)
                                      .shouldDecrementDesiredCapacity(decrementCount)
                                      .build()));
                        })
                    .doOnError(
                        e ->
                            log.error(
                                "Error detaching instances: {} from ASG: {}",
                                instanceIds,
                                asgName,
                                e))
                    .ignoreElements());
  }

  private Single<List<String>> getInstanceIdsInAsg(String asgName) {
    log.info("Getting instance ids in ASG: {}", asgName);
    return describe(asgName)
        .map(
            resp ->
                resp.autoScalingGroups().stream()
                    .findFirst()
                    .map(
                        asg ->
                            asg.instances().stream()
                                .map(Instance::instanceId)
                                .collect(Collectors.toList()))
                    .orElse(List.of()))
        .doOnSuccess(
            instanceIds -> log.info("Found {} instances in ASG: {}", instanceIds.size(), asgName));
  }

  @Override
  public Single<List<String>> getAllInServiceVmIdInAsg(String asgName) {
    return describe(asgName)
        .map(
            resp ->
                resp.autoScalingGroups().stream()
                    .flatMap(asg -> asg.instances().stream())
                    .filter(instance -> LifecycleState.IN_SERVICE.equals(instance.lifecycleState()))
                    .map(Instance::instanceId)
                    .collect(Collectors.toList()))
        .doOnSuccess(
            instanceIds ->
                log.info("Found {} in-service instances in ASG: {}", instanceIds.size(), asgName));
  }

  private Single<DescribeAutoScalingGroupsResponse> describe(String asgName) {
    return CompletableFutureUtils.toSingle(
        asgClient.describeAutoScalingGroups(
            DescribeAutoScalingGroupsRequest.builder().autoScalingGroupNames(asgName).build()));
  }

  private void createAsgClient() {
    AwsCredentialsProvider credentialsProvider = AwsClientUtils.getDefaultCredentialsProvider();

    asgClient =
        AutoScalingAsyncClient.builder()
            .region(Region.of(asgAwsConfig.getRegion()))
            .httpClient(AwsClientUtils.createHttpClient())
            .credentialsProvider(credentialsProvider)
            .overrideConfiguration(
                overrideConfig -> overrideConfig.retryPolicy(AwsClientUtils.createRetryPolicy()))
            .build();
  }
}
