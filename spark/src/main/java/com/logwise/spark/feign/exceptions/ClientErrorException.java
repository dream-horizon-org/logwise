package com.logwise.spark.feign.exceptions;

public class ClientErrorException extends RuntimeException {
  public ClientErrorException(int status, String body, String reason) {
    super(String.format("Client Error! Status: %d Body: %s Reason: %s", status, body, reason));
  }
}
