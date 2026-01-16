package com.logwise.spark.feign.decoders;

import com.logwise.spark.feign.exceptions.ClientErrorException;
import com.logwise.spark.feign.exceptions.FeignClientException;
import com.logwise.spark.feign.exceptions.ServerErrorException;
import feign.Response;
import feign.codec.ErrorDecoder;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class DefaultErrorDecoder implements ErrorDecoder {

  private static final List<Integer> CLIENT_ERROR_STATUSES = Arrays.asList(400, 401, 403, 404);
  private static final List<Integer> SERVER_ERROR_STATUSES = Arrays.asList(500, 501, 502, 503, 504);

  @Override
  public Exception decode(String methodKey, Response response) {
    int status = response.status();
    String reason = response.reason();
    String body = Objects.toString(response.body(), "");
    if (CLIENT_ERROR_STATUSES.contains(status)) {
      return new ClientErrorException(status, body, reason);
    }

    if (SERVER_ERROR_STATUSES.contains(status)) {
      return new ServerErrorException(status, body, reason);
    }

    return new FeignClientException(status, body, reason);
  }
}
