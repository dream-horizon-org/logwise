package com.dream11.logcentralorchestrator.rest.converter;

import com.dream11.logcentralorchestrator.rest.TypeValidationError;
import com.dream11.logcentralorchestrator.rest.exception.RestException;
import com.dream11.logcentralorchestrator.rest.io.Error;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;
import lombok.Getter;

public class BaseParamConverter {

  @Getter private Error error;
  @Getter private int httpStatusCode;

  public BaseParamConverter(Annotation[] annotations) {
    Optional<TypeValidationError> optionalAnnotation =
        Arrays.stream(annotations)
            .filter(annotation -> annotation.annotationType() == TypeValidationError.class)
            .map(TypeValidationError.class::cast)
            .findAny();
    optionalAnnotation.ifPresent(
        typeValidationError -> {
          this.error = Error.of(typeValidationError.code(), typeValidationError.message());
          this.httpStatusCode = typeValidationError.httpStatusCode();
        });
  }

  protected <T> T parseParam(String s, Function<String, T> parseMethod) {
    try {
      return parseMethod.apply(s);
    } catch (Exception e) {
      if (this.getError() != null) {
        throw new RestException(e, this.getError(), this.getHttpStatusCode());
      } else {
        throw e;
      }
    }
  }
}
