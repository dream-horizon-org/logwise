package com.logwise.orchestrator.tests.unit.util;

import com.logwise.orchestrator.common.util.MapUtils;
import com.logwise.orchestrator.setup.BaseTest;
import java.util.*;
import java.util.function.Predicate;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Unit tests for MapUtils. */
public class MapUtilsTest extends BaseTest {

  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
  }

  @AfterClass
  public static void tearDownClass() {
    BaseTest.cleanup();
  }

  @Test
  public void testFilter_WithValidPredicate_ReturnsFilteredMap() {
    Map<String, Integer> map = new LinkedHashMap<>();
    map.put("a", 1);
    map.put("b", 2);
    map.put("c", 3);
    map.put("d", 4);
    Predicate<Map.Entry<String, Integer>> selector = entry -> entry.getValue() > 2;

    Map<String, Integer> result = MapUtils.filter(selector, map);

    Assert.assertNotNull(result);
    Assert.assertEquals(result.size(), 2);
    Assert.assertTrue(result.containsKey("c"));
    Assert.assertTrue(result.containsKey("d"));
    Assert.assertFalse(result.containsKey("a"));
    Assert.assertFalse(result.containsKey("b"));
  }

  @Test
  public void testFilter_WithNoMatches_ReturnsEmptyMap() {
    Map<String, Integer> map = new LinkedHashMap<>();
    map.put("a", 1);
    map.put("b", 2);
    Predicate<Map.Entry<String, Integer>> selector = entry -> entry.getValue() > 10;

    Map<String, Integer> result = MapUtils.filter(selector, map);

    Assert.assertNotNull(result);
    Assert.assertTrue(result.isEmpty());
  }

  @Test
  public void testFilter_WithAllMatches_ReturnsAllEntries() {
    Map<String, Integer> map = new LinkedHashMap<>();
    map.put("a", 1);
    map.put("b", 2);
    Predicate<Map.Entry<String, Integer>> selector = entry -> entry.getValue() > 0;

    Map<String, Integer> result = MapUtils.filter(selector, map);

    Assert.assertNotNull(result);
    Assert.assertEquals(result.size(), 2);
  }

  @Test
  public void testPick_WithValidKeys_ReturnsSelectedEntries() {
    Map<String, Integer> map = new LinkedHashMap<>();
    map.put("a", 1);
    map.put("b", 2);
    map.put("c", 3);
    map.put("d", 4);
    List<String> keys = Arrays.asList("a", "c", "e");

    Map<String, Integer> result = MapUtils.pick(keys, map);

    Assert.assertNotNull(result);
    Assert.assertEquals(result.size(), 2);
    Assert.assertEquals(result.get("a"), Integer.valueOf(1));
    Assert.assertEquals(result.get("c"), Integer.valueOf(3));
    Assert.assertFalse(result.containsKey("e"));
  }

  @Test
  public void testPick_WithNoMatchingKeys_ReturnsEmptyMap() {
    Map<String, Integer> map = new LinkedHashMap<>();
    map.put("a", 1);
    map.put("b", 2);
    List<String> keys = Arrays.asList("x", "y", "z");

    Map<String, Integer> result = MapUtils.pick(keys, map);

    Assert.assertNotNull(result);
    Assert.assertTrue(result.isEmpty());
  }

  @Test
  public void testPick_WithEmptyKeyList_ReturnsEmptyMap() {
    Map<String, Integer> map = new LinkedHashMap<>();
    map.put("a", 1);
    List<String> keys = Collections.emptyList();

    Map<String, Integer> result = MapUtils.pick(keys, map);

    Assert.assertNotNull(result);
    Assert.assertTrue(result.isEmpty());
  }

  @Test
  public void testValues_WithValidMap_ReturnsValueList() {
    Map<String, Integer> map = new LinkedHashMap<>();
    map.put("a", 1);
    map.put("b", 2);
    map.put("c", 3);

    List<Integer> result = MapUtils.values(map);

    Assert.assertNotNull(result);
    Assert.assertEquals(result.size(), 3);
    Assert.assertTrue(result.contains(1));
    Assert.assertTrue(result.contains(2));
    Assert.assertTrue(result.contains(3));
  }

  @Test
  public void testValues_WithEmptyMap_ReturnsEmptyList() {
    Map<String, Integer> emptyMap = new LinkedHashMap<>();

    List<Integer> result = MapUtils.values(emptyMap);

    Assert.assertNotNull(result);
    Assert.assertTrue(result.isEmpty());
  }

  @Test
  public void testKeys_WithValidMap_ReturnsKeyList() {
    Map<String, Integer> map = new LinkedHashMap<>();
    map.put("a", 1);
    map.put("b", 2);
    map.put("c", 3);

    List<String> result = MapUtils.keys(map);

    Assert.assertNotNull(result);
    Assert.assertEquals(result.size(), 3);
    Assert.assertTrue(result.contains("a"));
    Assert.assertTrue(result.contains("b"));
    Assert.assertTrue(result.contains("c"));
  }

  @Test
  public void testKeys_WithEmptyMap_ReturnsEmptyList() {
    Map<String, Integer> emptyMap = new LinkedHashMap<>();

    List<String> result = MapUtils.keys(emptyMap);

    Assert.assertNotNull(result);
    Assert.assertTrue(result.isEmpty());
  }

  @Test
  public void testEntries_WithValidMap_ReturnsEntryList() {
    Map<String, Integer> map = new LinkedHashMap<>();
    map.put("a", 1);
    map.put("b", 2);

    List<Map.Entry<String, Integer>> result = MapUtils.entries(map);

    Assert.assertNotNull(result);
    Assert.assertEquals(result.size(), 2);
  }

  @Test
  public void testMap_WithValidMapper_ReturnsMappedMap() {
    Map<String, Integer> map = new LinkedHashMap<>();
    map.put("a", 1);
    map.put("b", 2);
    map.put("c", 3);
    java.util.function.Function<Integer, String> mapper = String::valueOf;

    Map<String, String> result = MapUtils.map(mapper, map);

    Assert.assertNotNull(result);
    Assert.assertEquals(result.size(), 3);
    Assert.assertEquals(result.get("a"), "1");
    Assert.assertEquals(result.get("b"), "2");
    Assert.assertEquals(result.get("c"), "3");
  }

  @Test
  public void testMapToList_WithValidMapper_ReturnsMappedList() {
    Map<String, Integer> map = new LinkedHashMap<>();
    map.put("a", 1);
    map.put("b", 2);
    java.util.function.BiFunction<String, Integer, String> mapper =
        (key, value) -> key + ":" + value;

    List<String> result = MapUtils.mapToList(mapper, map);

    Assert.assertNotNull(result);
    Assert.assertEquals(result.size(), 2);
    Assert.assertTrue(result.contains("a:1"));
    Assert.assertTrue(result.contains("b:2"));
  }

  @Test
  public void testSort_WithValidComparator_ReturnsSortedMap() {
    Map<String, Integer> map = new LinkedHashMap<>();
    map.put("c", 3);
    map.put("a", 1);
    map.put("b", 2);
    Comparator<Map.Entry<String, Integer>> comparator = Comparator.comparing(Map.Entry::getValue);

    LinkedHashMap<String, Integer> result = MapUtils.sort(comparator, map);

    Assert.assertNotNull(result);
    List<String> keys = new ArrayList<>(result.keySet());
    // Values should be sorted: 1, 2, 3
    Assert.assertEquals(result.get(keys.get(0)), Integer.valueOf(1));
    Assert.assertEquals(result.get(keys.get(1)), Integer.valueOf(2));
    Assert.assertEquals(result.get(keys.get(2)), Integer.valueOf(3));
  }
}
