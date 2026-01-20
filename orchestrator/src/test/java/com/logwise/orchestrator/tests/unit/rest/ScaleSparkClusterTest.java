package com.logwise.orchestrator.tests.unit.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;

import com.logwise.orchestrator.config.ApplicationConfig;
import com.logwise.orchestrator.dto.entity.SparkStageHistory;
import com.logwise.orchestrator.dto.request.ScaleSparkClusterRequest;
import com.logwise.orchestrator.dto.response.DefaultSuccessResponse;
import com.logwise.orchestrator.enums.Tenant;
import com.logwise.orchestrator.rest.ScaleSparkCluster;
import com.logwise.orchestrator.rest.io.Response;
import com.logwise.orchestrator.service.SparkService;
import com.logwise.orchestrator.setup.BaseTest;
import com.logwise.orchestrator.testconfig.ApplicationTestConfig;
import com.logwise.orchestrator.util.ApplicationConfigUtil;
import io.reactivex.Completable;
import java.util.concurrent.CompletionStage;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ScaleSparkClusterTest extends BaseTest {

  private ScaleSparkCluster scaleSparkCluster;
  private SparkService mockSparkService;

  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    mockSparkService = mock(SparkService.class);
    scaleSparkCluster = new ScaleSparkCluster(mockSparkService);
  }

  @Test
  public void testHandle_WithValidRequest_ReturnsSuccess() throws Exception {
    String tenantName = "ABC";
    ScaleSparkClusterRequest request = new ScaleSparkClusterRequest();
    request.setEnableUpScale(true);
    request.setEnableDownScale(false);
    SparkStageHistory stageHistory = new SparkStageHistory();
    request.setSparkStageHistory(stageHistory);

    // Create mock tenant config (non-docker cluster)
    ApplicationConfig.TenantConfig tenantConfig =
        ApplicationTestConfig.createMockTenantConfig("ABC");
    // Ensure cluster type is not docker
    tenantConfig.getSpark().getCluster().setClusterType("asg");

    try (MockedStatic<ApplicationConfigUtil> mockedConfigUtil =
        Mockito.mockStatic(ApplicationConfigUtil.class)) {
      mockedConfigUtil
          .when(() -> ApplicationConfigUtil.getTenantConfig(Tenant.ABC))
          .thenReturn(tenantConfig);
      mockedConfigUtil
          .when(() -> ApplicationConfigUtil.isDockerSparkCluster(tenantConfig))
          .thenReturn(false);

      when(mockSparkService.insertSparkStageHistory(any(SparkStageHistory.class)))
          .thenReturn(Completable.complete());
      when(mockSparkService.scaleSpark(any(Tenant.class), anyBoolean(), anyBoolean()))
          .thenReturn(Completable.complete());

      CompletionStage<Response<DefaultSuccessResponse>> result =
          scaleSparkCluster.handle(tenantName, request);

      // Wait a bit for async processing
      Thread.sleep(100);
      Response<DefaultSuccessResponse> response = result.toCompletableFuture().get();
      Assert.assertNotNull(response);
      Assert.assertNotNull(response.getData());
      Assert.assertEquals(response.getHttpStatusCode(), 200);
      verify(mockSparkService, times(1)).insertSparkStageHistory(any(SparkStageHistory.class));
      verify(mockSparkService, times(1)).scaleSpark(Tenant.ABC, true, false);
    }
  }
}
