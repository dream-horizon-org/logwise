package com.logwise.orchestrator.tests.unit.client;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.logwise.orchestrator.client.impl.ObjectStoreAwsImpl;
import com.logwise.orchestrator.config.ApplicationConfig;
import com.logwise.orchestrator.setup.BaseTest;
import com.logwise.orchestrator.testconfig.ApplicationTestConfig;
import com.logwise.orchestrator.util.S3Utils;
import io.reactivex.Completable;
import io.reactivex.Single;
import java.util.Arrays;
import java.util.List;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import software.amazon.awssdk.services.s3.S3AsyncClient;

/** Unit tests for ObjectStoreAwsImpl. */
public class ObjectStoreAwsImplTest extends BaseTest {

  private ObjectStoreAwsImpl objectStoreAwsImpl;
  private S3AsyncClient mockS3Client;

  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    objectStoreAwsImpl = new ObjectStoreAwsImpl();
    mockS3Client = mock(S3AsyncClient.class);
  }

  @AfterClass
  public static void tearDownClass() {
    BaseTest.cleanup();
  }

  @Test
  public void testRxConnect_WithValidConfig_CompletesSuccessfully() {
    ApplicationConfig.ObjectStoreConfig config =
        ApplicationTestConfig.createMockObjectStoreConfig();

    Completable result = objectStoreAwsImpl.rxConnect(config);

    Assert.assertNotNull(result);
    result.blockingAwait();
  }

  @Test
  public void testRxConnect_WithNullAwsConfig_CompletesSuccessfully() {
    ApplicationConfig.ObjectStoreConfig config = new ApplicationConfig.ObjectStoreConfig();
    config.setAws(null);

    Completable result = objectStoreAwsImpl.rxConnect(config);

    Assert.assertNotNull(result);
    result.blockingAwait();
  }

  @Test
  public void testListCommonPrefix_WithValidPrefix_ReturnsPrefixes() {
    ApplicationConfig.ObjectStoreConfig config =
        ApplicationTestConfig.createMockObjectStoreConfig();
    objectStoreAwsImpl.rxConnect(config).blockingAwait();

    String prefix = "logs/";
    String delimiter = "/";
    List<String> expectedPrefixes = Arrays.asList("logs/env1/", "logs/env2/");

    try (MockedStatic<S3Utils> mockedS3Utils = Mockito.mockStatic(S3Utils.class)) {
      mockedS3Utils
          .when(() -> S3Utils.listCommonPrefix(any(), any(), eq(prefix), eq(delimiter)))
          .thenReturn(Single.just(expectedPrefixes));

      Single<List<String>> result = objectStoreAwsImpl.listCommonPrefix(prefix, delimiter);
      List<String> prefixes = result.blockingGet();

      Assert.assertNotNull(prefixes);
      Assert.assertEquals(prefixes.size(), 2);
    }
  }

  @Test
  public void testListObjects_WithValidPrefix_ReturnsObjects() {
    ApplicationConfig.ObjectStoreConfig config =
        ApplicationTestConfig.createMockObjectStoreConfig();
    objectStoreAwsImpl.rxConnect(config).blockingAwait();

    String prefix = "logs/";
    List<String> expectedObjects = Arrays.asList("logs/file1.log", "logs/file2.log");

    try (MockedStatic<S3Utils> mockedS3Utils = Mockito.mockStatic(S3Utils.class)) {
      mockedS3Utils
          .when(() -> S3Utils.listObjects(any(), any(), eq(prefix)))
          .thenReturn(Single.just(expectedObjects));

      Single<List<String>> result = objectStoreAwsImpl.listObjects(prefix);
      List<String> objects = result.blockingGet();

      Assert.assertNotNull(objects);
      Assert.assertEquals(objects.size(), 2);
    }
  }

  @Test
  public void testDeleteFile_WithValidObjectKey_CompletesSuccessfully() {
    ApplicationConfig.ObjectStoreConfig config =
        ApplicationTestConfig.createMockObjectStoreConfig();
    objectStoreAwsImpl.rxConnect(config).blockingAwait();

    String objectKey = "logs/file.log";

    try (MockedStatic<S3Utils> mockedS3Utils = Mockito.mockStatic(S3Utils.class)) {
      mockedS3Utils
          .when(() -> S3Utils.deleteFile(any(), any(), eq(objectKey)))
          .thenReturn(Completable.complete());

      Completable result = objectStoreAwsImpl.deleteFile(objectKey);

      Assert.assertNotNull(result);
      result.blockingAwait();
    }
  }
}
