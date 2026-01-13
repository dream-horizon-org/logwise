package com.logwise.orchestrator.constant;

import java.util.function.UnaryOperator;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ApplicationConstants {

  public final int AWS_SDK_RETRIES = 10;
  public final int AWS_SDK_MAX_CONCURRENCY = 1024;
  public final int AWS_SDK_BASE_RETRY_DELAY_SECONDS = 3;
  public final int AWS_SDK_MAX_BACK_OFF_TIME_SECONDS = 8;

  public final UnaryOperator<String> OBJECT_STORE_INJECTOR_NAME =
      tenantName -> "objectStore-" + tenantName;

  public final int DEFAULT_RETRY_DELAY_SECONDS = 2;
  public final int DEFAULT_MAX_RETRIES = 3;

  public final String HEADER_TENANT_NAME = "X-Tenant-Name";

  public final String GET_SERVICE_DETAILS_CACHE = "get-service-details-cache";
  public final int KAFKA_MAX_PRODUCER_RATE_PER_PARTITION = 5500;
  public final int KAFKA_BROKER_PORT = 9092;
  public final int KAFKA_REQUEST_TIMEOUT_MS = 30000;

  public final String SPARK_METADATA_FILE_NAME = "_spark_metadata";
  public final int SPARK_MONITOR_POLL_INTERVAL_SECS = 15;
  public final int SPARK_MONITOR_TIME_IN_SECS = 60;
  public final String SPARK_GC_JAVA_OPTIONS = "-XX:+UnlockExperimentalVMOptions -XX:+UseG1GC";

  public final int MAX_LOGS_SYNC_DELAY_HOURS = 3;
}
