package com.logwise.orchestrator.tests.unit.client;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.logwise.orchestrator.client.impl.AsgClientAwsImpl;
import com.logwise.orchestrator.common.util.CompletableFutureUtils;
import com.logwise.orchestrator.config.ApplicationConfig;
import com.logwise.orchestrator.helper.HelperTestUtils;
import com.logwise.orchestrator.setup.BaseTest;
import io.reactivex.Completable;
import io.reactivex.Single;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import software.amazon.awssdk.services.autoscaling.AutoScalingAsyncClient;
import software.amazon.awssdk.services.autoscaling.model.*;

/** Unit tests for AsgClientAwsImpl. */
public class AsgClientAwsImplTest extends BaseTest {

  private AsgClientAwsImpl asgClientAwsImpl;
  private AutoScalingAsyncClient mockAsgClient;
  private ApplicationConfig.AsgConfig mockAsgConfig;
  private ApplicationConfig.AsgAwsConfig mockAsgAwsConfig;

  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    asgClientAwsImpl = new AsgClientAwsImpl();
    mockAsgClient = mock(AutoScalingAsyncClient.class);
    mockAsgConfig = mock(ApplicationConfig.AsgConfig.class);
    mockAsgAwsConfig = mock(ApplicationConfig.AsgAwsConfig.class);

    when(mockAsgConfig.getAws()).thenReturn(mockAsgAwsConfig);
    when(mockAsgAwsConfig.getRegion()).thenReturn("us-east-1");

