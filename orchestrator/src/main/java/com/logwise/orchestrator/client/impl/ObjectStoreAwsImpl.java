package com.logwise.orchestrator.client.impl;

import static com.logwise.orchestrator.config.ApplicationConfig.ObjectStoreConfig;
import static com.logwise.orchestrator.config.ApplicationConfig.S3Config;

import com.logwise.orchestrator.client.ObjectStoreClient;
import com.logwise.orchestrator.util.AwsClientUtils;
import com.logwise.orchestrator.util.S3Utils;
import io.reactivex.Completable;
import io.reactivex.Single;
import java.net.URI;
import java.util.List;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder;

@Slf4j
public class ObjectStoreAwsImpl implements ObjectStoreClient {
  private static final long PART_SIZE = 10L * 1024 * 1024;
  @NonFinal private S3Config s3Config;
  @NonFinal private S3AsyncClient s3AsyncClient;

  @Override
  public Completable rxConnect(ObjectStoreConfig config) {
    this.s3Config = config.getAws();
    if (s3Config != null) {
      log.debug("Connecting to s3 with config: {}", config.getAws());
      createS3Client();
    } else {
      log.info("S3 config is not present in {}", config);
    }
    return Completable.complete();
  }

  @Override
  public Single<List<String>> listCommonPrefix(String prefix, String delimiter) {
    return S3Utils.listCommonPrefix(s3AsyncClient, s3Config, prefix, delimiter);
  }

  @Override
  public Single<List<String>> listObjects(String prefix) {
    return S3Utils.listObjects(s3AsyncClient, s3Config, prefix);
  }

  @Override
  public Completable deleteFile(String objectKey) {
    return S3Utils.deleteFile(s3AsyncClient, s3Config, objectKey);
  }

  private void createS3Client() {
    S3AsyncClientBuilder builder = S3AsyncClient.builder();
    AwsCredentialsProvider credentialsProvider = AwsClientUtils.getDefaultCredentialsProvider();

    if (s3Config.getEndpointOverride() != null) {
      log.info("Initialising s3 on endpoint override {}", s3Config.getEndpointOverride());
      builder.endpointOverride(URI.create(s3Config.getEndpointOverride()));
    }

    s3AsyncClient =
        builder
            .region(Region.of(s3Config.getRegion()))
            .httpClient(AwsClientUtils.createHttpClient())
            .credentialsProvider(credentialsProvider)
            .multipartConfiguration(
                c ->
                    c.minimumPartSizeInBytes(PART_SIZE)
                        .thresholdInBytes(PART_SIZE * 2)
                        .apiCallBufferSizeInBytes(PART_SIZE * 2)
                        .build())
            .overrideConfiguration(
                overrideConfig -> overrideConfig.retryPolicy(AwsClientUtils.createRetryPolicy()))
            .build();
  }
}
