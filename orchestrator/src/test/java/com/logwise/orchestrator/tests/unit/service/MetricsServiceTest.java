package com.logwise.orchestrator.tests.unit.service;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.logwise.orchestrator.client.ObjectStoreClient;
import com.logwise.orchestrator.service.MetricsService;
import com.logwise.orchestrator.setup.BaseTest;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Unit tests for MetricsService. */
public class MetricsServiceTest extends BaseTest {

  private MetricsService metricsService;
  private ObjectStoreClient mockObjectStoreClient;

  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    // MetricsService uses @RequiredArgsConstructor
    metricsService = new MetricsService();
    mockObjectStoreClient = mock(ObjectStoreClient.class);
  }

  @Test
  public void testGetPrefixList_WithValidInputs_ReturnsPrefixList() throws Exception {
    Method method =
        MetricsService.class.getDeclaredMethod(
            "getPrefixList", LocalDateTime.class, String.class, String.class);
    method.setAccessible(true);

    LocalDateTime nowTime = LocalDateTime.of(2024, 1, 15, 12, 0);
    String dir = "logs";
    String serviceName = "api";

    @SuppressWarnings("unchecked")
    List<String> result = (List<String>) method.invoke(null, nowTime, dir, serviceName);

    Assert.assertNotNull(result);
    Assert.assertFalse(result.isEmpty());
    // Should generate prefixes for multiple hours
    Assert.assertTrue(result.size() > 1);
    // All prefixes should contain the expected pattern
    result.forEach(
        prefix -> {
          Assert.assertTrue(prefix.contains(serviceName));
        });
  }

  @Test
  public void testGetPrefixList_WithDifferentTimes_GeneratesDifferentPrefixes() throws Exception {
    Method method =
        MetricsService.class.getDeclaredMethod(
            "getPrefixList", LocalDateTime.class, String.class, String.class);
    method.setAccessible(true);

    LocalDateTime time1 = LocalDateTime.of(2024, 1, 15, 12, 0);
    LocalDateTime time2 = LocalDateTime.of(2024, 2, 20, 15, 30);

    @SuppressWarnings("unchecked")
    List<String> result1 = (List<String>) method.invoke(null, time1, "logs", "api");
    @SuppressWarnings("unchecked")
    List<String> result2 = (List<String>) method.invoke(null, time2, "logs", "api");

    Assert.assertNotNull(result1);
    Assert.assertNotNull(result2);
    // Should have different prefixes for different times
    Assert.assertNotEquals(result1, result2);
  }
}
