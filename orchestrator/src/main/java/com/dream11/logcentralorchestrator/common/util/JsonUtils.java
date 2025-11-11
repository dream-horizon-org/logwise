package com.dream11.logcentralorchestrator.common.util;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public final class JsonUtils {

  public static JsonObject jsonFrom(Map<String, Object> map) {
    return new JsonObject(MapUtils.map(JsonUtils::jsonFrom, map));
  }

  public static JsonObject jsonFrom(String key, List<Object> list) {
    JsonArray jsonArray = new JsonArray(list);
    return new JsonObject().put(key, jsonArray);
  }

  public static JsonObject jsonFrom(String key, Object[] list) {
    return JsonUtils.jsonFrom(key, Arrays.asList(list));
  }

  public static JsonObject jsonFrom(List<Object> list) {
    return JsonUtils.jsonFrom("values", list);
  }

  public static JsonObject jsonFrom(Object[] list) {
    return JsonUtils.jsonFrom(Arrays.asList(list));
  }

  public static JsonObject jsonFrom(Object obj) {
    return JsonObject.mapFrom(obj);
  }

  public static JsonObject jsonFrom(String key, JsonObject value) {
    return new JsonObject().put(key, value);
  }

  public static JsonObject jsonFrom(String key, String value) {
    return new JsonObject().put(key, value);
  }

  public static JsonObject jsonMerge(List<JsonObject> objs) {
    return CollectionUtils.reduce(new JsonObject(), JsonObject::mergeIn, objs);
  }

  public static JsonObject jsonMerge(Object[] objs) {
    return jsonMerge(ListUtils.map(JsonUtils::jsonFrom, Arrays.asList(objs)));
  }

  // Cast the result into the expected type
  public static Object getValueFromNestedJson(JsonObject json, String flattenedKey) {
    JsonObject cur = json;
    List<String> keys = Arrays.asList(flattenedKey.split("\\."));
    for (String key : keys.subList(0, keys.size() - 1)) {
      if (!cur.containsKey(key)) {
        return null;
      }
      cur = cur.getJsonObject(key);
    }
    return cur.containsKey(keys.get(keys.size() - 1))
        ? cur.getValue(keys.get(keys.size() - 1))
        : null;
  }
}
