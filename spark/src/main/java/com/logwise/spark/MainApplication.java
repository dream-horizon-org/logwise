package com.logwise.spark;

import com.logwise.spark.configs.ApplicationConfig;
import com.logwise.spark.guice.injectors.ApplicationInjector;
import com.logwise.spark.guice.modules.MainModule;
import com.logwise.spark.jobs.JobFactory;
import com.logwise.spark.singleton.CurrentSparkSession;
import com.typesafe.config.Config;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.SparkSession;

@Slf4j
public class MainApplication {
  private static Config appConfig;

  public static void main(String[] args) {
    log.info("\n<<<************************[Starting Application]************************>>>");
    init(args);
    SparkSession sparkSession = CurrentSparkSession.getInstance().getSparkSession();
    log.info("Application Started: {}", sparkSession.sparkContext().applicationId());
    String jobName = appConfig.getString("app.job.name");
    JobFactory.getSparkJob(jobName, sparkSession).start().join();
  }

  private static void init(String[] args) {
    try {
      setAppConfig(args);
      initInjector();
    } catch (Exception e) {
      log.error("Error while initializing application", e);
      System.exit(1);
    }
  }

  private static void setAppConfig(String[] args) {
    log.info("Initializing ApplicationConfig...");
    appConfig = ApplicationConfig.getConfig(args);
  }

  private static void initInjector() {
    log.info("Initializing injector...");
    ApplicationInjector.initInjection(new MainModule(appConfig));
  }
}
