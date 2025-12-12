package com.logwise.orchestrator.tests.unit.client.kafka;

import static org.testng.Assert.*;

import com.logwise.orchestrator.client.kafka.MskKafkaClient;
import com.logwise.orchestrator.config.ApplicationConfig;
import com.logwise.orchestrator.enums.KafkaType;
import com.logwise.orchestrator.setup.BaseTest;
import java.util.Map;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Unit tests for MskKafkaClient. */
public class MskKafkaClientTest extends BaseTest {

  private ApplicationConfig.KafkaConfig kafkaConfig;
  private MskKafkaClient mskKafkaClient;

  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    kafkaConfig = new ApplicationConfig.KafkaConfig();
    kafkaConfig.setKafkaType(KafkaType.MSK);
    kafkaConfig.setMskClusterArn("arn:aws:kafka:us-east-1:123456789012:cluster/test/abcd-1234");
    kafkaConfig.setMskRegion("us-east-1");
    mskKafkaClient = new MskKafkaClient(kafkaConfig);
  }

  @Test
  public void testGetKafkaType_ReturnsMSK() {
    KafkaType type = mskKafkaClient.getKafkaType();
    assertEquals(type, KafkaType.MSK);
  }

  @Test
  public void testBuildBootstrapServers_WithProvidedHost_ReturnsHost() {
    kafkaConfig.setKafkaBrokersHost("b-1.test.abc123.c1.kafka.us-east-1.amazonaws.com:9098");
    String bootstrapServers = mskKafkaClient.buildBootstrapServers().blockingGet();
    assertNotNull(bootstrapServers);
    assertTrue(bootstrapServers.contains("b-1.test.abc123.c1.kafka.us-east-1.amazonaws.com"));
  }

  @Test
  public void testBuildAdminClientConfig_WithIAMAuth_IncludesIAMConfig() {
    kafkaConfig.setKafkaBrokersHost("b-1.test.abc123.c1.kafka.us-east-1.amazonaws.com:9098");
    Map<String, Object> config = mskKafkaClient.buildAdminClientConfig().blockingGet();

    assertNotNull(config);
    assertEquals(config.get("security.protocol"), "SASL_SSL");
    assertEquals(config.get(SaslConfigs.SASL_MECHANISM), "AWS_MSK_IAM");
    assertTrue(
        config
            .get(SaslConfigs.SASL_CLIENT_CALLBACK_HANDLER_CLASS)
            .toString()
            .contains("IAMClientCallbackHandler"));
    assertEquals(config.get("ssl.endpoint.identification.algorithm"), "https");
  }

  @Test
  public void testBuildAdminClientConfig_IncludesBootstrapServers() {
    kafkaConfig.setKafkaBrokersHost("b-1.test.abc123.c1.kafka.us-east-1.amazonaws.com:9098");
    Map<String, Object> config = mskKafkaClient.buildAdminClientConfig().blockingGet();

    assertNotNull(config);
    assertTrue(config.containsKey(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG));
    String bootstrapServers = (String) config.get(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG);
    assertNotNull(bootstrapServers);
  }

  @Test
  public void testBuildAdminClientConfig_IncludesRequestTimeout() {
    kafkaConfig.setKafkaBrokersHost("b-1.test.abc123.c1.kafka.us-east-1.amazonaws.com:9098");
    Map<String, Object> config = mskKafkaClient.buildAdminClientConfig().blockingGet();

    assertNotNull(config);
    assertTrue(config.containsKey(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG));
  }

  @Test
  public void testBuildBootstrapServers_WithNoHostOrArn_ThrowsException() {
    // Create a new client with null configs to test the exception
    ApplicationConfig.KafkaConfig testConfig = new ApplicationConfig.KafkaConfig();
    testConfig.setKafkaType(KafkaType.MSK);
    testConfig.setKafkaBrokersHost(null);
    testConfig.setMskClusterArn(null);
    testConfig.setMskRegion("us-east-1");
    MskKafkaClient testClient = new MskKafkaClient(testConfig);

    try {
      testClient.buildBootstrapServers().blockingGet();
      fail("Expected IllegalStateException to be thrown");
    } catch (RuntimeException e) {
      // RxJava wraps exceptions, so check the cause
      Throwable cause = e.getCause();
      if (cause instanceof IllegalStateException) {
        assertTrue(cause.getMessage().contains("MSK bootstrap servers must be provided"));
      } else {
        // If not wrapped, the exception itself should be IllegalStateException
        if (e instanceof IllegalStateException) {
          assertTrue(e.getMessage().contains("MSK bootstrap servers must be provided"));
        } else {
          fail("Expected IllegalStateException but got: " + e.getClass().getName());
        }
      }
    }
  }
}
