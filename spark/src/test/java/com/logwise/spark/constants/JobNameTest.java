package com.logwise.spark.constants;

import static org.testng.Assert.*;

import org.testng.annotations.Test;

/** Unit tests for JobName enum. */
public class JobNameTest {

  @Test
  public void testFromValue_WithValidValue_ReturnsJobName() {
    // Act
    JobName jobName = JobName.fromValue("push-logs-to-s3");

    // Assert
    assertNotNull(jobName);
    assertEquals(jobName, JobName.PUSH_LOGS_TO_S3);
    assertEquals(jobName.getValue(), "push-logs-to-s3");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testFromValue_WithInvalidValue_ThrowsException() {
    // Act - should throw IllegalArgumentException
    JobName.fromValue("invalid-job-name");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testFromValue_WithNullValue_ThrowsException() {
    // Act - should throw IllegalArgumentException
    JobName.fromValue(null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testFromValue_WithEmptyValue_ThrowsException() {
    // Act - should throw IllegalArgumentException
    JobName.fromValue("");
  }

  @Test
  public void testGetValue_ReturnsCorrectValue() {
    // Act
    String value = JobName.PUSH_LOGS_TO_S3.getValue();

    // Assert
    assertNotNull(value);
    assertEquals(value, "push-logs-to-s3");
  }

  @Test
  public void testFromValue_WithCaseVariation_ThrowsException() {
    // Act - Job name is case-sensitive, so different case should fail
    try {
      JobName.fromValue("PUSH-LOGS-TO-S3");
      fail("Should throw IllegalArgumentException for case variation");
    } catch (IllegalArgumentException e) {
      // Expected
      assertTrue(e.getMessage().contains("Invalid value"));
    }
  }

  @Test
  public void testValues_ReturnsAllJobNames() {
    // Act
    JobName[] values = JobName.values();

    // Assert
    assertNotNull(values);
    assertEquals(values.length, 1, "Should have one job name");
    assertEquals(values[0], JobName.PUSH_LOGS_TO_S3);
  }

  @Test
  public void testValueOf_WithValidName_ReturnsJobName() {
    // Act
    JobName jobName = JobName.valueOf("PUSH_LOGS_TO_S3");

    // Assert
    assertNotNull(jobName);
    assertEquals(jobName, JobName.PUSH_LOGS_TO_S3);
  }
}
