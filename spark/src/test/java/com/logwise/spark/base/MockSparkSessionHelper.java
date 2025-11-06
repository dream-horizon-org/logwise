package com.logwise.spark.base;

import org.apache.spark.sql.SparkSession;
import org.mockito.Mockito;

/** Helper class for creating mock SparkSession objects for testing. */
public class MockSparkSessionHelper {

  /**
   * Creates a mock SparkSession with basic configuration. Does not create real SparkConf to avoid
   * Spark initialization issues.
   *
   * @return Mock SparkSession
   */
  public static SparkSession createMockSparkSession() {
    SparkSession mockSession = Mockito.mock(SparkSession.class);

    // Mock common SparkSession methods
    Mockito.when(mockSession.sparkContext()).thenReturn(null);
    Mockito.when(mockSession.newSession()).thenReturn(mockSession);

    return mockSession;
  }

  /**
   * Creates a mock SparkSession with custom configuration. Does not create real SparkConf to avoid
   * Spark initialization issues.
   *
   * @param appName Application name (not used in mock, but kept for API compatibility)
   * @param master Spark master URL (not used in mock, but kept for API compatibility)
   * @return Mock SparkSession
   */
  public static SparkSession createMockSparkSession(String appName, String master) {
    SparkSession mockSession = Mockito.mock(SparkSession.class);

    Mockito.when(mockSession.sparkContext()).thenReturn(null);
    Mockito.when(mockSession.newSession()).thenReturn(mockSession);

    return mockSession;
  }
}
