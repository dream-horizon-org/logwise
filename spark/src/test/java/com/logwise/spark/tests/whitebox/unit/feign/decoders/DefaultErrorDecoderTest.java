package com.logwise.spark.tests.whitebox.unit.feign.decoders;

import com.logwise.spark.constants.Groups;
import com.logwise.spark.dataproviders.DefaultErrorDecoderTestDataProvider;
import com.logwise.spark.feign.decoders.DefaultErrorDecoder;
import com.logwise.spark.feign.exceptions.ClientErrorException;
import com.logwise.spark.feign.exceptions.FeignClientException;
import com.logwise.spark.feign.exceptions.ServerErrorException;
import com.logwise.tests.utils.AssertionUtils;
import com.google.gson.JsonObject;
import feign.Request;
import feign.Response;
import java.util.LinkedHashMap;
import org.testng.annotations.Test;

@Test(
    groups = {Groups.TEST_RUN_TYPE_WHITEBOX},
    dataProviderClass = DefaultErrorDecoderTestDataProvider.class)
public class DefaultErrorDecoderTest {

  @Test(
      description = "Test decode method for client error",
      groups = {Groups.TEST_TYPE_UNIT, Groups.PURPOSE_POSITIVE_TESTS},
      dataProvider = "clientErrorStatuses")
  public void testDecodeForClientError(JsonObject jsonObject) {
    int status = jsonObject.get("status").getAsInt();
    Exception expectedException = getDecodeExceptionResponse(status);
    AssertionUtils.assertTrue(
        expectedException instanceof ClientErrorException,
        "Expected exception is instance of ClientErrorException");
  }

  @Test(
      description = "Test decode method for server error",
      groups = {Groups.TEST_TYPE_UNIT, Groups.PURPOSE_POSITIVE_TESTS},
      dataProvider = "serverErrorStatuses")
  public void testDecodeForServerError(JsonObject jsonObject) {
    int status = jsonObject.get("status").getAsInt();
    Exception expectedException = getDecodeExceptionResponse(status);
    AssertionUtils.assertTrue(
        expectedException instanceof ServerErrorException,
        "Expected exception is instance of ServerErrorException");
  }

  @Test(
      description = "Test decode method for feign error",
      groups = {Groups.TEST_TYPE_UNIT, Groups.PURPOSE_NEGATIVE_TESTS},
      dataProvider = "feignClientErrorStatuses")
  public void testDecodeForFeignError(JsonObject jsonObject) {
    int status = jsonObject.get("status").getAsInt();
    Exception expectedException = getDecodeExceptionResponse(status);
    AssertionUtils.assertTrue(
        expectedException instanceof FeignClientException,
        "Expected exception is instance of FeignClientException");
  }

  private Exception getDecodeExceptionResponse(int status) {
    Response response =
        Response.builder()
            .status(status)
            .request(
                Request.create(
                    Request.HttpMethod.GET,
                    "http://localhost",
                    new LinkedHashMap<>(),
                    "test".getBytes(),
                    null,
                    null))
            .reason("test")
            .build();
    DefaultErrorDecoder defaultErrorDecoder = new DefaultErrorDecoder();
    return defaultErrorDecoder.decode("get", response);
  }
}
