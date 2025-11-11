package com.dream11.logcentralorchestrator.service;

import static com.dream11.logcentralorchestrator.config.ApplicationConfig.TenantConfig;
import static java.lang.String.format;

import com.dream11.logcentralorchestrator.client.ObjectStoreClient;
import com.dream11.logcentralorchestrator.config.ApplicationConfig.KafkaConfig;
import com.dream11.logcentralorchestrator.config.ApplicationConfig.SparkConfig;
import com.dream11.logcentralorchestrator.constant.ApplicationConstants;
import com.dream11.logcentralorchestrator.dao.SparkSubmitStatusDao;
import com.dream11.logcentralorchestrator.dto.entity.*;
import com.dream11.logcentralorchestrator.dto.request.SubmitSparkJobRequest;
import com.dream11.logcentralorchestrator.dto.response.SparkMasterJsonResponse;
import com.dream11.logcentralorchestrator.dto.response.SparkMasterJsonResponse.Driver;
import com.dream11.logcentralorchestrator.enums.Tenant;
import com.dream11.logcentralorchestrator.error.ServiceError;
import com.dream11.logcentralorchestrator.factory.ObjectStoreFactory;
import com.dream11.logcentralorchestrator.rest.exception.RestException;
import com.dream11.logcentralorchestrator.util.ApplicationConfigUtil;
import com.dream11.logcentralorchestrator.util.WebClientUtils;
import com.dream11.logcentralorchestrator.webclient.reactivex.client.WebClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SparkService {
  private final WebClient d11WebClient;
  private final ObjectMapper objectMapper;
  private final SparkSubmitStatusDao sparkSubmitStatusDao;

  @Inject
  public SparkService(
      WebClient d11WebClient,
      ObjectMapper objectMapper,
      SparkSubmitStatusDao sparkSubmitStatusDao) {
    this.d11WebClient = d11WebClient;
    this.objectMapper = objectMapper;
    this.sparkSubmitStatusDao = sparkSubmitStatusDao;
  }

  private static SubmitSparkJobRequest getSparkSubmitRequestBody(
      TenantConfig tenantConfig,
      Long startingOffsetsTimestamp,
      Integer driverCores,
      Integer driverMemoryInGb) {
    log.info("Creating Spark Submit Request Body for tenant: {}", tenantConfig.getName());
    SparkConfig sparkConf = tenantConfig.getSpark();
    KafkaConfig kafkaConf = tenantConfig.getKafka();
    List<String> appArgs =
        List.of(
            format("kafka.cluster.dns=%s", kafkaConf.getKafkaBrokersHost()),
            format("kafka.cluster.name=%s", kafkaConf.getKafkaManagerClusterName()),
            format("kafka.manager.host=\"%s\"", kafkaConf.getKafkaManagerUrl()),
            format("kafka.maxRatePerPartition=%s", sparkConf.getKafkaMaxRatePerPartition()),
            format("kafka.startingOffsets=%s", sparkConf.getKafkaStartingOffsets()),
            format("kafka.subscribePattern=\"%s\"", sparkConf.getSubscribePattern()),
            format("spark.master.host=\"http://%s:8080\"", sparkConf.getSparkMasterHost()),
            format("kafka.startingOffsetsTimestamp=%d", startingOffsetsTimestamp));

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

    return SubmitSparkJobRequest.builder()
        .appArgs(appArgs)
        .appResource(sparkConf.getSparkJarPath())
        .clientSparkVersion(sparkConf.getClientSparkVersion())
        .mainClass(sparkConf.getMainClass())
        .environmentVariables(environmentVariables)
        .sparkProperties(sparkProperties)
        .build();
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
    AtomicBoolean jobSubmitted = new AtomicBoolean(false);
    return sparkSubmitStatusDao
        .getPendingSparkSubmitStatus(tenant)
        .map(status -> getSparkSubmitArgs(status, response))
        .flatMapCompletable(
            args -> {
              Completable completable = Completable.complete();
              if (args.getCleanStateRequired()) {
                completable =
                    completable
                        .andThen(
                            Completable.defer(
                                () -> {
                                  if (!isDriverNotRunning(response)) {
                                    return killSparkDriver(
                                        tenantConfig, response.getActivedrivers().get(0).getId());
                                  }
                                  return Completable.complete();
                                }))
                        .andThen(Completable.defer(() -> cleanSparkState(tenant)));
              }
              if (args.getSubmitJobRequired()) {
                completable =
                    completable.andThen(
                        Completable.defer(
                                () ->
                                    submitSparkJob(
                                            tenantConfig,
                                            args.getTimeStamp(),
                                            driverCores,
                                            driverMemoryInGb)
                                        .andThen(
                                            Completable.defer(
                                                () ->
                                                    updateSparkSubmitStatusFlags(
                                                        args.getSparkSubmitStatusId(),
                                                        args.getSubmittedForOffsetsTimestamp(),
                                                        args.getResumedToSubscribePattern()))))
                            .doOnComplete(() -> jobSubmitted.set(true)));
              }
              return completable;
            })
        .toSingle(jobSubmitted::get);
  }

  private Completable updateSparkSubmitStatusFlags(
      Long sparkSubmitStatusId,
      Boolean isSubmittedForOffsetsTimestamp,
      Boolean isResumedToSubscribePattern) {
    if (sparkSubmitStatusId != null && isSubmittedForOffsetsTimestamp != null) {
      return sparkSubmitStatusDao.updateIsSubmittedForOffsetsTimestampFlag(
          sparkSubmitStatusId, isSubmittedForOffsetsTimestamp);
    }
    if (sparkSubmitStatusId != null && isResumedToSubscribePattern != null) {
      return sparkSubmitStatusDao.updateIsResumedToSubscribePatternFlag(
          sparkSubmitStatusId, isResumedToSubscribePattern);
    }
    return Completable.complete();
  }

  private static SparkSubmitArgs getSparkSubmitArgs(
      SparkSubmitStatus status, SparkMasterJsonResponse response) {
    Long timeStamp = 0L;
    boolean cleanStateRequired = false;
    boolean submitJobRequired = false;
    Long sparkSubmitStatusId = status.getId();
    Boolean submittedForOffsetsTimestamp = null;
    Boolean resumedToSubscribePattern = null;

    // If the driver is not running, submit the job
    if (isDriverNotRunning(response)) {
      submitJobRequired = true;
      if (status.getIsSubmittedForOffsetsTimestamp() && !status.getIsResumedToSubscribePattern()) {
        timeStamp = status.getStartingOffsetsTimestamp();
      }
    } else if (sparkSubmitStatusId != null) {
      // If the job is not submitted for offsets, submit the job
      if (!status.getIsSubmittedForOffsetsTimestamp()
          && status.getStartingOffsetsTimestamp() <= System.currentTimeMillis()) {
        timeStamp = status.getStartingOffsetsTimestamp();
        submitJobRequired = true;
        cleanStateRequired = true;
        submittedForOffsetsTimestamp = true;
      } else if (!status.getIsResumedToSubscribePattern()
          && status.getResumeToSubscribePatternTimestamp() <= System.currentTimeMillis()) {
        // If the job is submitted for offsets but not resumed to subscribe pattern, submit the job
        cleanStateRequired = true;
        submitJobRequired = true;
        resumedToSubscribePattern = true;
      }
    }
    SparkSubmitArgs args =
        new SparkSubmitArgs(
            timeStamp,
            cleanStateRequired,
            submitJobRequired,
            sparkSubmitStatusId,
            submittedForOffsetsTimestamp,
            resumedToSubscribePattern);
    log.info("Spark Submit Args: {}", args);
    return args;
  }

  private static boolean isDriverNotRunning(SparkMasterJsonResponse response) {
    List<Driver> activeDrivers = response.getActivedrivers();
    return activeDrivers.isEmpty()
        || activeDrivers.stream().noneMatch(driver -> driver.getState().equals("RUNNING"));
  }

  private Single<SparkMasterJsonResponse> getSparkMasterJsonResponse(String sparkMasterHost) {
    log.info("Fetching Spark Master JSON Response for host: {}", sparkMasterHost);
    return d11WebClient
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

  public Completable killSparkDriver(TenantConfig tenantConfig, String driverId) {
    log.info("Killing Spark Driver: {} for tenant: {}", driverId, tenantConfig.getName());
    return this.d11WebClient
        .getWebClient()
        .postAbs(
            format(
                "http://%s:6066/v1/submissions/kill/%s",
                tenantConfig.getSpark().getSparkMasterHost(), driverId))
        .rxSend()
        .map(
            response -> {
              log.info(
                  "Killed Spark Driver: {} for tenant: {} with response: {}",
                  driverId,
                  tenantConfig.getName(),
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
              log.error("Error in killing spark driver: {}", driverId, error);
              return Single.error(
                  new RestException(
                      ServiceError.SPARK_DRIVER_KILL_ERROR.format(error.getMessage())));
            })
        .ignoreElement();
  }

  public Completable submitSparkJob(
      TenantConfig tenantConfig,
      Long startingOffsetsTimestamp,
      Integer driverCores,
      Integer driverMemoryInGb) {
    SubmitSparkJobRequest request =
        getSparkSubmitRequestBody(
            tenantConfig, startingOffsetsTimestamp, driverCores, driverMemoryInGb);
    log.info("Submitting Spark Job: {}", request);
    return this.d11WebClient
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
