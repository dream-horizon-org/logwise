package com.logwise.orchestrator.tests.unit.client;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.logwise.orchestrator.client.impl.VMClientAwsImpl;
import com.logwise.orchestrator.common.util.CompletableFutureUtils;
import com.logwise.orchestrator.config.ApplicationConfig;
import com.logwise.orchestrator.helper.HelperTestUtils;
import com.logwise.orchestrator.setup.BaseTest;
import io.reactivex.Completable;
import io.reactivex.Single;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import software.amazon.awssdk.services.ec2.Ec2AsyncClient;
import software.amazon.awssdk.services.ec2.model.*;

/** Unit tests for VMClientAwsImpl. */
public class VMClientAwsImplTest extends BaseTest {

  private VMClientAwsImpl vmClientAwsImpl;
  private Ec2AsyncClient mockEc2Client;
  private ApplicationConfig.VMConfig mockVmConfig;
  private ApplicationConfig.EC2Config mockEc2Config;

  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    vmClientAwsImpl = new VMClientAwsImpl();
    mockEc2Client = mock(Ec2AsyncClient.class);
    mockVmConfig = mock(ApplicationConfig.VMConfig.class);
    mockEc2Config = mock(ApplicationConfig.EC2Config.class);

    when(mockVmConfig.getAws()).thenReturn(mockEc2Config);
    when(mockEc2Config.getRegion()).thenReturn("us-east-1");

