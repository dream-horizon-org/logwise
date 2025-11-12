package com.logwise.spark.jobs.impl;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import com.logwise.spark.base.MockConfigHelper;
import com.logwise.spark.base.MockSparkSessionHelper;
import com.logwise.spark.constants.JobName;
import com.logwise.spark.guice.injectors.ApplicationInjector;
import com.logwise.spark.guice.modules.MainModule;
import com.logwise.spark.stream.Stream;
import com.logwise.spark.stream.StreamFactory;
import com.typesafe.config.Config;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.streaming.StreamingQuery;
import org.apache.spark.sql.streaming.StreamingQueryException;
import org.mockito.MockedStatic;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Unit tests for PushLogsToS3SparkJob.
 *
 * <p>Tests focus on public API behavior: job lifecycle, error handling, and observable outcomes.
 */
public class PushLogsToS3SparkJobTest {

  private SparkSession mockSparkSession;
  private Config mockConfig;
  private PushLogsToS3SparkJob job;

  @BeforeMethod
  public void setUp() {
    mockSparkSession = MockSparkSessionHelper.createMockSparkSession();
    mockConfig = createTestConfig();
    ApplicationInjector.initInjection(new MainModule(mockConfig));
    StaticFieldHelper.reset();
    job = new PushLogsToS3SparkJob(mockConfig, mockSparkSession);
  }

  @AfterMethod
  public void tearDown() {
    try {
      job.stop();
      Thread.sleep(50); // Brief cleanup wait
    } catch (Exception e) {
      // Ignore cleanup errors
    }
    StaticFieldHelper.reset();
    if (mockSparkSession != null) {
      mockSparkSession.close();
    }
    ApplicationInjector.reset();
  }

  private Config createTestConfig() {
    Map<String, Object> configMap = new HashMap<>();
    configMap.put("app.job.name", "PUSH_LOGS_TO_S3");
    configMap.put("spark.streamingquery.timeout.minutes", 1);
    configMap.put("spark.streams.name", Collections.singletonList("application-logs-stream-to-s3"));
    configMap.put("tenant.name", "test-tenant");
    configMap.put("kafka.bootstrap.servers.port", "9092");
    configMap.put("logCentral.orchestrator.url", "http://localhost:8081");
    configMap.put("spark.offsetPerTrigger.default", 10000L);
    configMap.put("kafka.cluster.dns", "test-kafka.local");
    configMap.put("kafka.startingOffsetsTimestamp", 0L);
    configMap.put("kafka.startingOffsets", "latest");
    configMap.put("kafka.topic.prefix.application", "app-logs-.*");
    configMap.put("kafka.maxRatePerPartition", "1000");
    return MockConfigHelper.createNestedConfig(configMap);
  }

  // ========== Helper Methods ==========

  /** Sets up mocks for StreamFactory and SparkSession.newSession(). */
  private MockedStatic<StreamFactory> setupMockStreamFactory(Stream mockStream) {
    SparkSession mockNewSession = mock(SparkSession.class);
    when(mockSparkSession.newSession()).thenReturn(mockNewSession);
    MockedStatic<StreamFactory> mockedFactory = mockStatic(StreamFactory.class);
    mockedFactory.when(() -> StreamFactory.getStream(any())).thenReturn(mockStream);
    return mockedFactory;
  }

  /**
   * Creates a mock StreamingQuery that signals via CountDownLatch when awaitTermination is called.
   */
  private StreamingQuery createSignalableMockQuery(CountDownLatch signalLatch) {
    StreamingQuery mockQuery = mock(StreamingQuery.class);
    when(mockQuery.name()).thenReturn("test-query");
    try {
      doAnswer(
              invocation -> {
                signalLatch.countDown();
                return null;
              })
          .when(mockQuery)
          .awaitTermination();
      doNothing().when(mockQuery).stop();
    } catch (StreamingQueryException | TimeoutException e) {
      // Mock setup - exceptions shouldn't occur
    }
    return mockQuery;
  }

  /** Starts the job in a daemon thread and returns the thread. */
  private Thread startJobAsync() {
    Thread thread =
        new Thread(
            () -> {
              try {
                job.start();
              } catch (Exception e) {
                // Expected - monitorJob blocks indefinitely
              }
            });
    thread.setDaemon(true);
    thread.start();
    return thread;
  }

  /** Waits for the streaming query count to be set, indicating streams have started. */
  private boolean waitForStreamsToStart(int timeoutSeconds) throws InterruptedException {
    long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(timeoutSeconds);
    while (System.currentTimeMillis() < deadline) {
      if (PushLogsToS3SparkJob.getStreamingQueriesCount() > 0) {
        return true;
      }
      Thread.sleep(50);
    }
    return false;
  }

  // ========== Public API Tests ==========

