package com.dream11.logcentralorchestrator.util;

import com.dream11.logcentralorchestrator.constant.ApplicationConstants;
import java.time.Duration;
import lombok.experimental.UtilityClass;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.core.retry.backoff.BackoffStrategy;
import software.amazon.awssdk.core.retry.backoff.EqualJitterBackoffStrategy;
import software.amazon.awssdk.core.retry.conditions.RetryCondition;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;

@UtilityClass
public class AwsClientUtils {

  public SdkAsyncHttpClient createHttpClient() {
    return NettyNioAsyncHttpClient.builder()
        .tcpKeepAlive(true)
        .maxConcurrency(ApplicationConstants.AWS_SDK_MAX_CONCURRENCY)
        .build();
  }

  public RetryPolicy createRetryPolicy() {
    return RetryPolicy.builder(RetryMode.STANDARD)
        .numRetries(ApplicationConstants.AWS_SDK_RETRIES)
        .retryCondition(RetryCondition.defaultRetryCondition())
        .throttlingBackoffStrategy(BackoffStrategy.defaultThrottlingStrategy(RetryMode.STANDARD))
        .backoffStrategy(createBackoffStrategy())
        .build();
  }

  public AwsCredentialsProvider getRoleArnCredentialsProvider(
      String roleArn, String sessionName, Region region) {
    return StsAssumeRoleCredentialsProvider.builder()
        .stsClient(
            StsClient.builder()
                .region(region)
                .credentialsProvider(getDefaultCredentialsProvider())
                .build())
        .refreshRequest(
            AssumeRoleRequest.builder().roleArn(roleArn).roleSessionName(sessionName).build())
        .build();
  }

  public AwsCredentialsProvider getDefaultCredentialsProvider() {
    return DefaultCredentialsProvider.create();
  }

  private BackoffStrategy createBackoffStrategy() {
    return EqualJitterBackoffStrategy.builder()
        .baseDelay(Duration.ofSeconds(ApplicationConstants.AWS_SDK_BASE_RETRY_DELAY_SECONDS))
        .maxBackoffTime(Duration.ofSeconds(ApplicationConstants.AWS_SDK_MAX_BACK_OFF_TIME_SECONDS))
        .build();
  }
}
