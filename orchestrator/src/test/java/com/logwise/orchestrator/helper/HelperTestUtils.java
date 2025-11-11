package com.logwise.orchestrator.helper;

import com.logwise.orchestrator.config.ApplicationConfig;
import com.logwise.orchestrator.testconfig.ApplicationTestConfig;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import lombok.experimental.UtilityClass;
import org.mockito.Mockito;
import software.amazon.awssdk.services.s3.S3AsyncClient;

/**
 * Helper utilities for unit tests. Provides common helper methods for setting up mocks, using
 * reflection, and other test utilities.
 */
@UtilityClass
public class HelperTestUtils {

  /**
   * Sets a private field value using reflection.
   *
   * @param target the target object
   * @param fieldName the name of the field to set
   * @param value the value to set
   * @throws Exception if reflection fails
   */
  public static void setPrivateField(Object target, String fieldName, Object value)
      throws Exception {
    Field field = target.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }

  /**
   * Sets up ObjectStoreAwsImpl with mocked S3Config and S3AsyncClient using reflection.
   *
   * @param objectStoreAwsImpl the ObjectStoreAwsImpl instance
   * @return array containing [mockS3Config, mockS3AsyncClient]
   * @throws Exception if reflection fails
   */
  public static Object[] setupObjectStoreAwsImpl(Object objectStoreAwsImpl) throws Exception {
    ApplicationConfig.S3Config mockS3Config = Mockito.mock(ApplicationConfig.S3Config.class);
    S3AsyncClient mockS3AsyncClient = Mockito.mock(S3AsyncClient.class);

    setPrivateField(objectStoreAwsImpl, "s3Config", mockS3Config);
    setPrivateField(objectStoreAwsImpl, "s3AsyncClient", mockS3AsyncClient);

    return new Object[] {mockS3Config, mockS3AsyncClient};
  }

  /**
   * Creates a list of test object keys for S3.
   *
   * @param prefix the prefix for object keys
   * @param count the number of object keys to create
   * @return list of object keys
   */
  public static List<String> createTestObjectKeys(String prefix, int count) {
    String[] keys = new String[count];
    for (int i = 0; i < count; i++) {
      keys[i] = prefix + "file" + i + ".log";
    }
    return Arrays.asList(keys);
  }

  /**
   * Creates a list of test common prefixes for S3.
   *
   * @param basePrefix the base prefix
   * @param envs list of environment names
   * @return list of common prefixes
   */
  public static List<String> createTestCommonPrefixes(String basePrefix, String... envs) {
    String[] prefixes = new String[envs.length];
    for (int i = 0; i < envs.length; i++) {
      prefixes[i] = basePrefix + envs[i] + "/";
    }
    return Arrays.asList(prefixes);
  }

  /**
   * Creates a mock TenantConfig with all required nested configurations.
   *
   * @param tenantName the tenant name
   * @return fully configured TenantConfig
   */
  public static ApplicationConfig.TenantConfig createFullMockTenantConfig(String tenantName) {
    return ApplicationTestConfig.createMockTenantConfig(tenantName);
  }
}
