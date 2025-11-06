package com.logwise.spark.dto.response;

import java.util.List;
import lombok.Data;

@Data
public class SparkApplicationStageResponse {
  private int shuffleWriteTime;
  private int numCompleteTasks;
  private int inputRecords;
  private String description;
  private int shuffleReadBytes;
  private Object killedTasksSummary;
  private int shuffleRemoteBytesReadToDisk;
  private int shuffleWriteBytes;
  private String schedulingPool;
  private Object peakExecutorMetrics;
  private String submissionTime;
  private int outputRecords;
  private int shuffleWriteRecords;
  private String completionTime;
  private int inputBytes;
  private long executorDeserializeCpuTime;
  private int shuffleRemoteBytesRead;
  private int resultSize;
  private int shuffleLocalBlocksFetched;
  private long peakExecutionMemory;
  private String details;
  private List<Integer> rddIds;
  private int stageId;
  private int attemptId;
  private int numTasks;
  private String firstTaskLaunchedTime;
  private int jvmGcTime;
  private int executorDeserializeTime;
  private long executorCpuTime;
  private int memoryBytesSpilled;
  private int executorRunTime;
  private int shuffleReadRecords;
  private int numActiveTasks;
  private long outputBytes;
  private int numFailedTasks;
  private int diskBytesSpilled;
  private int shuffleFetchWaitTime;
  private List<Object> accumulatorUpdates;
  private int resourceProfileId;
  private int resultSerializationTime;
  private String name;
  private int shuffleRemoteBlocksFetched;
  private int shuffleLocalBytesRead;
  private int numKilledTasks;
  private int numCompletedIndices;
  private String status;
}
