package com.logwise.spark.clients;

public interface FeignClient {
  <T> T createClient(Class<T> targetClass, String url);
}
