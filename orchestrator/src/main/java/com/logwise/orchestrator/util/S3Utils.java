package com.logwise.orchestrator.util;

import com.logwise.orchestrator.common.util.CompletableFutureUtils;
import com.logwise.orchestrator.config.ApplicationConfig.S3Config;
import io.reactivex.Completable;
import io.reactivex.Single;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Publisher;

@Slf4j
@UtilityClass
public class S3Utils {
  public Single<List<String>> listCommonPrefix(
      S3AsyncClient s3AsyncClient, S3Config s3Config, String prefix, String delimiter) {
    String bucketName = s3Config.getBucket();
    return CompletableFutureUtils.toSingle(
            s3AsyncClient.listObjectsV2(
                ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(prefix)
                    .delimiter(delimiter)
                    .build()))
        .map(
            response ->
                response.commonPrefixes().stream()
                    .map(CommonPrefix::prefix)
                    .collect(Collectors.toList()));
  }

  public Single<List<String>> listObjects(
      S3AsyncClient s3AsyncClient, S3Config s3Config, String prefix) {
    String bucketName = s3Config.getBucket();
    List<String> objectKeys = Collections.synchronizedList(new ArrayList<>());
    ListObjectsV2Request listRequest =
        ListObjectsV2Request.builder().bucket(bucketName).prefix(prefix).build();
    ListObjectsV2Publisher publisher = s3AsyncClient.listObjectsV2Paginator(listRequest);
    return CompletableFutureUtils.toSingle(
            publisher.contents().subscribe(s3Object -> objectKeys.add(s3Object.key())))
        .doOnError(e -> log.error("Error occurred while listing objects", e))
        .ignoreElement()
        .toSingle(() -> objectKeys);
  }

  public Completable deleteFile(S3AsyncClient s3AsyncClient, S3Config s3Config, String objectKey) {
    log.info("Deleting file: {}", objectKey);
    DeleteObjectRequest deleteObjectRequest =
        DeleteObjectRequest.builder().bucket(s3Config.getBucket()).key(objectKey).build();
    return CompletableFutureUtils.toSingle(s3AsyncClient.deleteObject(deleteObjectRequest))
        .doOnError(e -> log.error("Error occurred while deleting file: {}", objectKey, e))
        .ignoreElement();
  }

  public Completable copyObject(
      S3AsyncClient s3AsyncClient, S3Config s3Config, String srcObjectKey, String destObjectKey) {
    log.info("Copying file from {} to {}", srcObjectKey, destObjectKey);
    CopyObjectRequest copyObjectRequest =
        CopyObjectRequest.builder()
            .sourceBucket(s3Config.getBucket())
            .sourceKey(srcObjectKey)
            .destinationBucket(s3Config.getBucket())
            .destinationKey(destObjectKey)
            .build();
    return CompletableFutureUtils.toSingle(s3AsyncClient.copyObject(copyObjectRequest))
        .doOnError(e -> log.error("Error occurred while copying object: {}", srcObjectKey, e))
        .ignoreElement();
  }

  public Single<String> readFileContent(
      S3AsyncClient s3AsyncClient, S3Config s3Config, String objectKey) {
    log.debug("Reading file content from S3: {}", objectKey);
    GetObjectRequest getObjectRequest =
        GetObjectRequest.builder().bucket(s3Config.getBucket()).key(objectKey).build();
    return CompletableFutureUtils.toSingle(
            s3AsyncClient.getObject(
                getObjectRequest,
                software.amazon.awssdk.core.async.AsyncResponseTransformer.toBytes()))
        .map(response -> response.asUtf8String())
        .doOnError(e -> log.error("Error occurred while reading file: {}", objectKey, e));
  }
}
