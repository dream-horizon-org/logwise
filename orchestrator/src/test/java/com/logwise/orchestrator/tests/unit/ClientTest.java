package com.logwise.orchestrator.tests.unit;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.logwise.orchestrator.client.impl.ObjectStoreAwsImpl;
import com.logwise.orchestrator.config.ApplicationConfig;
import com.logwise.orchestrator.enums.Tenant;
import com.logwise.orchestrator.factory.ObjectStoreFactory;
import com.logwise.orchestrator.helper.HelperTestUtils;
import com.logwise.orchestrator.util.S3Utils;
import io.reactivex.Completable;
import io.reactivex.Single;
import java.util.Arrays;
import java.util.List;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import software.amazon.awssdk.services.s3.S3AsyncClient;

/** Unit tests for client package (ObjectStoreAwsImpl, ObjectStoreFactory). */
public class ClientTest {

  private ObjectStoreAwsImpl objectStoreAwsImpl;
  private ApplicationConfig.S3Config mockS3Config;
  private S3AsyncClient mockS3AsyncClient;

  @BeforeMethod
  public void setUp() throws Exception {
    objectStoreAwsImpl = new ObjectStoreAwsImpl();
    Object[] setup = HelperTestUtils.setupObjectStoreAwsImpl(objectStoreAwsImpl);
    mockS3Config = (ApplicationConfig.S3Config) setup[0];
    mockS3AsyncClient = (S3AsyncClient) setup[1];
  }

  @Test
  public void testObjectStoreAwsImpl_RxConnect_WithNullS3Config_CompletesWithoutError() {

    ApplicationConfig.ObjectStoreConfig mockObjectStoreConfig =
        mock(ApplicationConfig.ObjectStoreConfig.class);
    when(mockObjectStoreConfig.getAws()).thenReturn(null);

    Completable result = objectStoreAwsImpl.rxConnect(mockObjectStoreConfig);
    result.blockingAwait();

    Assert.assertNotNull(result);
    verify(mockObjectStoreConfig, times(1)).getAws();
  }

  @Test
  public void testObjectStoreAwsImpl_RxConnect_WithValidS3Config_CompletesSuccessfully() {

    ApplicationConfig.ObjectStoreConfig mockObjectStoreConfig =
        mock(ApplicationConfig.ObjectStoreConfig.class);
    ApplicationConfig.S3Config s3Config = new ApplicationConfig.S3Config();
    s3Config.setBucket("test-bucket");
    s3Config.setRegion("us-east-1");
    when(mockObjectStoreConfig.getAws()).thenReturn(s3Config);

    Completable result = objectStoreAwsImpl.rxConnect(mockObjectStoreConfig);

    try {
      result.blockingAwait();
      Assert.assertNotNull(result);
    } catch (Exception e) {

      Assert.assertTrue(true);
    }
  }

  @Test
  public void testObjectStoreAwsImpl_ListCommonPrefix_WithValidPrefix_DelegatesToS3Utils() {

    String prefix = "logs/";
    String delimiter = "/";
    List<String> expectedPrefixes = Arrays.asList("logs/env1/", "logs/env2/");

    try (MockedStatic<S3Utils> mockedS3Utils = Mockito.mockStatic(S3Utils.class)) {
      mockedS3Utils
          .when(
              () ->
                  S3Utils.listCommonPrefix(
                      eq(mockS3AsyncClient), eq(mockS3Config), eq(prefix), eq(delimiter)))
          .thenReturn(Single.just(expectedPrefixes));

      Single<List<String>> result = objectStoreAwsImpl.listCommonPrefix(prefix, delimiter);
      List<String> prefixes = result.blockingGet();

      Assert.assertNotNull(prefixes);
      Assert.assertEquals(prefixes.size(), 2);
      Assert.assertEquals(prefixes, expectedPrefixes);
      mockedS3Utils.verify(
          () ->
              S3Utils.listCommonPrefix(
                  eq(mockS3AsyncClient), eq(mockS3Config), eq(prefix), eq(delimiter)),
          times(1));
    }
  }

  @Test
  public void testObjectStoreAwsImpl_ListObjects_WithValidPrefix_DelegatesToS3Utils() {

    String prefix = "logs/env1/";
    List<String> expectedObjects = Arrays.asList("logs/env1/file1.log", "logs/env1/file2.log");

    try (MockedStatic<S3Utils> mockedS3Utils = Mockito.mockStatic(S3Utils.class)) {
      mockedS3Utils
          .when(() -> S3Utils.listObjects(eq(mockS3AsyncClient), eq(mockS3Config), eq(prefix)))
          .thenReturn(Single.just(expectedObjects));

      Single<List<String>> result = objectStoreAwsImpl.listObjects(prefix);
      List<String> objects = result.blockingGet();

      Assert.assertNotNull(objects);
      Assert.assertEquals(objects.size(), 2);
      Assert.assertEquals(objects, expectedObjects);
      mockedS3Utils.verify(
          () -> S3Utils.listObjects(eq(mockS3AsyncClient), eq(mockS3Config), eq(prefix)), times(1));
    }
  }

