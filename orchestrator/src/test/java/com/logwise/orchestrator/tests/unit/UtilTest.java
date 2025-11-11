package com.logwise.orchestrator.tests.unit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.logwise.orchestrator.common.app.AppContext;
import com.logwise.orchestrator.common.util.CompletableFutureUtils;
import com.logwise.orchestrator.config.ApplicationConfig;
import com.logwise.orchestrator.dto.entity.ServiceDetails;
import com.logwise.orchestrator.rest.io.Response;
import com.logwise.orchestrator.setup.BaseTest;
import com.logwise.orchestrator.testconfig.ApplicationTestConfig;
import com.logwise.orchestrator.util.ApplicationUtils;
import com.logwise.orchestrator.util.AwsClientUtils;
import com.logwise.orchestrator.util.Encryption;
import com.logwise.orchestrator.util.S3Utils;
import com.logwise.orchestrator.util.TestResponseWrapper;
import com.logwise.orchestrator.util.WebClientUtils;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import me.escoffier.vertx.completablefuture.VertxCompletableFuture;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.*;

/**
 * Unit tests for util package (S3Utils, ResponseWrapper, AwsClientUtils, WebClientUtils,
 * Encryption, ApplicationUtils).
 */
public class UtilTest extends BaseTest {

  private S3AsyncClient mockS3Client;
  private ApplicationConfig.S3Config s3Config;

  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    mockS3Client = mock(S3AsyncClient.class);
    s3Config = ApplicationTestConfig.createMockS3Config();
    BaseTest.getReactiveVertx().getOrCreateContext();
  }

  @AfterClass
  public static void tearDownClass() {
    BaseTest.cleanup();
  }

  @Test
  public void testS3Utils_ListCommonPrefix_WithValidInputs_ReturnsPrefixList() throws Exception {

    String prefix = "logs/env=";
    String delimiter = "/";

    ListObjectsV2Response response =
        ListObjectsV2Response.builder()
            .commonPrefixes(
                Arrays.asList(
                    CommonPrefix.builder().prefix("logs/env=prod/").build(),
                    CommonPrefix.builder().prefix("logs/env=staging/").build()))
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
          S3Utils.listCommonPrefix(mockS3Client, s3Config, prefix, delimiter);
      List<String> prefixes = result.blockingGet();

      Assert.assertNotNull(prefixes);
      Assert.assertEquals(prefixes.size(), 2);
      Assert.assertTrue(prefixes.contains("logs/env=prod/"));
      Assert.assertTrue(prefixes.contains("logs/env=staging/"));
      verify(mockS3Client, times(1)).listObjectsV2(any(ListObjectsV2Request.class));
    }
  }

  @Test
  public void testS3Utils_ListCommonPrefix_WithNoPrefixes_ReturnsEmptyList() throws Exception {

    String prefix = "logs/env=";
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
          S3Utils.listCommonPrefix(mockS3Client, s3Config, prefix, delimiter);
      List<String> prefixes = result.blockingGet();

      Assert.assertNotNull(prefixes);
      Assert.assertTrue(prefixes.isEmpty());
    }
  }

  @Test
  public void testS3Utils_ListCommonPrefix_WithError_PropagatesError() {

    String prefix = "logs/env=";
    String delimiter = "/";
    RuntimeException error = new RuntimeException("S3 error");

    CompletableFuture<ListObjectsV2Response> future = new CompletableFuture<>();
    future.completeExceptionally(error);
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
          S3Utils.listCommonPrefix(mockS3Client, s3Config, prefix, delimiter);

      try {
        result.blockingGet();
        Assert.fail("Should have thrown exception");
      } catch (RuntimeException e) {
        Assert.assertNotNull(e);
      }
    }
  }

  @Test
  public void testS3Utils_DeleteFile_WithValidObjectKey_CompletesSuccessfully() throws Exception {

    String objectKey = "logs/file.log";
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

      Completable result = S3Utils.deleteFile(mockS3Client, s3Config, objectKey);
      result.blockingAwait();

      verify(mockS3Client, times(1)).deleteObject(any(DeleteObjectRequest.class));
    }
  }

  @Test
  public void testS3Utils_DeleteFile_WithError_PropagatesError() {

    String objectKey = "logs/file.log";
    RuntimeException error = new RuntimeException("Delete error");
    CompletableFuture<DeleteObjectResponse> future = new CompletableFuture<>();
    future.completeExceptionally(error);
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

      Completable result = S3Utils.deleteFile(mockS3Client, s3Config, objectKey);

      try {
        result.blockingAwait();
        Assert.fail("Should have thrown exception");
      } catch (RuntimeException e) {
        Assert.assertNotNull(e);
      }
    }
  }

  @Test
  public void testS3Utils_CopyObject_WithValidKeys_CompletesSuccessfully() throws Exception {

    String srcObjectKey = "logs/src.log";
    String destObjectKey = "logs/dest.log";
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

      Completable result = S3Utils.copyObject(mockS3Client, s3Config, srcObjectKey, destObjectKey);
      result.blockingAwait();

      verify(mockS3Client, times(1)).copyObject(any(CopyObjectRequest.class));
    }
  }

  @Test
  public void testS3Utils_CopyObject_WithError_PropagatesError() {

    String srcObjectKey = "logs/src.log";
    String destObjectKey = "logs/dest.log";
    RuntimeException error = new RuntimeException("Copy error");
    CompletableFuture<CopyObjectResponse> future = new CompletableFuture<>();
    future.completeExceptionally(error);
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

      Completable result = S3Utils.copyObject(mockS3Client, s3Config, srcObjectKey, destObjectKey);

      try {
        result.blockingAwait();
        Assert.fail("Should have thrown exception");
      } catch (RuntimeException e) {
        Assert.assertNotNull(e);
      }
    }
  }

  @Test
  public void testResponseWrapper_FromMaybe_WithValue_ReturnsSuccessfulResponse() throws Exception {

    Maybe<String> source = Maybe.just("test-value");
    String defaultValue = "default";
    int httpStatusCode = 200;

    VertxCompletableFuture<Response<String>> future =
        TestResponseWrapper.fromMaybe(source, defaultValue, httpStatusCode);
    Response<String> response = future.get();

    Assert.assertNotNull(response);
    Assert.assertEquals(response.getData(), "test-value");
    Assert.assertEquals(response.getHttpStatusCode(), 200);
    Assert.assertNull(response.getError());
  }

  @Test
  public void testResponseWrapper_FromMaybe_WithEmpty_ReturnsDefaultValue() throws Exception {

    Maybe<String> source = Maybe.empty();
    String defaultValue = "default-value";
    int httpStatusCode = 201;

    VertxCompletableFuture<Response<String>> future =
        TestResponseWrapper.fromMaybe(source, defaultValue, httpStatusCode);
    Response<String> response = future.get();

    Assert.assertNotNull(response);
    Assert.assertEquals(response.getData(), defaultValue);
    Assert.assertEquals(response.getHttpStatusCode(), 201);
    Assert.assertNull(response.getError());
  }

  @Test
  public void testResponseWrapper_FromMaybe_WithError_CompletesExceptionally() throws Exception {

    RuntimeException error = new RuntimeException("Test error");
    Maybe<String> source = Maybe.error(error);
    String defaultValue = "default";
    int httpStatusCode = 200;

    VertxCompletableFuture<Response<String>> future =
        TestResponseWrapper.fromMaybe(source, defaultValue, httpStatusCode);

    try {
      future.get();
      Assert.fail("Should have thrown exception");
    } catch (Exception e) {
      Assert.assertTrue(e.getCause() instanceof RuntimeException);
      Assert.assertEquals(e.getCause().getMessage(), "Test error");
    }
  }

  @Test
  public void testResponseWrapper_FromSingle_WithValue_ReturnsSuccessfulResponse()
      throws Exception {

    Single<String> source = Single.just("success-value");
    int httpStatusCode = 200;

    VertxCompletableFuture<Response<String>> future =
        TestResponseWrapper.fromSingle(source, httpStatusCode);
    Response<String> response = future.get();

    Assert.assertNotNull(response);
    Assert.assertEquals(response.getData(), "success-value");
    Assert.assertEquals(response.getHttpStatusCode(), 200);
    Assert.assertNull(response.getError());
  }

  @Test
  public void testResponseWrapper_FromSingle_WithError_CompletesExceptionally() throws Exception {

    RuntimeException error = new RuntimeException("Single error");
    Single<String> source = Single.error(error);
    int httpStatusCode = 200;

    VertxCompletableFuture<Response<String>> future =
        TestResponseWrapper.fromSingle(source, httpStatusCode);

    try {
      future.get();
      Assert.fail("Should have thrown exception");
    } catch (Exception e) {
      Assert.assertTrue(e.getCause() instanceof RuntimeException);
      Assert.assertEquals(e.getCause().getMessage(), "Single error");
    }
  }

  @Test
  public void testResponseWrapper_FromSingle_WithCustomStatusCode_SetsStatusCode()
      throws Exception {

    Single<Integer> source = Single.just(42);
    int httpStatusCode = 201;

    VertxCompletableFuture<Response<Integer>> future =
        TestResponseWrapper.fromSingle(source, httpStatusCode);
    Response<Integer> response = future.get();

    Assert.assertNotNull(response);
    Assert.assertEquals(response.getData(), Integer.valueOf(42));
    Assert.assertEquals(response.getHttpStatusCode(), 201);
  }

  @Test
  public void testAwsClientUtils_CreateHttpClient_ReturnsNonNull() {

    SdkAsyncHttpClient httpClient = AwsClientUtils.createHttpClient();

    Assert.assertNotNull(httpClient);
  }

  @Test
  public void testAwsClientUtils_CreateRetryPolicy_ReturnsNonNull() {

    RetryPolicy retryPolicy = AwsClientUtils.createRetryPolicy();

    Assert.assertNotNull(retryPolicy);
  }

  @Test
  public void testAwsClientUtils_GetDefaultCredentialsProvider_ReturnsNonNull() {

    AwsCredentialsProvider credentialsProvider = AwsClientUtils.getDefaultCredentialsProvider();

    Assert.assertNotNull(credentialsProvider);
  }

  @Test
  public void testAwsClientUtils_GetRoleArnCredentialsProvider_WithValidInputs_ReturnsNonNull() {

    String roleArn = "arn:aws:iam::123456789012:role/test-role";
    String sessionName = "test-session";
    Region region = Region.US_EAST_1;

    AwsCredentialsProvider credentialsProvider =
        AwsClientUtils.getRoleArnCredentialsProvider(roleArn, sessionName, region);

    Assert.assertNotNull(credentialsProvider);
  }

  @Test
  public void testWebClientUtils_RetryWithDelay_WithMaxAttempts_RetriesCorrectly()
      throws Exception {

    int delay = 10; // Small delay for faster test
    TimeUnit delayTimeUnit = TimeUnit.MILLISECONDS;
    int maxAttempts = 2;

    var retryFunction = WebClientUtils.retryWithDelay(delay, delayTimeUnit, maxAttempts);

    Flowable<Throwable> errors =
        Flowable.just(
            new RuntimeException("Error 1"),
            new RuntimeException("Error 2"),
            new RuntimeException("Error 3"));

    Flowable<?> result = retryFunction.apply(errors);

    try {
      result.blockingLast();
      Assert.fail("Should have thrown exception");
    } catch (Exception e) {

      Assert.assertTrue(
          e instanceof RuntimeException
              || (e.getCause() != null && e.getCause() instanceof RuntimeException));
    }
  }

  @Test
  public void testWebClientUtils_RetryWithDelay_WithZeroMaxAttempts_PropagatesErrorImmediately()
      throws Exception {

    int delay = 10;
    TimeUnit delayTimeUnit = TimeUnit.MILLISECONDS;
    int maxAttempts = 0;

    var retryFunction = WebClientUtils.retryWithDelay(delay, delayTimeUnit, maxAttempts);

    Flowable<Throwable> errors = Flowable.just(new RuntimeException("Error"));

    Flowable<?> result = retryFunction.apply(errors);

    try {
      result.blockingLast();
      Assert.fail("Should have thrown exception");
    } catch (Exception e) {

      Assert.assertTrue(
          e instanceof RuntimeException
              || (e.getCause() != null && e.getCause() instanceof RuntimeException));
    }
  }

  @Test
  public void testEncryption_ClassExists() {

    Assert.assertNotNull(Encryption.class);
  }

  @Test
  public void testEncryption_MethodsExist() throws Exception {

    Method[] methods = Encryption.class.getDeclaredMethods();
    boolean hasEncrypt = false;
    boolean hasDecrypt = false;

    for (Method method : methods) {
      if (method.getName().equals("encrypt") && method.getParameterCount() == 1) {
        hasEncrypt = true;
      }
      if (method.getName().equals("decrypt") && method.getParameterCount() == 1) {
        hasDecrypt = true;
      }
    }

    Assert.assertTrue(hasEncrypt, "encrypt method should exist");
    Assert.assertTrue(hasDecrypt, "decrypt method should exist");
  }

  @Test
  public void testApplicationUtils_GetServiceFromObjectKey_WithValidPath_ReturnsServiceDetails() {

    String logPath =
        "logs/env=prod/service_name=test-service/component_name=test-component/year=2024/";

    ServiceDetails result = ApplicationUtils.getServiceFromObjectKey(logPath);

    Assert.assertNotNull(result);
    Assert.assertEquals(result.getEnv(), "prod");
    Assert.assertEquals(result.getServiceName(), "test-service");
    Assert.assertEquals(result.getComponentName(), "test-component");
  }

  @Test
  public void testApplicationUtils_GetServiceFromObjectKey_WithInvalidPath_ReturnsNull() {

    String logPath = "invalid/path/without/required/pattern";

    ServiceDetails result = ApplicationUtils.getServiceFromObjectKey(logPath);

    Assert.assertNull(result);
  }

  @Test(expectedExceptions = NullPointerException.class)
  public void
      testApplicationUtils_GetServiceFromObjectKey_WithNullPath_ThrowsNullPointerException() {

    ApplicationUtils.getServiceFromObjectKey(null);
  }

  @Test
  public void testApplicationUtils_GetServiceFromObjectKey_WithEmptyPath_ReturnsNull() {

    ServiceDetails result = ApplicationUtils.getServiceFromObjectKey("");

    Assert.assertNull(result);
  }

  @Test
  public void
      testApplicationUtils_GetServiceFromObjectKey_WithPartialMatch_ReturnsServiceDetails() {

    String logPath = "prefix/env=staging/service_name=api/component_name=web/extra/path";

    ServiceDetails result = ApplicationUtils.getServiceFromObjectKey(logPath);

    Assert.assertNotNull(result);
    Assert.assertEquals(result.getEnv(), "staging");
    Assert.assertEquals(result.getServiceName(), "api");
    Assert.assertEquals(result.getComponentName(), "web");
  }

  @Test
  public void testApplicationUtils_ExecuteBlockingCallable_WithValidCallable_ReturnsMaybe() {

    Callable<String> callable = () -> "test result";

    try (MockedStatic<AppContext> mockedAppContext = Mockito.mockStatic(AppContext.class)) {
      io.vertx.reactivex.core.Vertx reactiveVertx = BaseTest.getReactiveVertx();

      mockedAppContext
          .when(() -> AppContext.getInstance(io.vertx.reactivex.core.Vertx.class))
          .thenReturn(reactiveVertx);

      Maybe<String> result = ApplicationUtils.executeBlockingCallable(callable);

      Assert.assertNotNull(result);
      String value = result.blockingGet();
      Assert.assertEquals(value, "test result");
    }
  }

  @Test
  public void testApplicationUtils_ExecuteBlockingCallable_WithException_ReturnsErrorMaybe() {

    Callable<String> callable =
        () -> {
          throw new RuntimeException("Test error");
        };

    try (MockedStatic<AppContext> mockedAppContext = Mockito.mockStatic(AppContext.class)) {
      io.vertx.reactivex.core.Vertx reactiveVertx = BaseTest.getReactiveVertx();

      mockedAppContext
          .when(() -> AppContext.getInstance(io.vertx.reactivex.core.Vertx.class))
          .thenReturn(reactiveVertx);

      Maybe<String> result = ApplicationUtils.executeBlockingCallable(callable);

      Assert.assertNotNull(result);
      try {
        result.blockingGet();
        Assert.fail("Should have thrown exception");
      } catch (RuntimeException e) {
        Assert.assertEquals(e.getMessage(), "Test error");
      }
    }
  }

  @Test
  public void testApplicationUtils_GetGuiceInstance_WithValidClassAndName_ReturnsInstance() {

    String testName = "testInstance";

    try (MockedStatic<AppContext> mockedAppContext = Mockito.mockStatic(AppContext.class)) {
      mockedAppContext
          .when(() -> AppContext.getInstance(String.class, testName))
          .thenReturn("test value");

      String result = ApplicationUtils.getGuiceInstance(String.class, testName);

      Assert.assertNotNull(result);
      Assert.assertEquals(result, "test value");
    }
  }

  @Test
  public void testApplicationUtils_GetGuiceInstance_WithConfigurationException_ReturnsNull() {

    String testName = "nonexistent";

    try (MockedStatic<AppContext> mockedAppContext = Mockito.mockStatic(AppContext.class)) {
      com.google.inject.ConfigurationException configException =
          new com.google.inject.ConfigurationException(Collections.emptyList());
      mockedAppContext
          .when(() -> AppContext.getInstance(String.class, testName))
          .thenThrow(configException);

      String result = ApplicationUtils.getGuiceInstance(String.class, testName);

      Assert.assertNull(result);
    }
  }

  @Test
  public void testApplicationUtils_GetGuiceInstance_WithOtherException_ReturnsNull() {

    String testName = "error";

    try (MockedStatic<AppContext> mockedAppContext = Mockito.mockStatic(AppContext.class)) {
      mockedAppContext
          .when(() -> AppContext.getInstance(String.class, testName))
          .thenThrow(new RuntimeException("Unexpected error"));

      String result = ApplicationUtils.getGuiceInstance(String.class, testName);

      Assert.assertNull(result);
    }
  }

  @Test
  public void testApplicationUtils_RowSetToMapList_WithValidRowSet_ReturnsMapList() {

    @SuppressWarnings("unchecked")
    io.vertx.reactivex.sqlclient.RowSet<io.vertx.reactivex.sqlclient.Row> rowSet =
        mock(io.vertx.reactivex.sqlclient.RowSet.class);
    io.vertx.reactivex.sqlclient.Row row1 = mock(io.vertx.reactivex.sqlclient.Row.class);
    io.vertx.reactivex.sqlclient.Row row2 = mock(io.vertx.reactivex.sqlclient.Row.class);

    when(rowSet.spliterator()).thenReturn(Arrays.asList(row1, row2).spliterator());

    when(row1.size()).thenReturn(2);
    when(row1.getColumnName(0)).thenReturn("col1");
    when(row1.getColumnName(1)).thenReturn("col2");
    when(row1.getValue(0)).thenReturn("value1");
    when(row1.getValue(1)).thenReturn(123);

    when(row2.size()).thenReturn(2);
    when(row2.getColumnName(0)).thenReturn("col1");
    when(row2.getColumnName(1)).thenReturn("col2");
    when(row2.getValue(0)).thenReturn("value2");
    when(row2.getValue(1)).thenReturn(456);

    List<Map<String, Object>> result = ApplicationUtils.rowSetToMapList(rowSet);

    Assert.assertNotNull(result);
    Assert.assertEquals(result.size(), 2);
    Assert.assertEquals(result.get(0).get("col1"), "value1");
    Assert.assertEquals(result.get(0).get("col2"), 123);
    Assert.assertEquals(result.get(1).get("col1"), "value2");
    Assert.assertEquals(result.get(1).get("col2"), 456);
  }

  @Test
  public void testApplicationUtils_RowSetToMapList_WithLocalDateTime_ConvertsToDate() {

    @SuppressWarnings("unchecked")
    io.vertx.reactivex.sqlclient.RowSet<io.vertx.reactivex.sqlclient.Row> rowSet =
        mock(io.vertx.reactivex.sqlclient.RowSet.class);
    io.vertx.reactivex.sqlclient.Row row = mock(io.vertx.reactivex.sqlclient.Row.class);
    LocalDateTime localDateTime = LocalDateTime.of(2024, 1, 1, 12, 0, 0);

    when(rowSet.spliterator()).thenReturn(Collections.singletonList(row).spliterator());

    when(row.size()).thenReturn(1);
    when(row.getColumnName(0)).thenReturn("timestamp");
    when(row.getValue(0)).thenReturn(localDateTime);
    when(row.getLocalDateTime(0)).thenReturn(localDateTime);

    List<Map<String, Object>> result = ApplicationUtils.rowSetToMapList(rowSet);

    Assert.assertNotNull(result);
    Assert.assertEquals(result.size(), 1);
    Object timestamp = result.get(0).get("timestamp");
    Assert.assertTrue(timestamp instanceof Date);
  }

  @Test
  public void testApplicationUtils_RowSetToMapList_WithEmptyRowSet_ReturnsEmptyList() {

    @SuppressWarnings("unchecked")
    io.vertx.reactivex.sqlclient.RowSet<io.vertx.reactivex.sqlclient.Row> rowSet =
        mock(io.vertx.reactivex.sqlclient.RowSet.class);
    @SuppressWarnings("unchecked")
    java.util.List<io.vertx.reactivex.sqlclient.Row> emptyRowList = Collections.emptyList();
    when(rowSet.spliterator()).thenReturn(emptyRowList.spliterator());

    List<Map<String, Object>> result = ApplicationUtils.rowSetToMapList(rowSet);

    Assert.assertNotNull(result);
    Assert.assertTrue(result.isEmpty());
  }
}
