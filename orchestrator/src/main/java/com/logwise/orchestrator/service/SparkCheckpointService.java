package com.logwise.orchestrator.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.logwise.orchestrator.config.ApplicationConfig.S3Config;
import com.logwise.orchestrator.config.ApplicationConfig.SparkConfig;
import com.logwise.orchestrator.dto.kafka.SparkCheckpointOffsets;
import com.logwise.orchestrator.enums.Tenant;
import com.logwise.orchestrator.util.ApplicationConfigUtil;
import com.logwise.orchestrator.util.AwsClientUtils;
import com.logwise.orchestrator.util.S3Utils;
import io.reactivex.Single;
import java.net.URI;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.TopicPartition;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder;

/**
 * Service for reading and parsing Spark Structured Streaming checkpoint offsets from S3. Spark
 * stores checkpoint offsets in JSON format at: checkpoint/application/offsets/ Files are named with
 * numeric batch IDs: 0, 1, 2, etc.
 */
@Slf4j
public class SparkCheckpointService {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Inject
  public SparkCheckpointService() {}

  /**
   * Get Spark checkpoint offsets for a tenant. Parses the latest offset file from Spark checkpoint
   * directory.
   *
   * @param tenant Tenant
   * @return SparkCheckpointOffsets with partition offsets
   */
  public Single<SparkCheckpointOffsets> getSparkCheckpointOffsets(Tenant tenant) {
    try {
      var tenantConfig = ApplicationConfigUtil.getTenantConfig(tenant);
      SparkConfig sparkConfig = tenantConfig.getSpark();
      S3Config s3Config = tenantConfig.getObjectStore().getAws();

      // Build checkpoint path: checkpoint/application
      // checkpointDir is just "checkpoint" (relative to bucket root)
      String checkpointDir = sparkConfig.getCheckPointDir();
      String checkpointPath = checkpointDir + "/application";

      // Create S3 client
      S3AsyncClient s3Client = createS3Client(s3Config);

      return parseCheckpointOffsets(s3Client, s3Config, checkpointPath)
          .doFinally(
              () -> {
                if (s3Client != null) {
                  s3Client.close();
                }
              });
    } catch (Exception e) {
      log.error("Error getting Spark checkpoint offsets for tenant: {}", tenant, e);
      return Single.just(
          SparkCheckpointOffsets.builder()
              .checkpointPath("")
              .offsets(Collections.emptyMap())
              .available(false)
              .lastUpdatedTimestamp(System.currentTimeMillis())
              .build());
    }
  }

  private S3AsyncClient createS3Client(S3Config s3Config) {
    S3AsyncClientBuilder builder = S3AsyncClient.builder();
    builder
        .region(Region.of(s3Config.getRegion()))
        .httpClient(AwsClientUtils.createHttpClient())
        .credentialsProvider(AwsClientUtils.getDefaultCredentialsProvider())
        .overrideConfiguration(
            overrideConfig -> overrideConfig.retryPolicy(AwsClientUtils.createRetryPolicy()));

    if (s3Config.getEndpointOverride() != null) {
      builder.endpointOverride(URI.create(s3Config.getEndpointOverride()));
    }

    return builder.build();
  }

  private Single<SparkCheckpointOffsets> parseCheckpointOffsets(
      S3AsyncClient s3Client, S3Config s3Config, String checkpointPath) {
    // Spark stores offsets at: checkpoint/application/offsets/
    // Files are named with numeric batch IDs: 0, 1, 2, etc.
    String offsetsPath = checkpointPath + "/offsets";

    return S3Utils.listObjects(s3Client, s3Config, offsetsPath)
        .flatMap(
            offsetFiles -> {
              if (offsetFiles.isEmpty()) {
                log.warn("No offset files found in checkpoint path: {}", offsetsPath);
                return Single.just(createEmptyCheckpointOffsets(checkpointPath));
              }

              // Get the latest offset file by parsing numeric filenames
              // Files are named: 0, 1, 2, etc. (highest number is latest)
              String latestOffsetFile =
                  offsetFiles.stream()
                      .filter(
                          file -> {
                            // Extract filename from path (handle both full paths and just
                            // filenames)
                            String filename =
                                file.contains("/")
                                    ? file.substring(file.lastIndexOf("/") + 1)
                                    : file;
                            // Check if filename is a numeric string
                            try {
                              Integer.parseInt(filename);
                              return true;
                            } catch (NumberFormatException e) {
                              return false;
                            }
                          })
                      .max(
                          (file1, file2) -> {
                            // Compare numeric values of filenames
                            String filename1 =
                                file1.contains("/")
                                    ? file1.substring(file1.lastIndexOf("/") + 1)
                                    : file1;
                            String filename2 =
                                file2.contains("/")
                                    ? file2.substring(file2.lastIndexOf("/") + 1)
                                    : file2;
                            try {
                              int num1 = Integer.parseInt(filename1);
                              int num2 = Integer.parseInt(filename2);
                              return Integer.compare(num1, num2);
                            } catch (NumberFormatException e) {
                              return 0;
                            }
                          })
                      .orElse(null);

              if (latestOffsetFile == null) {
                log.warn("No numeric offset files found in checkpoint path: {}", offsetsPath);
                return Single.just(createEmptyCheckpointOffsets(checkpointPath));
              }

              // Read and parse the latest offset file
              return S3Utils.readFileContent(s3Client, s3Config, latestOffsetFile)
                  .map(content -> parseOffsetFile(content, checkpointPath))
                  .onErrorReturn(
                      throwable -> {
                        log.error("Error parsing offset file: {}", latestOffsetFile, throwable);
                        return createEmptyCheckpointOffsets(checkpointPath);
                      });
            })
        .onErrorReturn(
            throwable -> {
              log.error(
                  "Error listing offset files in checkpoint path: {}", offsetsPath, throwable);
              return createEmptyCheckpointOffsets(checkpointPath);
            });
  }

