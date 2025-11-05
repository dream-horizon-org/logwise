package com.logwise.spark.base;

import org.apache.spark.SparkConf;
import org.apache.spark.sql.SparkSession;
import org.mockito.Mockito;

/**
 * Helper class for creating mock SparkSession objects for testing.
 */
public class MockSparkSessionHelper {

    /**
     * Creates a mock SparkSession with basic configuration.
     *
     * @return Mock SparkSession
     */
    public static SparkSession createMockSparkSession() {
        SparkSession mockSession = Mockito.mock(SparkSession.class);
        SparkConf mockConf = new SparkConf();
        mockConf.setMaster("local[2]");
        mockConf.setAppName("test-app");

        // Mock common SparkSession methods
        Mockito.when(mockSession.sparkContext()).thenReturn(null);
        Mockito.when(mockSession.newSession()).thenReturn(mockSession);

        return mockSession;
    }

    /**
     * Creates a mock SparkSession with custom configuration.
     *
     * @param appName Application name
     * @param master  Spark master URL
     * @return Mock SparkSession
     */
    public static SparkSession createMockSparkSession(String appName, String master) {
        SparkSession mockSession = Mockito.mock(SparkSession.class);
        SparkConf mockConf = new SparkConf();
        mockConf.setMaster(master);
        mockConf.setAppName(appName);

        Mockito.when(mockSession.sparkContext()).thenReturn(null);
        Mockito.when(mockSession.newSession()).thenReturn(mockSession);

        return mockSession;
    }
}