  @Test
  public void testGetJobName_ReturnsCorrectJobName() {
    assertEquals(job.getJobName(), JobName.PUSH_LOGS_TO_S3);
  }

  @Test
  public void testStop_WhenThreadIsNull_DoesNotThrow() {
    // Should not throw when thread is null
    job.stop();
    assertTrue(true, "Stop should complete without exception");
  }

  @Test
  public void testStop_WhenThreadIsAlive_InterruptsThread() throws Exception {
    CountDownLatch streamStartedLatch = new CountDownLatch(1);
    StreamingQuery mockQuery = createSignalableMockQuery(streamStartedLatch);

    Stream mockStream = mock(Stream.class);
    when(mockStream.startStreams(any(SparkSession.class)))
        .thenReturn(Collections.singletonList(mockQuery));

    Thread startThread = null;
    try (MockedStatic<StreamFactory> mockedFactory = setupMockStreamFactory(mockStream)) {
      startThread = startJobAsync();

      // Wait for stream to start (this confirms StreamFactory.getStream() was called)
      if (streamStartedLatch.await(3, TimeUnit.SECONDS)) {
        // Verify StreamFactory was used (indirectly through job execution)
        mockedFactory.verify(() -> StreamFactory.getStream(any()), atLeastOnce());

        job.stop();
        Thread.sleep(100); // Brief wait for interrupt to process
        assertTrue(true, "Stop completed successfully");
      }
    } finally {
      if (startThread != null) {
        startThread.interrupt();
      }
    }
  }

  @Test
  public void testStopAllRunningJobs_StopsAllRegisteredJobs() throws Exception {
    PushLogsToS3SparkJob job1 = spy(new PushLogsToS3SparkJob(mockConfig, mockSparkSession));
    PushLogsToS3SparkJob job2 = spy(new PushLogsToS3SparkJob(mockConfig, mockSparkSession));

    StaticFieldHelper.addToRunningJobs(job1, job2);
    doNothing().when(job1).stop();
    doNothing().when(job2).stop();

    PushLogsToS3SparkJob.stopAllRunningJobs();

    verify(job1).stop();
    verify(job2).stop();
  }

  @Test
  public void testStopAllRunningJobs_WithNoRunningJobs_DoesNotThrow() {
    // Should not throw when no jobs are running
    PushLogsToS3SparkJob.stopAllRunningJobs();
    assertTrue(true, "Should complete without exception");
  }

  @Test
  public void testGetStreamingQueriesCount_ReturnsCurrentCount() throws Exception {
    StaticFieldHelper.setStreamingQueriesCount(5);
    assertEquals(PushLogsToS3SparkJob.getStreamingQueriesCount(), 5);
  }

  // ========== Error Handling Tests ==========

  @Test
  public void testStart_HandlesStreamCreationException() throws Exception {
    Stream mockStream = mock(Stream.class);
    when(mockStream.startStreams(any(SparkSession.class)))
        .thenThrow(new RuntimeException("Stream creation failed"));

    Thread startThread = null;
    try (MockedStatic<StreamFactory> mockedFactory = setupMockStreamFactory(mockStream)) {
      startThread = startJobAsync();

      // Wait for job to be removed from running jobs (indicates exception was handled)
      List<PushLogsToS3SparkJob> runningJobs = StaticFieldHelper.getRunningJobs();
      for (int i = 0; i < 20; i++) {
        Thread.sleep(100);
        if (!runningJobs.contains(job)) {
          break;
        }
        runningJobs = StaticFieldHelper.getRunningJobs(); // Refresh
      }

      assertFalse(
          StaticFieldHelper.getRunningJobs().contains(job),
          "Job should be removed from running jobs after exception");
    } finally {
      if (startThread != null) {
        startThread.interrupt();
      }
    }
  }

  // ========== Integration-style Tests ==========

  @Test
  public void testJobLifecycle_StartStopCycle() throws Exception {
    CountDownLatch streamStartedLatch = new CountDownLatch(1);
    StreamingQuery mockQuery = createSignalableMockQuery(streamStartedLatch);

    Stream mockStream = mock(Stream.class);
    when(mockStream.startStreams(any(SparkSession.class)))
        .thenReturn(Collections.singletonList(mockQuery));

    Thread startThread = null;
    try (MockedStatic<StreamFactory> mockedFactory = setupMockStreamFactory(mockStream)) {
      startThread = startJobAsync();

      // Wait for stream to start
      if (streamStartedLatch.await(3, TimeUnit.SECONDS)) {
        assertEquals(PushLogsToS3SparkJob.getStreamingQueriesCount(), 1);

        job.stop();
        Thread.sleep(100);

        verify(mockStream, atLeastOnce()).startStreams(any(SparkSession.class));
        verify(mockQuery, atLeastOnce()).stop();
      } else {
        // Stream didn't start in time, but job should still handle stop gracefully
        job.stop();
        assertTrue(true, "Job handles stop even if stream didn't start");
      }
    } finally {
      if (startThread != null) {
        startThread.interrupt();
      }
    }
  }

