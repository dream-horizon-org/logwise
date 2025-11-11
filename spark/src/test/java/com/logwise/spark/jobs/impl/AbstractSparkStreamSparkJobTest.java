package com.logwise.spark.jobs.impl;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import com.logwise.spark.base.MockConfigHelper;
import com.logwise.spark.base.MockSparkSessionHelper;
import com.logwise.spark.constants.JobName;
import com.typesafe.config.Config;
import java.util.concurrent.CompletableFuture;
import org.apache.spark.sql.SparkSession;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Unit tests for AbstractSparkStreamSparkJob.
 *
 * <p>Tests verify the base class functionality for Spark stream jobs.
 */
public class AbstractSparkStreamSparkJobTest {

  private SparkSession mockSparkSession;
  private Config mockConfig;

  // Concrete implementation for testing abstract class
  private static class TestSparkStreamJob extends AbstractSparkStreamSparkJob<String> {
    public TestSparkStreamJob(SparkSession sparkSession, Config config) {
      super(sparkSession, config);
    }

    public TestSparkStreamJob() {
      super();
    }

    @Override
    public JobName getJobName() {
      return JobName.PUSH_LOGS_TO_S3;
    }

    @Override
    public void stop() {
      // No-op for testing
    }

    @Override
    public CompletableFuture<String> start() {
      return CompletableFuture.completedFuture("test");
    }
  }

  @BeforeMethod
  public void setUp() {
    mockSparkSession = MockSparkSessionHelper.createMockSparkSession();
    mockConfig = MockConfigHelper.createMinimalSparkConfig();
  }

  @AfterMethod
  public void tearDown() {
    if (mockSparkSession != null) {
      mockSparkSession.close();
    }
  }

  @Test
  public void testConstructorWithParameters_InitializesFieldsCorrectly() {
    // Act
    TestSparkStreamJob job = new TestSparkStreamJob(mockSparkSession, mockConfig);

    // Assert
    assertNotNull(job, "Job should not be null");
    assertEquals(
        job.sparkSession, mockSparkSession, "SparkSession should be initialized correctly");
    assertEquals(job.config, mockConfig, "Config should be initialized correctly");
  }

  @Test
  public void testDefaultConstructor_InitializesFieldsToNull() {
    // Act
    TestSparkStreamJob job = new TestSparkStreamJob();

    // Assert
    assertNotNull(job, "Job should not be null");
    assertNull(job.sparkSession, "SparkSession should be null with default constructor");
    assertNull(job.config, "Config should be null with default constructor");
  }

  @Test
  public void testTimeout_ReturnsLongMaxValue() {
    // Arrange
    TestSparkStreamJob job = new TestSparkStreamJob(mockSparkSession, mockConfig);

    // Act
    Long timeout = job.timeout();

    // Assert
    assertNotNull(timeout, "Timeout should not be null");
    assertEquals(
        timeout, Long.valueOf(Long.MAX_VALUE), "Timeout should be Long.MAX_VALUE for stream jobs");
  }

  @Test
  public void testTimeout_WithDefaultConstructor_ReturnsLongMaxValue() {
    // Arrange
    TestSparkStreamJob job = new TestSparkStreamJob();

    // Act
    Long timeout = job.timeout();

    // Assert
    assertNotNull(timeout, "Timeout should not be null");
    assertEquals(
        timeout,
        Long.valueOf(Long.MAX_VALUE),
        "Timeout should be Long.MAX_VALUE even with default constructor");
  }

  @Test
  public void testJobImplementsSerializable() {
    // Arrange
    TestSparkStreamJob job = new TestSparkStreamJob(mockSparkSession, mockConfig);

    // Assert
    assertTrue(
        job instanceof java.io.Serializable,
        "Job should implement Serializable for Spark distribution");
  }

  @Test
  public void testGetJobName_ReturnsCorrectJobName() {
    // Arrange
    TestSparkStreamJob job = new TestSparkStreamJob(mockSparkSession, mockConfig);

    // Act
    JobName jobName = job.getJobName();

    // Assert
    assertNotNull(jobName, "Job name should not be null");
    assertEquals(jobName, JobName.PUSH_LOGS_TO_S3, "Job name should match expected value");
  }

  @Test
  public void testStart_CompletesSuccessfully() throws Exception {
    // Arrange
    TestSparkStreamJob job = new TestSparkStreamJob(mockSparkSession, mockConfig);

    // Act
    CompletableFuture<String> result = job.start();

    // Assert
    assertNotNull(result, "Start result should not be null");
    assertTrue(result.isDone(), "Future should be completed");
    assertEquals(result.get(), "test", "Future should contain expected value");
  }

  @Test
  public void testStop_CanBeCalledWithoutException() {
    // Arrange
    TestSparkStreamJob job = new TestSparkStreamJob(mockSparkSession, mockConfig);

    // Act & Assert - should not throw any exception
    job.stop();
  }

  @Test
  public void testConstructorWithNullParameters_AllowsNullValues() {
    // Act
    TestSparkStreamJob job = new TestSparkStreamJob(null, null);

    // Assert
    assertNotNull(job, "Job should not be null even with null parameters");
    assertNull(job.sparkSession, "SparkSession should be null");
    assertNull(job.config, "Config should be null");
  }

  @Test
  public void testMultipleJobInstances_AreIndependent() {
    // Arrange
    SparkSession mockSparkSession2 = MockSparkSessionHelper.createMockSparkSession();
    Config mockConfig2 = MockConfigHelper.createMinimalSparkConfig();

    try {
      // Act
      TestSparkStreamJob job1 = new TestSparkStreamJob(mockSparkSession, mockConfig);
      TestSparkStreamJob job2 = new TestSparkStreamJob(mockSparkSession2, mockConfig2);

      // Assert
      assertNotNull(job1, "First job should not be null");
      assertNotNull(job2, "Second job should not be null");
      assertNotSame(job1, job2, "Jobs should be different instances");
      assertNotSame(
          job1.sparkSession, job2.sparkSession, "SparkSessions should be different instances");
      // Note: Config objects might be the same if they have the same values
    } finally {
      if (mockSparkSession2 != null) {
        mockSparkSession2.close();
      }
    }
  }
}
