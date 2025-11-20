package com.logwise.orchestrator.tests.unit.util;

import com.logwise.orchestrator.common.util.ListUtils;
import com.logwise.orchestrator.setup.BaseTest;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Unit tests for ListUtils. */
public class ListUtilsTest extends BaseTest {

  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
  }

  @AfterClass
  public static void tearDownClass() {
    BaseTest.cleanup();
  }

  @Test
  public void testMap_WithValidMapper_ReturnsMappedList() {
    List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
    Function<Integer, Integer> mapper = x -> x * 2;

    List<Integer> result = ListUtils.map(mapper, list);

    Assert.assertNotNull(result);
    Assert.assertEquals(result.size(), 5);
    Assert.assertEquals(result.get(0), Integer.valueOf(2));
    Assert.assertEquals(result.get(4), Integer.valueOf(10));
  }

  @Test
  public void testMap_WithEmptyList_ReturnsEmptyList() {
    List<Integer> empty = Collections.emptyList();
    Function<Integer, String> mapper = String::valueOf;

    List<String> result = ListUtils.map(mapper, empty);

    Assert.assertNotNull(result);
    Assert.assertTrue(result.isEmpty());
  }

  @Test
  public void testMap_WithFunctionReturn_ReturnsFunction() {
    Function<Integer, String> mapper = String::valueOf;

    Function<List<Integer>, List<String>> result = ListUtils.map(mapper);

    Assert.assertNotNull(result);
    List<String> mapped = result.apply(Arrays.asList(1, 2, 3));
    Assert.assertEquals(mapped.size(), 3);
    Assert.assertEquals(mapped.get(0), "1");
  }

  @Test
  public void testFilter_WithValidPredicate_ReturnsFilteredList() {
    List<Integer> list = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    Predicate<Integer> selector = x -> x % 2 == 0;

    List<Integer> result = ListUtils.filter(selector, list);

    Assert.assertNotNull(result);
    Assert.assertEquals(result.size(), 5);
    Assert.assertEquals(result, Arrays.asList(2, 4, 6, 8, 10));
  }

  @Test
  public void testFilter_WithNoMatches_ReturnsEmptyList() {
    List<Integer> list = Arrays.asList(1, 3, 5, 7, 9);
    Predicate<Integer> selector = x -> x % 2 == 0;

    List<Integer> result = ListUtils.filter(selector, list);

    Assert.assertNotNull(result);
    Assert.assertTrue(result.isEmpty());
  }

  @Test
  public void testFilter_WithAllMatches_ReturnsAllElements() {
    List<Integer> list = Arrays.asList(2, 4, 6, 8, 10);
    Predicate<Integer> selector = x -> x % 2 == 0;

    List<Integer> result = ListUtils.filter(selector, list);

    Assert.assertNotNull(result);
    Assert.assertEquals(result.size(), 5);
  }

  @Test
  public void testFilter_WithFunctionReturn_ReturnsFunction() {
    Predicate<Integer> selector = x -> x > 5;

    Function<List<Integer>, List<Integer>> result = ListUtils.filter(selector);

    Assert.assertNotNull(result);
    List<Integer> filtered = result.apply(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
    Assert.assertEquals(filtered.size(), 5);
    Assert.assertTrue(filtered.contains(6));
    Assert.assertTrue(filtered.contains(10));
  }

  @Test
  public void testFilter_WithEmptyList_ReturnsEmptyList() {
    List<Integer> empty = Collections.emptyList();
    Predicate<Integer> selector = x -> x > 0;

    List<Integer> result = ListUtils.filter(selector, empty);

    Assert.assertNotNull(result);
    Assert.assertTrue(result.isEmpty());
  }
}
