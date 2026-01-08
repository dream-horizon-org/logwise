package com.logwise.orchestrator.service;

import static com.logwise.orchestrator.config.ApplicationConfig.TenantConfig;
import static java.lang.String.format;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.google.cloud.Tuple;
import com.google.inject.Inject;
import com.logwise.orchestrator.CaffeineCacheFactory;
import com.logwise.orchestrator.client.AsgClient;
import com.logwise.orchestrator.client.ObjectStoreClient;
import com.logwise.orchestrator.client.VMClient;
import com.logwise.orchestrator.common.util.CompletableFutureUtils;
import com.logwise.orchestrator.config.ApplicationConfig.KafkaConfig;
import com.logwise.orchestrator.config.ApplicationConfig.SparkConfig;
import com.logwise.orchestrator.constant.ApplicationConstants;
import com.logwise.orchestrator.dao.SparkScaleOverrideDao;
import com.logwise.orchestrator.dao.SparkStageHistoryDao;
import com.logwise.orchestrator.dto.entity.SparkScaleArgs;
import com.logwise.orchestrator.dto.entity.SparkScaleOverride;
import com.logwise.orchestrator.dto.entity.SparkStageHistory;
import com.logwise.orchestrator.dto.mapper.SparkScaleOverrideMapper;
import com.logwise.orchestrator.dto.request.SubmitSparkJobRequest;
import com.logwise.orchestrator.dto.request.UpdateSparkScaleOverrideRequest;
import com.logwise.orchestrator.dto.response.GetSparkStageHistoryResponse;
import com.logwise.orchestrator.dto.response.SparkMasterJsonResponse;
import com.logwise.orchestrator.dto.response.SparkMasterJsonResponse.Driver;
import com.logwise.orchestrator.enums.Tenant;
import com.logwise.orchestrator.error.ServiceError;
import com.logwise.orchestrator.factory.AsgFactory;
import com.logwise.orchestrator.factory.ObjectStoreFactory;
import com.logwise.orchestrator.factory.VMFactory;
import com.logwise.orchestrator.rest.exception.RestException;
import com.logwise.orchestrator.util.ApplicationConfigUtil;
import com.logwise.orchestrator.util.WebClientUtils;
import com.logwise.orchestrator.webclient.reactivex.client.WebClient;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.vertx.reactivex.core.Vertx;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public class SparkService {
  private final WebClient webClient;
  private final ObjectMapper objectMapper;
  private final SparkStageHistoryDao sparkStageHistoryDao;
  private final AsyncLoadingCache<String, SparkMasterJsonResponse> getSparkMasterJsonResponseCache;
  private final SparkScaleOverrideDao sparkScaleOverrideDao;

  @Inject
  public SparkService(
      Vertx vertx,
      WebClient webClient,
      ObjectMapper objectMapper,
      SparkStageHistoryDao sparkStageHistoryDao,
      SparkScaleOverrideDao sparkScaleOverrideDao) {

    this.webClient = webClient;
    this.objectMapper = objectMapper;
    this.sparkStageHistoryDao = sparkStageHistoryDao;
    this.getSparkMasterJsonResponseCache =
        CaffeineCacheFactory.createAsyncLoadingCache(
            vertx,
            ApplicationConstants.GET_SPARK_MASTER_JSON_RESPONSE_CACHE,
            this::getSparkMasterJsonResponse,
            ApplicationConstants.GET_SPARK_MASTER_JSON_RESPONSE_CACHE);
    this.sparkScaleOverrideDao = sparkScaleOverrideDao;
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
            format("logCentral.orchestrator.url=\"%s\"", tenantConfig.getOrchestrator().getUrl()),
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

    // S3A Hadoop configuration properties - always configure S3A
    sparkProperties.put("spark.hadoop.fs.s3a.impl", "org.apache.hadoop.fs.s3a.S3AFileSystem");
    sparkProperties.put("spark.hadoop.fs.s3a.path.style.access", "false");

    // Set S3A endpoint based on region
    String s3aEndpoint;
    if ("us-east-1".equals(awsRegion)) {
      s3aEndpoint = "s3.amazonaws.com";
    } else {
      s3aEndpoint = format("s3-%s.amazonaws.com", awsRegion);
    }
    sparkProperties.put("spark.hadoop.fs.s3a.endpoint", s3aEndpoint);

    // Configure credentials provider based on whether credentials are provided
    if (awsAccessKeyId != null
        && !awsAccessKeyId.isEmpty()
        && awsSecretAccessKey != null
        && !awsSecretAccessKey.isEmpty()) {
      // Use explicit credentials
      if (awsSessionToken != null && !awsSessionToken.isEmpty()) {
        sparkProperties.put(
            "spark.hadoop.fs.s3a.aws.credentials.provider",
            "org.apache.hadoop.fs.s3a.TemporaryAWSCredentialsProvider");
        sparkProperties.put("spark.hadoop.fs.s3a.session.token", awsSessionToken);
      } else {
        sparkProperties.put(
            "spark.hadoop.fs.s3a.aws.credentials.provider",
            "org.apache.hadoop.fs.s3a.SimpleAWSCredentialsProvider");
      }

      sparkProperties.put("spark.hadoop.fs.s3a.access.key", awsAccessKeyId);
      sparkProperties.put("spark.hadoop.fs.s3a.secret.key", awsSecretAccessKey);
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

  private static boolean isValidWalFile(String walFile) {
    return StringUtils.isNumeric(StringUtils.substringBefore(walFile, ".compact"));
  }

  private static String getLatestWalFile(List<String> walFiles) {
    String latestWafFile = walFiles.get(0);
    for (String walFile : walFiles) {
      if (Integer.parseInt(walFile.replaceAll(".compact", ""))
          > Integer.parseInt(latestWafFile.replaceAll(".compact", ""))) {
        latestWafFile = walFile;
      }
    }
    log.info("Latest WAL File: {} in list: {}", latestWafFile, walFiles);
    return latestWafFile;
  }

  private static double averageGrowthRate(List<Long> numbers) {
    if (numbers.size() < 2) {
      return 0;
    }
    double sumGrowthRates = 0;
    for (int i = 0; i < numbers.size() - 1; i++) {
      double growthRate = (double) (numbers.get(i) - numbers.get(i + 1)) / numbers.get(i + 1);
      sumGrowthRates += growthRate;
    }
    return sumGrowthRates / (numbers.size() - 1);
  }

  private static Integer getWorkersFromCores(Integer cores, TenantConfig tenantConfig) {
    if (cores == null || cores.equals(0)) {
      return null;
    }
    return (int) Math.ceil((double) cores / tenantConfig.getSpark().getExecutorCoresPerMachine());
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
      return submitSparkJob(tenantConfig, driverCores, driverMemoryInGb).toSingleDefault(true);
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

  public Completable insertSparkStageHistory(SparkStageHistory sparkStageHistory) {
    log.info("Inserting Spark Stage History: {}", sparkStageHistory);
    return sparkStageHistoryDao
        .insertSparkStageHistory(sparkStageHistory)
        .doOnComplete(() -> log.info("Inserted Spark Stage History: {}", sparkStageHistory));
  }

  public Completable scaleSpark(Tenant tenant, boolean enableUpScale, boolean enableDownScale) {
    log.info("scaling spark for tenant: {}", tenant.getValue());
    return sparkScaleOverrideDao
        .getSparkScaleOverride(tenant)
        .map(
            sparkScaleOverride -> {
              log.info("Spark Scale Override For tenant:{} is  {}", tenant, sparkScaleOverride);
              return Tuple.of(
                  sparkScaleOverride.getUpscale() != null
                      ? sparkScaleOverride.getUpscale()
                      : enableUpScale,
                  sparkScaleOverride.getDownscale() != null
                      ? sparkScaleOverride.getDownscale()
                      : enableDownScale);
            })
        .flatMapCompletable(tuple -> processSparkScaling(tenant, tuple.x(), tuple.y()));
  }

  private Completable processSparkScaling(
      Tenant tenant, boolean enableUpScale, boolean enableDownScale) {
    if (!(enableDownScale || enableUpScale)) {
      log.info(
          "Ignoring spark scaling as both up and down scaling are disabled for tenant: {}",
          tenant.getValue());
      return Completable.complete();
    }
    log.info("Scaling Spark for tenant: {}", tenant.getValue());
    TenantConfig tenantConfig = ApplicationConfigUtil.getTenantConfig(tenant);
    Single<List<SparkStageHistory>> stageHistoryListSingle =
        sparkStageHistoryDao.getSparkStageHistory(
            tenant, ApplicationConstants.SPARK_HISTORY_MONITOR_COUNT, true);
    Single<List<String>> walFileListSingle = getWalFileList(tenant);
    Single<Integer> actualWorkersSingle = getActualSparkWorkers(tenant);

    Single<SparkScaleArgs> sparkScaleArgsSingle =
        Single.zip(
            stageHistoryListSingle,
            walFileListSingle,
            (stageHistoryList, walFileList) -> {
              Collections.sort(stageHistoryList);
              String latestWalFile = getLatestWalFile(walFileList);
              if (!StringUtils.isNumeric(latestWalFile)) {
                log.info("Ignoring spark scaling as latest WAL file is compacted.");
                throw new RestException(
                    ServiceError.IGNORE_SPARK_SCALING.format("Latest WAL file is compacted"));
              }
              int latestWafFileNumber = Integer.parseInt(latestWalFile);
              double bufferFactor = latestWafFileNumber % 10 >= 7 ? 0.5 : 0;
              Integer expectedWorkerCount =
                  getExpectedExecutorCount(stageHistoryList, bufferFactor, tenantConfig);
              return SparkScaleArgs.builder()
                  .minWorkerCount(tenantConfig.getSpark().getMinWorkerCount())
                  .maxWorkerCount(tenantConfig.getSpark().getMaxWorkerCount())
                  .workerCount(expectedWorkerCount)
                  .enableDownscale(enableDownScale && latestWafFileNumber % 10 != 8)
                  .enableUpscale(enableUpScale)
                  .build();
            });

    return Single.zip(
            actualWorkersSingle,
            sparkScaleArgsSingle,
            (actualWorkers, args) ->
                scaleSpark(actualWorkers, args, tenant).andThen(Single.just(true)))
        .flatMap(result -> result)
        .ignoreElement();
  }

  public Completable updateSparkScaleOverride(
      Tenant tenant, UpdateSparkScaleOverrideRequest request) {
    SparkScaleOverride sparkScaleOverride =
        SparkScaleOverrideMapper.toSparkScaleOverride(tenant.getValue(), request);
    log.info("Updating Spark Scale Override: {}", sparkScaleOverride);
    return sparkScaleOverrideDao.updateSparkScaleOverride(sparkScaleOverride);
  }

  private Completable scaleSpark(Integer actualWorkers, SparkScaleArgs args, Tenant tenant) {
    Integer expectedWorkers = args.getWorkerCount();
    if (expectedWorkers == null || expectedWorkers == 0 || actualWorkers == 0) {
      log.info("Not enough data to scale spark for tenant: {}", tenant.getValue());
      return Completable.complete();
    }

    expectedWorkers =
        Math.min(args.getMaxWorkerCount(), Math.max(args.getMinWorkerCount(), expectedWorkers));
    log.info(
        "expectedWorkers: {} actualWorkers: {} Tenant: {}", expectedWorkers, actualWorkers, tenant);

    if (expectedWorkers.equals(actualWorkers)) {
      log.info(
          "Actual Workers: {} and Expected Workers: {} are same for tenant: {}",
          actualWorkers,
          expectedWorkers,
          tenant.getValue());
      return Completable.complete();
    }

    if (expectedWorkers < actualWorkers
        && args.isEnableDownscale()
        && actualWorkers - expectedWorkers >= args.getMinimumDownscale()) {
      int maxDownscale = Math.min(args.getMaximumDownscale(), actualWorkers - expectedWorkers);
      if (1 - (double) expectedWorkers / actualWorkers >= args.getDownscaleProportion()) {
        maxDownscale =
            Math.min(
                args.getMaximumDownscale(),
                (int) Math.ceil(actualWorkers * args.getDownscaleProportion()));
      }
      expectedWorkers = actualWorkers - maxDownscale;
      return downscaleSpark(tenant, actualWorkers, expectedWorkers);
    }

    if (expectedWorkers > actualWorkers
        && args.isEnableUpscale()
        && expectedWorkers - actualWorkers >= args.getMinimumUpscale()) {
      int expectedWorker =
          actualWorkers + Math.min(args.getMaximumUpscale(), expectedWorkers - actualWorkers);
      return upscaleSpark(tenant, expectedWorker);
    }

    log.error(
        "Ignoring Spark Scaling for tenant: {} actualWorker: {} SparkScaleArgs: {}",
        tenant.getValue(),
        actualWorkers,
        args);
    return Completable.complete();
  }

  private Single<Integer> getActualSparkWorkers(Tenant tenant) {
    TenantConfig tenantConfig = ApplicationConfigUtil.getTenantConfig(tenant);
    return getSparkMasterJsonResponseFromCache(tenantConfig.getSpark().getSparkMasterHost())
        .onErrorReturnItem(new SparkMasterJsonResponse())
        .map(
            resp -> {
              Integer workersCount = resp.getAliveworkers();
              log.info("Actual Workers: {} for tenant: {}", workersCount, tenant.getValue());
              return workersCount;
            });
  }

  private Single<SparkMasterJsonResponse> getSparkMasterJsonResponseFromCache(
      String sparkMasterHost) {
    return CompletableFutureUtils.toSingle(
        this.getSparkMasterJsonResponseCache.get(sparkMasterHost));
  }

  private Completable downscaleSpark(Tenant tenant, int actualWorkers, int expectedWorkers) {
    log.info(
        "Downscaling Spark for tenant: {} from: {} to {}",
        tenant.getValue(),
        actualWorkers,
        expectedWorkers);
    TenantConfig config = ApplicationConfigUtil.getTenantConfig(tenant);
    VMClient vmClient = VMFactory.getSparkClient(tenant);
    AsgClient asgClient = AsgFactory.getSparkClient(tenant);
    if (vmClient != null && asgClient != null) {
      int downscaleCount = actualWorkers - expectedWorkers;
      Single<List<String>> nonDriverWorkerIps = getNonDriverWorkerIps(config);
      return nonDriverWorkerIps
          .map(ips -> ips.subList(0, downscaleCount))
          .flatMapCompletable(
              ipsToRemove ->
                  vmClient
                      .getInstanceIds(ipsToRemove)
                      .flatMapCompletable(
                          ipToIdMap -> {
                            String asgName =
                                config.getSpark().getCluster().getAsg().getAws().getName();
                            List<String> instancesToRemove =
                                ipToIdMap.values().stream()
                                    .filter(Objects::nonNull)
                                    .collect(Collectors.toList());
                            return asgClient
                                .removeInstances(asgName, instancesToRemove, true)
                                .andThen(
                                    Completable.defer(
                                        () -> vmClient.terminateInstances(instancesToRemove)));
                          }));
    }

    return Completable.complete();
  }

  private Single<List<String>> getNonDriverWorkerIps(TenantConfig config) {
    return getSparkMasterJsonResponseFromCache(config.getSpark().getSparkMasterHost())
        .map(
            resp -> {
              String driverWorkerId = resp.getActivedrivers().get(0).getWorker();
              List<String> ips =
                  resp.getWorkers().stream()
                      .filter(
                          worker ->
                              !worker.getId().equals(driverWorkerId)
                                  && worker.getState().equals("ALIVE"))
                      .map(SparkMasterJsonResponse.Worker::getHost)
                      .collect(Collectors.toList());
              log.info("Non driver worker IPs: {} for tenant: {}", ips, config.getName());
              return ips;
            });
  }

  private Completable upscaleSpark(Tenant tenant, int expectedWorker) {
    log.info("Upscaling Spark for tenant: {} workers: {}", tenant.getValue(), expectedWorker);
    TenantConfig config = ApplicationConfigUtil.getTenantConfig(tenant);

    AsgClient asgClient = AsgFactory.getSparkClient(tenant);
    if (asgClient != null) {
      String asgName = config.getSpark().getCluster().getAsg().getAws().getName();
      return asgClient.updateDesiredCapacity(asgName, expectedWorker);
    }
    log.error("AsgClient: {} is null for tenant: {}", asgClient, tenant.getValue());

    return Completable.complete();
  }

  private Single<List<String>> getWalFileList(Tenant tenant) {
    TenantConfig tenantConfig = ApplicationConfigUtil.getTenantConfig(tenant);
    return ObjectStoreFactory.getClient(tenant)
        .listObjects(
            tenantConfig.getSpark().getLogsDir()
                + "/"
                + ApplicationConstants.SPARK_METADATA_FILE_NAME)
        .map(
            keys ->
                keys.stream()
                    .map(
                        key ->
                            StringUtils.substringAfter(
                                    key, ApplicationConstants.SPARK_METADATA_FILE_NAME + "/")
                                .trim())
                    .filter(SparkService::isValidWalFile)
                    .collect(Collectors.toList()))
        .doOnSuccess(
            walFiles ->
                log.info("Found WAL Files: {} for tenant: {}", walFiles, tenant.getValue()));
  }

  private Integer getExpectedExecutorCount(
      List<SparkStageHistory> stageHistoryList, double bufferFactor, TenantConfig tenantConfig) {
    if (stageHistoryList.size() < ApplicationConstants.SPARK_HISTORY_MONITOR_COUNT) {
      log.info(
          "Not enough stageHistoryList to calculate expectedWorkersCount: {}", stageHistoryList);
      return null;
    }

    long maxInputRecords =
        stageHistoryList.stream().mapToLong(SparkStageHistory::getInputRecords).max().orElse(0);
    log.info("Max Input Records: {} in stageHistoryList: {}", maxInputRecords, stageHistoryList);

    boolean isIncrementalInputRecords = true;
    double incrementalBuffer = 0.0;

    for (int i = 1; i < Math.min(4, ApplicationConstants.SPARK_HISTORY_MONITOR_COUNT); i++) {
      if (stageHistoryList.get(i - 1).getInputRecords()
          < stageHistoryList.get(i).getInputRecords()) {
        isIncrementalInputRecords = false;
        break;
      }
    }

    if (isIncrementalInputRecords && maxInputRecords == stageHistoryList.get(0).getInputRecords()) {
      List<Long> incrementalInputRecords =
          stageHistoryList.stream()
              .map(SparkStageHistory::getInputRecords)
              .collect(Collectors.toList());
      incrementalBuffer = Math.max(0, averageGrowthRate(incrementalInputRecords));
    }

    double totalBuffer = incrementalBuffer + bufferFactor;
    log.info(
        "incrementalBuffer: {} bufferFactor: {} total: {}",
        incrementalBuffer,
        bufferFactor,
        totalBuffer);
    long maxInputRecordsWithBuffer =
        maxInputRecords + (long) Math.ceil(totalBuffer * maxInputRecords);
    log.info(
        "Expected InputRecords with buffer of {} = {}", totalBuffer, maxInputRecordsWithBuffer);

    int perCoreLogsProcess = tenantConfig.getSpark().getPerCoreLogsProcess();

    int expectedExecutorCores =
        (int) Math.ceil((double) maxInputRecordsWithBuffer / perCoreLogsProcess);
    log.info("Expected Executor Cores: {}", expectedExecutorCores);

    return getWorkersFromCores(expectedExecutorCores, tenantConfig);
  }

  public Single<GetSparkStageHistoryResponse> getSparkStageHistory(Tenant tenant, int limit) {
    return sparkStageHistoryDao
        .getSparkStageHistory(tenant, limit, false)
        .map(list -> GetSparkStageHistoryResponse.builder().sparkStageHistory(list).build());
  }
}
