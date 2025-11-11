package com.dream11.logcentralorchestrator.rest;

import com.dream11.logcentralorchestrator.common.app.AppContext;
import com.dream11.logcentralorchestrator.common.entity.VertxEntity;
import com.dream11.logcentralorchestrator.rest.request.Sorting;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.lang.annotation.Annotation;
import java.util.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;

@Slf4j
public class RestUtil {

  private static final List<Class> collectionClasses =
      Arrays.asList(
          List.class,
          Set.class,
          ArrayList.class,
          HashSet.class,
          LinkedHashSet.class,
          LinkedList.class);
  private static Reflections ref;

  public static List<Class<?>> annotatedClasses(
      Class<? extends Annotation> annotation, List<String> packageNames) {
    List<Class<?>> annotatedClasses = new ArrayList<>();
    packageNames.forEach(
        packageName -> {
          try {
            setRef(packageName);
            annotatedClasses.addAll(new ArrayList<>(ref.getTypesAnnotatedWith(annotation)));
          } catch (Exception e) {
            log.error(
                "Failed to get classes with annotation {} from package {}",
                annotation,
                packageName,
                e);
          }
        });

    return annotatedClasses;
  }

  public static List<AbstractRoute> abstractRouteList(List<String> packageNames) {
    List<AbstractRoute> routes = new ArrayList<>();
    List<Class<?>> classes = RestUtil.annotatedClasses(Route.class, packageNames);
    if (classes != null && !classes.isEmpty()) {
      classes.forEach(
          clazz -> {
            try {
              Route routeAnnotation = clazz.getAnnotation(Route.class);
              if (routeAnnotation != null) {
                AbstractRoute route = (AbstractRoute) AppContext.getInstance(clazz);
                route.setPath(routeAnnotation.path());
                route.setHttpMethod(HttpMethod.valueOf(routeAnnotation.httpMethod().toString()));
                route.setProduces(routeAnnotation.produces());
                route.setConsumes(routeAnnotation.consumes());
                route.setRequiredHeaders(Arrays.asList(routeAnnotation.requiredHeaders()));
                route.setRequiredBodyParams(Arrays.asList(routeAnnotation.requiredBodyParams()));
                route.setRequiredQueryParams(Arrays.asList(routeAnnotation.requiredQueryParams()));
                route.setTimeout(routeAnnotation.timeout());
                routes.add(route);
              }
            } catch (Exception e) {
              log.error("Failed to initialize route", e);
            }
          });
    }
    return routes;
  }

  private static synchronized void setRef(String packageName) {
    ref = new Reflections(packageName);
  }

  public static List<Sorting> toSortingIterator(String sort) {
    Objects.requireNonNull(sort);
    return Arrays.stream(sort.split(","))
        .map(
            sortKey -> {
              if (sortKey.charAt(0) == '-') {
                return Sorting.of(sortKey.substring(1), "DESC");
              }
              return Sorting.of(sortKey, "ASC");
            })
        .collect(Collectors.toList());
  }

  public static String toSortingString(String sort) {
    Objects.requireNonNull(sort);
    return Arrays.stream(sort.split(","))
        .map(
            sortKey -> {
              if (sortKey.charAt(0) == '-') {
                return Sorting.of(sortKey.substring(1), "DESC");
              }
              return Sorting.of(sortKey, "ASC");
            })
        .map(Sorting::toString)
        .collect(Collectors.joining(", "));
  }

  public static String getString(Object object) throws JsonProcessingException {
    ObjectMapper objectMapper = AppContext.getInstance(ObjectMapper.class);
    String str;
    if (object instanceof String) {
      str = (String) object;
    } else if (object instanceof JsonObject) {
      str = String.valueOf(object);
    } else if (collectionClasses.contains(object.getClass())) {
      str = new JsonArray(new ArrayList((Collection) object)).toString();
    } else if (object instanceof VertxEntity) {
      str = ((VertxEntity) object).toJson().toString();
    } else {
      str = objectMapper.writeValueAsString(object);
    }
    return str;
  }
}
