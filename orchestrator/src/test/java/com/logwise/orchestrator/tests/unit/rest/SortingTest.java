package com.logwise.orchestrator.tests.unit.rest;

import com.logwise.orchestrator.rest.request.Sorting;
import io.vertx.core.json.JsonObject;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Unit tests for Sorting. */
public class SortingTest {

  @Test
  public void testOf_WithValidInputs_CreatesSorting() {

    Sorting sorting = Sorting.of("name", "ASC");

    Assert.assertNotNull(sorting);
    Assert.assertEquals(sorting.getSortingKey(), "name");
    Assert.assertEquals(sorting.getSortingOrder(), Sorting.Order.ASC);
  }

  @Test
  public void testOf_WithDescOrder_CreatesSorting() {

    Sorting sorting = Sorting.of("date", "DESC");

    Assert.assertEquals(sorting.getSortingKey(), "date");
    Assert.assertEquals(sorting.getSortingOrder(), Sorting.Order.DESC);
  }

  @Test
  public void testConstructor_Default_CreatesEmptySorting() {

    Sorting sorting = new Sorting();

    Assert.assertNotNull(sorting);
    Assert.assertNull(sorting.getSortingKey());
    Assert.assertNull(sorting.getSortingOrder());
  }

  @Test
  public void testConstructor_WithJsonObject_CreatesSorting() {

    JsonObject json = new JsonObject();
    json.put("sortingKey", "testKey");
    json.put("sortingOrder", "ASC");

    Sorting sorting = new Sorting(json);

    Assert.assertEquals(sorting.getSortingKey(), "testKey");
    Assert.assertEquals(sorting.getSortingOrder(), Sorting.Order.ASC);
  }

  @Test
  public void testToJson_WithValidSorting_ReturnsJsonObject() {

    Sorting sorting = Sorting.of("name", "ASC");

    JsonObject json = sorting.toJson();

    Assert.assertNotNull(json);
    Assert.assertEquals(json.getString("sortingKey"), "name");
    Assert.assertEquals(json.getString("sortingOrder"), "ASC");
  }

  @Test
  public void testToString_WithValidSorting_ReturnsString() {

    Sorting sorting = Sorting.of("name", "ASC");

    String result = sorting.toString();

    Assert.assertEquals(result, "name ASC");
  }

  @Test
  public void testOrderEnum_Values() {

    Assert.assertNotNull(Sorting.Order.ASC);
    Assert.assertNotNull(Sorting.Order.DESC);
  }
}
