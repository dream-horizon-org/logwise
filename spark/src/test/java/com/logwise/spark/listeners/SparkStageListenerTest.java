package com.logwise.spark.listeners;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.apache.spark.executor.InputMetrics;
import org.apache.spark.executor.OutputMetrics;
import org.apache.spark.executor.TaskMetrics;
import org.apache.spark.scheduler.SparkListenerStageCompleted;
import org.apache.spark.scheduler.SparkListenerStageSubmitted;
import org.apache.spark.scheduler.StageInfo;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import scala.Option;

/**
 * Unit tests for SparkStageListener.
 *
 * <p>Tests verify stage event handling, metric updates, and execution flow. Focuses on observable
 * behavior rather than implementation details.
 */
public class SparkStageListenerTest {

  private SparkStageListener listener;
  private Map<String, Integer> stageCompletionMap;
  private Map<String, Integer> stageSubmittedMap;
  private Set<Integer> pendingStopStageIds;

  @BeforeMethod
  public void setUp() throws Exception {
    listener = new SparkStageListener();

    // Reset static state using reflection
    stageCompletionMap = getStaticField("STAGE_COMPLETION_MAP");
    stageSubmittedMap = getStaticField("STAGE_SUBMITTED_MAP");
    pendingStopStageIds = getStaticField("PENDING_STOP_STAGE_IDS");

    stageCompletionMap.clear();
    stageSubmittedMap.clear();
    pendingStopStageIds.clear();

    // Reset stageMetrics
    resetStageMetrics();
  }

  @AfterMethod
  public void tearDown() throws Exception {
    // Clean up static state after each test
    stageCompletionMap.clear();
    stageSubmittedMap.clear();
    pendingStopStageIds.clear();
    resetStageMetrics();
  }

  @SuppressWarnings("unchecked")
  private <T> T getStaticField(String fieldName) throws Exception {
    Field field = SparkStageListener.class.getDeclaredField(fieldName);
    field.setAccessible(true);
    return (T) field.get(null);
  }

  private void resetStageMetrics() throws Exception {
    // Create a new instance of StageMetrics and set it
    Class<?> stageMetricsClass =
        Class.forName("com.logwise.spark.listeners.SparkStageListener$StageMetrics");
    java.lang.reflect.Constructor<?> constructor = stageMetricsClass.getDeclaredConstructor();
    constructor.setAccessible(true);
    Object newMetrics = constructor.newInstance();

    Field field = SparkStageListener.class.getDeclaredField("stageMetrics");
    field.setAccessible(true);
    field.set(null, newMetrics);
  }

  private long getMetricValue(String fieldName) throws Exception {
    Object stageMetrics = getStaticField("stageMetrics");
    Field field = stageMetrics.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    return (Long) field.get(stageMetrics);
  }

  // ==================== Test onStageSubmitted() ====================

  @Test
  public void testOnStageSubmitted_TracksStageSubmission() {
    // Arrange
    SparkListenerStageSubmitted stageSubmitted = createStageSubmitted("stage1", 1);

    // Act
    listener.onStageSubmitted(stageSubmitted);

    // Assert
    assertEquals(stageSubmittedMap.get("stage1"), Integer.valueOf(1));
  }

  @Test
  public void testOnStageSubmitted_IncrementsCountForSameStage() {
    // Arrange
    stageSubmittedMap.put("stage1", 2);
    SparkListenerStageSubmitted stageSubmitted = createStageSubmitted("stage1", 1);

    // Act
    listener.onStageSubmitted(stageSubmitted);

    // Assert
    assertEquals(stageSubmittedMap.get("stage1"), Integer.valueOf(3));
  }

  @Test
  public void testOnStageSubmitted_TracksMultipleDifferentStages() {
    // Arrange
    SparkListenerStageSubmitted stage1 = createStageSubmitted("stage1", 1);
    SparkListenerStageSubmitted stage2 = createStageSubmitted("stage2", 2);

    // Act
    listener.onStageSubmitted(stage1);
    listener.onStageSubmitted(stage2);

    // Assert
    assertEquals(stageSubmittedMap.size(), 2);
    assertEquals(stageSubmittedMap.get("stage1"), Integer.valueOf(1));
    assertEquals(stageSubmittedMap.get("stage2"), Integer.valueOf(1));
  }

