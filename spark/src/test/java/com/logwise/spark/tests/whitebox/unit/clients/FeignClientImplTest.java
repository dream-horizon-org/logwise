package com.logwise.spark.tests.whitebox.unit.clients;

import com.logwise.spark.clients.impl.FeignClientImpl;
import com.logwise.spark.constants.Groups;
import com.logwise.tests.utils.AssertionUtils;
import feign.Param;
import feign.RequestLine;
import org.testng.annotations.Test;

@Test(groups = {Groups.TEST_RUN_TYPE_WHITEBOX})
public class FeignClientImplTest {

  @Test(
      description = "Test create client",
      groups = {Groups.TEST_TYPE_UNIT, Groups.PURPOSE_POSITIVE_TESTS})
  public void testCreateClient() {
    // Mock
    FeignClientImpl feignClientImpl = new FeignClientImpl();

    // Act
    TestFeignClient testFeignClient =
        feignClientImpl.createClient(TestFeignClient.class, "http://localhost:8080");

    // Assert
    AssertionUtils.assertTrue(testFeignClient != null, "Client should not be null");
  }

  private interface TestFeignClient {
    @RequestLine("GET /test")
    String getTest(@Param("test") String test);
  }
}
