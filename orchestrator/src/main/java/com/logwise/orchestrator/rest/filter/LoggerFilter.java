package com.logwise.orchestrator.rest.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.logwise.orchestrator.rest.RestUtil;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

@Slf4j
@Provider
public class LoggerFilter implements ContainerResponseFilter, ContainerRequestFilter {

  private static final String REQUEST_START_TIME = "REQUEST_START_TIME";

  @Override
  public void filter(
      ContainerRequestContext containerRequestContext,
      ContainerResponseContext containerResponseContext)
      throws JsonProcessingException {
    if (containerResponseContext.hasEntity()) {
      containerResponseContext.setEntity(RestUtil.getString(containerResponseContext.getEntity()));
    }
    if (containerRequestContext.getProperty(REQUEST_START_TIME) != null) {
      log.info(
          "[RESPONSE TIME] Time taken for route: {} {} : {}ms, Status code : {}",
          containerRequestContext.getMethod(),
          containerRequestContext.getUriInfo().getPath(),
          System.currentTimeMillis()
              - (Long) containerRequestContext.getProperty(REQUEST_START_TIME),
          containerResponseContext.getStatus());
      containerRequestContext.removeProperty(REQUEST_START_TIME);
    }
  }

  @Override
  public void filter(ContainerRequestContext containerRequestContext) throws IOException {
    containerRequestContext.setProperty(REQUEST_START_TIME, System.currentTimeMillis());

    log.info(
        "STATED REQUEST : {} {}",
        containerRequestContext.getMethod(),
        containerRequestContext.getUriInfo().getPath());
    String body = "{}";
    if (containerRequestContext.hasEntity() && containerRequestContext.getEntityStream() != null) {
      body =
          IOUtils.toString(containerRequestContext.getEntityStream(), StandardCharsets.UTF_8)
              .replace("\n", " ");
      containerRequestContext.getEntityStream().reset();
    }
    log.info(
        "Path: {} {}  Request: Body : {}, Headers : {}, PathParams : {}, QueryParams : {}",
        containerRequestContext.getMethod(),
        containerRequestContext.getUriInfo().getPath(),
        body,
        containerRequestContext.getHeaders(),
        containerRequestContext.getUriInfo().getPathParameters(),
        containerRequestContext.getUriInfo().getQueryParameters());
  }
}