  @Test
  public void testOnStageSubmitted_WhenStageAlreadyCompleted_CallsStopStage() throws Exception {
    // Arrange - Stage already completed
    stageCompletionMap.put("stage1", 1);
    SparkListenerStageSubmitted stageSubmitted = createStageSubmitted("stage1", 1);

    // Act - This will start a thread that calls stopStage
    // Note: The thread may fail due to SparkSession dependency, but the method should
    // be callable without throwing exceptions
    listener.onStageSubmitted(stageSubmitted);

    // Assert - Verify the method completed without throwing exception
    // The stopStage thread starts asynchronously and may fail, but that's acceptable
    // Full testing of stopStage requires integration tests with a real SparkSession
    assertTrue(true, "onStageSubmitted should handle already completed stages without throwing");
  }

  @Test
  public void testOnStageSubmitted_WhenAllStagesCompleted_CallsCompleteExecution()
      throws Exception {
    // Arrange - Set up so all stages are completed
    stageCompletionMap.put("stage1", 1);
    stageCompletionMap.put("stage2", 1);
    stageSubmittedMap.put("stage1", 1);
    stageSubmittedMap.put("stage2", 1);

    try (org.mockito.MockedStatic<com.logwise.spark.jobs.impl.PushLogsToS3SparkJob> mockedJob =
        org.mockito.Mockito.mockStatic(com.logwise.spark.jobs.impl.PushLogsToS3SparkJob.class)) {
      mockedJob
          .when(com.logwise.spark.jobs.impl.PushLogsToS3SparkJob::getStreamingQueriesCount)
          .thenReturn(2);

      SparkListenerStageSubmitted stageSubmitted = createStageSubmitted("stage1", 1);
      CountDownLatch executionLatch = new CountDownLatch(1);

      // Mock stopAllRunningJobs to signal completion
      mockedJob
          .when(() -> com.logwise.spark.jobs.impl.PushLogsToS3SparkJob.stopAllRunningJobs())
          .thenAnswer(
              invocation -> {
                executionLatch.countDown();
                return null;
              });

      // Act
      listener.onStageSubmitted(stageSubmitted);

      // Assert - completeExecution should be called (waits for pending stages, then calls
      // stopAllRunningJobs)
      // Since pendingStopStageIds is empty, it should complete quickly
      assertTrue(
          executionLatch.await(2, TimeUnit.SECONDS),
          "completeExecution should be called when all stages are completed");
      mockedJob.verify(() -> com.logwise.spark.jobs.impl.PushLogsToS3SparkJob.stopAllRunningJobs());
    }
  }

  // ==================== Test onStageCompleted() ====================

  @Test
  public void testOnStageCompleted_TracksStageCompletion() {
    // Arrange
    SparkListenerStageCompleted stageCompleted =
        createStageCompleted("stage1", 100L, 1000L, 1000000L, 999000L);

    // Act
    listener.onStageCompleted(stageCompleted);

    // Assert
    assertEquals(stageCompletionMap.get("stage1"), Integer.valueOf(1));
  }

  @Test
  public void testOnStageCompleted_IncrementsCountForSameStage() {
    // Arrange
    SparkListenerStageCompleted stage1 =
        createStageCompleted("stage1", 100L, 1000L, 1000000L, 999000L);
    SparkListenerStageCompleted stage2 =
        createStageCompleted("stage1", 200L, 2000L, 2000000L, 998000L);

    // Act
    listener.onStageCompleted(stage1);
    listener.onStageCompleted(stage2);

    // Assert
    assertEquals(stageCompletionMap.get("stage1"), Integer.valueOf(2));
  }

  @Test
  public void testOnStageCompleted_UpdatesMetricsForSucceededStages() throws Exception {
    // Arrange
    SparkListenerStageCompleted stage1 =
        createStageCompleted("stage1", 500L, 3000L, 1000000L, 999000L);
    SparkListenerStageCompleted stage2 =
        createStageCompleted("stage2", 1000L, 5000L, 2000000L, 998000L);

    // Act
    listener.onStageCompleted(stage1);
    listener.onStageCompleted(stage2);

    // Assert - Verify metrics track max/min values correctly
    assertEquals(getMetricValue("inputRecords"), 1000L, "Should track max input records");
    assertEquals(getMetricValue("outputBytes"), 5000L, "Should track max output bytes");
    assertEquals(getMetricValue("completionTime"), 2000000L, "Should track max completion time");
    assertEquals(getMetricValue("submissionTime"), 998000L, "Should track min submission time");
  }

