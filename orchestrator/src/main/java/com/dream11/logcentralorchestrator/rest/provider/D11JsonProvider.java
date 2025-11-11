package com.dream11.logcentralorchestrator.rest.provider;

import com.dream11.logcentralorchestrator.common.app.AppContext;
import com.dream11.logcentralorchestrator.rest.TypeValidationError;
import com.dream11.logcentralorchestrator.rest.exception.RestException;
import com.dream11.logcentralorchestrator.rest.io.Error;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;
import lombok.SneakyThrows;
import org.jboss.resteasy.plugins.providers.jackson.ResteasyJackson2Provider;

@Provider
@Consumes({"application/json", "application/*+json", "text/json"})
@Produces({"application/json", "application/*+json", "text/json"})
public class D11JsonProvider extends ResteasyJackson2Provider {

  public D11JsonProvider() {
    this.setMapper(AppContext.getInstance(ObjectMapper.class));
  }

  @SneakyThrows
  @Override
  public Object readFrom(
      Class<Object> type,
      Type genericType,
      Annotation[] annotations,
      MediaType mediaType,
      MultivaluedMap<String, String> httpHeaders,
      InputStream entityStream)
      throws IOException {
    try {
      return super.readFrom(type, genericType, annotations, mediaType, httpHeaders, entityStream);
    } catch (JsonMappingException e) {
      List<JsonMappingException.Reference> referenceList = e.getPath();
      if (!referenceList.isEmpty()) {
        String fieldName = referenceList.get(0).getFieldName();
        if (fieldName != null) {
          TypeValidationError typeValidationError =
              type.getDeclaredField(fieldName).getAnnotation(TypeValidationError.class);
          if (typeValidationError != null) {
            throw new RestException(
                e,
                Error.of(typeValidationError.code(), typeValidationError.message()),
                typeValidationError.httpStatusCode());
          }
        }
      }
      throw new RestException(e, Error.of("INVALID_REQUEST", e.getMessage()));
    } catch (IOException e) {
      throw new RestException(e, Error.of("INVALID_REQUEST", e.getMessage()));
    }
  }
}
