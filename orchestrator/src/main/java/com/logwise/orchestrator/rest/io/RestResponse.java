package com.logwise.orchestrator.rest.io;

import com.logwise.orchestrator.common.util.CompletableFutureUtils;
import com.logwise.orchestrator.rest.exception.RestError;
import com.logwise.orchestrator.rest.exception.RestException;
import io.reactivex.Single;
import io.reactivex.functions.Function;
import java.util.concurrent.CompletionStage;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

@Value
@Slf4j
public class RestResponse {

  private static Response error(Throwable throwable) {
    val restException = (RestException) parseThrowable(throwable);
    val httpStatusCode = restException.getHttpStatusCode();
    val error = restException.getError();
    val errorCode = error.getCode();
    val errorMessage = error.getMessage();

    log.error("Error: " + errorCode + " " + httpStatusCode + " " + errorMessage, throwable);
    return Response.errorResponse(error, httpStatusCode);
  }

  public static <T> Function<Single<T>, Single<Response<T>>> restHandler() {
    return single -> single.map(Response::successfulResponse).onErrorReturn(RestResponse::error);
  }

  public static <T> Function<Single<T>, Single<Response<T>>> restHandler(RestError restError) {
    return single ->
        single
            .onErrorResumeNext(err -> Single.error(new RestException(restError, err)))
            .to(restHandler());
  }

  public static <T> Function<Single<T>, CompletionStage<Response<T>>> jaxrsRestHandler() {
    return single ->
        single.map(Response::successfulResponse).to(CompletableFutureUtils::fromSingle);
  }

  /** Temporary method to handle common exceptions so they have a printable cause. */
  public static Throwable parseThrowable(Throwable throwable) {
    return throwable instanceof RestException
        ? throwable
        : new RestException(throwable, Error.of("UNKNOWN-EXCEPTION", throwable.toString()), 500);
  }

  public static Throwable parseThrowable(Throwable throwable, boolean oldErrorKeys) {
    Throwable error =
        throwable instanceof RestException
            ? throwable
            : new RestException(
                throwable, Error.of("UNKNOWN-EXCEPTION", throwable.toString()), 500);
    if (oldErrorKeys) {
      ((RestException) error).setOldErrorKeys(true);
    }
    return error;
  }
}
