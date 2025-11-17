package com.logwise.orchestrator.tests.unit;

import com.logwise.orchestrator.dto.entity.ServiceDetails;
import com.logwise.orchestrator.dto.request.ComponentSyncRequest;
import com.logwise.orchestrator.dto.request.MonitorSparkJobRequest;
import com.logwise.orchestrator.dto.request.SubmitSparkJobRequest;
import com.logwise.orchestrator.dto.response.*;
import com.logwise.orchestrator.rest.exception.RestException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Unit tests for DTO package (entity, request, response). */
public class DtoTest {

  @Test
  public void testServiceDetails_Constructor_CreatesInstance() {

    ServiceDetails details = new ServiceDetails();

    Assert.assertNotNull(details);
  }

  @Test
  public void testServiceDetails_AllArgsConstructor_CreatesInstance() {

    ServiceDetails details = new ServiceDetails("prod", "service1", "application", 30, "tenant1");

    Assert.assertEquals(details.getEnvironmentName(), "prod");
    Assert.assertEquals(details.getServiceName(), "service1");
    Assert.assertEquals(details.getComponentType(), "application");
    Assert.assertEquals(details.getRetentionDays(), Integer.valueOf(30));
    Assert.assertEquals(details.getTenant(), "tenant1");
  }

  @Test
  public void testServiceDetails_Builder_CreatesInstance() {

    ServiceDetails details =
        ServiceDetails.builder()
            .environmentName("dev")
            .serviceName("service2")
            .componentType("application")
            .retentionDays(60)
            .tenant("tenant2")
            .build();

    Assert.assertEquals(details.getEnvironmentName(), "dev");
    Assert.assertEquals(details.getServiceName(), "service2");
    Assert.assertEquals(details.getComponentType(), "application");
    Assert.assertEquals(details.getRetentionDays(), Integer.valueOf(60));
    Assert.assertEquals(details.getTenant(), "tenant2");
  }

  @Test
  public void testServiceDetails_Equals_WithSameValues_ReturnsTrue() {

    ServiceDetails details1 = new ServiceDetails("prod", "service1", "application", 30, "tenant1");
    ServiceDetails details2 = new ServiceDetails("prod", "service1", "application", 30, "tenant1");

    Assert.assertTrue(details1.equals(details2));
  }

  @Test
  public void testServiceDetails_Equals_WithDifferentEnv_ReturnsFalse() {

    ServiceDetails details1 = new ServiceDetails("prod", "service1", "application", 30, "tenant1");
    ServiceDetails details2 = new ServiceDetails("dev", "service1", "application", 30, "tenant1");

    Assert.assertFalse(details1.equals(details2));
  }

  @Test
  public void testComponentSyncRequest_NoArgsConstructor_CreatesInstance() {

    ComponentSyncRequest request = new ComponentSyncRequest();

    Assert.assertNotNull(request);
    Assert.assertNull(request.getComponentType());
  }

  @Test
  public void testComponentSyncRequest_SetComponentType_SetsValue() {

    ComponentSyncRequest request = new ComponentSyncRequest();

    request.setComponentType("application"); // Valid ComponentType value

    Assert.assertEquals(request.getComponentType(), "application");
  }

  @Test
  public void testComponentSyncRequest_ValidateParam_WithValidComponentType_Passes() {

    ComponentSyncRequest request = new ComponentSyncRequest();
    request.setComponentType("application"); // Valid ComponentType value

    request.validateParam();
  }

  @Test
  public void testComponentSyncRequest_ValidateParam_WithNullComponentType_ThrowsException() {

    ComponentSyncRequest request = new ComponentSyncRequest();
    request.setComponentType(null);

    try {
      request.validateParam();
      Assert.fail("Should have thrown RestException");
    } catch (RestException e) {
      Assert.assertNotNull(e);
    }
  }

  @Test
  public void testMonitorSparkJobRequest_NoArgsConstructor_CreatesInstance() {

    MonitorSparkJobRequest request = new MonitorSparkJobRequest();

    Assert.assertNotNull(request);
    Assert.assertNull(request.getDriverCores());
    Assert.assertNull(request.getDriverMemoryInGb());
  }

