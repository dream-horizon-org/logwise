package com.logwise.spark.stream.impl;

import static org.apache.spark.sql.functions.*;

import com.google.inject.Inject;
import com.logwise.spark.constants.Constants;
import com.logwise.spark.protobuf.VectorLogs;
import com.logwise.spark.schema.Schema;
import com.logwise.spark.services.KafkaService;
import com.logwise.spark.utils.ApplicationUtils;
import com.logwise.spark.utils.ConfigUtils;
import com.typesafe.config.Config;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.catalyst.encoders.ExpressionEncoder;
import org.apache.spark.sql.catalyst.encoders.RowEncoder;
import org.apache.spark.sql.streaming.OutputMode;
import org.apache.spark.sql.streaming.StreamingQuery;
import org.apache.spark.sql.streaming.Trigger;

@Slf4j
public class ApplicationLogsStreamToS3 extends AbstractApplicationLogsStream {

  @Inject
  public ApplicationLogsStreamToS3(Config config, KafkaService kafkaService) {
    super(config, kafkaService);
  }

  private StreamingQuery pushApplicationLogsToS3(Dataset<Row> flattenedDataFrame) {
    log.info("Starting To Push Application Logs to S3...");
    Trigger trigger =
        Trigger.ProcessingTime(config.getInt("spark.processing.time.seconds"), TimeUnit.SECONDS);
    Map<String, String> configMap = ConfigUtils.getSparkConfig(config);
    return flattenedDataFrame
        .writeStream()
        .queryName(Constants.APPLICATION_LOGS_TO_S3_QUERY_NAME)
        .trigger(trigger)
        .outputMode(OutputMode.Append())
        .format(Constants.WRITE_STREAM_PARQUET_FORMAT)
        .partitionBy(Constants.APPLICATION_LOG_S3_PARTITION_COLUMNS)
        .option("compression", Constants.WRITE_STREAM_GZIP_COMPRESSION)
        .option("checkpointLocation", config.getString("s3.path.checkpoint.application"))
        .options(configMap)
        .start(config.getString("s3.path.logs.application"));
  }

  protected StreamingQuery getVectorApplicationLogsStreamQuery(Dataset<Row> kafkaValueTopicStream) {
    log.info("Creating Vector Application Logs DataFrame from Kafka Stream with Proto Format");
    ExpressionEncoder<Row> rowEncoder = RowEncoder.apply(Schema.getVectorApplicationLogsSchema());
    Dataset<Row> stream =
        kafkaValueTopicStream
            .map(
                (MapFunction<Row, Row>)
                    row -> {
                      byte[] bytes = row.getAs("value");
                      VectorLogs vectorLogs = VectorLogs.parseFrom(bytes);
                      // Schema order: message, timestamp, service_name
                      return RowFactory.create(
                          vectorLogs.getMessage() != null ? vectorLogs.getMessage() : "",
                          ApplicationUtils.convertProtoTimestampToIso(vectorLogs.getTimestamp()),
                          vectorLogs.getServiceName() != null ? vectorLogs.getServiceName() : "");
                    },
                rowEncoder)
            .withColumn(
                Constants.APPLICATION_LOG_COLUMN_YEAR,
                year(col(Constants.APPLICATION_LOG_COLUMN_TIMESTAMP)))
            .withColumn(
                Constants.APPLICATION_LOG_COLUMN_MONTH,
                lpad(month(col(Constants.APPLICATION_LOG_COLUMN_TIMESTAMP)), 2, "0"))
            .withColumn(
                Constants.APPLICATION_LOG_COLUMN_DAY,
                lpad(dayofmonth(col(Constants.APPLICATION_LOG_COLUMN_TIMESTAMP)), 2, "0"))
            .withColumn(
                Constants.APPLICATION_LOG_COLUMN_HOUR,
                lpad(hour(col(Constants.APPLICATION_LOG_COLUMN_TIMESTAMP)), 2, "0"))
            .withColumn(
                Constants.APPLICATION_LOG_COLUMN_MINUTE,
                lpad(minute(col(Constants.APPLICATION_LOG_COLUMN_TIMESTAMP)), 2, "0"));
    return pushApplicationLogsToS3(stream);
  }
}
