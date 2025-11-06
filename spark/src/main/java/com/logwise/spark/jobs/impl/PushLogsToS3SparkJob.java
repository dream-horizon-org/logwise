package com.logwise.spark.jobs.impl;

import com.google.inject.Inject;
import com.logwise.spark.constants.JobName;
import com.logwise.spark.constants.StreamName;
import com.logwise.spark.stream.StreamFactory;
import com.typesafe.config.Config;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.streaming.StreamingQuery;
import org.apache.spark.sql.streaming.StreamingQueryException;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({@Inject}))
public class PushLogsToS3SparkJob extends AbstractSparkStreamSparkJob<Void> {
  private final Config config;
  private final SparkSession sparkSession;

  {
    this.pushLogsToS3Thread = new Thread(getPushLogsToS3Runnable());
  }

  @NonFinal private Thread pushLogsToS3Thread;
  @NonFinal private Long pushLogsToS3ThreadStartTime = null;

  private static final List<PushLogsToS3SparkJob> RUNNING_JOBS = new CopyOnWriteArrayList<>();
  private static int streamingQueriesCount = 0;

  public static int getStreamingQueriesCount() {
    return streamingQueriesCount;
  }

  public static void stopAllRunningJobs() {
    log.info("Stopping all running PushLogsToS3SparkJob instances...");
    RUNNING_JOBS.parallelStream().forEach(PushLogsToS3SparkJob::stop);
  }

  @Override
  public JobName getJobName() {
    return JobName.PUSH_LOGS_TO_S3;
  }

  @Override
  public void stop() {
    if (pushLogsToS3Thread.isAlive()) {
      pushLogsToS3Thread.interrupt();
    } else {
      log.info("Job {} is not running, nothing to stop.", getJobName());
    }
  }

  @Override
  @SneakyThrows
  public CompletableFuture<Void> start() {
    monitorJob();
    return CompletableFuture.completedFuture(null);
  }

  @SneakyThrows
  @SuppressWarnings("InfiniteLoopStatement")
  private void monitorJob() {
    log.info("Started Monitoring Job: {}", getJobName());
    long timeOutInMillis =
        TimeUnit.MINUTES.toMillis(config.getLong("spark.streamingquery.timeout.minutes"));
    while (true) {
      if (isJobTimeOut(timeOutInMillis)) {
        log.error(
            "Job {} timed out after {} minutes",
            getJobName(),
            TimeUnit.MILLISECONDS.toMinutes(timeOutInMillis));
        stop();
      } else if (!pushLogsToS3Thread.isAlive()) {
        startGetPushLogsToS3Runnable();
      }
      Thread.sleep(100);
    }
  }

  private void startGetPushLogsToS3Runnable() {
    pushLogsToS3Thread.start();
    pushLogsToS3ThreadStartTime = System.currentTimeMillis();
    RUNNING_JOBS.add(this);
    log.info("Started Job {}: Time: {}", getJobName(), pushLogsToS3ThreadStartTime);
  }

  private boolean isJobTimeOut(long timeOutInMillis) {
    return pushLogsToS3ThreadStartTime != null
        && System.currentTimeMillis() - pushLogsToS3ThreadStartTime > timeOutInMillis;
  }

  private Runnable getPushLogsToS3Runnable() {
    return () -> {
      try {
        log.info("Starting PushLogsToS3Runnable...");

        List<StreamingQuery> streamingQueries =
            config.getStringList("spark.streams.name").stream()
                .map(
                    name -> {
                      StreamName streamName = StreamName.fromValue(name);
                      SparkSession newSparkSession = sparkSession.newSession();
                      log.info(
                          "Creating new Spark Session: {} for Steam: {}",
                          newSparkSession,
                          streamName.getValue());
                      return StreamFactory.getStream(streamName).startStreams(sparkSession);
                    })
                .flatMap(List::stream)
                .collect(Collectors.toList());

        streamingQueriesCount = streamingQueries.size();
        awaitAndStopStreamingQueries(streamingQueries);
      } catch (Exception e) {
        if (!(e instanceof InterruptedException)) {
          log.error("Error in PushLogsToS3Runnable: ", e);
        }
      } finally {
        log.error("Stopping PushLogsToS3Runnable and removing from running jobs.");
        pushLogsToS3ThreadStartTime = null;
        RUNNING_JOBS.remove(this);
      }
    };
  }

  private void awaitAndStopStreamingQueries(List<StreamingQuery> queries) {
    queries.forEach(
        query -> {
          try {
            log.info("Awaiting termination of streaming query: {}", query.name());
            query.awaitTermination();
          } catch (StreamingQueryException e) {
            log.error("Error in awaiting termination of streaming query: {}", query.name(), e);
          } finally {
            if (query != null) {
              try {
                log.info("Stopping streaming query: {}", query.name());
                query.stop();
              } catch (TimeoutException e) {
                log.error("Error in stopping streaming query: {}", query.name(), e);
              }
            }
          }
        });
  }
}
