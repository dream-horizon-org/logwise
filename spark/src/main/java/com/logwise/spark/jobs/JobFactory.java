package com.logwise.spark.jobs;

import com.logwise.spark.constants.JobName;
import com.logwise.spark.guice.injectors.ApplicationInjector;
import com.logwise.spark.jobs.impl.PushLogsToS3SparkJob;
import com.typesafe.config.Config;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.SparkSession;

@Slf4j
public class JobFactory {

  public static SparkJob getSparkJob(String jobName, SparkSession sparkSession) {
    log.info("Creating job: {}", jobName);
    JobName name = JobName.fromValue(jobName);
    switch (name) {
      case PUSH_LOGS_TO_S3:
        return new PushLogsToS3SparkJob(
            ApplicationInjector.getInstance(Config.class), sparkSession);
      default:
        throw new IllegalArgumentException("Invalid job name: " + jobName);
    }
  }
}
