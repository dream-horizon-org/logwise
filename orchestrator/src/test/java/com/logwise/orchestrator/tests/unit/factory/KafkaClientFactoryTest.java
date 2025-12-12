package com.logwise.orchestrator.tests.unit.factory;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import com.logwise.orchestrator.client.kafka.ConfluentKafkaClient;
import com.logwise.orchestrator.client.kafka.Ec2KafkaClient;
import com.logwise.orchestrator.client.kafka.KafkaClient;
import com.logwise.orchestrator.client.kafka.MskKafkaClient;
import com.logwise.orchestrator.config.ApplicationConfig;
import com.logwise.orchestrator.enums.KafkaType;
import com.logwise.orchestrator.factory.KafkaClientFactory;
import com.logwise.orchestrator.setup.BaseTest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Unit tests for KafkaClientFactory. */
public class KafkaClientFactoryTest extends BaseTest {

  private KafkaClientFactory factory;
  private Ec2KafkaClient.Factory ec2Factory;
  private MskKafkaClient.Factory mskFactory;
  private ConfluentKafkaClient.Factory confluentFactory;

  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    ec2Factory = mock(Ec2KafkaClient.Factory.class);
    mskFactory = mock(MskKafkaClient.Factory.class);
    confluentFactory = mock(ConfluentKafkaClient.Factory.class);
    factory = new KafkaClientFactory(ec2Factory, mskFactory, confluentFactory);
  }

  @Test
  public void testCreateKafkaClient_WithEC2Type_ReturnsEc2Client() {
    ApplicationConfig.KafkaConfig config = new ApplicationConfig.KafkaConfig();
    config.setKafkaType(KafkaType.EC2);
    config.setKafkaBrokersHost("localhost:9092");

    Ec2KafkaClient mockClient = mock(Ec2KafkaClient.class);
    when(ec2Factory.create(config)).thenReturn(mockClient);

    KafkaClient client = factory.createKafkaClient(config);

    assertNotNull(client);
    assertTrue(client instanceof Ec2KafkaClient);
    verify(ec2Factory, times(1)).create(config);
  }

  @Test
  public void testCreateKafkaClient_WithMSKType_ReturnsMskClient() {
    ApplicationConfig.KafkaConfig config = new ApplicationConfig.KafkaConfig();
    config.setKafkaType(KafkaType.MSK);
    config.setKafkaBrokersHost("b-1.test.abc123.c1.kafka.us-east-1.amazonaws.com:9098");

    MskKafkaClient mockClient = mock(MskKafkaClient.class);
    when(mskFactory.create(config)).thenReturn(mockClient);

    KafkaClient client = factory.createKafkaClient(config);

    assertNotNull(client);
    assertTrue(client instanceof MskKafkaClient);
    verify(mskFactory, times(1)).create(config);
  }

  @Test
  public void testCreateKafkaClient_WithConfluentType_ReturnsConfluentClient() {
    ApplicationConfig.KafkaConfig config = new ApplicationConfig.KafkaConfig();
    config.setKafkaType(KafkaType.CONFLUENT);
    config.setKafkaBrokersHost("pkc-xxxxx.us-east-1.aws.confluent.cloud:9092");
    config.setConfluentApiKey("test-key");
    config.setConfluentApiSecret("test-secret");

    ConfluentKafkaClient mockClient = mock(ConfluentKafkaClient.class);
    when(confluentFactory.create(config)).thenReturn(mockClient);

    KafkaClient client = factory.createKafkaClient(config);

    assertNotNull(client);
    assertTrue(client instanceof ConfluentKafkaClient);
    verify(confluentFactory, times(1)).create(config);
  }

  @Test
  public void testCreateKafkaClient_WithNullType_ThrowsException() {
    ApplicationConfig.KafkaConfig config = new ApplicationConfig.KafkaConfig();
    // The setter converts null to EC2, so we need to use reflection to set it to null
    try {
      java.lang.reflect.Field field =
          ApplicationConfig.KafkaConfig.class.getDeclaredField("kafkaType");
      field.setAccessible(true);
      field.set(config, null);
    } catch (Exception e) {
      fail("Failed to set kafkaType to null via reflection: " + e.getMessage());
    }

    try {
      factory.createKafkaClient(config);
      fail("Expected IllegalArgumentException to be thrown");
    } catch (IllegalArgumentException e) {
      assertTrue(e.getMessage().contains("Kafka type cannot be null"));
    }
  }
}