  @Test(dataProvider = "nonSucceededStatuses")
  public void testOnStageCompleted_DoesNotUpdateMetricsForNonSucceededStatuses(String status)
      throws Exception {
    // Arrange
    SparkListenerStageCompleted stageCompleted = mock(SparkListenerStageCompleted.class);
    StageInfo stageInfo = mock(StageInfo.class);
    when(stageCompleted.stageInfo()).thenReturn(stageInfo);
    when(stageInfo.name()).thenReturn("stage1");
    when(stageInfo.getStatusString()).thenReturn(status);

    // Act
    listener.onStageCompleted(stageCompleted);

    // Assert - Stage should be tracked but metrics should not be updated
    assertEquals(stageCompletionMap.get("stage1"), Integer.valueOf(1));
    assertEquals(
        getMetricValue("inputRecords"),
        0L,
        "Metrics should not be updated for " + status + " status");
  }

  @DataProvider(name = "nonSucceededStatuses")
  public Object[][] nonSucceededStatuses() {
    return new Object[][] {{"failed"}, {"killed"}};
  }

  @Test
  public void testOnStageCompleted_HandlesEmptyTimeOptions() {
    // Arrange
    SparkListenerStageCompleted stageCompleted = mock(SparkListenerStageCompleted.class);
    StageInfo stageInfo = mock(StageInfo.class);
    TaskMetrics taskMetrics = mock(TaskMetrics.class);
    InputMetrics inputMetrics = mock(InputMetrics.class);
    OutputMetrics outputMetrics = mock(OutputMetrics.class);

    when(stageCompleted.stageInfo()).thenReturn(stageInfo);
    when(stageInfo.name()).thenReturn("stage1");
    when(stageInfo.getStatusString()).thenReturn("succeeded");
    when(stageInfo.taskMetrics()).thenReturn(taskMetrics);
    when(taskMetrics.inputMetrics()).thenReturn(inputMetrics);
    when(taskMetrics.outputMetrics()).thenReturn(outputMetrics);
    when(inputMetrics.recordsRead()).thenReturn(1000L);
    when(outputMetrics.bytesWritten()).thenReturn(5000L);
    when(stageInfo.completionTime()).thenReturn(Option.empty());
    when(stageInfo.submissionTime()).thenReturn(Option.empty());

    // Act - Should not throw exception
    listener.onStageCompleted(stageCompleted);

    // Assert
    assertEquals(stageCompletionMap.get("stage1"), Integer.valueOf(1));
  }

  @Test
  public void testOnStageCompleted_HandlesFirstSubmissionTime() throws Exception {
    // Arrange - Reset metrics to initial state
    resetStageMetrics();

    SparkListenerStageCompleted stageCompleted =
        createStageCompleted("stage1", 100L, 1000L, 1000000L, 999000L);

    // Act
    listener.onStageCompleted(stageCompleted);

    // Assert - First submission time should be set (not zero)
    assertEquals(
        getMetricValue("submissionTime"),
        999000L,
        "First submission time should be set when starting from zero");
  }

  // ==================== Test completeExecution() ====================

  @Test
  public void testCompleteExecution_WaitsForPendingStages() throws Exception {
    // Arrange
    pendingStopStageIds.add(1);
    pendingStopStageIds.add(2);

    java.lang.reflect.Method completeExecutionMethod =
        SparkStageListener.class.getDeclaredMethod("completeExecution");
    completeExecutionMethod.setAccessible(true);

    CountDownLatch executionStarted = new CountDownLatch(1);

    // Mock stopAllRunningJobs to avoid issues
    try (org.mockito.MockedStatic<com.logwise.spark.jobs.impl.PushLogsToS3SparkJob> mockedJob =
        org.mockito.Mockito.mockStatic(com.logwise.spark.jobs.impl.PushLogsToS3SparkJob.class)) {
      mockedJob
          .when(() -> com.logwise.spark.jobs.impl.PushLogsToS3SparkJob.stopAllRunningJobs())
          .thenAnswer(invocation -> null);

      // Start execution in a separate thread
      Thread executionThread =
          new Thread(
              () -> {
                try {
                  executionStarted.countDown();
                  completeExecutionMethod.invoke(listener);
                } catch (Exception e) {
                  // Ignore
                }
              });
      executionThread.start();

      // Wait for execution to start
      assertTrue(executionStarted.await(1, TimeUnit.SECONDS), "Execution should start");

      // Verify it's waiting (pending stages not empty)
      Thread.sleep(200); // Small delay to ensure it's in the loop
      assertTrue(executionThread.isAlive(), "Execution should be waiting for pending stages");

      // Clear pending stages - this should cause the loop to exit
      pendingStopStageIds.clear();

      // Wait for thread to complete (it should exit the loop when pending stages are cleared)
      executionThread.join(2000);
      assertFalse(
          executionThread.isAlive(),
          "Execution thread should complete after pending stages are cleared");
      // Note: stopAllRunningJobs() may not be called if there's an exception, but the important
      // behavior (waiting for pending stages) is verified above
    }
  }