  private SparkCheckpointOffsets parseOffsetFile(String content, String checkpointPath) {
    try {
      // Spark checkpoint file format:
      // v1
      // {config JSON}
      // {offsets JSON}
      //
      // We need to skip the version header and config, then parse the offsets JSON
      // Handle both \n and \r\n line endings
      String normalizedContent = content.replace("\r\n", "\n").replace("\r", "\n");
      String[] lines = normalizedContent.split("\n");

      if (lines.length < 2) {
        log.warn("Checkpoint file has unexpected format, expected at least 2 lines");
        return createEmptyCheckpointOffsets(checkpointPath);
      }

      // Find the last non-empty line which should contain the offsets JSON
      // Skip version header (starts with "v") and parse the last JSON object
      String offsetsJson = null;
      for (int i = lines.length - 1; i >= 0; i--) {
        String line = lines[i].trim();
        if (!line.isEmpty() && !line.startsWith("v") && line.startsWith("{")) {
          offsetsJson = line;
          break;
        }
      }

      if (offsetsJson == null) {
        log.warn("No offsets JSON found in checkpoint file");
        return createEmptyCheckpointOffsets(checkpointPath);
      }

      // Parse the offsets JSON: {"topic-name": {"partition": offset}}
      JsonNode offsetsNode = objectMapper.readTree(offsetsJson);
      Map<TopicPartition, Long> offsets = new HashMap<>();

      // Parse offsets - expect structure: {"topic": {"partition": offset}}
      if (offsetsNode.isObject()) {
        offsetsNode
            .fields()
            .forEachRemaining(
                topicEntry -> {
                  String topic = topicEntry.getKey();
                  JsonNode partitionOffsets = topicEntry.getValue();
                  // Check if this looks like partition offsets (object with numeric keys)
                  if (partitionOffsets.isObject()) {
                    partitionOffsets
                        .fields()
                        .forEachRemaining(
                            partitionEntry -> {
                              try {
                                int partition = Integer.parseInt(partitionEntry.getKey());
                                long offset = partitionEntry.getValue().asLong();
                                offsets.put(new TopicPartition(topic, partition), offset);
                              } catch (NumberFormatException e) {
                                // Ignore invalid partition numbers
                              }
                            });
                  }
                });
      }

      // If no offsets were parsed, this might be config JSON, not offsets JSON
      if (offsets.isEmpty()) {
        log.warn("Checkpoint file does not contain valid offsets structure");
        return createEmptyCheckpointOffsets(checkpointPath);
      }

      log.info("Parsed {} partition offsets from checkpoint", offsets.size());
      return SparkCheckpointOffsets.builder()
          .checkpointPath(checkpointPath)
          .offsets(offsets)
          .available(true)
          .lastUpdatedTimestamp(System.currentTimeMillis())
          .build();
    } catch (Exception e) {
      log.error("Error parsing checkpoint offset file content", e);
      return createEmptyCheckpointOffsets(checkpointPath);
    }
  }

  private SparkCheckpointOffsets createEmptyCheckpointOffsets(String checkpointPath) {
    return SparkCheckpointOffsets.builder()
        .checkpointPath(checkpointPath)
        .offsets(Collections.emptyMap())
        .available(false)
        .lastUpdatedTimestamp(System.currentTimeMillis())
        .build();
  }
}
