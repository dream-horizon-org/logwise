package com.logwise.spark.dataproviders;

import com.logwise.spark.constants.TestConstants;
import com.logwise.tests.utils.DataProviderUtils;
import org.testng.annotations.DataProvider;

public class DefaultErrorDecoderTestDataProvider {
  private static final String DEFAULT_ERROR_DECODER_TEST_DATA_PATH =
      TestConstants.TEST_DATA_PATH.apply("defaultErrorDecoderTestData.json");
  private static final DataProviderUtils DATA_PROVIDER_UTILS = new DataProviderUtils();

  @DataProvider(name = "clientErrorStatuses")
  public static Object[][] clientErrorStatuses() {
    return DATA_PROVIDER_UTILS.getValueFromJson(
        DEFAULT_ERROR_DECODER_TEST_DATA_PATH, "clientErrorStatuses");
  }

  @DataProvider(name = "serverErrorStatuses")
  public static Object[][] serverErrorStatuses() {
    return DATA_PROVIDER_UTILS.getValueFromJson(
        DEFAULT_ERROR_DECODER_TEST_DATA_PATH, "serverErrorStatuses");
  }

  @DataProvider(name = "feignClientErrorStatuses")
  public static Object[][] feignClientErrorStatuses() {
    return DATA_PROVIDER_UTILS.getValueFromJson(
        DEFAULT_ERROR_DECODER_TEST_DATA_PATH, "feignClientErrorStatuses");
  }
}