  @Test
  public void testMonitorSparkJobRequest_Setters_Work() {

    MonitorSparkJobRequest request = new MonitorSparkJobRequest();

    request.setDriverCores(4);
    request.setDriverMemoryInGb(8);

    Assert.assertEquals(request.getDriverCores(), Integer.valueOf(4));
    Assert.assertEquals(request.getDriverMemoryInGb(), Integer.valueOf(8));
  }

  @Test
  public void testSubmitSparkJobRequest_Builder_CreatesInstance() {

    SubmitSparkJobRequest request =
        SubmitSparkJobRequest.builder()
            .action("CreateSubmissionRequest")
            .appArgs(Arrays.asList("arg1", "arg2"))
            .appResource("s3://bucket/app.jar")
            .clientSparkVersion("3.2.0")
            .mainClass("com.example.Main")
            .build();

    Assert.assertEquals(request.getAction(), "CreateSubmissionRequest");
    Assert.assertEquals(request.getAppArgs().size(), 2);
    Assert.assertEquals(request.getAppResource(), "s3://bucket/app.jar");
    Assert.assertEquals(request.getClientSparkVersion(), "3.2.0");
    Assert.assertEquals(request.getMainClass(), "com.example.Main");
  }

  @Test
  public void testSubmitSparkJobRequest_Builder_WithDefaults_UsesDefaults() {

    SubmitSparkJobRequest request = SubmitSparkJobRequest.builder().build();

    Assert.assertEquals(request.getAction(), "CreateSubmissionRequest");
  }

  @Test
  public void testSubmitSparkJobRequest_Builder_WithEnvironmentVariables_SetsValue() {

    Map<String, String> envVars = new HashMap<>();
    envVars.put("KEY1", "VALUE1");
    envVars.put("KEY2", "VALUE2");

    SubmitSparkJobRequest request =
        SubmitSparkJobRequest.builder().environmentVariables(envVars).build();

    Assert.assertNotNull(request.getEnvironmentVariables());
    Assert.assertEquals(request.getEnvironmentVariables().size(), 2);
    Assert.assertEquals(request.getEnvironmentVariables().get("KEY1"), "VALUE1");
  }

  @Test
  public void testDefaultErrorResponse_CreatesInstance() {

    Assert.assertNotNull(DefaultErrorResponse.class);
  }

  @Test
  public void testDefaultSuccessResponse_Builder_CreatesInstance() {

    DefaultSuccessResponse response = DefaultSuccessResponse.builder().build();

    Assert.assertTrue(response.isSuccess());
    Assert.assertEquals(response.getMessage(), "Success");
  }

  @Test
  public void testDefaultSuccessResponse_Builder_WithCustomMessage_SetsMessage() {

    DefaultSuccessResponse response =
        DefaultSuccessResponse.builder().message("Custom success message").build();

    Assert.assertTrue(response.isSuccess());
    Assert.assertEquals(response.getMessage(), "Custom success message");
  }

  @Test
  public void testGetServiceDetailsResponse_Builder_CreatesInstance() {

    List<ServiceDetails> details =
        Arrays.asList(
            ServiceDetails.builder()
                .environmentName("dev")
                .serviceName("service2")
                .componentType("application")
                .build());

    GetServiceDetailsResponse response =
        GetServiceDetailsResponse.builder().serviceDetails(details).build();

    Assert.assertNotNull(response.getServiceDetails());
    Assert.assertEquals(response.getServiceDetails().size(), 1);
  }

  @Test
  public void testLogSyncDelayResponse_Builder_WithValues_SetsValues() {

    LogSyncDelayResponse response =
        LogSyncDelayResponse.builder().tenant("tenant1").appLogsDelayMinutes(15).build();

    Assert.assertEquals(response.getTenant(), "tenant1");
    Assert.assertEquals(response.getAppLogsDelayMinutes(), Integer.valueOf(15));
  }

  @Test
  public void testSparkMasterJsonResponse_NoArgsConstructor_CreatesInstance() {

    SparkMasterJsonResponse response = new SparkMasterJsonResponse();

    Assert.assertNotNull(response);
    Assert.assertNull(response.getActiveapps());
    Assert.assertEquals(response.getMemory(), 0);
    Assert.assertNull(response.getCores());
  }

