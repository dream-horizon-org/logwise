package com.logwise.spark.stream;

import java.util.List;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.streaming.StreamingQuery;

public interface Stream {
  List<StreamingQuery> startStreams(SparkSession sparkSession);
}
