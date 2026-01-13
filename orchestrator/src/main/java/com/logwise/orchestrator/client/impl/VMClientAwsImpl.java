package com.logwise.orchestrator.client.impl;

import com.logwise.orchestrator.client.VMClient;
import com.logwise.orchestrator.common.util.CompletableFutureUtils;
import com.logwise.orchestrator.config.ApplicationConfig;
import com.logwise.orchestrator.util.AwsClientUtils;
import io.reactivex.Completable;
import io.reactivex.Single;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2AsyncClient;
import software.amazon.awssdk.services.ec2.model.*;

@Slf4j
public class VMClientAwsImpl implements VMClient {

  @NonFinal ApplicationConfig.EC2Config ec2Config;
  @NonFinal Ec2AsyncClient ec2Client;

  @Override
  public Completable rxConnect(ApplicationConfig.VMConfig config) {
    this.ec2Config = config.getAws();
    if (ec2Config != null) {
      log.debug("Connecting to ec2 with config: {}", config.getAws());
      return Completable.fromAction(this::createEc2Client);
    } else {
      log.info("Aws ec2 config is not present in {}", config);
    }
    return Completable.complete();
  }

  @Override
  public Single<Map<String, String>> getInstanceIds(List<String> ips) {
    log.info("Getting instance ids for ips: {}", ips);
    if (ips == null || ips.isEmpty()) {
      return Single.just(Map.of());
    }

    DescribeInstancesRequest request =
        DescribeInstancesRequest.builder()
            .filters(Filter.builder().name("private-ip-address").values(ips).build())
            .build();
    return describe(request)
        .map(
            response ->
                response.reservations().stream()
                    .flatMap(reservation -> reservation.instances().stream())
                    .collect(Collectors.toMap(Instance::privateIpAddress, Instance::instanceId)))
        .doOnSuccess(map -> log.info("Instance ids for ips: {}", map));
  }

  @Override
  public Completable terminateInstances(List<String> instanceIds) {
    log.info("Terminating instances: {}", instanceIds);
    return CompletableFutureUtils.toSingle(
            ec2Client.terminateInstances(
                TerminateInstancesRequest.builder().instanceIds(instanceIds).build()))
        .doOnError(e -> log.error("Error terminating instances: {}", instanceIds, e))
        .ignoreElement();
  }

  @Override
  public Single<Map<String, String>> getInstanceIPs(List<String> ids) {
    log.info("Getting instance ips for ids: {}", ids);
    if (ids == null || ids.isEmpty()) {
      return Single.just(Map.of());
    }

    DescribeInstancesRequest request = DescribeInstancesRequest.builder().instanceIds(ids).build();
    return describe(request)
        .map(
            response ->
                response.reservations().stream()
                    .flatMap(reservation -> reservation.instances().stream())
                    .collect(Collectors.toMap(Instance::instanceId, Instance::privateIpAddress)));
  }

  private Single<DescribeInstancesResponse> describe(DescribeInstancesRequest request) {
    return CompletableFutureUtils.toSingle(ec2Client.describeInstances(request));
  }

  private void createEc2Client() {
    AwsCredentialsProvider credentialsProvider = AwsClientUtils.getDefaultCredentialsProvider();

    ec2Client =
        Ec2AsyncClient.builder()
            .region(Region.of(ec2Config.getRegion()))
            .httpClient(AwsClientUtils.createHttpClient())
            .credentialsProvider(credentialsProvider)
            .overrideConfiguration(
                overrideConfig -> overrideConfig.retryPolicy(AwsClientUtils.createRetryPolicy()))
            .build();
  }
}
