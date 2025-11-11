package com.logwise.orchestrator.tests.unit.mysql;

import com.logwise.orchestrator.mysql.client.MysqlConfig;
import io.vertx.core.json.JsonObject;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Unit tests for MysqlConfig. */
public class MysqlTest {

  @Test
  public void testMysqlConfig_Constructor_WithJsonObject_CreatesConfig() {

    JsonObject json = new JsonObject();
    json.put("database", "testdb");
    json.put("username", "testuser");
    json.put("password", "testpass");
    json.put("masterHost", "master.example.com");
    json.put("slaveHost", "slave.example.com");
    json.put("port", 3306);

    MysqlConfig config = new MysqlConfig(json);

    Assert.assertNotNull(config);
    Assert.assertEquals(config.getDatabase(), "testdb");
    Assert.assertEquals(config.getUsername(), "testuser");
    Assert.assertEquals(config.getPassword(), "testpass");
    Assert.assertEquals(config.getMasterHost(), "master.example.com");
    Assert.assertEquals(config.getSlaveHost(), "slave.example.com");
    Assert.assertEquals(config.getPort(), Integer.valueOf(3306));
  }

  @Test
  public void testMysqlConfig_Constructor_WithNullJson_UsesDefaults() {

    MysqlConfig config = new MysqlConfig(null);

    Assert.assertNotNull(config);
    Assert.assertEquals(config.getMasterHost(), "127.0.0.1"); // DEFAULT_HOST value
    Assert.assertEquals(config.getSlaveHost(), "127.0.0.1"); // DEFAULT_HOST value
    Assert.assertEquals(config.getPort(), Integer.valueOf(3306)); // DEFAULT_PORT value
  }

  @Test
  public void testMysqlConfig_GetMaxMasterPoolSize_WithNull_ReturnsMaxPoolSize() {

    JsonObject json = new JsonObject();
    json.put("maxPoolSize", 20);
    MysqlConfig config = new MysqlConfig(json);

    Integer maxMasterPoolSize = config.getMaxMasterPoolSize();

    Assert.assertEquals(maxMasterPoolSize, Integer.valueOf(20));
  }

  @Test
  public void testMysqlConfig_GetMaxMasterPoolSize_WithValue_ReturnsValue() {

    JsonObject json = new JsonObject();
    json.put("maxMasterPoolSize", 15);
    json.put("maxPoolSize", 20);
    MysqlConfig config = new MysqlConfig(json);

    Integer maxMasterPoolSize = config.getMaxMasterPoolSize();

    Assert.assertEquals(maxMasterPoolSize, Integer.valueOf(15));
  }

  @Test
  public void testMysqlConfig_GetMaxSlavePoolSize_WithNull_ReturnsMaxPoolSize() {

    JsonObject json = new JsonObject();
    json.put("maxPoolSize", 20);
    MysqlConfig config = new MysqlConfig(json);

    Integer maxSlavePoolSize = config.getMaxSlavePoolSize();

    Assert.assertEquals(maxSlavePoolSize, Integer.valueOf(20));
  }

  @Test
  public void testMysqlConfig_GetMaxSlavePoolSize_WithValue_ReturnsValue() {

    JsonObject json = new JsonObject();
    json.put("maxSlavePoolSize", 10);
    json.put("maxPoolSize", 20);
    MysqlConfig config = new MysqlConfig(json);

    Integer maxSlavePoolSize = config.getMaxSlavePoolSize();

    Assert.assertEquals(maxSlavePoolSize, Integer.valueOf(10));
  }
}
