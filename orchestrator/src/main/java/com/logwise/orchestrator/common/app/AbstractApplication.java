package com.logwise.orchestrator.common.app;

import com.google.inject.Module;
import com.logwise.orchestrator.common.guice.DefaultModule;
import com.logwise.orchestrator.common.util.CompletableUtils;
import com.logwise.orchestrator.common.util.MaintenanceUtils;
import com.logwise.orchestrator.config.constant.Constants;
import com.logwise.orchestrator.config.utils.WatchUtils;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.plugins.RxJavaPlugins;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Verticle;
import io.vertx.core.VertxOptions;
import io.vertx.core.impl.cpu.CpuCoreSensor;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.consul.KeyValue;
import io.vertx.ext.consul.Watch;
import io.vertx.ext.dropwizard.DropwizardMetricsOptions;
import io.vertx.reactivex.core.RxHelper;
import io.vertx.reactivex.core.Vertx;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

@Slf4j
public abstract class AbstractApplication {

  // TODO: Keep the event loop size less than #cores to allow for acceptor thread,
  // logback and other
  // processes
  public static final Integer NUM_OF_CORES = CpuCoreSensor.availableProcessors();

  @Getter protected Vertx vertx;
  private Watch<KeyValue> restartWatch;
  private final Thread shutdownThread = new Thread(this::gracefulShutdown);

  protected static Completable deployVerticles(
      final Vertx vertx, final List<Deployable> deployables) {
    return Observable.fromIterable(deployables)
        .flatMapSingle(
            deployable -> deployVerticle(vertx, deployable::getVerticle, deployable.getConfig()))
        .toList()
        .ignoreElement()
        .doOnError(error -> log.error("Failed to initialize application", error))
        .doOnComplete(() -> log.info("Started Application"));
  }

  protected static Single<String> deployVerticle(
      final Vertx vertx, final Supplier<Verticle> verticleSupplier, final VerticleConfig config) {
    val verticleName = verticleSupplier.getClass().getName();
    return vertx
        .rxDeployVerticle(verticleSupplier, getDeploymentOptions(config, verticleName))
        .doOnError(error -> log.error("Error in deploying verticle : {}", verticleName, error))
        .doOnSuccess(deploymentId -> log.info("Deployed verticle : {}", verticleName));
  }

  private static DeploymentOptions getDeploymentOptions(VerticleConfig config, String name) {
    DeploymentOptions deploymentOptions = new DeploymentOptions();
    deploymentOptions.setInstances(config.getInstances());
    switch (config.getVerticleType()) {
      case 1:
        log.info("deploying Worker Verticle");
        deploymentOptions
            .setWorkerPoolName(name)
            .setWorker(true)
            .setWorkerPoolSize(config.getThreadPoolSize());
        break;
      default:
        log.info("deploying Standard Verticle");
        break;
    }
    return deploymentOptions;
  }

  public Completable rxStartApplication() {
    // explicitly setting context class loader if not already set
    if (Thread.currentThread().getContextClassLoader() == null) {
      Thread.currentThread().setContextClassLoader(ClassLoader.getSystemClassLoader());
    }
    return rxInitVertx()
        .doOnSuccess(
            vertx -> {
              this.vertx = vertx; // set vertx instance
              setDefaultRxSchedulers(vertx);
              Runtime.getRuntime().addShutdownHook(shutdownThread);
            })
        .ignoreElement()
        .andThen(Completable.defer(this::rxDeployVerticles))
        .compose(CompletableUtils.applyDebugLogs(log));
  }

  protected Completable rxDeployVerticles() {
    log.info("Vertx Container Initialized.\nInitializing Google Guice Container.");
    AppContext.initialize(getAllGoogleGuiceModules(vertx));
    log.info("Initialized Google Guice Container.\nStarted Deploying Verticle");
    return deployVerticles(vertx, Arrays.asList(getVerticlesToDeploy(vertx)))
        .doOnError(error -> vertx.close());
  }

  /** Use `rxStartApplication` instead */
  @Deprecated
  public void startApplication() {
    rxStartApplication().subscribe();
  }

  public Completable rxStopApplication(Integer healthCheckWaitPeriod) {
    return Completable.complete()
        .doOnComplete(() -> MaintenanceUtils.setMaintenance(vertx))
        .doOnComplete(
            () ->
                log.info(
                    "Waiting for {} seconds so ELB stops sending new traffic",
                    healthCheckWaitPeriod))
        .delay(healthCheckWaitPeriod, TimeUnit.SECONDS)
        .andThen(vertx.rxClose())
        .doOnComplete(this::closeWatch) // stop current watch
        .doOnComplete(AppContext::reset)
        .doOnComplete(() -> log.info("Successfully stopped application"))
        .compose(CompletableUtils.applyDebugLogs(log));
  }

