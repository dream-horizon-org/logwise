package com.logwise.orchestrator.tests.unit.util;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.logwise.orchestrator.common.util.CompletableFutureUtils;
import com.logwise.orchestrator.config.ApplicationConfig;
import com.logwise.orchestrator.setup.BaseTest;
import com.logwise.orchestrator.util.S3Utils;
import io.reactivex.Completable;
import io.reactivex.Single;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.*;

public class S3UtilsTest extends BaseTest {

  private S3AsyncClient mockS3Client;
  private ApplicationConfig.S3Config mockS3Config;

  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    mockS3Client = mock(S3AsyncClient.class);
    mockS3Config = mock(ApplicationConfig.S3Config.class);
    when(mockS3Config.getBucket()).thenReturn("test-bucket");
  }

  @Test
  public void testListCommonPrefix_WithValidPrefix_ReturnsPrefixes() throws Exception {
    String prefix = "logs/";
    String delimiter = "/";

    CommonPrefix commonPrefix1 = CommonPrefix.builder().prefix("logs/service1/").build();
    CommonPrefix commonPrefix2 = CommonPrefix.builder().prefix("logs/service2/").build();

    ListObjectsV2Response response =
        ListObjectsV2Response.builder()
            .commonPrefixes(Arrays.asList(commonPrefix1, commonPrefix2))
            .build();

    CompletableFuture<ListObjectsV2Response> future = CompletableFuture.completedFuture(response);
    when(mockS3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(future);

    try (MockedStatic<CompletableFutureUtils> mockedUtils =
        Mockito.mockStatic(CompletableFutureUtils.class)) {
      mockedUtils
          .when(() -> CompletableFutureUtils.toSingle(any(CompletableFuture.class)))
          .thenAnswer(
              invocation -> {
                CompletableFuture<ListObjectsV2Response> cf = invocation.getArgument(0);
                return Single.fromFuture(cf);
              });

      Single<List<String>> result =
          S3Utils.listCommonPrefix(mockS3Client, mockS3Config, prefix, delimiter);

      List<String> prefixes = result.blockingGet();
      Assert.assertNotNull(prefixes);
      Assert.assertEquals(prefixes.size(), 2);
      Assert.assertTrue(prefixes.contains("logs/service1/"));
      Assert.assertTrue(prefixes.contains("logs/service2/"));
    }
  }

  @Test
  public void testListCommonPrefix_WithEmptyPrefixes_ReturnsEmptyList() throws Exception {
    String prefix = "logs/";
    String delimiter = "/";

    ListObjectsV2Response response =
        ListObjectsV2Response.builder().commonPrefixes(Collections.emptyList()).build();

    CompletableFuture<ListObjectsV2Response> future = CompletableFuture.completedFuture(response);
    when(mockS3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(future);

    try (MockedStatic<CompletableFutureUtils> mockedUtils =
        Mockito.mockStatic(CompletableFutureUtils.class)) {
      mockedUtils
          .when(() -> CompletableFutureUtils.toSingle(any(CompletableFuture.class)))
          .thenAnswer(
              invocation -> {
                CompletableFuture<ListObjectsV2Response> cf = invocation.getArgument(0);
                return Single.fromFuture(cf);
              });

      Single<List<String>> result =
          S3Utils.listCommonPrefix(mockS3Client, mockS3Config, prefix, delimiter);

      List<String> prefixes = result.blockingGet();
      Assert.assertNotNull(prefixes);
      Assert.assertTrue(prefixes.isEmpty());
    }
  }

  @Test
  public void testDeleteFile_WithValidKey_DeletesSuccessfully() throws Exception {
    String objectKey = "logs/test.log";
    DeleteObjectResponse response = DeleteObjectResponse.builder().build();
    CompletableFuture<DeleteObjectResponse> future = CompletableFuture.completedFuture(response);

    when(mockS3Client.deleteObject(any(DeleteObjectRequest.class))).thenReturn(future);

    try (MockedStatic<CompletableFutureUtils> mockedUtils =
        Mockito.mockStatic(CompletableFutureUtils.class)) {
      mockedUtils
          .when(() -> CompletableFutureUtils.toSingle(any(CompletableFuture.class)))
          .thenAnswer(
              invocation -> {
                CompletableFuture<DeleteObjectResponse> cf = invocation.getArgument(0);
                return Single.fromFuture(cf);
              });

      Completable result = S3Utils.deleteFile(mockS3Client, mockS3Config, objectKey);

      result.blockingAwait();
      verify(mockS3Client, times(1)).deleteObject(any(DeleteObjectRequest.class));
    }
  }

  @Test
  public void testCopyObject_WithValidKeys_CopiesSuccessfully() throws Exception {
    String srcKey = "logs/source.log";
    String destKey = "logs/dest.log";
    CopyObjectResponse response = CopyObjectResponse.builder().build();
    CompletableFuture<CopyObjectResponse> future = CompletableFuture.completedFuture(response);

    when(mockS3Client.copyObject(any(CopyObjectRequest.class))).thenReturn(future);

    try (MockedStatic<CompletableFutureUtils> mockedUtils =
        Mockito.mockStatic(CompletableFutureUtils.class)) {
      mockedUtils
          .when(() -> CompletableFutureUtils.toSingle(any(CompletableFuture.class)))
          .thenAnswer(
              invocation -> {
                CompletableFuture<CopyObjectResponse> cf = invocation.getArgument(0);
                return Single.fromFuture(cf);
              });

      Completable result = S3Utils.copyObject(mockS3Client, mockS3Config, srcKey, destKey);

      result.blockingAwait();
      verify(mockS3Client, times(1)).copyObject(any(CopyObjectRequest.class));
    }
  }

  @Test
  public void testReadFileContent_WithValidKey_ReturnsContent() throws Exception {
    String objectKey = "logs/test.log";
    String content = "test content";
    GetObjectResponse response = GetObjectResponse.builder().build();
    software.amazon.awssdk.core.ResponseBytes<GetObjectResponse> responseBytes =
        software.amazon.awssdk.core.ResponseBytes.fromByteArray(response, content.getBytes());
    CompletableFuture<software.amazon.awssdk.core.ResponseBytes<GetObjectResponse>> future =
        CompletableFuture.completedFuture(responseBytes);

    when(mockS3Client.getObject(any(GetObjectRequest.class), any(AsyncResponseTransformer.class)))
        .thenReturn(future);

    try (MockedStatic<CompletableFutureUtils> mockedUtils =
        Mockito.mockStatic(CompletableFutureUtils.class)) {
      mockedUtils
          .when(() -> CompletableFutureUtils.toSingle(any(CompletableFuture.class)))
          .thenAnswer(
              invocation -> {
                CompletableFuture<software.amazon.awssdk.core.ResponseBytes<GetObjectResponse>> cf =
                    invocation.getArgument(0);
                return Single.fromFuture(cf);
              });

      Single<String> result = S3Utils.readFileContent(mockS3Client, mockS3Config, objectKey);

      String fileContent = result.blockingGet();
      Assert.assertNotNull(fileContent);
      Assert.assertEquals(fileContent, content);
    }
  }

  @Test
  public void testReadFileContent_WithError_PropagatesError() {
    String objectKey = "logs/test.log";
    RuntimeException error = new RuntimeException("S3 error");
    CompletableFuture<software.amazon.awssdk.core.ResponseBytes<GetObjectResponse>> future =
        CompletableFuture.failedFuture(error);

    when(mockS3Client.getObject(any(GetObjectRequest.class), any(AsyncResponseTransformer.class)))
        .thenReturn(future);

    try (MockedStatic<CompletableFutureUtils> mockedUtils =
        Mockito.mockStatic(CompletableFutureUtils.class)) {
      mockedUtils
          .when(() -> CompletableFutureUtils.toSingle(any(CompletableFuture.class)))
          .thenAnswer(
              invocation -> {
                CompletableFuture<software.amazon.awssdk.core.ResponseBytes<GetObjectResponse>> cf =
                    invocation.getArgument(0);
                return Single.fromFuture(cf);
              });

      Single<String> result = S3Utils.readFileContent(mockS3Client, mockS3Config, objectKey);

      try {
        result.blockingGet();
        Assert.fail("Should have thrown exception");
      } catch (Exception e) {
        Assert.assertNotNull(e);
      }
    }
  }
}