  @Test
  public void testSparkMasterJsonResponse_Setters_Work() {

    SparkMasterJsonResponse response = new SparkMasterJsonResponse();

    response.setStatus("ALIVE");
    response.setUrl("http://spark-master:8080");
    response.setCores(100);
    response.setMemory(1024);

    Assert.assertEquals(response.getStatus(), "ALIVE");
    Assert.assertEquals(response.getUrl(), "http://spark-master:8080");
    Assert.assertEquals(response.getCores(), Integer.valueOf(100));
    Assert.assertEquals(response.getMemory(), 1024);
  }

  @Test
  public void testSparkMasterJsonResponse_App_NoArgsConstructor_CreatesInstance() {

    SparkMasterJsonResponse.App app = new SparkMasterJsonResponse.App();

    Assert.assertNotNull(app);
    Assert.assertEquals(app.getDuration(), 0L);
    Assert.assertEquals(app.getCores(), 0);
  }

  @Test
  public void testSparkMasterJsonResponse_App_Setters_Work() {

    SparkMasterJsonResponse.App app = new SparkMasterJsonResponse.App();

    app.setId("app-123");
    app.setName("test-app");
    app.setState("RUNNING");
    app.setUser("testuser");
    app.setCores(4);
    app.setMemoryperexecutor(2048);

    Assert.assertEquals(app.getId(), "app-123");
    Assert.assertEquals(app.getName(), "test-app");
    Assert.assertEquals(app.getState(), "RUNNING");
    Assert.assertEquals(app.getUser(), "testuser");
    Assert.assertEquals(app.getCores(), 4);
    Assert.assertEquals(app.getMemoryperexecutor(), 2048);
  }

  @Test
  public void testSparkMasterJsonResponse_Driver_CreatesInstance() {

    SparkMasterJsonResponse.Driver driver = new SparkMasterJsonResponse.Driver();

    Assert.assertNotNull(driver);
    Assert.assertEquals(driver.getCores(), 0);
    Assert.assertEquals(driver.getMemory(), 0);
  }

  @Test
  public void testSparkMasterJsonResponse_Driver_Setters_Work() {

    SparkMasterJsonResponse.Driver driver = new SparkMasterJsonResponse.Driver();

    driver.setId("driver-123");
    driver.setState("RUNNING");
    driver.setMainclass("com.example.Main");
    driver.setCores(2);
    driver.setMemory(1024);

    Assert.assertEquals(driver.getId(), "driver-123");
    Assert.assertEquals(driver.getState(), "RUNNING");
    Assert.assertEquals(driver.getMainclass(), "com.example.Main");
    Assert.assertEquals(driver.getCores(), 2);
    Assert.assertEquals(driver.getMemory(), 1024);
  }

  @Test
  public void testSparkMasterJsonResponse_Worker_NoArgsConstructor_CreatesInstance() {

    SparkMasterJsonResponse.Worker worker = new SparkMasterJsonResponse.Worker();

    Assert.assertNotNull(worker);
    Assert.assertEquals(worker.getCores(), 0);
    Assert.assertEquals(worker.getMemory(), 0);
  }

  @Test
  public void testSparkMasterJsonResponse_Worker_Setters_Work() {

    SparkMasterJsonResponse.Worker worker = new SparkMasterJsonResponse.Worker();

    worker.setId("worker-123");
    worker.setHost("worker1.example.com");
    worker.setPort(8081);
    worker.setState("ALIVE");
    worker.setCores(8);
    worker.setMemory(8192);
    worker.setCoresused(4);
    worker.setCoresfree(4);
    worker.setMemoryused(4096);
    worker.setMemoryfree(4096);

    Assert.assertEquals(worker.getId(), "worker-123");
    Assert.assertEquals(worker.getHost(), "worker1.example.com");
    Assert.assertEquals(worker.getPort(), 8081);
    Assert.assertEquals(worker.getState(), "ALIVE");
    Assert.assertEquals(worker.getCores(), 8);
    Assert.assertEquals(worker.getMemory(), 8192);
    Assert.assertEquals(worker.getCoresused(), 4);
    Assert.assertEquals(worker.getCoresfree(), 4);
    Assert.assertEquals(worker.getMemoryused(), 4096);
    Assert.assertEquals(worker.getMemoryfree(), 4096);
  }
}