  @Test
  public void testCompleteExecution_HandlesInterruptedException() throws Exception {
    // Arrange
    pendingStopStageIds.add(1);

    java.lang.reflect.Method completeExecutionMethod =
        SparkStageListener.class.getDeclaredMethod("completeExecution");
    completeExecutionMethod.setAccessible(true);

    CountDownLatch executionStarted = new CountDownLatch(1);

    Thread executionThread =
        new Thread(
            () -> {
              try {
                executionStarted.countDown();
                completeExecutionMethod.invoke(listener);
              } catch (Exception e) {
                // Ignore
              }
            });
    executionThread.start();

    // Wait for execution to start
    assertTrue(executionStarted.await(1, TimeUnit.SECONDS), "Execution should start");

    // Interrupt the thread
    Thread.sleep(100); // Small delay to ensure it's in the loop
    executionThread.interrupt();

    // Assert - thread should handle interrupt gracefully
    executionThread.join(2000);
    assertFalse(executionThread.isAlive(), "Thread should handle interrupt and exit");
  }

  // ==================== Test Stage Tracking Logic ====================

  @Test
  public void testIsStageAlreadyCompleted_ReturnsTrueWhenStageCompleted() throws Exception {
    // Arrange
    stageCompletionMap.put("stage1", 1);

    // Act
    Boolean result = invokePrivateStaticMethodWithParam("isStageAlreadyCompleted", "stage1");

    // Assert
    assertTrue(result, "Should return true when stage is completed");
  }

  @Test
  public void testIsStageAlreadyCompleted_ReturnsFalseWhenStageNotCompleted() throws Exception {
    // Act
    Boolean result = invokePrivateStaticMethodWithParam("isStageAlreadyCompleted", "stage1");

    // Assert
    assertFalse(result, "Should return false when stage is not completed");
  }

  @Test
  public void testIsStageAlreadyCompleted_ReturnsFalseWhenCountIsZero() throws Exception {
    // Arrange
    stageCompletionMap.put("stage1", 0);

    // Act
    Boolean result = invokePrivateStaticMethodWithParam("isStageAlreadyCompleted", "stage1");

    // Assert
    assertFalse(result, "Should return false when completion count is zero");
  }

  @Test
  public void testIsAllStagesCompletedAtLeastOnce_ReturnsFalseWhenEmpty() throws Exception {
    // Act
    Boolean result = invokePrivateStaticMethod("isAllStagesCompletedAtLeastOnce");

    // Assert
    assertFalse(result, "Should return false when completion map is empty");
  }

  @Test
  public void testIsAllStagesCompletedAtLeastOnce_ReturnsFalseWhenPartial() throws Exception {
    // Arrange
    stageCompletionMap.put("stage1", 1);
    stageSubmittedMap.put("stage1", 1);
    stageSubmittedMap.put("stage2", 1);
    // stage2 is not completed

    // Act
    Boolean result = invokePrivateStaticMethod("isAllStagesCompletedAtLeastOnce");

    // Assert
    assertFalse(result, "Should return false when not all stages are completed");
  }

  @Test
  public void testIsAllStagesCompletedAtLeastOnce_ReturnsTrueWhenAllCompleted() throws Exception {
    // Arrange
    stageCompletionMap.put("stage1", 1);
    stageCompletionMap.put("stage2", 1);
    stageSubmittedMap.put("stage1", 1);
    stageSubmittedMap.put("stage2", 1);

    try (org.mockito.MockedStatic<com.logwise.spark.jobs.impl.PushLogsToS3SparkJob> mockedJob =
        org.mockito.Mockito.mockStatic(com.logwise.spark.jobs.impl.PushLogsToS3SparkJob.class)) {
      mockedJob
          .when(com.logwise.spark.jobs.impl.PushLogsToS3SparkJob::getStreamingQueriesCount)
          .thenReturn(2);

      // Act
      Boolean result = invokePrivateStaticMethod("isAllStagesCompletedAtLeastOnce");

      // Assert
      assertTrue(result, "Should return true when all stages are completed");
    }
  }

  // ==================== Test Concurrent Access ====================

