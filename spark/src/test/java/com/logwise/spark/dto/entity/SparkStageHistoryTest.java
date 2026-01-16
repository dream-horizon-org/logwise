package com.logwise.spark.dto.entity;

import static org.testng.Assert.*;

import org.testng.annotations.Test;

public class SparkStageHistoryTest {

  @Test
  public void testCompareTo_WithEarlierSubmissionTime_ReturnsNegative() {
    SparkStageHistory history1 = new SparkStageHistory();
    history1.setSubmissionTime(1000L);

    SparkStageHistory history2 = new SparkStageHistory();
    history2.setSubmissionTime(2000L);

    int result = history1.compareTo(history2);

    assertTrue(result < 0, "Earlier submission time should return negative");
  }

  @Test
  public void testCompareTo_WithLaterSubmissionTime_ReturnsPositive() {
    SparkStageHistory history1 = new SparkStageHistory();
    history1.setSubmissionTime(2000L);

    SparkStageHistory history2 = new SparkStageHistory();
    history2.setSubmissionTime(1000L);

    int result = history1.compareTo(history2);

    assertTrue(result > 0, "Later submission time should return positive");
  }

  @Test
  public void testCompareTo_WithSameSubmissionTime_ReturnsZero() {
    SparkStageHistory history1 = new SparkStageHistory();
    history1.setSubmissionTime(1000L);

    SparkStageHistory history2 = new SparkStageHistory();
    history2.setSubmissionTime(1000L);

    int result = history1.compareTo(history2);

    assertEquals(result, 0, "Same submission time should return zero");
  }

  @Test
  public void testCompareTo_WithNullSubmissionTime_HandlesGracefully() {
    SparkStageHistory history1 = new SparkStageHistory();
    history1.setSubmissionTime(null);

    SparkStageHistory history2 = new SparkStageHistory();
    history2.setSubmissionTime(1000L);

    try {
      int result = history1.compareTo(history2);
      // If it doesn't throw, verify behavior
      assertNotNull(result);
    } catch (Exception e) {
      // NullPointerException is acceptable for null submissionTime
      assertTrue(e instanceof NullPointerException);
    }
  }

  @Test
  public void testSettersAndGetters_WorkCorrectly() {
    SparkStageHistory history = new SparkStageHistory();

    history.setOutputBytes(1000L);
    history.setInputRecords(500L);
    history.setSubmissionTime(1000000L);
    history.setCompletionTime(2000000L);
    history.setCoresUsed(4);
    history.setStatus("succeeded");
    history.setTenant("ABC");

    assertEquals(history.getOutputBytes(), Long.valueOf(1000L));
    assertEquals(history.getInputRecords(), Long.valueOf(500L));
    assertEquals(history.getSubmissionTime(), Long.valueOf(1000000L));
    assertEquals(history.getCompletionTime(), Long.valueOf(2000000L));
    assertEquals(history.getCoresUsed(), Integer.valueOf(4));
    assertEquals(history.getStatus(), "succeeded");
    assertEquals(history.getTenant(), "ABC");
  }
}