  public Completable rxRestartApplication(Integer healthCheckWaitPeriod, Integer maxDelaySeconds) {
    int restartDelay = new Random().nextInt(maxDelaySeconds);
    return Completable.complete()
        .doOnComplete(
            () ->
                log.info(
                    "Waiting for {} seconds (generated randomly) so not all nodes restart at same time",
                    restartDelay))
        .delay(restartDelay, TimeUnit.SECONDS)
        .andThen(rxStopApplication(healthCheckWaitPeriod))
        .doOnComplete(() -> Runtime.getRuntime().removeShutdownHook(shutdownThread))
        .doOnComplete(() -> MaintenanceUtils.clearMaintenance(vertx))
        .andThen(Completable.defer(this::rxStartApplication));
  }

  private List<Module> getAllGoogleGuiceModules(final Vertx vertx) {
    List<Module> modules = new ArrayList<>();
    modules.add(new DefaultModule(vertx));
    modules.addAll(Arrays.asList(getGoogleGuiceModules(vertx)));
    return modules;
  }

  /**
   * Return Array of Google Guice Module's. Ref: https://github.com/google/guice/wiki/GettingStarted
   *
   * @param vertx io.vertx.reactivex.core.Vertx
   * @return com.google.inject.Module[]
   */
  protected abstract Module[] getGoogleGuiceModules(final Vertx vertx);

  /**
   * Return Array of Deployable.
   *
   * @param vertx io.vertx.reactivex.core.Vertx
   * @return Deployable[]
   */
  protected abstract Deployable[] getVerticlesToDeploy(final Vertx vertx);

  protected Integer getEventLoopSize() {
    return NUM_OF_CORES;
  }

  protected Vertx initVertx() {
    log.info("Initializing Vertx...");
    return Vertx.vertx(
        new VertxOptions()
            .setEventLoopPoolSize(getEventLoopSize())
            .setPreferNativeTransport(true)
            .setMetricsOptions(new DropwizardMetricsOptions().setJmxEnabled(true)));
  }

  protected Single<Vertx> rxInitVertx() {
    return Single.just(initVertx());
  }

  private void setDefaultRxSchedulers(Vertx vertx) {
    RxJavaPlugins.setComputationSchedulerHandler(s -> RxHelper.scheduler(vertx));
    RxJavaPlugins.setIoSchedulerHandler(s -> RxHelper.blockingScheduler(vertx));
    RxJavaPlugins.setNewThreadSchedulerHandler(s -> RxHelper.scheduler(vertx));
  }

  @SneakyThrows
  private void gracefulShutdown() {
    val healthCheckWaitPeriod = getHealthCheckWaitPeriod();
    rxStopApplication(healthCheckWaitPeriod).subscribe();
    val vertxCloseDelay = 2;
    Thread.sleep(1000L * (healthCheckWaitPeriod + vertxCloseDelay));
    log.info("completed shutdown handler");
  }

  protected void initDDClient() {
    // Datadog client initialization removed
  }

  protected int getHealthCheckWaitPeriod() {
    int elbInterval = 6;
    int elbUnhealthyThreshold = 5;
    int requestTimeout = 2;
    return elbInterval * (elbUnhealthyThreshold + 1) + requestTimeout; // seconds
  }

  protected String getConsulKey() {
    return "d11/"
        + Constants.NAMESPACE
        + "/"
        + Constants.SERVICE_NAME
        + "/"
        + Constants.ENV
        + "/restart.json";
  }

  public void registerRestartApplicationWatch(Long timeoutSeconds) {
    final int DEFAULT_MAX_RESTART_DELAY = 2 * 60; // seconds
    this.restartWatch =
        WatchUtils.setConsulKeyWatch(
            this.vertx.getDelegate(),
            getConsulKey(),
            timeoutSeconds,
            watchResult -> {
              if (watchResult.succeeded()) {
                if (watchResult.prevResult() != null) { // this is null when application is started
                  if (watchResult.nextResult().isPresent()) {
                    log.info("Signal to restart application detected from consul");
                    JsonObject config = new JsonObject(watchResult.nextResult().getValue());
                    Integer maxDelaySeconds =
                        config.containsKey("maxDelaySeconds")
                            ? config.getInteger("maxDelaySeconds")
                            : DEFAULT_MAX_RESTART_DELAY;
                    Integer healthCheckWaitPeriod =
                        config.containsKey("healthCheckWaitPeriod")
                            ? config.getInteger("healthCheckWaitPeriod")
                            : getHealthCheckWaitPeriod();
                    rxRestartApplication(healthCheckWaitPeriod, maxDelaySeconds).subscribe();
                  } else {
                    log.info("Next result is not present");
                  }
                }
              } else {
                log.error("Unable to retrieve keys from consul", watchResult.cause());
              }
            });
  }

  public void registerRestartApplicationWatch() {
    registerRestartApplicationWatch(45L);
  }

  private void closeWatch() {
    if (this.restartWatch != null) {
      this.restartWatch.stop();
      this.restartWatch = null;
    }
  }
}