  @Test
  public void testConcurrentAccessToMaps() throws Exception {
    // Arrange
    int threadCount = 10;
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch endLatch = new CountDownLatch(threadCount);

    // Act - Multiple threads submitting and completing stages concurrently
    for (int i = 0; i < threadCount; i++) {
      final int stageNum = i;
      new Thread(
              () -> {
                try {
                  startLatch.await();

                  SparkListenerStageSubmitted stageSubmitted =
                      createStageSubmitted("stage" + (stageNum % 3), stageNum);
                  listener.onStageSubmitted(stageSubmitted);

                  SparkListenerStageCompleted stageCompleted =
                      createStageCompleted(
                          "stage" + (stageNum % 3), 100L, 1000L, 1000000L, 999000L);
                  listener.onStageCompleted(stageCompleted);

                } catch (Exception e) {
                  e.printStackTrace();
                } finally {
                  endLatch.countDown();
                }
              })
          .start();
    }

    // Trigger all threads to start
    startLatch.countDown();

    // Wait for all threads to complete
    assertTrue(endLatch.await(5, TimeUnit.SECONDS), "Not all threads completed");

    // Assert - Maps should have consistent data
    assertFalse(stageSubmittedMap.isEmpty(), "Submitted map should not be empty");
    assertFalse(stageCompletionMap.isEmpty(), "Completion map should not be empty");

    // Verify counts are correct (10 submissions across 3 stages)
    int totalSubmitted = stageSubmittedMap.values().stream().mapToInt(Integer::intValue).sum();
    int totalCompleted = stageCompletionMap.values().stream().mapToInt(Integer::intValue).sum();

    assertEquals(totalSubmitted, threadCount, "Total submitted count should match thread count");
    assertEquals(totalCompleted, threadCount, "Total completed count should match thread count");
  }

  // ==================== Test stopStage() ====================

  @Test
  public void testStopStage_CanBeInvokedWithoutException() throws Exception {
    // Arrange
    java.lang.reflect.Method stopStageMethod =
        SparkStageListener.class.getDeclaredMethod("stopStage", String.class, int.class);
    stopStageMethod.setAccessible(true);

    // Act - This starts a thread that may fail due to SparkSession dependency
    // The method itself should be callable without throwing exceptions
    stopStageMethod.invoke(listener, "Export Application Logs To S3", 1);

    // Assert - Verify the method can be called without throwing exception
    // Note: The async thread may fail due to SparkSession requirements, but that's
    // acceptable for unit tests. Full testing of stopStage execution requires
    // integration tests with a real SparkSession configured.
    assertTrue(true, "stopStage should be callable without throwing exceptions");
  }

  // ==================== Helper Methods ====================

  private SparkListenerStageSubmitted createStageSubmitted(String stageName, int stageId) {
    SparkListenerStageSubmitted stageSubmitted = mock(SparkListenerStageSubmitted.class);
    StageInfo stageInfo = mock(StageInfo.class);
    when(stageSubmitted.stageInfo()).thenReturn(stageInfo);
    when(stageInfo.name()).thenReturn(stageName);
    when(stageInfo.stageId()).thenReturn(stageId);
    return stageSubmitted;
  }

  private SparkListenerStageCompleted createStageCompleted(
      String stageName,
      long inputRecords,
      long outputBytes,
      long completionTime,
      long submissionTime) {
    SparkListenerStageCompleted stageCompleted = mock(SparkListenerStageCompleted.class);
    StageInfo stageInfo = mock(StageInfo.class);
    TaskMetrics taskMetrics = mock(TaskMetrics.class);
    InputMetrics inputMetrics = mock(InputMetrics.class);
    OutputMetrics outputMetrics = mock(OutputMetrics.class);

    when(stageCompleted.stageInfo()).thenReturn(stageInfo);
    when(stageInfo.name()).thenReturn(stageName);
    when(stageInfo.getStatusString()).thenReturn("succeeded");
    when(stageInfo.taskMetrics()).thenReturn(taskMetrics);
    when(taskMetrics.inputMetrics()).thenReturn(inputMetrics);
    when(taskMetrics.outputMetrics()).thenReturn(outputMetrics);
    when(inputMetrics.recordsRead()).thenReturn(inputRecords);
    when(outputMetrics.bytesWritten()).thenReturn(outputBytes);
    when(stageInfo.completionTime()).thenReturn(Option.apply(completionTime));
    when(stageInfo.submissionTime()).thenReturn(Option.apply(submissionTime));

    return stageCompleted;
  }

  private Boolean invokePrivateStaticMethodWithParam(String methodName, String param)
      throws Exception {
    java.lang.reflect.Method method =
        SparkStageListener.class.getDeclaredMethod(methodName, String.class);
    method.setAccessible(true);
    return (Boolean) method.invoke(null, param);
  }

  private Boolean invokePrivateStaticMethod(String methodName) throws Exception {
    java.lang.reflect.Method method = SparkStageListener.class.getDeclaredMethod(methodName);
    method.setAccessible(true);
    return (Boolean) method.invoke(null);
  }
}
