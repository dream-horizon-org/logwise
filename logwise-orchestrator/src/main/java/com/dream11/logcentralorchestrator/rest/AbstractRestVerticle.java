package com.dream11.logcentralorchestrator.rest;

import com.dream11.logcentralorchestrator.common.app.AppContext;
import com.dream11.logcentralorchestrator.common.util.ConfigUtils;
import com.dream11.logcentralorchestrator.common.util.ListUtils;
import com.dream11.logcentralorchestrator.rest.config.HttpConfig;
import com.dream11.logcentralorchestrator.rest.handler.HttpLoggerHandlerImpl;
import com.google.common.base.Strings;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.RxHelper;
import io.vertx.reactivex.core.http.HttpServer;
import io.vertx.reactivex.core.http.HttpServerRequest;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.handler.BodyHandler;
import io.vertx.reactivex.ext.web.handler.ResponseContentTypeHandler;
import io.vertx.reactivex.ext.web.handler.StaticHandler;
import io.vertx.reactivex.ext.web.handler.TimeoutHandler;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jboss.resteasy.plugins.server.vertx.VertxRequestHandler;
import org.jboss.resteasy.plugins.server.vertx.VertxResteasyDeployment;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

@Slf4j
public abstract class AbstractRestVerticle extends AbstractVerticle {

  private final List<String> packageNames;
  private final String mountPath;

  private HttpServer httpServer;
  private HttpConfig httpConfig;

  private final List<String> abstractRouteEndPoints = new ArrayList<>();

  public AbstractRestVerticle(String packageName) {
    this(List.of(packageName));
  }

  public AbstractRestVerticle(List<String> packageNames) {
    this.packageNames = packageNames;
    this.mountPath = "";
  }

  public AbstractRestVerticle(String packageName, String mountPath) {
    this(List.of(packageName), mountPath);
  }

  public AbstractRestVerticle(List<String> packageNames, String mountPath) {
    this.packageNames = packageNames;
    if (!Strings.isNullOrEmpty(mountPath)) {
      this.mountPath = "/" + mountPath;
    } else {
      this.mountPath = "";
    }
  }

  @Override
  public Completable rxStart() {
    init();
    return startHttpServer()
        .doOnSuccess(
            server -> {
              this.httpServer = server;
            })
        .ignoreElement();
  }

  private Single<HttpServer> startHttpServer() {
    HttpServerOptions options =
        new HttpServerOptions()
            .setHost(httpConfig.getHost())
            .setPort(httpConfig.getPort())
            .setIdleTimeout(httpConfig.getIdleTimeOut())
            .setCompressionSupported(httpConfig.isCompressionEnabled())
            .setCompressionLevel(httpConfig.getCompressionLevel()) // default is 6
            .setLogActivity(httpConfig.isLogActivity())
            .setReusePort(httpConfig.isReusePort())
            .setReuseAddress(httpConfig.isReuseAddress())
            .setTcpFastOpen(httpConfig.isTcpFastOpen())
            .setTcpNoDelay(httpConfig.isTcpNoDelay())
            .setTcpQuickAck(httpConfig.isTcpQuickAck())
            .setTcpKeepAlive(httpConfig.isTcpKeepAlive())
            .setUseAlpn(httpConfig.isUseAlpn());

    VertxResteasyDeployment deployment = getResteasyDeployment();
    Router router = getRouter();
    // add swagger regex
    abstractRouteEndPoints.add("/swagger(.*)");
    VertxRequestHandler vertxRequestHandler =
        new VertxRequestHandler(vertx.getDelegate(), deployment);
    val server = vertx.createHttpServer(options);
    val handleRequests =
        server
            .requestStream()
            .toFlowable()
            .map(HttpServerRequest::pause)
            .onBackpressureDrop(
                req -> {
                  log.error("Dropping request with status 503");
                  req.response().setStatusCode(503).end();
                })
            // TODO: Understand why removing this seemingly redundant observeOn increases latency &
            // CPU
            .observeOn(RxHelper.scheduler(new io.vertx.reactivex.core.Context(this.context)))
            .doOnNext(
                req -> {
                  List<String> matches =
                      ListUtils.filter(regex -> req.path().matches(regex), abstractRouteEndPoints);
                  if (!matches.isEmpty()) {
                    router.handle(req);
                  } else {
                    vertxRequestHandler.handle(req.getDelegate());
                  }
                })
            .map(HttpServerRequest::resume)
            .doOnError(error -> log.error("Uncaught ERROR while handling request", error))
            .ignoreElements();

    return server
        .rxListen()
        .doOnSuccess(
            res ->
                log.info(
                    "Started http server at " + options.getPort() + " packages : " + packageNames))
        .doOnError(
            error ->
                log.error(
                    "Failed to start http server at port :"
                        + options.getPort()
                        + " with error "
                        + error.getMessage()))
        .doOnSubscribe(disposable -> handleRequests.subscribe());
  }

  protected Router getRouter() {
    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());
    router.route().handler(ResponseContentTypeHandler.create());
    router.route().handler(StaticHandler.create());
    router.get("/liveness").handler(routingContext -> routingContext.response().end("Success"));
    abstractRouteEndPoints.add("/liveness");
    if (this.httpConfig.isAccessLoggerEnable()) {
      router
          .route()
          .handler((Handler) new HttpLoggerHandlerImpl(this.httpConfig.getAccessLoggerName()));
    }
    List<AbstractRoute> routes = RestUtil.abstractRouteList(this.packageNames);
    log.info("AbstractRoutes : " + routes.size());
    routes.forEach(
        route -> {
          router
              .routeWithRegex(route.getHttpMethod(), "(?i)" + this.mountPath + route.getPath())
              .consumes(route.getConsumes())
              .produces(route.getProduces())
              .handler(TimeoutHandler.create(route.getTimeout(), 594))
              .handler(route);
          abstractRouteEndPoints.add("(?i)" + this.mountPath + route.getPath());
        });
    return router;
  }

  @Override
  public void stop(Promise<Void> stopPromise) throws Exception {
    if (httpServer != null) {
      log.info("stopping http server.");
      httpServer.close();
    }
    super.stop(stopPromise);
  }

  protected void init() {
    initDDClient();
    httpConfig =
        ConfigUtils.fromConfigFile("config/http-server/http-server-%s.conf", HttpConfig.class);
    System.setProperty(
        "vertx.logger-delegate-factory-class-name",
        this.httpConfig.getVertxLoggerDelegateFactoryClassName());
  }

  private VertxResteasyDeployment getResteasyDeployment() {
    VertxResteasyDeployment deployment = new VertxResteasyDeployment();
    deployment.start();
    List<Class<?>> routes = RestUtil.annotatedClasses(Path.class, packageNames);
    log.info("JAX-RS routes : " + routes.size());
    ResteasyProviderFactory resteasyProviderFactory = deployment.getProviderFactory();
    registerProviders().forEach(resteasyProviderFactory::register);
    // not using deployment.getRegistry().addPerInstanceResource because it creates new instance of
    // resource for each request
    routes.forEach(
        route -> deployment.getRegistry().addSingletonResource(AppContext.getInstance(route)));
    return deployment;
  }

  private List<Class<?>> registerProviders() {
    // Avoid duplicate provider registration: rely on @Provider discovery only
    return new ArrayList<>(RestUtil.annotatedClasses(Provider.class, packageNames));
  }

  protected void initDDClient() {
    // Datadog client initialization removed
  }
}
