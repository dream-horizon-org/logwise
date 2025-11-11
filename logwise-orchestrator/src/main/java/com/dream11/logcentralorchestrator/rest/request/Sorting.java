package com.dream11.logcentralorchestrator.rest.request;

import io.vertx.core.json.JsonObject;
import lombok.Data;

@Data
public class Sorting {

  private String sortingKey;
  private Order sortingOrder;

  public Sorting() {}

  public Sorting(JsonObject jsonObject) {
    fromJson(jsonObject, this);
  }

  public static Sorting of(String sortingKey, String sortingOrder) {
    Sorting sorting = new Sorting();
    sorting.setSortingKey(sortingKey);
    sorting.setSortingOrder(Order.valueOf(sortingOrder));
    return sorting;
  }

  private static void fromJson(Iterable<java.util.Map.Entry<String, Object>> json, Sorting obj) {
    for (java.util.Map.Entry<String, Object> member : json) {
      switch (member.getKey()) {
        case "sortingKey":
          if (member.getValue() instanceof String) {
            obj.setSortingKey((String) member.getValue());
          }
          break;
        case "sortingOrder":
          if (member.getValue() instanceof String) {
            obj.setSortingOrder(Order.valueOf((String) member.getValue()));
          }
          break;
        default:
          break;
      }
    }
  }

  private static void toJson(Sorting obj, JsonObject json) {
    toJson(obj, json.getMap());
  }

  private static void toJson(Sorting obj, java.util.Map<String, Object> json) {
    if (obj.getSortingKey() != null) {
      json.put("sortingKey", obj.getSortingKey());
    }
    if (obj.getSortingOrder() != null) {
      json.put("sortingOrder", obj.getSortingOrder().name());
    }
  }

  public JsonObject toJson() {
    JsonObject jsonObject = new JsonObject();
    toJson(this, jsonObject);
    return jsonObject;
  }

  @Override
  public String toString() {
    return this.getSortingKey() + " " + this.getSortingOrder().toString();
  }

  public enum Order {
    ASC,
    DESC
  }
}
