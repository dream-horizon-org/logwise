package com.logwise.orchestrator.tests.unit.service;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import com.logwise.orchestrator.config.ApplicationConfig;
import com.logwise.orchestrator.dto.kafka.SparkCheckpointOffsets;
import com.logwise.orchestrator.enums.Tenant;
import com.logwise.orchestrator.service.SparkCheckpointService;
import com.logwise.orchestrator.setup.BaseTest;
import com.logwise.orchestrator.testconfig.ApplicationTestConfig;
import com.logwise.orchestrator.util.ApplicationConfigUtil;
import com.logwise.orchestrator.util.S3Utils;
import io.reactivex.Single;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import org.apache.kafka.common.TopicPartition;
import org.mockito.MockedStatic;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Unit tests for SparkCheckpointService. */
public class SparkCheckpointServiceTest extends BaseTest {

  private SparkCheckpointService sparkCheckpointService;

  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    sparkCheckpointService = new SparkCheckpointService();
  }

  @Test
  public void testGetSparkCheckpointOffsets_WithValidCheckpoint_ReturnsOffsets() {
    Tenant tenant = Tenant.ABC;
    // Spark checkpoint format: v1\n{config}\n{offsets}
    String checkpointContent =
        "v1\n"
            + "{\"batchWatermarkMs\":0,\"batchTimestampMs\":1234567890,\"conf\":{}}\n"
            + "{\"topic-0\":{\"0\":100,\"1\":200},\"topic-1\":{\"0\":50}}";

    try (MockedStatic<ApplicationConfigUtil> mockedConfigUtil =
            mockStatic(ApplicationConfigUtil.class);
        MockedStatic<S3Utils> mockedS3Utils = mockStatic(S3Utils.class)) {
      ApplicationConfig.TenantConfig tenantConfig =
          ApplicationTestConfig.createMockTenantConfig("ABC");
      mockedConfigUtil
          .when(() -> ApplicationConfigUtil.getTenantConfig(tenant))
          .thenReturn(tenantConfig);

      // Mock listObjects to return offset files (numeric filenames: 0, 1, 2, etc.)
      mockedS3Utils
          .when(() -> S3Utils.listObjects(any(), any(), anyString()))
          .thenReturn(Single.just(Arrays.asList("checkpoint/application/offsets/1")));

      // Mock readFileContent to return checkpoint content
      mockedS3Utils
          .when(() -> S3Utils.readFileContent(any(), any(), anyString()))
          .thenReturn(Single.just(checkpointContent));

      SparkCheckpointOffsets result =
          sparkCheckpointService.getSparkCheckpointOffsets(tenant).blockingGet();

      assertNotNull(result);
      assertTrue(result.isAvailable());
      Map<TopicPartition, Long> offsets = result.getOffsets();
      assertEquals(offsets.size(), 3);
      assertEquals(offsets.get(new TopicPartition("topic-0", 0)), Long.valueOf(100));
      assertEquals(offsets.get(new TopicPartition("topic-0", 1)), Long.valueOf(200));
      assertEquals(offsets.get(new TopicPartition("topic-1", 0)), Long.valueOf(50));
    }
  }

  @Test
  public void testGetSparkCheckpointOffsets_WithNoOffsetFiles_ReturnsEmptyOffsets() {
    Tenant tenant = Tenant.ABC;

    try (MockedStatic<ApplicationConfigUtil> mockedConfigUtil =
            mockStatic(ApplicationConfigUtil.class);
        MockedStatic<S3Utils> mockedS3Utils = mockStatic(S3Utils.class)) {
      ApplicationConfig.TenantConfig tenantConfig =
          ApplicationTestConfig.createMockTenantConfig("ABC");
      mockedConfigUtil
          .when(() -> ApplicationConfigUtil.getTenantConfig(tenant))
          .thenReturn(tenantConfig);

      // Mock listObjects to return empty list
      mockedS3Utils
          .when(() -> S3Utils.listObjects(any(), any(), anyString()))
          .thenReturn(Single.just(Collections.emptyList()));

      SparkCheckpointOffsets result =
          sparkCheckpointService.getSparkCheckpointOffsets(tenant).blockingGet();

      assertNotNull(result);
      assertFalse(result.isAvailable());
      assertTrue(result.getOffsets().isEmpty());
    }
  }

  @Test
  public void testGetSparkCheckpointOffsets_WithInvalidJson_ReturnsEmptyOffsets() {
    Tenant tenant = Tenant.ABC;
    String invalidJson = "invalid json";

    try (MockedStatic<ApplicationConfigUtil> mockedConfigUtil =
            mockStatic(ApplicationConfigUtil.class);
        MockedStatic<S3Utils> mockedS3Utils = mockStatic(S3Utils.class)) {
      ApplicationConfig.TenantConfig tenantConfig =
          ApplicationTestConfig.createMockTenantConfig("ABC");
      mockedConfigUtil
          .when(() -> ApplicationConfigUtil.getTenantConfig(tenant))
          .thenReturn(tenantConfig);

      mockedS3Utils
          .when(() -> S3Utils.listObjects(any(), any(), anyString()))
          .thenReturn(Single.just(Arrays.asList("checkpoint/application/offsets/1")));

      mockedS3Utils
          .when(() -> S3Utils.readFileContent(any(), any(), anyString()))
          .thenReturn(Single.just(invalidJson));

      SparkCheckpointOffsets result =
          sparkCheckpointService.getSparkCheckpointOffsets(tenant).blockingGet();

      assertNotNull(result);
      assertFalse(result.isAvailable());
      assertTrue(result.getOffsets().isEmpty());
    }
  }

  @Test
  public void testGetSparkCheckpointOffsets_WithS3Error_ReturnsEmptyOffsets() {
    Tenant tenant = Tenant.ABC;

    try (MockedStatic<ApplicationConfigUtil> mockedConfigUtil =
            mockStatic(ApplicationConfigUtil.class);
        MockedStatic<S3Utils> mockedS3Utils = mockStatic(S3Utils.class)) {
      ApplicationConfig.TenantConfig tenantConfig =
          ApplicationTestConfig.createMockTenantConfig("ABC");
      mockedConfigUtil
          .when(() -> ApplicationConfigUtil.getTenantConfig(tenant))
          .thenReturn(tenantConfig);

      // Mock listObjects to throw error
      mockedS3Utils
          .when(() -> S3Utils.listObjects(any(), any(), anyString()))
          .thenReturn(Single.error(new RuntimeException("S3 error")));

      SparkCheckpointOffsets result =
          sparkCheckpointService.getSparkCheckpointOffsets(tenant).blockingGet();

      assertNotNull(result);
      assertFalse(result.isAvailable());
      assertTrue(result.getOffsets().isEmpty());
    }
  }

  @Test
  public void testGetSparkCheckpointOffsets_WithNoPartitionsNode_ReturnsEmptyOffsets() {
    Tenant tenant = Tenant.ABC;
    // Checkpoint with config but no offsets
    String checkpointWithoutOffsets = "v1\n{\"batchWatermarkMs\":0,\"conf\":{}}\n";

    try (MockedStatic<ApplicationConfigUtil> mockedConfigUtil =
            mockStatic(ApplicationConfigUtil.class);
        MockedStatic<S3Utils> mockedS3Utils = mockStatic(S3Utils.class)) {
      ApplicationConfig.TenantConfig tenantConfig =
          ApplicationTestConfig.createMockTenantConfig("ABC");
      mockedConfigUtil
          .when(() -> ApplicationConfigUtil.getTenantConfig(tenant))
          .thenReturn(tenantConfig);

      mockedS3Utils
          .when(() -> S3Utils.listObjects(any(), any(), anyString()))
          .thenReturn(Single.just(Arrays.asList("checkpoint/application/offsets/1")));

      mockedS3Utils
          .when(() -> S3Utils.readFileContent(any(), any(), anyString()))
          .thenReturn(Single.just(checkpointWithoutOffsets));

      SparkCheckpointOffsets result =
          sparkCheckpointService.getSparkCheckpointOffsets(tenant).blockingGet();

      assertNotNull(result);
      assertFalse(result.isAvailable());
      assertTrue(result.getOffsets().isEmpty());
    }
  }

  @Test
  public void testGetSparkCheckpointOffsets_WithMultipleFiles_SelectsLatest() {
    Tenant tenant = Tenant.ABC;
    // Spark checkpoint format: v1\n{config}\n{offsets}
    String checkpointContent =
        "v1\n"
            + "{\"batchWatermarkMs\":0,\"batchTimestampMs\":1234567890,\"conf\":{}}\n"
            + "{\"topic-0\":{\"0\":100,\"1\":200}}";

    try (MockedStatic<ApplicationConfigUtil> mockedConfigUtil =
            mockStatic(ApplicationConfigUtil.class);
        MockedStatic<S3Utils> mockedS3Utils = mockStatic(S3Utils.class)) {
      ApplicationConfig.TenantConfig tenantConfig =
          ApplicationTestConfig.createMockTenantConfig("ABC");
      mockedConfigUtil
          .when(() -> ApplicationConfigUtil.getTenantConfig(tenant))
          .thenReturn(tenantConfig);

      // Mock listObjects to return multiple offset files (0, 1, 2, 5, 10)
      // Should select file "10" as it's the highest number
      mockedS3Utils
          .when(() -> S3Utils.listObjects(any(), any(), anyString()))
          .thenReturn(
              Single.just(
                  Arrays.asList(
                      "checkpoint/application/offsets/0",
                      "checkpoint/application/offsets/1",
                      "checkpoint/application/offsets/2",
                      "checkpoint/application/offsets/5",
                      "checkpoint/application/offsets/10")));

      // Mock readFileContent to return checkpoint content when reading file "10"
      mockedS3Utils
          .when(
              () -> S3Utils.readFileContent(any(), any(), eq("checkpoint/application/offsets/10")))
          .thenReturn(Single.just(checkpointContent));

      SparkCheckpointOffsets result =
          sparkCheckpointService.getSparkCheckpointOffsets(tenant).blockingGet();

      assertNotNull(result);
      assertTrue(result.isAvailable());
      Map<TopicPartition, Long> offsets = result.getOffsets();
      assertEquals(offsets.size(), 2);
      assertEquals(offsets.get(new TopicPartition("topic-0", 0)), Long.valueOf(100));
      assertEquals(offsets.get(new TopicPartition("topic-0", 1)), Long.valueOf(200));
    }
  }

  @Test
  public void testGetSparkCheckpointOffsets_WithActualFormat_ReturnsOffsets() {
    Tenant tenant = Tenant.ABC;
    // Test with the actual format from production
    String checkpointContent =
        "v1\n"
            + "{\"batchWatermarkMs\":0,\"batchTimestampMs\":1765526672198,\"conf\":{\"spark.sql.streaming.stateStore.providerClass\":\"org.apache.spark.sql.execution.streaming.state.HDFSBackedStateStoreProvider\"}}\n"
            + "{\"logs.healthcheck-dummy\":{\"0\":79}}";

    try (MockedStatic<ApplicationConfigUtil> mockedConfigUtil =
            mockStatic(ApplicationConfigUtil.class);
        MockedStatic<S3Utils> mockedS3Utils = mockStatic(S3Utils.class)) {
      ApplicationConfig.TenantConfig tenantConfig =
          ApplicationTestConfig.createMockTenantConfig("ABC");
      mockedConfigUtil
          .when(() -> ApplicationConfigUtil.getTenantConfig(tenant))
          .thenReturn(tenantConfig);

      mockedS3Utils
          .when(() -> S3Utils.listObjects(any(), any(), anyString()))
          .thenReturn(Single.just(Arrays.asList("checkpoint/application/offsets/1")));

      mockedS3Utils
          .when(() -> S3Utils.readFileContent(any(), any(), anyString()))
          .thenReturn(Single.just(checkpointContent));

      SparkCheckpointOffsets result =
          sparkCheckpointService.getSparkCheckpointOffsets(tenant).blockingGet();

      assertNotNull(result);
      assertTrue(result.isAvailable());
      Map<TopicPartition, Long> offsets = result.getOffsets();
      assertEquals(offsets.size(), 1);
      assertEquals(offsets.get(new TopicPartition("logs.healthcheck-dummy", 0)), Long.valueOf(79));
    }
  }
}
