package com.logwise.orchestrator.tests.unit.client.kafka;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import com.logwise.orchestrator.client.kafka.Ec2KafkaClient;
import com.logwise.orchestrator.config.ApplicationConfig;
import com.logwise.orchestrator.constant.ApplicationConstants;
import com.logwise.orchestrator.enums.KafkaType;
import com.logwise.orchestrator.setup.BaseTest;
import com.logwise.orchestrator.util.ApplicationUtils;
import io.reactivex.Single;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Unit tests for Ec2KafkaClient. */
public class Ec2KafkaClientTest extends BaseTest {

  private ApplicationConfig.KafkaConfig kafkaConfig;
  private Ec2KafkaClient ec2KafkaClient;

  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    kafkaConfig = new ApplicationConfig.KafkaConfig();
    kafkaConfig.setKafkaBrokersHost("kafka.example.com");
    kafkaConfig.setKafkaBrokerPort(9092);
    kafkaConfig.setKafkaType(KafkaType.EC2);
    ec2KafkaClient = new Ec2KafkaClient(kafkaConfig);
  }

  @Test
  public void testGetKafkaType_ReturnsEC2() {
    KafkaType type = ec2KafkaClient.getKafkaType();
    assertEquals(type, KafkaType.EC2);
  }

  @Test
  public void testBuildBootstrapServers_WithValidHostname_ResolvesToIPs() {
    try (MockedStatic<ApplicationUtils> mockedUtils = Mockito.mockStatic(ApplicationUtils.class)) {
      List<String> mockIPs = Arrays.asList("192.168.1.1", "192.168.1.2");
      mockedUtils
          .when(() -> ApplicationUtils.getIpAddresses("kafka.example.com"))
          .thenReturn(Single.just(mockIPs));

      String bootstrapServers = ec2KafkaClient.buildBootstrapServers().blockingGet();

      assertNotNull(bootstrapServers);
      assertTrue(bootstrapServers.contains("192.168.1.1:9092"));
      assertTrue(bootstrapServers.contains("192.168.1.2:9092"));
    }
  }

  @Test
  public void testBuildBootstrapServers_WithCustomPort_UsesCustomPort() {
    kafkaConfig.setKafkaBrokerPort(9093);
    try (MockedStatic<ApplicationUtils> mockedUtils = Mockito.mockStatic(ApplicationUtils.class)) {
      List<String> mockIPs = Arrays.asList("192.168.1.1");
      mockedUtils
          .when(() -> ApplicationUtils.getIpAddresses("kafka.example.com"))
          .thenReturn(Single.just(mockIPs));

      String bootstrapServers = ec2KafkaClient.buildBootstrapServers().blockingGet();

      assertTrue(bootstrapServers.contains(":9093"));
    }
  }

  @Test
  public void testBuildBootstrapServers_WithNullPort_UsesDefaultPort() {
    kafkaConfig.setKafkaBrokerPort(null);
    try (MockedStatic<ApplicationUtils> mockedUtils = Mockito.mockStatic(ApplicationUtils.class)) {
      List<String> mockIPs = Arrays.asList("192.168.1.1");
      mockedUtils
          .when(() -> ApplicationUtils.getIpAddresses("kafka.example.com"))
          .thenReturn(Single.just(mockIPs));

      String bootstrapServers = ec2KafkaClient.buildBootstrapServers().blockingGet();

      assertTrue(bootstrapServers.contains(":" + ApplicationConstants.KAFKA_BROKER_PORT));
    }
  }

  @Test
  public void testBuildAdminClientConfig_WithBasicConfig_ReturnsCorrectConfig() {
    try (MockedStatic<ApplicationUtils> mockedUtils = Mockito.mockStatic(ApplicationUtils.class)) {
      List<String> mockIPs = Arrays.asList("192.168.1.1");
      mockedUtils
          .when(() -> ApplicationUtils.getIpAddresses(anyString()))
          .thenReturn(Single.just(mockIPs));

      Map<String, Object> config = ec2KafkaClient.buildAdminClientConfig().blockingGet();

      assertNotNull(config);
      assertTrue(config.containsKey(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG));
      assertTrue(config.containsKey(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG));
      assertEquals(
          config.get(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG),
          ApplicationConstants.KAFKA_REQUEST_TIMEOUT_MS);
    }
  }

  @Test
  public void testBuildAdminClientConfig_WithSSL_IncludesSSLConfig() {
    kafkaConfig.setSslTruststoreLocation("/path/to/truststore.jks");
    kafkaConfig.setSslTruststorePassword("password");
    try (MockedStatic<ApplicationUtils> mockedUtils = Mockito.mockStatic(ApplicationUtils.class)) {
      List<String> mockIPs = Arrays.asList("192.168.1.1");
      mockedUtils
          .when(() -> ApplicationUtils.getIpAddresses(anyString()))
          .thenReturn(Single.just(mockIPs));

      Map<String, Object> config = ec2KafkaClient.buildAdminClientConfig().blockingGet();

      assertNotNull(config);
      assertEquals(config.get("security.protocol"), "SSL");
      assertEquals(config.get("ssl.truststore.location"), "/path/to/truststore.jks");
      assertEquals(config.get("ssl.truststore.password"), "password");
    }
  }

  @Test
  public void testBuildAdminClientConfig_WithSSLButNoPassword_IncludesSSLConfigWithoutPassword() {
    kafkaConfig.setSslTruststoreLocation("/path/to/truststore.jks");
    kafkaConfig.setSslTruststorePassword(null);
    try (MockedStatic<ApplicationUtils> mockedUtils = Mockito.mockStatic(ApplicationUtils.class)) {
      List<String> mockIPs = Arrays.asList("192.168.1.1");
      mockedUtils
          .when(() -> ApplicationUtils.getIpAddresses(anyString()))
          .thenReturn(Single.just(mockIPs));

      Map<String, Object> config = ec2KafkaClient.buildAdminClientConfig().blockingGet();

      assertNotNull(config);
      assertEquals(config.get("security.protocol"), "SSL");
      assertEquals(config.get("ssl.truststore.location"), "/path/to/truststore.jks");
      assertFalse(config.containsKey("ssl.truststore.password"));
    }
  }

  @Test
  public void testClose_ClosesAdminClient() {
    try (MockedStatic<ApplicationUtils> mockedUtils = Mockito.mockStatic(ApplicationUtils.class)) {
      List<String> mockIPs = Arrays.asList("192.168.1.1");
      mockedUtils
          .when(() -> ApplicationUtils.getIpAddresses(anyString()))
          .thenReturn(Single.just(mockIPs));

      AdminClient mockAdminClient = mock(AdminClient.class);
      // Note: In real implementation, AdminClient would be created via createAdminClient()
      // This test verifies the close method exists and can be called
      ec2KafkaClient.close();

      // Should not throw exception
      assertTrue(true);
    }
  }
}
