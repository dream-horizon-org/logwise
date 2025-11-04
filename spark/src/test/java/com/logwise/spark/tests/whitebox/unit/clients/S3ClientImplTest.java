package com.logwise.spark.tests.whitebox.unit.clients;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.logwise.clients.impl.S3ClientImpl;
import com.logwise.spark.constants.Groups;
import com.logwise.tests.utils.AssertionUtils;
import com.logwise.spark.utils.TestUtils;
import java.io.ByteArrayInputStream;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

@Test(groups = {Groups.TEST_RUN_TYPE_WHITEBOX})
@Listeners(MockitoTestNGListener.class)
public class S3ClientImplTest {
  private static final String AMAZON_S3_FIELD_NAME = "s3Client";
  @Mock private AmazonS3 s3Client;

  @Test(
      description = "Test connection when S3 client is null",
      groups = {Groups.TEST_TYPE_UNIT, Groups.PURPOSE_POSITIVE_TESTS})
  public void testConnectionWhenS3ClientIsNull() {
    // Act
    S3ClientImpl s3ClientImpl = new S3ClientImpl();
    s3ClientImpl.connect();
    AmazonS3 s3Client = (AmazonS3) TestUtils.getPrivateField(s3ClientImpl, AMAZON_S3_FIELD_NAME);

    // Assert
    Assert.assertNotNull(s3Client, "S3Client is not set");
  }

  @Test(
      description = "Test connection when S3 client is not null",
      groups = {Groups.TEST_TYPE_UNIT, Groups.PURPOSE_POSITIVE_TESTS})
  public void testConnectionWhenS3ClientIsNotNull() {
    // Act
    S3ClientImpl s3ClientImpl = new S3ClientImpl();
    s3ClientImpl.connect();
    AmazonS3 s3ClientBefore =
        (AmazonS3) TestUtils.getPrivateField(s3ClientImpl, AMAZON_S3_FIELD_NAME);
    s3ClientImpl.connect();
    AmazonS3 s3ClientAfter =
        (AmazonS3) TestUtils.getPrivateField(s3ClientImpl, AMAZON_S3_FIELD_NAME);

    // Assert
    AssertionUtils.assertEquals(s3ClientBefore, s3ClientAfter, "S3Client before and after connect");
  }

  @Test(
      description = "Test putObject",
      groups = {Groups.TEST_TYPE_UNIT, Groups.PURPOSE_POSITIVE_TESTS})
  public void testGetObject() {
    // Mock
    String data = "testData";
    S3Object s3Object = new S3Object();
    s3Object.setObjectContent(
        new S3ObjectInputStream(new ByteArrayInputStream(data.getBytes()), null));
    when(s3Client.getObject("testBucket", "testObjectKey")).thenReturn(s3Object);

    // Act
    S3ClientImpl s3ClientImpl = new S3ClientImpl();
    TestUtils.setPrivateField(s3ClientImpl, AMAZON_S3_FIELD_NAME, s3Client);
    String object = s3ClientImpl.getObject("testBucket", "testObjectKey");

    // Assert
    Assert.assertEquals(object, data, "Object data mismatch");
  }

  @Test(
      description = "Test putObject",
      groups = {Groups.TEST_TYPE_UNIT, Groups.PURPOSE_POSITIVE_TESTS})
  public void testPutObject() {
    // Mock
    String data = "testData";
    when(s3Client.putObject(eq("testBucket"), eq("testObjectKey"), any(), any())).thenReturn(null);

    // Act
    S3ClientImpl s3ClientImpl = new S3ClientImpl();
    TestUtils.setPrivateField(s3ClientImpl, AMAZON_S3_FIELD_NAME, s3Client);
    s3ClientImpl.putObject("testBucket", "testObjectKey", data);

    // Assert
    verify(s3Client, times(1)).putObject(eq("testBucket"), eq("testObjectKey"), any(), any());
  }
}
