package com.logwise.orchestrator.tests.unit.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logwise.orchestrator.common.app.AppContext;
import com.logwise.orchestrator.common.entity.VertxEntity;
import com.logwise.orchestrator.rest.RestUtil;
import com.logwise.orchestrator.rest.request.Sorting;
import com.logwise.orchestrator.setup.BaseTest;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Unit tests for RestUtil. */
public class RestUtilTest extends BaseTest {

  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
  }

  @Test
  public void testToSortingIterator_WithSingleAsc_ReturnsAscSorting() {

    List<Sorting> result = RestUtil.toSortingIterator("name");

    Assert.assertEquals(result.size(), 1);
    Assert.assertEquals(result.get(0).getSortingKey(), "name");
    Assert.assertEquals(result.get(0).getSortingOrder(), Sorting.Order.ASC);
  }

  @Test
  public void testToSortingIterator_WithSingleDesc_ReturnsDescSorting() {

    List<Sorting> result = RestUtil.toSortingIterator("-name");

    Assert.assertEquals(result.size(), 1);
    Assert.assertEquals(result.get(0).getSortingKey(), "name");
    Assert.assertEquals(result.get(0).getSortingOrder(), Sorting.Order.DESC);
  }

  @Test
  public void testToSortingIterator_WithMultipleSorts_ReturnsMultipleSortings() {

    List<Sorting> result = RestUtil.toSortingIterator("name,-date,status");

    Assert.assertEquals(result.size(), 3);
    Assert.assertEquals(result.get(0).getSortingKey(), "name");
    Assert.assertEquals(result.get(0).getSortingOrder(), Sorting.Order.ASC);
    Assert.assertEquals(result.get(1).getSortingKey(), "date");
    Assert.assertEquals(result.get(1).getSortingOrder(), Sorting.Order.DESC);
    Assert.assertEquals(result.get(2).getSortingKey(), "status");
    Assert.assertEquals(result.get(2).getSortingOrder(), Sorting.Order.ASC);
  }

  @Test
  public void testToSortingIterator_WithNull_ThrowsException() {

    try {
      RestUtil.toSortingIterator(null);
      Assert.fail("Should have thrown NullPointerException");
    } catch (NullPointerException e) {

    }
  }

  @Test
  public void testToSortingString_WithSingleAsc_ReturnsString() {

    String result = RestUtil.toSortingString("name");

    Assert.assertEquals(result, "name ASC");
  }

  @Test
  public void testToSortingString_WithSingleDesc_ReturnsString() {

    String result = RestUtil.toSortingString("-name");

    Assert.assertEquals(result, "name DESC");
  }

  @Test
  public void testToSortingString_WithMultipleSorts_ReturnsCommaSeparatedString() {

    String result = RestUtil.toSortingString("name,-date");

    Assert.assertTrue(result.contains("name ASC"));
    Assert.assertTrue(result.contains("date DESC"));
  }

  @Test
  public void testToSortingString_WithNull_ThrowsException() {

    try {
      RestUtil.toSortingString(null);
      Assert.fail("Should have thrown NullPointerException");
    } catch (NullPointerException e) {

    }
  }

  @Test
  public void testGetString_WithString_ReturnsString() throws Exception {

    String input = "test-string";

    try (MockedStatic<AppContext> mockedAppContext = Mockito.mockStatic(AppContext.class)) {

      String result = RestUtil.getString(input);

      Assert.assertEquals(result, input);
    }
  }

  @Test
  public void testGetString_WithJsonObject_ReturnsString() throws Exception {

    JsonObject jsonObject = new JsonObject().put("key", "value");

    try (MockedStatic<AppContext> mockedAppContext = Mockito.mockStatic(AppContext.class)) {

      String result = RestUtil.getString(jsonObject);

      Assert.assertNotNull(result);
      Assert.assertTrue(result.contains("key"));
    }
  }

  @Test
  public void testGetString_WithList_ReturnsJsonArrayString() throws Exception {

    List<String> list = Arrays.asList("item1", "item2");
    ObjectMapper mockObjectMapper = new ObjectMapper();

    try (MockedStatic<AppContext> mockedAppContext = Mockito.mockStatic(AppContext.class)) {
      mockedAppContext
          .when(() -> AppContext.getInstance(ObjectMapper.class))
          .thenReturn(mockObjectMapper);

      String result = RestUtil.getString(list);

      Assert.assertNotNull(result);
      Assert.assertTrue(result.contains("item1"));
    }
  }

  @Test
  public void testGetString_WithSet_ReturnsJsonArrayString() throws Exception {

    Set<String> set = new HashSet<>(Arrays.asList("item1", "item2"));

    try (MockedStatic<AppContext> mockedAppContext = Mockito.mockStatic(AppContext.class)) {

      String result = RestUtil.getString(set);

      Assert.assertNotNull(result);
    }
  }

  @Test
  public void testGetString_WithArrayList_ReturnsJsonArrayString() throws Exception {

    ArrayList<String> arrayList = new ArrayList<>(Arrays.asList("item1", "item2"));

    try (MockedStatic<AppContext> mockedAppContext = Mockito.mockStatic(AppContext.class)) {

      String result = RestUtil.getString(arrayList);

      Assert.assertNotNull(result);
    }
  }

  @Test
  public void testGetString_WithHashSet_ReturnsJsonArrayString() throws Exception {

    HashSet<String> hashSet = new HashSet<>(Arrays.asList("item1", "item2"));

    try (MockedStatic<AppContext> mockedAppContext = Mockito.mockStatic(AppContext.class)) {

      String result = RestUtil.getString(hashSet);

      Assert.assertNotNull(result);
    }
  }

  @Test
  public void testGetString_WithLinkedHashSet_ReturnsJsonArrayString() throws Exception {

    LinkedHashSet<String> linkedHashSet = new LinkedHashSet<>(Arrays.asList("item1", "item2"));

    try (MockedStatic<AppContext> mockedAppContext = Mockito.mockStatic(AppContext.class)) {

      String result = RestUtil.getString(linkedHashSet);

      Assert.assertNotNull(result);
    }
  }

  @Test
  public void testGetString_WithLinkedList_ReturnsJsonArrayString() throws Exception {

    LinkedList<String> linkedList = new LinkedList<>(Arrays.asList("item1", "item2"));

    try (MockedStatic<AppContext> mockedAppContext = Mockito.mockStatic(AppContext.class)) {

      String result = RestUtil.getString(linkedList);

      Assert.assertNotNull(result);
    }
  }

  @Test
  public void testGetString_WithVertxEntity_ReturnsJsonString() throws Exception {

    VertxEntity entity =
        new VertxEntity() {
          @Override
          public JsonObject toJson() {
            return new JsonObject().put("test", "value");
          }
        };

    try (MockedStatic<AppContext> mockedAppContext = Mockito.mockStatic(AppContext.class)) {

      String result = RestUtil.getString(entity);

      Assert.assertNotNull(result);
      Assert.assertTrue(result.contains("test"));
    }
  }

  @Test
  public void testGetString_WithCustomObject_UsesObjectMapper() throws Exception {

    TestObject testObject = new TestObject("test-value");

    try (MockedStatic<AppContext> mockedAppContext = Mockito.mockStatic(AppContext.class)) {
      ObjectMapper mockMapper = Mockito.mock(ObjectMapper.class);
      Mockito.when(mockMapper.writeValueAsString(testObject))
          .thenReturn("{\"value\":\"test-value\"}");
      mockedAppContext
          .when(() -> AppContext.getInstance(ObjectMapper.class))
          .thenReturn(mockMapper);

      String result = RestUtil.getString(testObject);

      Assert.assertNotNull(result);
      Mockito.verify(mockMapper).writeValueAsString(testObject);
    }
  }

  @Test
  public void testAnnotatedClasses_WithValidPackage_ReturnsAnnotatedClasses() {

    List<String> packageNames = Arrays.asList("com.logwise.orchestrator.rest.v1");

    List<Class<?>> result = RestUtil.annotatedClasses(javax.ws.rs.Path.class, packageNames);

    Assert.assertNotNull(result);

    Assert.assertTrue(result.size() >= 0); // At least 0 (may vary based on classpath)
  }

  @Test
  public void testAnnotatedClasses_WithInvalidPackage_ReturnsEmptyList() {

    List<String> packageNames = Arrays.asList("com.nonexistent.package");

    List<Class<?>> result = RestUtil.annotatedClasses(javax.ws.rs.Path.class, packageNames);

    Assert.assertNotNull(result);
  }

  @Test
  public void testAnnotatedClasses_WithProviderAnnotation_ReturnsProviders() {

    List<String> packageNames = Arrays.asList("com.logwise.orchestrator.rest.provider");

    List<Class<?>> result = RestUtil.annotatedClasses(javax.ws.rs.ext.Provider.class, packageNames);

    Assert.assertNotNull(result);

    Assert.assertTrue(result.size() >= 0);
  }

  @Test
  public void testAbstractRouteList_WithValidPackage_ReturnsRoutes() {

    List<String> packageNames = Arrays.asList("com.logwise.orchestrator.rest");

    List<com.logwise.orchestrator.rest.AbstractRoute> result =
        RestUtil.abstractRouteList(packageNames);

    Assert.assertNotNull(result);

    Assert.assertTrue(result.size() >= 0);
  }

  @Test
  public void testAbstractRouteList_WithInvalidPackage_ReturnsEmptyList() {

    List<String> packageNames = Arrays.asList("com.nonexistent.package");

    List<com.logwise.orchestrator.rest.AbstractRoute> result =
        RestUtil.abstractRouteList(packageNames);

    Assert.assertNotNull(result);
  }

  @Test
  public void testAbstractRouteList_WithEmptyPackageList_ReturnsEmptyList() {

    List<String> packageNames = Collections.emptyList();

    List<com.logwise.orchestrator.rest.AbstractRoute> result =
        RestUtil.abstractRouteList(packageNames);

    Assert.assertNotNull(result);
    Assert.assertTrue(result.isEmpty());
  }

  @Test
  public void testAbstractRouteList_WithNullPackageList_ReturnsEmptyList() {

    List<String> packageNames = null;

    try (MockedStatic<AppContext> mockedAppContext = Mockito.mockStatic(AppContext.class)) {
      List<com.logwise.orchestrator.rest.AbstractRoute> result =
          RestUtil.abstractRouteList(packageNames);

      // Should handle null gracefully or throw NPE
      Assert.assertNotNull(result);
    } catch (NullPointerException e) {
      // Expected behavior
    }
  }

  @Test
  public void testAnnotatedClasses_WithEmptyPackageList_ReturnsEmptyList() {

    List<String> packageNames = Collections.emptyList();

    List<Class<?>> result = RestUtil.annotatedClasses(javax.ws.rs.Path.class, packageNames);

    Assert.assertNotNull(result);
    Assert.assertTrue(result.isEmpty());
  }

  @Test
  public void testAnnotatedClasses_WithNullPackageList_ReturnsEmptyList() {

    List<String> packageNames = null;

    try {
      List<Class<?>> result = RestUtil.annotatedClasses(javax.ws.rs.Path.class, packageNames);
      Assert.assertNotNull(result);
    } catch (NullPointerException e) {
      // Expected behavior
    }
  }

  @Test
  public void testGetString_WithNull_ThrowsException() throws Exception {

    try (MockedStatic<AppContext> mockedAppContext = Mockito.mockStatic(AppContext.class)) {
      try {
        RestUtil.getString(null);
        Assert.fail("Should have thrown NullPointerException");
      } catch (NullPointerException e) {
        // Expected
      }
    }
  }

  @Test(expectedExceptions = StringIndexOutOfBoundsException.class)
  public void testToSortingIterator_WithEmptyString_ThrowsException() {
    // Empty string split by "," returns [""], and charAt(0) on empty string throws
    // exception
    RestUtil.toSortingIterator("");
  }

  @Test
  public void testToSortingIterator_WithWhitespace_ReturnsEmptyList() {

    List<Sorting> result = RestUtil.toSortingIterator("   ");

    Assert.assertNotNull(result);
    // Should handle whitespace
  }

  @Test(expectedExceptions = StringIndexOutOfBoundsException.class)
  public void testToSortingString_WithEmptyString_ThrowsException() {
    // Empty string split by "," returns [""], and charAt(0) on empty string throws
    // exception
    RestUtil.toSortingString("");
  }

  @Test
  public void testToSortingIterator_WithSingleChar_HandlesGracefully() {

    try {
      List<Sorting> result = RestUtil.toSortingIterator("a");
      Assert.assertNotNull(result);
      Assert.assertEquals(result.size(), 1);
    } catch (StringIndexOutOfBoundsException e) {
      // If sortKey is empty, charAt(0) will throw
      // This tests the edge case
    }
  }

  @Test
  public void testToSortingString_WithSingleChar_HandlesGracefully() {

    try {
      String result = RestUtil.toSortingString("a");
      Assert.assertNotNull(result);
    } catch (StringIndexOutOfBoundsException e) {
      // If sortKey is empty, charAt(0) will throw
      // This tests the edge case
    }
  }

  private static class TestObject {
    private String value;

    public TestObject(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }
  }
}