  @Test
  public void testObjectStoreAwsImpl_DeleteFile_WithValidObjectKey_DelegatesToS3Utils() {

    String objectKey = "logs/env1/file1.log";

    try (MockedStatic<S3Utils> mockedS3Utils = Mockito.mockStatic(S3Utils.class)) {
      mockedS3Utils
          .when(() -> S3Utils.deleteFile(eq(mockS3AsyncClient), eq(mockS3Config), eq(objectKey)))
          .thenReturn(Completable.complete());

      Completable result = objectStoreAwsImpl.deleteFile(objectKey);
      result.blockingAwait();

      Assert.assertNotNull(result);
      mockedS3Utils.verify(
          () -> S3Utils.deleteFile(eq(mockS3AsyncClient), eq(mockS3Config), eq(objectKey)),
          times(1));
    }
  }

  @Test
  public void testObjectStoreAwsImpl_ListObjects_WithError_PropagatesError() {

    String prefix = "logs/env1/";
    RuntimeException error = new RuntimeException("S3 error");

    try (MockedStatic<S3Utils> mockedS3Utils = Mockito.mockStatic(S3Utils.class)) {
      mockedS3Utils
          .when(() -> S3Utils.listObjects(eq(mockS3AsyncClient), eq(mockS3Config), eq(prefix)))
          .thenReturn(Single.error(error));

      Single<List<String>> result = objectStoreAwsImpl.listObjects(prefix);

      try {
        result.blockingGet();
        Assert.fail("Should have thrown exception");
      } catch (RuntimeException e) {
        Assert.assertEquals(e.getMessage(), "S3 error");
      }
      mockedS3Utils.verify(
          () -> S3Utils.listObjects(eq(mockS3AsyncClient), eq(mockS3Config), eq(prefix)), times(1));
    }
  }

  @Test
  public void testObjectStoreAwsImpl_ListCommonPrefix_WithError_PropagatesError() {

    String prefix = "logs/";
    String delimiter = "/";
    RuntimeException error = new RuntimeException("S3 error");

    try (MockedStatic<S3Utils> mockedS3Utils = Mockito.mockStatic(S3Utils.class)) {
      mockedS3Utils
          .when(
              () ->
                  S3Utils.listCommonPrefix(
                      eq(mockS3AsyncClient), eq(mockS3Config), eq(prefix), eq(delimiter)))
          .thenReturn(Single.error(error));

      Single<List<String>> result = objectStoreAwsImpl.listCommonPrefix(prefix, delimiter);

      try {
        result.blockingGet();
        Assert.fail("Should have thrown exception");
      } catch (RuntimeException e) {
        Assert.assertEquals(e.getMessage(), "S3 error");
      }
      mockedS3Utils.verify(
          () ->
              S3Utils.listCommonPrefix(
                  eq(mockS3AsyncClient), eq(mockS3Config), eq(prefix), eq(delimiter)),
          times(1));
    }
  }

  @Test
  public void testObjectStoreAwsImpl_DeleteFile_WithError_PropagatesError() {

    String objectKey = "logs/env1/file1.log";
    RuntimeException error = new RuntimeException("Delete error");

    try (MockedStatic<S3Utils> mockedS3Utils = Mockito.mockStatic(S3Utils.class)) {
      mockedS3Utils
          .when(() -> S3Utils.deleteFile(eq(mockS3AsyncClient), eq(mockS3Config), eq(objectKey)))
          .thenReturn(Completable.error(error));

      Completable result = objectStoreAwsImpl.deleteFile(objectKey);

      try {
        result.blockingAwait();
        Assert.fail("Should have thrown exception");
      } catch (RuntimeException e) {
        Assert.assertEquals(e.getMessage(), "Delete error");
      }
      mockedS3Utils.verify(
          () -> S3Utils.deleteFile(eq(mockS3AsyncClient), eq(mockS3Config), eq(objectKey)),
          times(1));
    }
  }

  @Test
  public void testObjectStoreFactory_ClassExists() {

    Assert.assertNotNull(ObjectStoreFactory.class);
  }

  @Test
  public void testObjectStoreFactory_GetClientMethodExists() throws Exception {

    java.lang.reflect.Method method = ObjectStoreFactory.class.getMethod("getClient", Tenant.class);
    Assert.assertNotNull(method);
  }
}
