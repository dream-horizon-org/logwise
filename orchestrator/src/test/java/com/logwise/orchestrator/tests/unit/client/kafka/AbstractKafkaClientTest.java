package com.logwise.orchestrator.tests.unit.client.kafka;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import com.logwise.orchestrator.client.kafka.AbstractKafkaClient;
import com.logwise.orchestrator.client.kafka.Ec2KafkaClient;
import com.logwise.orchestrator.config.ApplicationConfig;
import com.logwise.orchestrator.dto.kafka.TopicOffsetInfo;
import com.logwise.orchestrator.enums.KafkaType;
import com.logwise.orchestrator.setup.BaseTest;
import com.logwise.orchestrator.util.ApplicationUtils;
import io.reactivex.Single;
import java.util.*;
import java.util.concurrent.ExecutionException;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Unit tests for AbstractKafkaClient.getEndOffsetSum method. */
@SuppressWarnings({"unchecked", "deprecation"})
public class AbstractKafkaClientTest extends BaseTest {

  private ApplicationConfig.KafkaConfig kafkaConfig;
  private Ec2KafkaClient ec2KafkaClient;
  private AdminClient mockAdminClient;

  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    kafkaConfig = new ApplicationConfig.KafkaConfig();
    kafkaConfig.setKafkaBrokersHost("kafka.example.com");
    kafkaConfig.setKafkaBrokerPort(9092);
    kafkaConfig.setKafkaType(KafkaType.EC2);
    ec2KafkaClient = new Ec2KafkaClient(kafkaConfig);
    mockAdminClient = mock(AdminClient.class);
  }

  @Test
  public void testGetEndOffsetSum_WithSingleTopicAndMultiplePartitions_ReturnsCorrectSum()
      throws Exception {
    String topic = "test-topic";
    List<String> topics = Arrays.asList(topic);

    // Mock topic description with 3 partitions
    TopicDescription topicDescription = createMockTopicDescription(topic, 3);
    Map<String, TopicDescription> topicDescriptions = new HashMap<>();
    topicDescriptions.put(topic, topicDescription);

    // Mock DescribeTopicsResult to return a KafkaFuture
    DescribeTopicsResult mockDescribeResult = mock(DescribeTopicsResult.class);
    KafkaFuture<Map<String, TopicDescription>> kafkaFuture =
        createMockKafkaFuture(topicDescriptions);
    when(mockDescribeResult.all()).thenReturn(kafkaFuture);
    when(mockAdminClient.describeTopics(topics)).thenReturn(mockDescribeResult);

    // Mock getEndOffsets to return offsets: partition 0=100, partition 1=200, partition 2=300
    Map<TopicPartition, Long> endOffsets = new HashMap<>();
    endOffsets.put(new TopicPartition(topic, 0), 100L);
    endOffsets.put(new TopicPartition(topic, 1), 200L);
    endOffsets.put(new TopicPartition(topic, 2), 300L);

    try (MockedStatic<ApplicationUtils> mockedUtils = Mockito.mockStatic(ApplicationUtils.class)) {
      List<String> mockIPs = Arrays.asList("192.168.1.1");
      mockedUtils
          .when(() -> ApplicationUtils.getIpAddresses(anyString()))
          .thenReturn(Single.just(mockIPs));

      // Mock getEndOffsets method using spy
      AbstractKafkaClient spyClient = spy(ec2KafkaClient);
      // Mock createAdminClient to return our mocked adminClient
      when(spyClient.createAdminClient()).thenReturn(Single.just(mockAdminClient));
      when(spyClient.getEndOffsets(anyList())).thenReturn(Single.just(endOffsets));

      // Execute
      Map<String, TopicOffsetInfo> result = spyClient.getEndOffsetSum(topics).blockingGet();

      // Verify
      assertNotNull(result);
      assertEquals(result.size(), 1);
      TopicOffsetInfo offsetInfo = result.get(topic);
      assertNotNull(offsetInfo);
      assertEquals(offsetInfo.getSumOfEndOffsets(), 600L); // 100 + 200 + 300
      assertEquals(offsetInfo.getCurrentNumberOfPartitions(), 3);

      // Verify that adminClient.describeTopics was called
      verify(mockAdminClient, times(1)).describeTopics(topics);
    }
  }

  @Test
  public void testGetEndOffsetSum_WithMultipleTopics_ReturnsCorrectSums() throws Exception {
    List<String> topics = Arrays.asList("topic1", "topic2");

    // Mock topic descriptions
    TopicDescription topic1Desc = createMockTopicDescription("topic1", 2);
    TopicDescription topic2Desc = createMockTopicDescription("topic2", 3);
    Map<String, TopicDescription> topicDescriptions = new HashMap<>();
    topicDescriptions.put("topic1", topic1Desc);
    topicDescriptions.put("topic2", topic2Desc);

    // Mock DescribeTopicsResult to return a KafkaFuture
    DescribeTopicsResult mockDescribeResult = mock(DescribeTopicsResult.class);
    KafkaFuture<Map<String, TopicDescription>> kafkaFuture =
        createMockKafkaFuture(topicDescriptions);
    when(mockDescribeResult.all()).thenReturn(kafkaFuture);
    when(mockAdminClient.describeTopics(topics)).thenReturn(mockDescribeResult);

    // Mock getEndOffsets - will be called separately for each topic
    Map<TopicPartition, Long> topic1Offsets = new HashMap<>();
    topic1Offsets.put(new TopicPartition("topic1", 0), 50L);
    topic1Offsets.put(new TopicPartition("topic1", 1), 150L);

    Map<TopicPartition, Long> topic2Offsets = new HashMap<>();
    topic2Offsets.put(new TopicPartition("topic2", 0), 100L);
    topic2Offsets.put(new TopicPartition("topic2", 1), 200L);
    topic2Offsets.put(new TopicPartition("topic2", 2), 300L);

    try (MockedStatic<ApplicationUtils> mockedUtils = Mockito.mockStatic(ApplicationUtils.class)) {
      List<String> mockIPs = Arrays.asList("192.168.1.1");
      mockedUtils
          .when(() -> ApplicationUtils.getIpAddresses(anyString()))
          .thenReturn(Single.just(mockIPs));

      AbstractKafkaClient spyClient = spy(ec2KafkaClient);
      // Mock createAdminClient to return our mocked adminClient
      when(spyClient.createAdminClient()).thenReturn(Single.just(mockAdminClient));
      // Mock getEndOffsets to return different results based on topic
      when(spyClient.getEndOffsets(
              argThat(
                  list -> {
                    if (list == null || list.isEmpty()) return false;
                    TopicPartition tp = (TopicPartition) list.get(0);
                    return tp != null && "topic1".equals(tp.topic());
                  })))
          .thenReturn(Single.just(topic1Offsets));
      when(spyClient.getEndOffsets(
              argThat(
                  list -> {
                    if (list == null || list.isEmpty()) return false;
                    TopicPartition tp = (TopicPartition) list.get(0);
                    return tp != null && "topic2".equals(tp.topic());
                  })))
          .thenReturn(Single.just(topic2Offsets));

      // Execute
      Map<String, TopicOffsetInfo> result = spyClient.getEndOffsetSum(topics).blockingGet();

      // Verify
      assertNotNull(result);
      assertEquals(result.size(), 2);

      TopicOffsetInfo topic1Info = result.get("topic1");
      assertNotNull(topic1Info);
      assertEquals(topic1Info.getSumOfEndOffsets(), 200L); // 50 + 150
      assertEquals(topic1Info.getCurrentNumberOfPartitions(), 2);

      TopicOffsetInfo topic2Info = result.get("topic2");
      assertNotNull(topic2Info);
      assertEquals(topic2Info.getSumOfEndOffsets(), 600L); // 100 + 200 + 300
      assertEquals(topic2Info.getCurrentNumberOfPartitions(), 3);

      // Verify that adminClient.describeTopics was called
      verify(mockAdminClient, times(1)).describeTopics(topics);
    }
  }

  @Test
  public void testGetEndOffsetSum_WithNonExistentTopic_ReturnsDefaultValues() throws Exception {
    String topic = "non-existent-topic";
    List<String> topics = Arrays.asList(topic);

    // Mock topic descriptions - topic doesn't exist
    Map<String, TopicDescription> topicDescriptions = new HashMap<>();

    // Mock DescribeTopicsResult to return a KafkaFuture
    DescribeTopicsResult mockDescribeResult = mock(DescribeTopicsResult.class);
    KafkaFuture<Map<String, TopicDescription>> kafkaFuture =
        createMockKafkaFuture(topicDescriptions);
    when(mockDescribeResult.all()).thenReturn(kafkaFuture);
    when(mockAdminClient.describeTopics(topics)).thenReturn(mockDescribeResult);

    try (MockedStatic<ApplicationUtils> mockedUtils = Mockito.mockStatic(ApplicationUtils.class)) {
      List<String> mockIPs = Arrays.asList("192.168.1.1");
      mockedUtils
          .when(() -> ApplicationUtils.getIpAddresses(anyString()))
          .thenReturn(Single.just(mockIPs));

      AbstractKafkaClient spyClient = spy(ec2KafkaClient);
      // Mock createAdminClient to return our mocked adminClient
      when(spyClient.createAdminClient()).thenReturn(Single.just(mockAdminClient));

      // Execute
      Map<String, TopicOffsetInfo> result = spyClient.getEndOffsetSum(topics).blockingGet();

      // Verify
      assertNotNull(result);
      assertEquals(result.size(), 1);
      TopicOffsetInfo offsetInfo = result.get(topic);
      assertNotNull(offsetInfo);
      assertEquals(offsetInfo.getSumOfEndOffsets(), 0L);
      assertEquals(offsetInfo.getCurrentNumberOfPartitions(), 0);

      // Verify getEndOffsets was never called for non-existent topic
      verify(spyClient, never()).getEndOffsets(anyList());
      // Verify that adminClient.describeTopics was called
      verify(mockAdminClient, times(1)).describeTopics(topics);
    }
  }

  @Test
  public void testGetEndOffsetSum_WithEmptyTopicsList_ReturnsEmptyMap() throws Exception {
    List<String> topics = Collections.emptyList();

    // Mock DescribeTopicsResult to return a KafkaFuture
    DescribeTopicsResult mockDescribeResult = mock(DescribeTopicsResult.class);
    KafkaFuture<Map<String, TopicDescription>> kafkaFuture =
        createMockKafkaFuture(Collections.emptyMap());
    when(mockDescribeResult.all()).thenReturn(kafkaFuture);
    when(mockAdminClient.describeTopics(topics)).thenReturn(mockDescribeResult);

    try (MockedStatic<ApplicationUtils> mockedUtils = Mockito.mockStatic(ApplicationUtils.class)) {
      List<String> mockIPs = Arrays.asList("192.168.1.1");
      mockedUtils
          .when(() -> ApplicationUtils.getIpAddresses(anyString()))
          .thenReturn(Single.just(mockIPs));

      AbstractKafkaClient spyClient = spy(ec2KafkaClient);
      // Mock createAdminClient to return our mocked adminClient
      when(spyClient.createAdminClient()).thenReturn(Single.just(mockAdminClient));

      // Execute
      Map<String, TopicOffsetInfo> result = spyClient.getEndOffsetSum(topics).blockingGet();

      // Verify
      assertNotNull(result);
      assertTrue(result.isEmpty());

      // Verify that adminClient.describeTopics was called even with empty list
      verify(mockAdminClient, times(1)).describeTopics(topics);
    }
  }

  @Test
  public void testGetEndOffsetSum_WithNullOffsets_IgnoresNullValues() throws Exception {
    String topic = "test-topic";
    List<String> topics = Arrays.asList(topic);

    // Mock topic description with 3 partitions
    TopicDescription topicDescription = createMockTopicDescription(topic, 3);
    Map<String, TopicDescription> topicDescriptions = new HashMap<>();
    topicDescriptions.put(topic, topicDescription);

    // Mock DescribeTopicsResult to return a KafkaFuture
    DescribeTopicsResult mockDescribeResult = mock(DescribeTopicsResult.class);
    KafkaFuture<Map<String, TopicDescription>> kafkaFuture =
        createMockKafkaFuture(topicDescriptions);
    when(mockDescribeResult.all()).thenReturn(kafkaFuture);
    when(mockAdminClient.describeTopics(topics)).thenReturn(mockDescribeResult);

    // Mock getEndOffsets with one null offset
    Map<TopicPartition, Long> endOffsets = new HashMap<>();
    endOffsets.put(new TopicPartition(topic, 0), 100L);
    endOffsets.put(new TopicPartition(topic, 1), null); // null offset
    endOffsets.put(new TopicPartition(topic, 2), 300L);

    try (MockedStatic<ApplicationUtils> mockedUtils = Mockito.mockStatic(ApplicationUtils.class)) {
      List<String> mockIPs = Arrays.asList("192.168.1.1");
      mockedUtils
          .when(() -> ApplicationUtils.getIpAddresses(anyString()))
          .thenReturn(Single.just(mockIPs));

      AbstractKafkaClient spyClient = spy(ec2KafkaClient);
      // Mock createAdminClient to return our mocked adminClient
      when(spyClient.createAdminClient()).thenReturn(Single.just(mockAdminClient));
      when(spyClient.getEndOffsets(anyList())).thenReturn(Single.just(endOffsets));

      // Execute
      Map<String, TopicOffsetInfo> result = spyClient.getEndOffsetSum(topics).blockingGet();

      // Verify - null offset should be ignored
      assertNotNull(result);
      assertEquals(result.size(), 1);
      TopicOffsetInfo offsetInfo = result.get(topic);
      assertNotNull(offsetInfo);
      assertEquals(offsetInfo.getSumOfEndOffsets(), 400L); // 100 + 0 (null ignored) + 300
      assertEquals(offsetInfo.getCurrentNumberOfPartitions(), 3);

      // Verify that null offset was actually skipped (only 2 offsets should be summed)
      // The getEndOffsets map has 3 entries but one is null, so sum should be 400 (100 + 300)
      // Note: getEndOffsets is called internally by the implementation, verification may count
      // both the mock setup and actual call, so we verify the result instead
      // Verify that adminClient.describeTopics was called
      verify(mockAdminClient, times(1)).describeTopics(topics);
    }
  }

  @Test
  public void testGetEndOffsetSum_WithInterruptedException_ReturnsError() throws Exception {
    String topic = "test-topic";
    List<String> topics = Arrays.asList(topic);

    // Mock DescribeTopicsResult to throw InterruptedException
    DescribeTopicsResult mockDescribeResult = mock(DescribeTopicsResult.class);
    KafkaFuture<Map<String, TopicDescription>> kafkaFuture =
        createMockKafkaFutureWithException(new InterruptedException("Interrupted"));
    when(mockDescribeResult.all()).thenReturn(kafkaFuture);
    when(mockAdminClient.describeTopics(topics)).thenReturn(mockDescribeResult);

    try (MockedStatic<ApplicationUtils> mockedUtils = Mockito.mockStatic(ApplicationUtils.class)) {
      List<String> mockIPs = Arrays.asList("192.168.1.1");
      mockedUtils
          .when(() -> ApplicationUtils.getIpAddresses(anyString()))
          .thenReturn(Single.just(mockIPs));

      AbstractKafkaClient spyClient = spy(ec2KafkaClient);
      // Mock createAdminClient to return our mocked adminClient
      when(spyClient.createAdminClient()).thenReturn(Single.just(mockAdminClient));

      // Execute and verify error
      try {
        spyClient.getEndOffsetSum(topics).blockingGet();
        fail("Expected exception to be thrown");
      } catch (RuntimeException e) {
        assertNotNull(e);
        // RxJava wraps exceptions, so check cause
        Throwable cause = e.getCause();
        assertTrue(
            cause instanceof InterruptedException,
            "Exception cause should be InterruptedException, but got: "
                + (cause != null ? cause.getClass().getName() : "null"));
        assertEquals(cause.getMessage(), "Interrupted");
      }

      // Verify that adminClient.describeTopics was called
      verify(mockAdminClient, times(1)).describeTopics(topics);
    }
  }

  @Test
  public void testGetEndOffsetSum_WithExecutionException_ReturnsError() throws Exception {
    String topic = "test-topic";
    List<String> topics = Arrays.asList(topic);

    // Mock DescribeTopicsResult to throw ExecutionException
    DescribeTopicsResult mockDescribeResult = mock(DescribeTopicsResult.class);
    KafkaFuture<Map<String, TopicDescription>> kafkaFuture =
        createMockKafkaFutureWithException(new ExecutionException("Execution failed", null));
    when(mockDescribeResult.all()).thenReturn(kafkaFuture);
    when(mockAdminClient.describeTopics(topics)).thenReturn(mockDescribeResult);

    try (MockedStatic<ApplicationUtils> mockedUtils = Mockito.mockStatic(ApplicationUtils.class)) {
      List<String> mockIPs = Arrays.asList("192.168.1.1");
      mockedUtils
          .when(() -> ApplicationUtils.getIpAddresses(anyString()))
          .thenReturn(Single.just(mockIPs));

      AbstractKafkaClient spyClient = spy(ec2KafkaClient);
      // Mock createAdminClient to return our mocked adminClient
      when(spyClient.createAdminClient()).thenReturn(Single.just(mockAdminClient));

      // Execute and verify error
      try {
        spyClient.getEndOffsetSum(topics).blockingGet();
        fail("Expected exception to be thrown");
      } catch (RuntimeException e) {
        assertNotNull(e);
        // RxJava wraps exceptions, so check cause
        Throwable cause = e.getCause();
        assertTrue(
            cause instanceof ExecutionException,
            "Exception cause should be ExecutionException, but got: "
                + (cause != null ? cause.getClass().getName() : "null"));
        assertTrue(
            cause.getMessage() != null && cause.getMessage().contains("Execution failed"),
            "Exception message should contain 'Execution failed'");
      }

      // Verify that adminClient.describeTopics was called
      verify(mockAdminClient, times(1)).describeTopics(topics);
    }
  }

  @Test
  public void testGetEndOffsetSum_WithZeroPartitions_ReturnsZeroSum() throws Exception {
    String topic = "test-topic";
    List<String> topics = Arrays.asList(topic);

    // Mock topic description with 0 partitions
    TopicDescription topicDescription = createMockTopicDescription(topic, 0);
    Map<String, TopicDescription> topicDescriptions = new HashMap<>();
    topicDescriptions.put(topic, topicDescription);

    // Mock DescribeTopicsResult to return a KafkaFuture
    DescribeTopicsResult mockDescribeResult = mock(DescribeTopicsResult.class);
    KafkaFuture<Map<String, TopicDescription>> kafkaFuture =
        createMockKafkaFuture(topicDescriptions);
    when(mockDescribeResult.all()).thenReturn(kafkaFuture);
    when(mockAdminClient.describeTopics(topics)).thenReturn(mockDescribeResult);

    // Mock getEndOffsets to return empty map
    Map<TopicPartition, Long> endOffsets = new HashMap<>();

    try (MockedStatic<ApplicationUtils> mockedUtils = Mockito.mockStatic(ApplicationUtils.class)) {
      List<String> mockIPs = Arrays.asList("192.168.1.1");
      mockedUtils
          .when(() -> ApplicationUtils.getIpAddresses(anyString()))
          .thenReturn(Single.just(mockIPs));

      AbstractKafkaClient spyClient = spy(ec2KafkaClient);
      // Mock createAdminClient to return our mocked adminClient
      when(spyClient.createAdminClient()).thenReturn(Single.just(mockAdminClient));
      when(spyClient.getEndOffsets(anyList())).thenReturn(Single.just(endOffsets));

      // Execute
      Map<String, TopicOffsetInfo> result = spyClient.getEndOffsetSum(topics).blockingGet();

      // Verify
      assertNotNull(result);
      assertEquals(result.size(), 1);
      TopicOffsetInfo offsetInfo = result.get(topic);
      assertNotNull(offsetInfo);
      assertEquals(offsetInfo.getSumOfEndOffsets(), 0L);
      assertEquals(offsetInfo.getCurrentNumberOfPartitions(), 0);

      // Verify that adminClient.describeTopics was called
      verify(mockAdminClient, times(1)).describeTopics(topics);
    }
  }

  @Test
  public void testGetEndOffsetSum_WithMixedExistingAndNonExistentTopics_ReturnsCorrectResults()
      throws Exception {
    List<String> topics = Arrays.asList("existing-topic", "non-existent-topic");

    // Mock topic descriptions - only one topic exists
    TopicDescription existingTopicDesc = createMockTopicDescription("existing-topic", 2);
    Map<String, TopicDescription> topicDescriptions = new HashMap<>();
    topicDescriptions.put("existing-topic", existingTopicDesc);
    // non-existent-topic is not in the map

    // Mock DescribeTopicsResult to return a KafkaFuture
    DescribeTopicsResult mockDescribeResult = mock(DescribeTopicsResult.class);
    KafkaFuture<Map<String, TopicDescription>> kafkaFuture =
        createMockKafkaFuture(topicDescriptions);
    when(mockDescribeResult.all()).thenReturn(kafkaFuture);
    when(mockAdminClient.describeTopics(topics)).thenReturn(mockDescribeResult);

    // Mock getEndOffsets for existing topic
    Map<TopicPartition, Long> existingTopicOffsets = new HashMap<>();
    existingTopicOffsets.put(new TopicPartition("existing-topic", 0), 100L);
    existingTopicOffsets.put(new TopicPartition("existing-topic", 1), 200L);

    try (MockedStatic<ApplicationUtils> mockedUtils = Mockito.mockStatic(ApplicationUtils.class)) {
      List<String> mockIPs = Arrays.asList("192.168.1.1");
      mockedUtils
          .when(() -> ApplicationUtils.getIpAddresses(anyString()))
          .thenReturn(Single.just(mockIPs));

      AbstractKafkaClient spyClient = spy(ec2KafkaClient);
      // Mock createAdminClient to return our mocked adminClient
      when(spyClient.createAdminClient()).thenReturn(Single.just(mockAdminClient));
      when(spyClient.getEndOffsets(anyList())).thenReturn(Single.just(existingTopicOffsets));

      // Execute
      Map<String, TopicOffsetInfo> result = spyClient.getEndOffsetSum(topics).blockingGet();

      // Verify
      assertNotNull(result);
      assertEquals(result.size(), 2);

      // Existing topic should have correct values
      TopicOffsetInfo existingInfo = result.get("existing-topic");
      assertNotNull(existingInfo);
      assertEquals(existingInfo.getSumOfEndOffsets(), 300L);
      assertEquals(existingInfo.getCurrentNumberOfPartitions(), 2);

      // Non-existent topic should have default values
      TopicOffsetInfo nonExistentInfo = result.get("non-existent-topic");
      assertNotNull(nonExistentInfo);
      assertEquals(nonExistentInfo.getSumOfEndOffsets(), 0L);
      assertEquals(nonExistentInfo.getCurrentNumberOfPartitions(), 0);

      // Verify that adminClient.describeTopics was called
      verify(mockAdminClient, times(1)).describeTopics(topics);
    }
  }

  /**
   * Helper method to create a mock TopicDescription with specified number of partitions.
   *
   * @param topicName The name of the topic
   * @param partitionCount The number of partitions
   * @return A mock TopicDescription
   */
  private TopicDescription createMockTopicDescription(String topicName, int partitionCount) {
    TopicDescription topicDescription = mock(TopicDescription.class);
    List<TopicPartitionInfo> partitions = new ArrayList<>();
    for (int i = 0; i < partitionCount; i++) {
      TopicPartitionInfo partitionInfo = mock(TopicPartitionInfo.class);
      when(partitionInfo.partition()).thenReturn(i);
      partitions.add(partitionInfo);
    }
    when(topicDescription.partitions()).thenReturn(partitions);
    when(topicDescription.name()).thenReturn(topicName);
    return topicDescription;
  }

  /**
   * Helper method to create a mock KafkaFuture that returns the given value when get() is called.
   *
   * @param value The value to return when get() is called
   * @param <T> The type of the value
   * @return A mock KafkaFuture
   */
  @SuppressWarnings("unchecked")
  private <T> KafkaFuture<T> createMockKafkaFuture(T value) {
    KafkaFuture<T> kafkaFuture = mock(KafkaFuture.class);
    try {
      when(kafkaFuture.get()).thenReturn(value);
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException("Failed to setup KafkaFuture mock", e);
    }
    return kafkaFuture;
  }

  /**
   * Helper method to create a mock KafkaFuture that throws an exception when get() is called.
   *
   * @param exception The exception to throw when get() is called
   * @param <T> The type of the value
   * @return A mock KafkaFuture
   */
  @SuppressWarnings("unchecked")
  private <T> KafkaFuture<T> createMockKafkaFutureWithException(Exception exception) {
    KafkaFuture<T> kafkaFuture = mock(KafkaFuture.class);
    try {
      if (exception instanceof InterruptedException) {
        when(kafkaFuture.get()).thenThrow((InterruptedException) exception);
      } else if (exception instanceof ExecutionException) {
        when(kafkaFuture.get()).thenThrow((ExecutionException) exception);
      } else {
        when(kafkaFuture.get()).thenThrow(exception);
      }
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException("Failed to setup KafkaFuture mock", e);
    }
    return kafkaFuture;
  }
}
