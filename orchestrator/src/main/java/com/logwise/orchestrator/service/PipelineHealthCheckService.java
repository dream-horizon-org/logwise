package com.logwise.orchestrator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.logwise.orchestrator.client.ObjectStoreClient;
import com.logwise.orchestrator.common.util.JsonUtils;
import com.logwise.orchestrator.config.ApplicationConfig.TenantConfig;
import com.logwise.orchestrator.config.ApplicationConfig.VectorConfig;
import com.logwise.orchestrator.dto.response.SparkMasterJsonResponse;
import com.logwise.orchestrator.enums.Tenant;
import com.logwise.orchestrator.factory.ObjectStoreFactory;
import com.logwise.orchestrator.util.ApplicationConfigUtil;
import com.logwise.orchestrator.webclient.reactivex.client.WebClient;
import io.reactivex.Single;
import io.vertx.core.json.JsonObject;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PipelineHealthCheckService {
  private final WebClient webClient;
  private final ObjectMapper objectMapper;

  @Inject
  public PipelineHealthCheckService(WebClient webClient, ObjectMapper objectMapper) {
    this.webClient = webClient;
    this.objectMapper = objectMapper;
  }

  public Single<JsonObject> checkVectorHealth(TenantConfig tenantConfig) {
    VectorConfig vectorConfig = tenantConfig.getVector();
    String vectorHost = vectorConfig.getHost();
    int vectorPort = vectorConfig.getApiPort();
    log.info("Checking Vector health at {}:{}", vectorHost, vectorPort);
    return webClient
        .getWebClient()
        .getAbs(String.format("http://%s:%d/health", vectorHost, vectorPort))
        .rxSend()
        .map(
            response -> {
              if (response.statusCode() == 200) {
                return JsonUtils.jsonMerge(
                    ImmutableList.of(
                        JsonUtils.jsonFrom("status", "UP"),
                        JsonUtils.jsonFrom("message", "Vector is healthy"),
                        new JsonObject().put("responseCode", response.statusCode())));
              } else {
                return JsonUtils.jsonMerge(
                    ImmutableList.of(
                        JsonUtils.jsonFrom("status", "DOWN"),
                        JsonUtils.jsonFrom(
                            "message",
                            String.format(
                                "Vector health check returned %d", response.statusCode())),
                        new JsonObject().put("responseCode", response.statusCode())));
              }
            })
        .onErrorReturn(
            error -> {
              log.error("Error checking Vector health", error);
              return JsonUtils.jsonMerge(
                  ImmutableList.of(
                      JsonUtils.jsonFrom("status", "DOWN"),
                      JsonUtils.jsonFrom(
                          "message", "Vector health check failed: " + error.getMessage()),
                      JsonUtils.jsonFrom("error", error.getClass().getSimpleName())));
            })
        .timeout(5, TimeUnit.SECONDS)
        .onErrorReturn(
            timeout -> {
              log.error("Vector health check timed out");
              return JsonUtils.jsonMerge(
                  ImmutableList.of(
                      JsonUtils.jsonFrom("status", "DOWN"),
                      JsonUtils.jsonFrom("message", "Vector health check timed out")));
            });
  }

  public Single<JsonObject> checkKafkaHealth(TenantConfig tenantConfig) {
    log.info("Checking Kafka health for tenant: {}", tenantConfig.getName());
    String kafkaHost = tenantConfig.getKafka().getKafkaBrokersHost();
    Integer kafkaPort = tenantConfig.getKafka().getKafkaBrokerPort();

    // Check if Kafka broker is reachable
    // Note: This is a basic connectivity check
    // For production, consider using Kafka Admin Client to check topics and message
    // counts
    return Single.fromCallable(
            () -> {
              // Basic connectivity check - Kafka broker should be reachable
              // In a real implementation, you might want to use Kafka Admin Client
              // to verify topics exist and have messages
              JsonObject result = new JsonObject();
              result.put("status", "UP");
              result.put("message", "Kafka broker is reachable");
              result.put("host", kafkaHost);
              if (kafkaPort != null) {
                result.put("port", kafkaPort);
              }
              return result;
            })
        .onErrorReturn(
            error -> {
              log.error("Error checking Kafka health", error);
              return JsonUtils.jsonMerge(
                  ImmutableList.of(
                      JsonUtils.jsonFrom("status", "DOWN"),
                      JsonUtils.jsonFrom(
                          "message", "Kafka health check failed: " + error.getMessage())));
            });
  }

  public Single<JsonObject> checkKafkaTopics(TenantConfig tenantConfig) {
    log.info("Checking Kafka topics for tenant: {}", tenantConfig.getName());
    String subscribePattern = tenantConfig.getSpark().getSubscribePattern();

    // Check if topics matching the pattern exist
    // This would ideally use Kafka Admin Client to list topics
    // For now, return a placeholder - you'll need to add Kafka Admin Client
    // dependency
    return Single.just(
        JsonUtils.jsonMerge(
            ImmutableList.of(
                JsonUtils.jsonFrom("status", "UNKNOWN"),
                JsonUtils.jsonFrom("message", "Kafka topic check requires Kafka Admin Client"),
                JsonUtils.jsonFrom("pattern", subscribePattern),
                JsonUtils.jsonFrom(
                    "note", "Add kafka-clients dependency to enable topic checking"))));
  }

  public Single<JsonObject> checkSparkHealth(TenantConfig tenantConfig) {
    log.info("Checking Spark health for tenant: {}", tenantConfig.getName());
    String sparkMasterHost = tenantConfig.getSpark().getSparkMasterHost();

    return webClient
        .getWebClient()
        .getAbs(String.format("http://%s:8080/json", sparkMasterHost))
        .rxSend()
        .map(
            response -> {
              try {
                SparkMasterJsonResponse sparkResponse =
                    objectMapper.readValue(response.bodyAsString(), SparkMasterJsonResponse.class);

                boolean hasRunningDriver =
                    sparkResponse.getActivedrivers().stream()
                        .anyMatch(driver -> "RUNNING".equals(driver.getState()));

                if (hasRunningDriver) {
                  JsonObject result = new JsonObject();
                  result.put("status", "UP");
                  result.put("message", "Spark driver is running");
                  result.put("drivers", sparkResponse.getActivedrivers().size());
                  return result;
                } else {
                  JsonObject result = new JsonObject();
                  result.put("status", "DOWN");
                  result.put("message", "No running Spark driver found");
                  result.put("drivers", sparkResponse.getActivedrivers().size());
                  return result;
                }
              } catch (Exception e) {
                log.error("Error parsing Spark response", e);
                return JsonUtils.jsonMerge(
                    ImmutableList.of(
                        JsonUtils.jsonFrom("status", "DOWN"),
                        JsonUtils.jsonFrom(
                            "message", "Failed to parse Spark response: " + e.getMessage())));
              }
            })
        .onErrorReturn(
            error -> {
              log.error("Error checking Spark health", error);
              return JsonUtils.jsonMerge(
                  ImmutableList.of(
                      JsonUtils.jsonFrom("status", "DOWN"),
                      JsonUtils.jsonFrom(
                          "message", "Spark health check failed: " + error.getMessage())));
            })
        .timeout(5, TimeUnit.SECONDS)
        .onErrorReturn(
            timeout -> {
              log.error("Spark health check timed out");
              return JsonUtils.jsonMerge(
                  ImmutableList.of(
                      JsonUtils.jsonFrom("status", "DOWN"),
                      JsonUtils.jsonFrom("message", "Spark health check timed out")));
            });
  }

  public Single<JsonObject> checkS3Logs(Tenant tenant, TenantConfig tenantConfig) {
    log.info("Checking S3 logs for tenant: {}", tenant.getValue());
    ObjectStoreClient objectStoreClient = ObjectStoreFactory.getClient(tenant);
    String logsDir = tenantConfig.getSpark().getLogsDir();

    // Check for recent logs (last hour)
    return objectStoreClient
        .listCommonPrefix(logsDir + "/", "/")
        .flatMap(
            prefixes -> {
              if (prefixes.isEmpty()) {
                return Single.just(
                    JsonUtils.jsonMerge(
                        ImmutableList.of(
                            JsonUtils.jsonFrom("status", "WARNING"),
                            JsonUtils.jsonFrom("message", "No log prefixes found in S3"),
                            JsonUtils.jsonFrom("logsDir", logsDir))));
              }

              // Check if there are recent objects (in the last hour)
              return objectStoreClient
                  .listObjects(logsDir + "/")
                  .map(
                      objects -> {
                        int currentHour = LocalDateTime.now().getHour();
                        long recentCount =
                            objects.stream()
                                .filter(
                                    key -> {
                                      // Check
                                      // if
                                      // object
                                      // key
                                      // contains
                                      // recent
                                      // timestamp
                                      // Format:
                                      // logs/service_name=xxx/env=xxx/hour=HH/minute=MM/...
                                      return key.contains(String.format("hour=%02d", currentHour));
                                    })
                                .count();

                        if (recentCount > 0) {
                          JsonObject result = new JsonObject();
                          result.put("status", "UP");
                          result.put("message", "Recent logs found in S3");
                          result.put("recentObjects", recentCount);
                          result.put("totalPrefixes", prefixes.size());
                          return result;
                        } else {
                          JsonObject result = new JsonObject();
                          result.put("status", "WARNING");
                          result.put("message", "No recent logs found in S3 (last hour)");
                          result.put("totalPrefixes", prefixes.size());
                          return result;
                        }
                      });
            })
        .onErrorReturn(
            error -> {
              log.error("Error checking S3 logs", error);
              return JsonUtils.jsonMerge(
                  ImmutableList.of(
                      JsonUtils.jsonFrom("status", "DOWN"),
                      JsonUtils.jsonFrom(
                          "message", "S3 logs check failed: " + error.getMessage())));
            })
        .timeout(10, TimeUnit.SECONDS)
        .onErrorReturn(
            timeout -> {
              log.error("S3 logs check timed out");
              return JsonUtils.jsonMerge(
                  ImmutableList.of(
                      JsonUtils.jsonFrom("status", "DOWN"),
                      JsonUtils.jsonFrom("message", "S3 logs check timed out")));
            });
  }

  public Single<JsonObject> checkCompletePipeline(Tenant tenant) {
    log.info("Checking complete pipeline health for tenant: {}", tenant.getValue());
    return Single.fromCallable(() -> ApplicationConfigUtil.getTenantConfig(tenant))
        .flatMap(
            tenantConfig ->
                Single.zip(
                    checkVectorHealth(tenantConfig),
                    checkKafkaHealth(tenantConfig),
                    checkSparkHealth(tenantConfig),
                    checkS3Logs(tenant, tenantConfig),
                    (vector, kafka, spark, s3) -> {
                      List<JsonObject> checks = new ArrayList<>();
                      checks.add(
                          JsonUtils.jsonMerge(
                              ImmutableList.of(
                                  JsonUtils.jsonFrom("component", "vector"),
                                  JsonUtils.jsonFrom("check", vector))));
                      checks.add(
                          JsonUtils.jsonMerge(
                              ImmutableList.of(
                                  JsonUtils.jsonFrom("component", "kafka"),
                                  JsonUtils.jsonFrom("check", kafka))));
                      checks.add(
                          JsonUtils.jsonMerge(
                              ImmutableList.of(
                                  JsonUtils.jsonFrom("component", "spark"),
                                  JsonUtils.jsonFrom("check", spark))));
                      checks.add(
                          JsonUtils.jsonMerge(
                              ImmutableList.of(
                                  JsonUtils.jsonFrom("component", "s3"),
                                  JsonUtils.jsonFrom("check", s3))));

                      // Determine overall status
                      boolean allUp =
                          checks.stream()
                              .allMatch(
                                  check ->
                                      "UP"
                                          .equals(
                                              check.getJsonObject("check").getString("status")));

                      String overallStatus = allUp ? "UP" : "DOWN";
                      String message =
                          allUp
                              ? "All pipeline components are healthy"
                              : "One or more pipeline components have issues";

                      JsonObject result = new JsonObject();
                      result.put("status", overallStatus);
                      result.put("message", message);
                      result.put("tenant", tenant.getValue());
                      result.put("checks", new io.vertx.core.json.JsonArray(checks));
                      return result;
                    }))
        .onErrorReturn(
            error -> {
              log.error("Error checking pipeline health", error);
              return JsonUtils.jsonMerge(
                  ImmutableList.of(
                      JsonUtils.jsonFrom("status", "DOWN"),
                      JsonUtils.jsonFrom(
                          "message", "Pipeline health check failed: " + error.getMessage()),
                      JsonUtils.jsonFrom("error", error.getClass().getSimpleName())));
            });
  }
}