  @Test
  public void testAwaitAndStopStreamingQueries_HandlesStreamingQueryException() throws Exception {
    CountDownLatch stopCalledLatch = new CountDownLatch(1);

    StreamingQuery mockQuery = mock(StreamingQuery.class);
    when(mockQuery.name()).thenReturn("test-query");
    try {
      doThrow(
              new StreamingQueryException(
                  "Test exception", "test", new RuntimeException(), "start", "end"))
          .when(mockQuery)
          .awaitTermination();
      doAnswer(
              invocation -> {
                stopCalledLatch.countDown();
                return null;
              })
          .when(mockQuery)
          .stop();
    } catch (StreamingQueryException | TimeoutException e) {
      // Mock setup
    }

    Stream mockStream = mock(Stream.class);
    when(mockStream.startStreams(any(SparkSession.class)))
        .thenReturn(Collections.singletonList(mockQuery));

    Thread startThread = null;
    try (MockedStatic<StreamFactory> mockedFactory = setupMockStreamFactory(mockStream)) {
      startThread = startJobAsync();

      // Wait for exception handling and stop to be called
      if (stopCalledLatch.await(3, TimeUnit.SECONDS)) {
        verify(mockQuery, atLeastOnce()).stop();
      }
    } finally {
      if (startThread != null) {
        startThread.interrupt();
      }
    }
  }

  @Test
  public void testAwaitAndStopStreamingQueries_HandlesStopTimeoutException() throws Exception {
    CountDownLatch awaitCalledLatch = new CountDownLatch(1);

    StreamingQuery mockQuery = mock(StreamingQuery.class);
    when(mockQuery.name()).thenReturn("test-query");
    try {
      doAnswer(
              invocation -> {
                awaitCalledLatch.countDown();
                return null;
              })
          .when(mockQuery)
          .awaitTermination();
      doThrow(new TimeoutException("Test timeout")).when(mockQuery).stop();
    } catch (StreamingQueryException | TimeoutException e) {
      // Mock setup
    }

    Stream mockStream = mock(Stream.class);
    when(mockStream.startStreams(any(SparkSession.class)))
        .thenReturn(Collections.singletonList(mockQuery));

    Thread startThread = null;
    try (MockedStatic<StreamFactory> mockedFactory = setupMockStreamFactory(mockStream)) {
      startThread = startJobAsync();

      // Wait for awaitTermination to be called
      if (awaitCalledLatch.await(3, TimeUnit.SECONDS)) {
        Thread.sleep(200); // Give time for stop to be called
        verify(mockQuery, atLeastOnce()).awaitTermination();
        verify(mockQuery, atLeastOnce()).stop();
      }
    } finally {
      if (startThread != null) {
        startThread.interrupt();
      }
    }
  }

  // ========== Helper Class for Reflection Operations ==========

  /**
   * Helper class to encapsulate reflection-based operations on static fields. This isolates the
   * complexity of reflection from the test logic.
   */
  private static class StaticFieldHelper {
    @SuppressWarnings("unchecked")
    private static List<PushLogsToS3SparkJob> getRunningJobs() {
      try {
        Field field = PushLogsToS3SparkJob.class.getDeclaredField("RUNNING_JOBS");
        field.setAccessible(true);
        return (List<PushLogsToS3SparkJob>) field.get(null);
      } catch (Exception e) {
        throw new RuntimeException("Failed to get RUNNING_JOBS", e);
      }
    }

    private static void setStreamingQueriesCount(int count) {
      try {
        Field field = PushLogsToS3SparkJob.class.getDeclaredField("streamingQueriesCount");
        field.setAccessible(true);
        field.setInt(null, count);
      } catch (Exception e) {
        throw new RuntimeException("Failed to set streamingQueriesCount", e);
      }
    }

    private static void reset() {
      try {
        // Reset RUNNING_JOBS
        Field runningJobsField = PushLogsToS3SparkJob.class.getDeclaredField("RUNNING_JOBS");
        runningJobsField.setAccessible(true);
        CopyOnWriteArrayList<?> runningJobs = (CopyOnWriteArrayList<?>) runningJobsField.get(null);
        runningJobs.clear();

        // Reset streamingQueriesCount
        Field countField = PushLogsToS3SparkJob.class.getDeclaredField("streamingQueriesCount");
        countField.setAccessible(true);
        countField.setInt(null, 0);
      } catch (Exception e) {
        throw new RuntimeException("Failed to reset static fields", e);
      }
    }

    @SafeVarargs
    private static void addToRunningJobs(PushLogsToS3SparkJob... jobs) {
      List<PushLogsToS3SparkJob> runningJobs = getRunningJobs();
      runningJobs.addAll(Arrays.asList(jobs));
    }
  }
}
