package com.logwise.spark.singleton;

import com.logwise.spark.constants.Constants;
import com.logwise.spark.guice.injectors.ApplicationInjector;
import com.logwise.spark.utils.ConfigUtils;
import com.logwise.spark.utils.SparkUtils;
import com.typesafe.config.Config;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.SparkConf;
import org.apache.spark.SparkContext;
import org.apache.spark.sql.SparkSession;

@Slf4j
public class CurrentSparkSession {

  private SparkSession sparkSession = null;

  private CurrentSparkSession() {}

  private static class CurrentSparkSessionHolder {
    private static final CurrentSparkSession INSTANCE = new CurrentSparkSession();
  }

  public static CurrentSparkSession getInstance() {
    return CurrentSparkSessionHolder.INSTANCE;
  }

  public synchronized SparkSession getSparkSession() {
    if (sparkSession == null) {
      sparkSession = createSparkSession();
      log.info("Creating new Spark Session: {}", sparkSession);
    }
    return sparkSession;
  }

  private SparkSession createSparkSession() {
    log.info("Creating Spark Session");
    SparkConf sparkConf = new SparkConf();
    SparkContext sparkContext = new SparkContext();
    Config config = ApplicationInjector.getInstance(Config.class);

    SparkUtils.getSparkListeners().forEach(sparkContext::addSparkListener);
    ConfigUtils.getSparkConfig(config).forEach(sparkConf::set);
    ConfigUtils.getSparkHadoopConfig(config).forEach(sparkContext.hadoopConfiguration()::set);

    return SparkSession.builder()
        .appName(Constants.APP_NAME)
        .sparkContext(sparkContext)
        .config(sparkConf)
        .getOrCreate();
  }
}
