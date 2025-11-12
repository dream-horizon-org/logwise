package com.logwise.orchestrator.service;

import static com.logwise.orchestrator.config.ApplicationConfig.TenantConfig;
import static java.lang.String.format;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.logwise.orchestrator.client.ObjectStoreClient;
import com.logwise.orchestrator.config.ApplicationConfig.KafkaConfig;
import com.logwise.orchestrator.config.ApplicationConfig.SparkConfig;
import com.logwise.orchestrator.constant.ApplicationConstants;
import com.logwise.orchestrator.dto.request.SubmitSparkJobRequest;
import com.logwise.orchestrator.dto.response.SparkMasterJsonResponse;
import com.logwise.orchestrator.dto.response.SparkMasterJsonResponse.Driver;
import com.logwise.orchestrator.enums.Tenant;
import com.logwise.orchestrator.error.ServiceError;
import com.logwise.orchestrator.factory.ObjectStoreFactory;
import com.logwise.orchestrator.rest.exception.RestException;
import com.logwise.orchestrator.util.ApplicationConfigUtil;
import com.logwise.orchestrator.util.WebClientUtils;
import com.logwise.orchestrator.webclient.reactivex.client.WebClient;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SparkService {
  private final WebClient webClient;
  private final ObjectMapper objectMapper;

  @Inject
  public SparkService(WebClient webClient, ObjectMapper objectMapper) {
    this.webClient = webClient;
    this.objectMapper = objectMapper;
  }

  private static SubmitSparkJobRequest getSparkSubmitRequestBody(
      TenantConfig tenantConfig, Integer driverCores, Integer driverMemoryInGb) {
    log.info("Creating Spark Submit Request Body for tenant: {}", tenantConfig.getName());
    SparkConfig sparkConf = tenantConfig.getSpark();
    KafkaConfig kafkaConf = tenantConfig.getKafka();
    String bucketName = tenantConfig.getObjectStore().getAws().getBucket();
    List<String> appArgs =
        List.of(
            format("kafka.cluster.dns=%s", kafkaConf.getKafkaBrokersHost()),
            format("kafka.maxRatePerPartition=%s", sparkConf.getKafkaMaxRatePerPartition()),
            format("kafka.startingOffsets=%s", sparkConf.getKafkaStartingOffsets()),
            format("kafka.subscribePattern=\"%s\"", sparkConf.getSubscribePattern()),
            format("spark.master.host=\"http://%s:8080\"", sparkConf.getSparkMasterHost()),
            format("s3.bucket=%s", bucketName));

    String extraJavaOptions =
        format(
            "%s -Dlog4j.configuration=%s",
            ApplicationConstants.SPARK_GC_JAVA_OPTIONS, sparkConf.getLog4jPropertiesFilePath());

    Map<String, String> environmentVariables = new HashMap<>();
    environmentVariables.put("SPARK_ENV_LOADED", "1");
    environmentVariables.put(ApplicationConstants.HEADER_TENANT_NAME, tenantConfig.getName());
    if (sparkConf.getS3aAccessKey() != null) {
      environmentVariables.put("S3A-ACCESS-KEY", sparkConf.getS3aAccessKey());
    }
    if (sparkConf.getS3aSecretKey() != null) {
      environmentVariables.put("S3A-SECRET-KEY", sparkConf.getS3aSecretKey());
    }

    Map<String, Object> sparkProperties = new HashMap<>();
    sparkProperties.put("spark.app.name", sparkConf.getAppName());
    sparkProperties.put("spark.cores.max", sparkConf.getDriverCoresMax());
    sparkProperties.put(
        "spark.driver.cores",
        driverCores != null ? String.valueOf(driverCores) : sparkConf.getDriverCores());
    sparkProperties.put(
        "spark.driver.memory",
        driverMemoryInGb != null ? driverMemoryInGb + "G" : sparkConf.getDriverMemory());
    sparkProperties.put("spark.driver.extraJavaOptions", extraJavaOptions);
    sparkProperties.put("spark.driver.maxResultSize", sparkConf.getDriverMaxResultSize());
    sparkProperties.put("spark.driver.supervise", true);
    sparkProperties.put("spark.executor.cores", sparkConf.getExecutorCores());
    sparkProperties.put("spark.executor.memory", sparkConf.getExecutorMemory());
    sparkProperties.put("spark.executor.extraJavaOptions", extraJavaOptions);
    sparkProperties.put("spark.jars", sparkConf.getSparkJarPath());
    sparkProperties.put("spark.master", format("spark://%s:7077", sparkConf.getSparkMasterHost()));
    sparkProperties.put("spark.submit.deployMode", "cluster");
    sparkProperties.put("spark.scheduler.mode", "FAIR");
    sparkProperties.put("spark.scheduler.pool", "production");
    sparkProperties.put("spark.dynamicAllocation.enabled", true);
    sparkProperties.put("spark.shuffle.service.enabled", true);
    sparkProperties.put("spark.dynamicAllocation.executorIdleTimeout", 15);

    // AWS credentials for driver and executor environments
    String awsAccessKeyId = sparkConf.getAwsAccessKeyId();
    String awsSecretAccessKey = sparkConf.getAwsSecretAccessKey();
    String awsSessionToken = sparkConf.getAwsSessionToken();
    String awsRegion = sparkConf.getAwsRegion();
    if (awsRegion == null || awsRegion.isEmpty()) {
      awsRegion = "us-east-1";
    }

    if (awsAccessKeyId != null && !awsAccessKeyId.isEmpty()) {
      sparkProperties.put("spark.driverEnv.AWS_ACCESS_KEY_ID", awsAccessKeyId);
      sparkProperties.put("spark.executorEnv.AWS_ACCESS_KEY_ID", awsAccessKeyId);
    }
    if (awsSecretAccessKey != null && !awsSecretAccessKey.isEmpty()) {
      sparkProperties.put("spark.driverEnv.AWS_SECRET_ACCESS_KEY", awsSecretAccessKey);
      sparkProperties.put("spark.executorEnv.AWS_SECRET_ACCESS_KEY", awsSecretAccessKey);
    }
    if (awsSessionToken != null && !awsSessionToken.isEmpty()) {
      sparkProperties.put("spark.driverEnv.AWS_SESSION_TOKEN", awsSessionToken);
      sparkProperties.put("spark.executorEnv.AWS_SESSION_TOKEN", awsSessionToken);
    }
    sparkProperties.put("spark.driverEnv.AWS_REGION", awsRegion);
    sparkProperties.put("spark.executorEnv.AWS_REGION", awsRegion);

    // S3A Hadoop configuration properties
    if (awsAccessKeyId != null && !awsAccessKeyId.isEmpty() 
        && awsSecretAccessKey != null && !awsSecretAccessKey.isEmpty()) {
      sparkProperties.put("spark.hadoop.fs.s3a.impl", "org.apache.hadoop.fs.s3a.S3AFileSystem");
      sparkProperties.put("spark.hadoop.fs.s3a.path.style.access", "false");
      
      // Set credentials provider based on whether session token is present
      if (awsSessionToken != null && !awsSessionToken.isEmpty()) {
        sparkProperties.put("spark.hadoop.fs.s3a.aws.credentials.provider", 
            "org.apache.hadoop.fs.s3a.TemporaryAWSCredentialsProvider");
        sparkProperties.put("spark.hadoop.fs.s3a.session.token", awsSessionToken);
      } else {
        sparkProperties.put("spark.hadoop.fs.s3a.aws.credentials.provider", 
            "org.apache.hadoop.fs.s3a.SimpleAWSCredentialsProvider");
      }
      
      sparkProperties.put("spark.hadoop.fs.s3a.access.key", awsAccessKeyId);
      sparkProperties.put("spark.hadoop.fs.s3a.secret.key", awsSecretAccessKey);
      
      // Set S3A endpoint based on region
      String s3aEndpoint;
      if ("us-east-1".equals(awsRegion)) {
        s3aEndpoint = "s3.amazonaws.com";
      } else {
        s3aEndpoint = format("s3-%s.amazonaws.com", awsRegion);
      }
      sparkProperties.put("spark.hadoop.fs.s3a.endpoint", s3aEndpoint);
    }

    return SubmitSparkJobRequest.builder()
        .appArgs(appArgs)
        .appResource(sparkConf.getSparkJarPath())
        .clientSparkVersion(sparkConf.getClientSparkVersion())
        .mainClass(sparkConf.getMainClass())
        .environmentVariables(environmentVariables)
        .sparkProperties(sparkProperties)
        .build();
  }

  private static boolean isDriverNotRunning(SparkMasterJsonResponse response) {
    List<Driver> activeDrivers = response.getActivedrivers();
    return activeDrivers.isEmpty()
        || activeDrivers.stream().noneMatch(driver -> driver.getState().equals("RUNNING"));
  }

  /**
   * Monitor the spark job for 1 minute. If the job is not running, submit the job.
   *
   * @param tenant Tenant
   * @return completable
   */
  public Completable monitorSparkJob(Tenant tenant, Integer driverCores, Integer driverMemoryInGb) {
    TenantConfig tenantConfig = ApplicationConfigUtil.getTenantConfig(tenant);
    AtomicBoolean jobSubmitted = new AtomicBoolean(false);
    int take =
        ApplicationConstants.SPARK_MONITOR_TIME_IN_SECS
            / ApplicationConstants.SPARK_MONITOR_POLL_INTERVAL_SECS;
    return Flowable.interval(
            0, ApplicationConstants.SPARK_MONITOR_POLL_INTERVAL_SECS, TimeUnit.SECONDS)
        .take(take)
        .takeUntil(
            __ -> {
              jobSubmitted.get();
            })
        .flatMapSingle(
            __ -> getSparkMasterJsonResponse(tenantConfig.getSpark().getSparkMasterHost()))
        .flatMapCompletable(
            response ->
                validateAndSubmitSparkJob(tenant, response, driverCores, driverMemoryInGb)
                    .flatMapCompletable(
                        submitted -> Completable.fromAction(() -> jobSubmitted.set(submitted))));
  }

  public Single<Boolean> validateAndSubmitSparkJob(
      Tenant tenant,
      SparkMasterJsonResponse response,
      Integer driverCores,
      Integer driverMemoryInGb) {
    TenantConfig tenantConfig = ApplicationConfigUtil.getTenantConfig(tenant);

    if (isDriverNotRunning(response)) {
      return cleanSparkState(tenant)
          .andThen(submitSparkJob(tenantConfig, driverCores, driverMemoryInGb))
          .toSingleDefault(true);
    }

    return Single.just(false);
  }

  private Single<SparkMasterJsonResponse> getSparkMasterJsonResponse(String sparkMasterHost) {
    log.info("Fetching Spark Master JSON Response for host: {}", sparkMasterHost);
    return webClient
        .getWebClient()
        .getAbs(format("http://%s:8080/json", sparkMasterHost))
        .rxSend()
        .map(
            response ->
                objectMapper.readValue(response.bodyAsString(), SparkMasterJsonResponse.class))
        .retryWhen(
            WebClientUtils.retryWithDelay(
                ApplicationConstants.DEFAULT_RETRY_DELAY_SECONDS,
                TimeUnit.SECONDS,
                ApplicationConstants.DEFAULT_MAX_RETRIES))
        .onErrorResumeNext(
            error -> {
              log.error(
                  "Error Fetching Spark Master JSON Response for host: {}", sparkMasterHost, error);
              return Single.error(
                  new RestException(ServiceError.SPARK_MASTER_ERROR.format(error.getMessage())));
            });
  }

  public Completable cleanSparkState(Tenant tenant) {
    log.info("Cleaning Spark State for tenant: {}", tenant.getValue());
    TenantConfig tenantConfig = ApplicationConfigUtil.getTenantConfig(tenant);
    ObjectStoreClient objectStoreClient = ObjectStoreFactory.getClient(tenant);

    Single<List<String>> checkPointFilesSingle =
        objectStoreClient.listObjects(tenantConfig.getSpark().getCheckPointDir() + "/");

    Single<List<String>> appSparkMetaDataFilesSingle =
        objectStoreClient.listObjects(
            format(
                "%s/%s/",
                tenantConfig.getSpark().getLogsDir(),
                ApplicationConstants.SPARK_METADATA_FILE_NAME));

    return Single.zip(
            checkPointFilesSingle,
            appSparkMetaDataFilesSingle,
            (checkPointFiles, sparkMetaDataFiles) -> {
              List<String> files = new ArrayList<>();
              files.addAll(checkPointFiles);
              files.addAll(sparkMetaDataFiles);
              log.info("Files to delete: {}", files);
              return files;
            })
        .flatMapCompletable(
            objects ->
                Flowable.fromIterable(objects)
                    .flatMapCompletable(objectStoreClient::deleteFile)
                    .onErrorResumeNext(
                        e -> {
                          log.error("Error occurred while deleting checkpoint files", e);
                          return Completable.error(e);
                        }))
        .doOnError(e -> log.error("Error occurred while deleting S3Objects", e));
  }

  public Completable submitSparkJob(
      TenantConfig tenantConfig, Integer driverCores, Integer driverMemoryInGb) {
    SubmitSparkJobRequest request =
        getSparkSubmitRequestBody(tenantConfig, driverCores, driverMemoryInGb);
    log.info("Submitting Spark Job: {}", request);
    return this.webClient
        .getWebClient()
        .postAbs(
            format(
                "http://%s:6066/v1/submissions/create",
                tenantConfig.getSpark().getSparkMasterHost()))
        .rxSendJson(request)
        .map(
            response -> {
              log.info(
                  "Spark Job Submission Response: {}",
                  response.bodyAsString().replaceAll("\\r?\\n", ""));
              return response;
            })
        .retryWhen(
            WebClientUtils.retryWithDelay(
                ApplicationConstants.DEFAULT_RETRY_DELAY_SECONDS,
                TimeUnit.SECONDS,
                ApplicationConstants.DEFAULT_MAX_RETRIES))
        .onErrorResumeNext(
            error -> {
              log.error("Error in submitting spark job: {}", request, error);
              return Single.error(
                  new RestException(ServiceError.SPARK_SUBMIT_ERROR.format(error.getMessage())));
            })
        .ignoreElement();
  }
}
