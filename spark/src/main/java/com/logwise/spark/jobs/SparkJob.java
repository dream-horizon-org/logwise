package com.logwise.spark.jobs;

import com.logwise.spark.constants.JobName;
import java.util.concurrent.CompletableFuture;

public interface SparkJob<T extends Object> {
  JobName getJobName();

  Long timeout();

  CompletableFuture<T> start();

  void stop();
}
