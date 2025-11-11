package com.logwise.spark.jobs;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import com.logwise.spark.base.MockConfigHelper;
import com.logwise.spark.base.MockSparkSessionHelper;
import com.logwise.spark.constants.JobName;
import com.logwise.spark.guice.injectors.ApplicationInjector;
import com.logwise.spark.guice.modules.MainModule;
import com.logwise.spark.jobs.impl.PushLogsToS3SparkJob;
import com.typesafe.config.Config;
import org.apache.spark.sql.SparkSession;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Unit tests for JobFactory.
 *
 * <p>Tests verify that the factory correctly creates job instances based on job name.
 */
public class JobFactoryTest {

  private SparkSession mockSparkSession;
  private Config mockConfig;

  @BeforeMethod
  public void setUp() {
    mockSparkSession = MockSparkSessionHelper.createMockSparkSession();
    mockConfig = MockConfigHelper.createMinimalSparkConfig();
    ApplicationInjector.initInjection(new MainModule(mockConfig));
  }

  @AfterMethod
  public void tearDown() {
    if (mockSparkSession != null) {
      mockSparkSession.close();
    }
    ApplicationInjector.reset();
  }

  @Test
  public void testGetSparkJob_WithPushLogsToS3JobName_ReturnsCorrectJob() {
    // Act
    SparkJob job = JobFactory.getSparkJob(JobName.PUSH_LOGS_TO_S3.getValue(), mockSparkSession);

    // Assert
    assertNotNull(job, "Job should not be null");
    assertTrue(
        job instanceof PushLogsToS3SparkJob, "Job should be instance of PushLogsToS3SparkJob");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testGetSparkJob_WithInvalidJobName_ThrowsException() {
    // Act - should throw IllegalArgumentException
    JobFactory.getSparkJob("invalid-job-name", mockSparkSession);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testGetSparkJob_WithNullJobName_ThrowsException() {
    // Act - should throw IllegalArgumentException
    JobFactory.getSparkJob(null, mockSparkSession);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testGetSparkJob_WithEmptyJobName_ThrowsException() {
    // Act - should throw IllegalArgumentException
    JobFactory.getSparkJob("", mockSparkSession);
  }

  @Test
  public void testGetSparkJob_CalledMultipleTimes_ReturnsNewInstances() {
    // Act - Create jobs multiple times
    SparkJob job1 = JobFactory.getSparkJob(JobName.PUSH_LOGS_TO_S3.getValue(), mockSparkSession);
    SparkJob job2 = JobFactory.getSparkJob(JobName.PUSH_LOGS_TO_S3.getValue(), mockSparkSession);

    // Assert - Should return different instances (factory creates new instances)
    assertNotNull(job1, "First job should not be null");
    assertNotNull(job2, "Second job should not be null");
    assertNotSame(job1, job2, "Factory should create new instances for each call");
  }

  @Test
  public void testGetSparkJob_WithDifferentSparkSessions_CreatesJobsWithDifferentSessions() {
    // Arrange
    SparkSession mockSparkSession2 = MockSparkSessionHelper.createMockSparkSession();

    try {
      // Act
      SparkJob job1 = JobFactory.getSparkJob(JobName.PUSH_LOGS_TO_S3.getValue(), mockSparkSession);
      SparkJob job2 = JobFactory.getSparkJob(JobName.PUSH_LOGS_TO_S3.getValue(), mockSparkSession2);

      // Assert
      assertNotNull(job1, "First job should not be null");
      assertNotNull(job2, "Second job should not be null");
      // Both jobs should be created successfully with different sessions
    } finally {
      if (mockSparkSession2 != null) {
        mockSparkSession2.close();
      }
    }
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testGetSparkJob_WithUnknownJobNameString_ThrowsException() {
    // Act - Try with a string that doesn't match any enum value
    JobFactory.getSparkJob("unknown-job-type", mockSparkSession);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testGetSparkJob_WithCaseVariation_ThrowsException() {
    // Act - Job name is case-sensitive, so different case should fail
    JobFactory.getSparkJob("PUSH-LOGS-TO-S3", mockSparkSession);
  }

  @Test
  public void testGetSparkJob_WithExactJobName_Succeeds() {
    // Arrange
    String exactJobName = "push-logs-to-s3";

    // Act
    SparkJob job = JobFactory.getSparkJob(exactJobName, mockSparkSession);

    // Assert
    assertNotNull(job, "Job should be created with exact job name");
  }
}