    // Inject mock client using reflection
    HelperTestUtils.setPrivateField(asgClientAwsImpl, "asgClient", mockAsgClient);
    HelperTestUtils.setPrivateField(asgClientAwsImpl, "asgAwsConfig", mockAsgAwsConfig);
  }

  @AfterClass
  public static void tearDownClass() {
    BaseTest.cleanup();
  }

  @Test
  public void testRxConnect_WithValidConfig_CompletesSuccessfully() {
    Completable result = asgClientAwsImpl.rxConnect(mockAsgConfig);

    Assert.assertNotNull(result);
    result.blockingAwait();
  }

  @Test
  public void testRxConnect_WithNullAwsConfig_CompletesSuccessfully() {
    ApplicationConfig.AsgConfig configWithNullAws = mock(ApplicationConfig.AsgConfig.class);
    when(configWithNullAws.getAws()).thenReturn(null);

    Completable result = asgClientAwsImpl.rxConnect(configWithNullAws);

    Assert.assertNotNull(result);
    result.blockingAwait();
  }

  @Test
  public void testUpdateDesiredCapacity_WithValidCapacity_UpdatesSuccessfully() {
    String asgName = "test-asg";
    int desiredCapacity = 5;

    AutoScalingGroup asg =
        AutoScalingGroup.builder()
            .autoScalingGroupName(asgName)
            .minSize(1)
            .maxSize(10)
            .desiredCapacity(3)
            .instances(Collections.emptyList())
            .build();

    DescribeAutoScalingGroupsResponse describeResponse =
        DescribeAutoScalingGroupsResponse.builder().autoScalingGroups(Arrays.asList(asg)).build();

    SetDesiredCapacityResponse setCapacityResponse = SetDesiredCapacityResponse.builder().build();

    CompletableFuture<DescribeAutoScalingGroupsResponse> describeFuture =
        CompletableFuture.completedFuture(describeResponse);
    CompletableFuture<SetDesiredCapacityResponse> setCapacityFuture =
        CompletableFuture.completedFuture(setCapacityResponse);

    when(mockAsgClient.describeAutoScalingGroups(any(DescribeAutoScalingGroupsRequest.class)))
        .thenReturn(describeFuture);
    when(mockAsgClient.setDesiredCapacity(any(SetDesiredCapacityRequest.class)))
        .thenReturn(setCapacityFuture);

    try (MockedStatic<CompletableFutureUtils> mockedUtils =
        Mockito.mockStatic(CompletableFutureUtils.class)) {
      mockedUtils
          .when(() -> CompletableFutureUtils.toSingle(any(CompletableFuture.class)))
          .thenAnswer(invocation -> Single.fromFuture(invocation.getArgument(0)));

      Completable result = asgClientAwsImpl.updateDesiredCapacity(asgName, desiredCapacity);

      result.blockingAwait();
      verify(mockAsgClient, times(1)).setDesiredCapacity(any(SetDesiredCapacityRequest.class));
    }
  }

  @Test
  public void testUpdateDesiredCapacity_WithCapacityAboveMax_ClampsToMax() {
    String asgName = "test-asg";
    int desiredCapacity = 15; // Above max of 10

    AutoScalingGroup asg =
        AutoScalingGroup.builder()
            .autoScalingGroupName(asgName)
            .minSize(1)
            .maxSize(10)
            .desiredCapacity(3)
            .instances(Collections.emptyList())
            .build();

    DescribeAutoScalingGroupsResponse describeResponse =
        DescribeAutoScalingGroupsResponse.builder().autoScalingGroups(Arrays.asList(asg)).build();

    SetDesiredCapacityResponse setCapacityResponse = SetDesiredCapacityResponse.builder().build();

    CompletableFuture<DescribeAutoScalingGroupsResponse> describeFuture =
        CompletableFuture.completedFuture(describeResponse);
    CompletableFuture<SetDesiredCapacityResponse> setCapacityFuture =
        CompletableFuture.completedFuture(setCapacityResponse);

    when(mockAsgClient.describeAutoScalingGroups(any(DescribeAutoScalingGroupsRequest.class)))
        .thenReturn(describeFuture);
    when(mockAsgClient.setDesiredCapacity(any(SetDesiredCapacityRequest.class)))
        .thenReturn(setCapacityFuture);

    try (MockedStatic<CompletableFutureUtils> mockedUtils =
        Mockito.mockStatic(CompletableFutureUtils.class)) {
      mockedUtils
          .when(() -> CompletableFutureUtils.toSingle(any(CompletableFuture.class)))
          .thenAnswer(invocation -> Single.fromFuture(invocation.getArgument(0)));

      Completable result = asgClientAwsImpl.updateDesiredCapacity(asgName, desiredCapacity);

      result.blockingAwait();
      verify(mockAsgClient, times(1))
          .setDesiredCapacity(
              argThat(
                  (SetDesiredCapacityRequest req) ->
                      req.autoScalingGroupName().equals(asgName)
                          && req.desiredCapacity() == 10)); // Clamped to max
    }
  }

  @Test
  public void testUpdateDesiredCapacity_WithCapacityBelowMin_ClampsToMin() {
    String asgName = "test-asg";
    int desiredCapacity = 0; // Below min of 1

    AutoScalingGroup asg =
        AutoScalingGroup.builder()
            .autoScalingGroupName(asgName)
            .minSize(1)
            .maxSize(10)
            .desiredCapacity(3)
            .instances(Collections.emptyList())
            .build();

    DescribeAutoScalingGroupsResponse describeResponse =
        DescribeAutoScalingGroupsResponse.builder().autoScalingGroups(Arrays.asList(asg)).build();

    SetDesiredCapacityResponse setCapacityResponse = SetDesiredCapacityResponse.builder().build();

    CompletableFuture<DescribeAutoScalingGroupsResponse> describeFuture =
        CompletableFuture.completedFuture(describeResponse);
    CompletableFuture<SetDesiredCapacityResponse> setCapacityFuture =
        CompletableFuture.completedFuture(setCapacityResponse);

    when(mockAsgClient.describeAutoScalingGroups(any(DescribeAutoScalingGroupsRequest.class)))
        .thenReturn(describeFuture);
    when(mockAsgClient.setDesiredCapacity(any(SetDesiredCapacityRequest.class)))
        .thenReturn(setCapacityFuture);

    try (MockedStatic<CompletableFutureUtils> mockedUtils =
        Mockito.mockStatic(CompletableFutureUtils.class)) {
      mockedUtils
          .when(() -> CompletableFutureUtils.toSingle(any(CompletableFuture.class)))
          .thenAnswer(invocation -> Single.fromFuture(invocation.getArgument(0)));

      Completable result = asgClientAwsImpl.updateDesiredCapacity(asgName, desiredCapacity);

      result.blockingAwait();
      verify(mockAsgClient, times(1))
          .setDesiredCapacity(
              argThat(
                  (SetDesiredCapacityRequest req) ->
                      req.autoScalingGroupName().equals(asgName)
                          && req.desiredCapacity() == 1)); // Clamped to min
    }
  }

  @Test
  public void testUpdateDesiredCapacity_WithNoAsgFound_ReturnsError() {
    String asgName = "non-existent-asg";
    int desiredCapacity = 5;

    DescribeAutoScalingGroupsResponse describeResponse =
        DescribeAutoScalingGroupsResponse.builder()
            .autoScalingGroups(Collections.emptyList())
            .build();

    CompletableFuture<DescribeAutoScalingGroupsResponse> describeFuture =
        CompletableFuture.completedFuture(describeResponse);

    when(mockAsgClient.describeAutoScalingGroups(any(DescribeAutoScalingGroupsRequest.class)))
        .thenReturn(describeFuture);

    try (MockedStatic<CompletableFutureUtils> mockedUtils =
        Mockito.mockStatic(CompletableFutureUtils.class)) {
      mockedUtils
          .when(() -> CompletableFutureUtils.toSingle(any(CompletableFuture.class)))
          .thenAnswer(invocation -> Single.fromFuture(invocation.getArgument(0)));

      Completable result = asgClientAwsImpl.updateDesiredCapacity(asgName, desiredCapacity);

      try {
        result.blockingAwait();
        Assert.fail("Should have thrown exception");
      } catch (Exception e) {
        Assert.assertTrue(e.getMessage().contains("No ASG found"));
      }
    }
  }

  @Test
  public void testRemoveInstances_WithValidInstances_RemovesSuccessfully() {
    String asgName = "test-asg";
    List<String> instanceIds = Arrays.asList("i-123", "i-456");
    boolean decrementCount = false;

    Instance instance1 =
        Instance.builder().instanceId("i-123").lifecycleState(LifecycleState.IN_SERVICE).build();
    Instance instance2 =
        Instance.builder().instanceId("i-456").lifecycleState(LifecycleState.IN_SERVICE).build();

    AutoScalingGroup asg =
        AutoScalingGroup.builder()
            .autoScalingGroupName(asgName)
            .instances(Arrays.asList(instance1, instance2))
            .build();

    DescribeAutoScalingGroupsResponse describeResponse =
        DescribeAutoScalingGroupsResponse.builder().autoScalingGroups(Arrays.asList(asg)).build();

    DetachInstancesResponse detachResponse = DetachInstancesResponse.builder().build();

    CompletableFuture<DescribeAutoScalingGroupsResponse> describeFuture =
        CompletableFuture.completedFuture(describeResponse);
    CompletableFuture<DetachInstancesResponse> detachFuture =
        CompletableFuture.completedFuture(detachResponse);

    when(mockAsgClient.describeAutoScalingGroups(any(DescribeAutoScalingGroupsRequest.class)))
        .thenReturn(describeFuture);
    when(mockAsgClient.detachInstances(any(DetachInstancesRequest.class))).thenReturn(detachFuture);

    try (MockedStatic<CompletableFutureUtils> mockedUtils =
        Mockito.mockStatic(CompletableFutureUtils.class)) {
      mockedUtils
          .when(() -> CompletableFutureUtils.toSingle(any(CompletableFuture.class)))
          .thenAnswer(invocation -> Single.fromFuture(invocation.getArgument(0)));

      Completable result = asgClientAwsImpl.removeInstances(asgName, instanceIds, decrementCount);

      result.blockingAwait();
      verify(mockAsgClient, times(1)).detachInstances(any(DetachInstancesRequest.class));
    }
  }

  @Test
  public void testGetAllInServiceVmIdInAsg_WithInServiceInstances_ReturnsIds() {
    String asgName = "test-asg";

    Instance instance1 =
        Instance.builder().instanceId("i-123").lifecycleState(LifecycleState.IN_SERVICE).build();
    Instance instance2 =
        Instance.builder().instanceId("i-456").lifecycleState(LifecycleState.IN_SERVICE).build();
    Instance instance3 =
        Instance.builder().instanceId("i-789").lifecycleState(LifecycleState.TERMINATING).build();

    AutoScalingGroup asg =
        AutoScalingGroup.builder()
            .autoScalingGroupName(asgName)
            .instances(Arrays.asList(instance1, instance2, instance3))
            .build();

    DescribeAutoScalingGroupsResponse describeResponse =
        DescribeAutoScalingGroupsResponse.builder().autoScalingGroups(Arrays.asList(asg)).build();

    CompletableFuture<DescribeAutoScalingGroupsResponse> describeFuture =
        CompletableFuture.completedFuture(describeResponse);

    when(mockAsgClient.describeAutoScalingGroups(any(DescribeAutoScalingGroupsRequest.class)))
        .thenReturn(describeFuture);

    try (MockedStatic<CompletableFutureUtils> mockedUtils =
        Mockito.mockStatic(CompletableFutureUtils.class)) {
      mockedUtils
          .when(() -> CompletableFutureUtils.toSingle(any(CompletableFuture.class)))
          .thenAnswer(invocation -> Single.fromFuture(invocation.getArgument(0)));

      Single<List<String>> result = asgClientAwsImpl.getAllInServiceVmIdInAsg(asgName);

      List<String> instanceIds = result.blockingGet();
      Assert.assertNotNull(instanceIds);
      Assert.assertEquals(instanceIds.size(), 2);
      Assert.assertTrue(instanceIds.contains("i-123"));
      Assert.assertTrue(instanceIds.contains("i-456"));
      Assert.assertFalse(instanceIds.contains("i-789"));
    }
  }

  @Test
  public void testGetAllInServiceVmIdInAsg_WithNoInstances_ReturnsEmptyList() {
    String asgName = "test-asg";

    AutoScalingGroup asg =
        AutoScalingGroup.builder()
            .autoScalingGroupName(asgName)
            .instances(Collections.emptyList())
            .build();

    DescribeAutoScalingGroupsResponse describeResponse =
        DescribeAutoScalingGroupsResponse.builder().autoScalingGroups(Arrays.asList(asg)).build();

    CompletableFuture<DescribeAutoScalingGroupsResponse> describeFuture =
        CompletableFuture.completedFuture(describeResponse);

    when(mockAsgClient.describeAutoScalingGroups(any(DescribeAutoScalingGroupsRequest.class)))
        .thenReturn(describeFuture);

    try (MockedStatic<CompletableFutureUtils> mockedUtils =
        Mockito.mockStatic(CompletableFutureUtils.class)) {
      mockedUtils
          .when(() -> CompletableFutureUtils.toSingle(any(CompletableFuture.class)))
          .thenAnswer(invocation -> Single.fromFuture(invocation.getArgument(0)));

      Single<List<String>> result = asgClientAwsImpl.getAllInServiceVmIdInAsg(asgName);

      List<String> instanceIds = result.blockingGet();
      Assert.assertNotNull(instanceIds);
      Assert.assertTrue(instanceIds.isEmpty());
    }
  }
}