    // Inject mock client using reflection
    HelperTestUtils.setPrivateField(vmClientAwsImpl, "ec2Client", mockEc2Client);
    HelperTestUtils.setPrivateField(vmClientAwsImpl, "ec2Config", mockEc2Config);
  }

  @AfterClass
  public static void tearDownClass() {
    BaseTest.cleanup();
  }

  @Test
  public void testRxConnect_WithValidConfig_CompletesSuccessfully() {
    Completable result = vmClientAwsImpl.rxConnect(mockVmConfig);

    Assert.assertNotNull(result);
    result.blockingAwait();
  }

  @Test
  public void testRxConnect_WithNullAwsConfig_CompletesSuccessfully() {
    ApplicationConfig.VMConfig configWithNullAws = mock(ApplicationConfig.VMConfig.class);
    when(configWithNullAws.getAws()).thenReturn(null);

    Completable result = vmClientAwsImpl.rxConnect(configWithNullAws);

    Assert.assertNotNull(result);
    result.blockingAwait();
  }

  @Test
  public void testGetInstanceIds_WithValidIPs_ReturnsInstanceIds() {
    List<String> ips = Arrays.asList("10.0.1.1", "10.0.1.2");

    Instance instance1 =
        Instance.builder().instanceId("i-123").privateIpAddress("10.0.1.1").build();
    Instance instance2 =
        Instance.builder().instanceId("i-456").privateIpAddress("10.0.1.2").build();

    Reservation reservation =
        Reservation.builder().instances(Arrays.asList(instance1, instance2)).build();

    DescribeInstancesResponse describeResponse =
        DescribeInstancesResponse.builder().reservations(Arrays.asList(reservation)).build();

    CompletableFuture<DescribeInstancesResponse> describeFuture =
        CompletableFuture.completedFuture(describeResponse);

    when(mockEc2Client.describeInstances(any(DescribeInstancesRequest.class)))
        .thenReturn(describeFuture);

    try (MockedStatic<CompletableFutureUtils> mockedUtils =
        Mockito.mockStatic(CompletableFutureUtils.class)) {
      mockedUtils
          .when(() -> CompletableFutureUtils.toSingle(any(CompletableFuture.class)))
          .thenAnswer(invocation -> Single.fromFuture(invocation.getArgument(0)));

      Single<Map<String, String>> result = vmClientAwsImpl.getInstanceIds(ips);

      Map<String, String> ipToInstanceId = result.blockingGet();
      Assert.assertNotNull(ipToInstanceId);
      Assert.assertEquals(ipToInstanceId.size(), 2);
      Assert.assertEquals(ipToInstanceId.get("10.0.1.1"), "i-123");
      Assert.assertEquals(ipToInstanceId.get("10.0.1.2"), "i-456");
    }
  }

  @Test
  public void testGetInstanceIds_WithNullIPs_ReturnsEmptyMap() {
    Single<Map<String, String>> result = vmClientAwsImpl.getInstanceIds(null);

    Map<String, String> ipToInstanceId = result.blockingGet();
    Assert.assertNotNull(ipToInstanceId);
    Assert.assertTrue(ipToInstanceId.isEmpty());
  }

  @Test
  public void testGetInstanceIds_WithEmptyIPs_ReturnsEmptyMap() {
    Single<Map<String, String>> result = vmClientAwsImpl.getInstanceIds(Collections.emptyList());

    Map<String, String> ipToInstanceId = result.blockingGet();
    Assert.assertNotNull(ipToInstanceId);
    Assert.assertTrue(ipToInstanceId.isEmpty());
  }

  @Test
  public void testGetInstanceIds_WithNoMatchingInstances_ReturnsEmptyMap() {
    List<String> ips = Arrays.asList("10.0.1.1", "10.0.1.2");

    DescribeInstancesResponse describeResponse =
        DescribeInstancesResponse.builder().reservations(Collections.emptyList()).build();

    CompletableFuture<DescribeInstancesResponse> describeFuture =
        CompletableFuture.completedFuture(describeResponse);

    when(mockEc2Client.describeInstances(any(DescribeInstancesRequest.class)))
        .thenReturn(describeFuture);

    try (MockedStatic<CompletableFutureUtils> mockedUtils =
        Mockito.mockStatic(CompletableFutureUtils.class)) {
      mockedUtils
          .when(() -> CompletableFutureUtils.toSingle(any(CompletableFuture.class)))
          .thenAnswer(invocation -> Single.fromFuture(invocation.getArgument(0)));

      Single<Map<String, String>> result = vmClientAwsImpl.getInstanceIds(ips);

      Map<String, String> ipToInstanceId = result.blockingGet();
      Assert.assertNotNull(ipToInstanceId);
      Assert.assertTrue(ipToInstanceId.isEmpty());
    }
  }

  @Test
  public void testTerminateInstances_WithValidInstanceIds_TerminatesSuccessfully() {
    List<String> instanceIds = Arrays.asList("i-123", "i-456");

    TerminateInstancesResponse terminateResponse =
        TerminateInstancesResponse.builder()
            .terminatingInstances(
                Arrays.asList(
                    InstanceStateChange.builder()
                        .instanceId("i-123")
                        .currentState(
                            InstanceState.builder().name(InstanceStateName.TERMINATED).build())
                        .build(),
                    InstanceStateChange.builder()
                        .instanceId("i-456")
                        .currentState(
                            InstanceState.builder().name(InstanceStateName.TERMINATED).build())
                        .build()))
            .build();

    CompletableFuture<TerminateInstancesResponse> terminateFuture =
        CompletableFuture.completedFuture(terminateResponse);

    when(mockEc2Client.terminateInstances(any(TerminateInstancesRequest.class)))
        .thenReturn(terminateFuture);

    try (MockedStatic<CompletableFutureUtils> mockedUtils =
        Mockito.mockStatic(CompletableFutureUtils.class)) {
      mockedUtils
          .when(() -> CompletableFutureUtils.toSingle(any(CompletableFuture.class)))
          .thenAnswer(invocation -> Single.fromFuture(invocation.getArgument(0)));

      Completable result = vmClientAwsImpl.terminateInstances(instanceIds);

      result.blockingAwait();
      verify(mockEc2Client, times(1)).terminateInstances(any(TerminateInstancesRequest.class));
    }
  }

  @Test
  public void testGetInstanceIPs_WithValidInstanceIds_ReturnsIPs() {
    List<String> instanceIds = Arrays.asList("i-123", "i-456");

    Instance instance1 =
        Instance.builder().instanceId("i-123").privateIpAddress("10.0.1.1").build();
    Instance instance2 =
        Instance.builder().instanceId("i-456").privateIpAddress("10.0.1.2").build();

    Reservation reservation =
        Reservation.builder().instances(Arrays.asList(instance1, instance2)).build();

    DescribeInstancesResponse describeResponse =
        DescribeInstancesResponse.builder().reservations(Arrays.asList(reservation)).build();

    CompletableFuture<DescribeInstancesResponse> describeFuture =
        CompletableFuture.completedFuture(describeResponse);

    when(mockEc2Client.describeInstances(any(DescribeInstancesRequest.class)))
        .thenReturn(describeFuture);

    try (MockedStatic<CompletableFutureUtils> mockedUtils =
        Mockito.mockStatic(CompletableFutureUtils.class)) {
      mockedUtils
          .when(() -> CompletableFutureUtils.toSingle(any(CompletableFuture.class)))
          .thenAnswer(invocation -> Single.fromFuture(invocation.getArgument(0)));

      Single<Map<String, String>> result = vmClientAwsImpl.getInstanceIPs(instanceIds);

      Map<String, String> instanceIdToIp = result.blockingGet();
      Assert.assertNotNull(instanceIdToIp);
      Assert.assertEquals(instanceIdToIp.size(), 2);
      Assert.assertEquals(instanceIdToIp.get("i-123"), "10.0.1.1");
      Assert.assertEquals(instanceIdToIp.get("i-456"), "10.0.1.2");
    }
  }

  @Test
  public void testGetInstanceIPs_WithNullInstanceIds_ReturnsEmptyMap() {
    Single<Map<String, String>> result = vmClientAwsImpl.getInstanceIPs(null);

    Map<String, String> instanceIdToIp = result.blockingGet();
    Assert.assertNotNull(instanceIdToIp);
    Assert.assertTrue(instanceIdToIp.isEmpty());
  }

  @Test
  public void testGetInstanceIPs_WithEmptyInstanceIds_ReturnsEmptyMap() {
    Single<Map<String, String>> result = vmClientAwsImpl.getInstanceIPs(Collections.emptyList());

    Map<String, String> instanceIdToIp = result.blockingGet();
    Assert.assertNotNull(instanceIdToIp);
    Assert.assertTrue(instanceIdToIp.isEmpty());
  }

  @Test
  public void testGetInstanceIPs_WithNoMatchingInstances_ReturnsEmptyMap() {
    List<String> instanceIds = Arrays.asList("i-123", "i-456");

    DescribeInstancesResponse describeResponse =
        DescribeInstancesResponse.builder().reservations(Collections.emptyList()).build();

    CompletableFuture<DescribeInstancesResponse> describeFuture =
        CompletableFuture.completedFuture(describeResponse);

    when(mockEc2Client.describeInstances(any(DescribeInstancesRequest.class)))
        .thenReturn(describeFuture);

    try (MockedStatic<CompletableFutureUtils> mockedUtils =
        Mockito.mockStatic(CompletableFutureUtils.class)) {
      mockedUtils
          .when(() -> CompletableFutureUtils.toSingle(any(CompletableFuture.class)))
          .thenAnswer(invocation -> Single.fromFuture(invocation.getArgument(0)));

      Single<Map<String, String>> result = vmClientAwsImpl.getInstanceIPs(instanceIds);

      Map<String, String> instanceIdToIp = result.blockingGet();
      Assert.assertNotNull(instanceIdToIp);
      Assert.assertTrue(instanceIdToIp.isEmpty());
    }
  }
}
