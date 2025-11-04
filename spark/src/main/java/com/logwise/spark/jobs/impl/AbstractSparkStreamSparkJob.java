package com.logwise.spark.jobs.impl;

import com.logwise.spark.jobs.SparkJob;
import com.typesafe.config.Config;
import java.io.Serializable;
import lombok.RequiredArgsConstructor;
import org.apache.spark.sql.SparkSession;

@RequiredArgsConstructor
public abstract class AbstractSparkStreamSparkJob<T> implements SparkJob<T>, Serializable {
  protected final SparkSession sparkSession;
  protected final Config config;

  public AbstractSparkStreamSparkJob() {
    this.sparkSession = null;
    this.config = null;
  }

  @Override
  public Long timeout() {
    // This is a stream job, so it should never timeout.
    return Long.MAX_VALUE;
  }
}
