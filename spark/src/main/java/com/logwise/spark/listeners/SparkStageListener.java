package com.logwise.spark.listeners;

import com.google.inject.Inject;
import com.logwise.spark.constants.Constants;
import com.logwise.spark.jobs.impl.PushLogsToS3SparkJob;
import com.logwise.spark.singleton.CurrentSparkSession;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.scheduler.SparkListener;
import org.apache.spark.scheduler.SparkListenerStageCompleted;
import org.apache.spark.scheduler.SparkListenerStageSubmitted;
import org.apache.spark.sql.streaming.StreamingQuery;
import org.apache.spark.sql.streaming.StreamingQueryManager;
import scala.Option;

/**
 * SparkStageListener listens to Spark stage events and manages the execution of streaming queries.
 * It tracks stage submission and completion, updates metrics, and stops queries when necessary.
 */
@Slf4j
@RequiredArgsConstructor(onConstructor = @__({@Inject}))
public class SparkStageListener extends SparkListener {

  private static class StageMetrics {
    Long inputRecords = 0L;
    Long outputBytes = 0L;
    Long submissionTime = 0L;
    Long completionTime = 0L;
  }

  private static final Map<String, Integer> STAGE_COMPLETION_MAP = new ConcurrentHashMap<>();
  private static final Map<String, Integer> STAGE_SUBMITTED_MAP = new ConcurrentHashMap<>();
  private static final Set<Integer> PENDING_STOP_STAGE_IDS = ConcurrentHashMap.newKeySet();
  private static volatile StageMetrics stageMetrics = new StageMetrics();

  /**
   * Handles the event when a stage is submitted.
   *
   * @param stageSubmitted The submitted stage event.
   */
  @Override
  public void onStageSubmitted(SparkListenerStageSubmitted stageSubmitted) {
    String stageName = stageSubmitted.stageInfo().name();
    int stageId = stageSubmitted.stageInfo().stageId();
    STAGE_SUBMITTED_MAP.merge(stageName, 1, Integer::sum);
    log.info("Total submitted stages: {}", STAGE_SUBMITTED_MAP);

    if (isAllStagesCompletedAtLeastOnce()) {
      log.info("All stages completed at least once");
      completeExecution();
      return;
    }

    if (isStageAlreadyCompleted(stageName)) {
      log.info("Stage: [{}] already completed", stageName);
      stopStage(stageName, stageId);
    }
  }

  /**
   * Handles the event when a stage is completed.
   *
   * @param stageCompleted The completed stage event.
   */
  @Override
  public void onStageCompleted(SparkListenerStageCompleted stageCompleted) {
    String status = stageCompleted.stageInfo().getStatusString();
    String stageName = stageCompleted.stageInfo().name();
    STAGE_COMPLETION_MAP.merge(stageName, 1, Integer::sum);
    log.info(
        "Stage [{}] completed with status: [{}]. Completed stages map: {}",
        stageName,
        status,
        STAGE_COMPLETION_MAP);
    if (status.equals("succeeded")) {
      long currentInputRecords =
          stageCompleted.stageInfo().taskMetrics().inputMetrics().recordsRead();
      long currentOutputBytes =
          stageCompleted.stageInfo().taskMetrics().outputMetrics().bytesWritten();
      Option<Object> completionTime = stageCompleted.stageInfo().completionTime();
      Option<Object> submissionTime = stageCompleted.stageInfo().submissionTime();

      log.info(
          "Stage [{}] - InputRecords: {}, OutputBytes: {}",
          stageName,
          currentInputRecords,
          currentOutputBytes);

      stageMetrics.inputRecords = Math.max(currentInputRecords, stageMetrics.inputRecords);
      stageMetrics.outputBytes = Math.max(currentOutputBytes, stageMetrics.outputBytes);
      if (completionTime.nonEmpty() && completionTime.isDefined()) {
        stageMetrics.completionTime =
            Math.max(stageMetrics.completionTime, Long.parseLong(completionTime.get().toString()));
      }
      if (submissionTime.nonEmpty() && submissionTime.isDefined()) {
        long currentSubmissionTime = Long.parseLong(submissionTime.get().toString());
        stageMetrics.submissionTime =
            stageMetrics.submissionTime.equals(0L)
                ? currentSubmissionTime
                : Math.min(stageMetrics.submissionTime, currentSubmissionTime);
      }
    }
  }

  /**
   * Stops the streaming query associated with the given stage name.
   *
   * @param stageName The name of the stage to stop.
   */
  private void stopStage(String stageName, int stageId) {
    new Thread(
            () -> {
              StreamingQueryManager streamingQueryManager =
                  CurrentSparkSession.getInstance().getSparkSession().streams();
              StreamingQuery[] activeQueries = streamingQueryManager.active();

              PENDING_STOP_STAGE_IDS.add(stageId);
              for (StreamingQuery query : activeQueries) {
                String queryName = query.name();
                String expectedStageName = Constants.QUERY_NAME_TO_STAGE_MAP.get(queryName);
                if (stageName.equals(expectedStageName)) {
                  log.info("Stopping stream query: [{}] for stage: [{}]", queryName, stageName);
                  try {
                    query.stop();
                    query.awaitTermination();
                    streamingQueryManager.resetTerminated();
                  } catch (Exception e) {
                    log.error(
                        "Error stopping stream query: [{}] for stage: [{}]",
                        queryName,
                        stageName,
                        e);
                  }
                  break;
                }
              }
              PENDING_STOP_STAGE_IDS.remove(stageId);
            })
        .start();
  }

  /** Completes the execution by waiting for all stages to finish and updating the stage history. */
  private void completeExecution() {
    log.info("All stages completed at least once. Waiting for all stages to finish...");
    while (true) {
      if (PENDING_STOP_STAGE_IDS.isEmpty()) {
        break;
      }
    }
    PushLogsToS3SparkJob.stopAllRunningJobs();
  }

  /**
   * Checks if all stages have been completed at least once.
   *
   * @return True if all stages are completed at least once, false otherwise.
   */
  private static Boolean isAllStagesCompletedAtLeastOnce() {
    if (STAGE_COMPLETION_MAP.isEmpty()
        || STAGE_COMPLETION_MAP.keySet().size()
            != PushLogsToS3SparkJob.getStreamingQueriesCount()) {
      return false;
    }
    return STAGE_SUBMITTED_MAP.entrySet().stream()
        .allMatch(
            entry ->
                STAGE_COMPLETION_MAP.containsKey(entry.getKey())
                    && STAGE_COMPLETION_MAP.get(entry.getKey()) > 0);
  }

  /**
   * Checks if the given stage has already been completed.
   *
   * @param stageName The name of the stage to check.
   * @return True if the stage is already completed, false otherwise.
   */
  private static Boolean isStageAlreadyCompleted(String stageName) {
    return STAGE_COMPLETION_MAP.entrySet().stream()
        .anyMatch(entry -> entry.getKey().equals(stageName) && entry.getValue() > 0);
  }
}
