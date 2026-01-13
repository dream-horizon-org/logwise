package com.logwise.orchestrator.tests.unit.client.kafka;

import static org.testng.Assert.*;

import com.logwise.orchestrator.client.kafka.ConfluentKafkaClient;
import com.logwise.orchestrator.config.ApplicationConfig;
import com.logwise.orchestrator.enums.KafkaType;
import com.logwise.orchestrator.setup.BaseTest;
import java.util.Map;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Unit tests for ConfluentKafkaClient. */
public class ConfluentKafkaClientTest extends BaseTest {

  private ApplicationConfig.KafkaConfig kafkaConfig;
  private ConfluentKafkaClient confluentKafkaClient;

  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    kafkaConfig = new ApplicationConfig.KafkaConfig();
    kafkaConfig.setKafkaType(KafkaType.CONFLUENT);
    kafkaConfig.setKafkaBrokersHost("pkc-xxxxx.us-east-1.aws.confluent.cloud:9092");
    kafkaConfig.setConfluentApiKey("test-api-key");
    kafkaConfig.setConfluentApiSecret("test-api-secret");
    confluentKafkaClient = new ConfluentKafkaClient(kafkaConfig);
  }

  @Test
  public void testGetKafkaType_ReturnsConfluent() {
    KafkaType type = confluentKafkaClient.getKafkaType();
    assertEquals(type, KafkaType.CONFLUENT);
  }

  @Test
  public void testBuildBootstrapServers_ReturnsProvidedHost() {
    String bootstrapServers = confluentKafkaClient.buildBootstrapServers().blockingGet();
    assertNotNull(bootstrapServers);
    assertEquals(bootstrapServers, "pkc-xxxxx.us-east-1.aws.confluent.cloud:9092");
  }

  @Test(expectedExceptions = IllegalStateException.class)
  public void testBuildBootstrapServers_WithNullHost_ThrowsException() {
    kafkaConfig.setKafkaBrokersHost(null);
    new ConfluentKafkaClient(kafkaConfig).buildBootstrapServers().blockingGet();
  }

  @Test(expectedExceptions = IllegalStateException.class)
  public void testConstructor_WithNullApiKey_ThrowsException() {
    kafkaConfig.setConfluentApiKey(null);
    new ConfluentKafkaClient(kafkaConfig);
  }

  @Test(expectedExceptions = IllegalStateException.class)
  public void testConstructor_WithNullApiSecret_ThrowsException() {
    kafkaConfig.setConfluentApiSecret(null);
    new ConfluentKafkaClient(kafkaConfig);
  }

  @Test
  public void testBuildAdminClientConfig_WithSASL_IncludesSASLConfig() {
    Map<String, Object> config = confluentKafkaClient.buildAdminClientConfig().blockingGet();

    assertNotNull(config);
    assertEquals(config.get("security.protocol"), "SASL_SSL");
    assertEquals(config.get(SaslConfigs.SASL_MECHANISM), "PLAIN");
    assertTrue(config.get(SaslConfigs.SASL_JAAS_CONFIG).toString().contains("test-api-key"));
    assertTrue(config.get(SaslConfigs.SASL_JAAS_CONFIG).toString().contains("test-api-secret"));
  }

  @Test
  public void testBuildAdminClientConfig_IncludesBootstrapServers() {
    Map<String, Object> config = confluentKafkaClient.buildAdminClientConfig().blockingGet();

    assertNotNull(config);
    assertTrue(config.containsKey(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG));
    String bootstrapServers = (String) config.get(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG);
    assertEquals(bootstrapServers, "pkc-xxxxx.us-east-1.aws.confluent.cloud:9092");
  }

  @Test
  public void testBuildAdminClientConfig_IncludesSSLConfig() {
    Map<String, Object> config = confluentKafkaClient.buildAdminClientConfig().blockingGet();

    assertNotNull(config);
    assertEquals(config.get("ssl.endpoint.identification.algorithm"), "https");
  }

  @Test
  public void testBuildAdminClientConfig_IncludesRequestTimeout() {
    Map<String, Object> config = confluentKafkaClient.buildAdminClientConfig().blockingGet();

    assertNotNull(config);
    assertTrue(config.containsKey(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG));
  }
}
