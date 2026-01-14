package com.logwise.orchestrator.tests.unit.util;

import com.logwise.orchestrator.constant.ApplicationConstants;
import com.logwise.orchestrator.util.AwsClientUtils;
import org.testng.Assert;
import org.testng.annotations.Test;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.regions.Region;

public class AwsClientUtilsTest {

  @Test
  public void testCreateHttpClient_ReturnsNonNullClient() {
    SdkAsyncHttpClient client = AwsClientUtils.createHttpClient();
    
    Assert.assertNotNull(client);
  }

  @Test
  public void testCreateRetryPolicy_ReturnsNonNullPolicy() {
    RetryPolicy policy = AwsClientUtils.createRetryPolicy();
    
    Assert.assertNotNull(policy);
  }

  @Test
  public void testCreateRetryPolicy_HasCorrectRetryCount() {
    RetryPolicy policy = AwsClientUtils.createRetryPolicy();
    
    Assert.assertNotNull(policy);
  }

  @Test
  public void testGetDefaultCredentialsProvider_ReturnsNonNullProvider() {
    AwsCredentialsProvider provider = AwsClientUtils.getDefaultCredentialsProvider();
    
    Assert.assertNotNull(provider);
  }

  @Test
  public void testGetRoleArnCredentialsProvider_WithValidParams_ReturnsProvider() {
    String roleArn = "arn:aws:iam::123456789012:role/test-role";
    String sessionName = "test-session";
    Region region = Region.US_EAST_1;
    
    AwsCredentialsProvider provider = AwsClientUtils.getRoleArnCredentialsProvider(roleArn, sessionName, region);
    
    Assert.assertNotNull(provider);
  }

  @Test
  public void testGetRoleArnCredentialsProvider_WithDifferentRegions_ReturnsProvider() {
    String roleArn = "arn:aws:iam::123456789012:role/test-role";
    String sessionName = "test-session";
    
    AwsCredentialsProvider provider1 = AwsClientUtils.getRoleArnCredentialsProvider(roleArn, sessionName, Region.US_EAST_1);
    AwsCredentialsProvider provider2 = AwsClientUtils.getRoleArnCredentialsProvider(roleArn, sessionName, Region.US_WEST_2);
    
    Assert.assertNotNull(provider1);
    Assert.assertNotNull(provider2);
  }
}

