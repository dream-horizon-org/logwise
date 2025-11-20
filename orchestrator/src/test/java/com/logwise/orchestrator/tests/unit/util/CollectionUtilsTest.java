package com.logwise.orchestrator.tests.unit.util;

import com.logwise.orchestrator.common.util.CollectionUtils;
import com.logwise.orchestrator.setup.BaseTest;
import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Unit tests for CollectionUtils. */
public class CollectionUtilsTest extends BaseTest {

  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
  }

  @AfterClass
  public static void tearDownClass() {
    BaseTest.cleanup();
  }

  @Test
  public void testMapToList_WithValidCollection_ReturnsMappedList() {
    List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);
    Function<Integer, String> mapper = String::valueOf;

    List<String> result = CollectionUtils.mapToList(mapper, numbers);

    Assert.assertNotNull(result);
    Assert.assertEquals(result.size(), 5);
    Assert.assertEquals(result.get(0), "1");
    Assert.assertEquals(result.get(4), "5");
  }

  @Test
  public void testMapToList_WithEmptyCollection_ReturnsEmptyList() {
    List<Integer> empty = Collections.emptyList();
    Function<Integer, String> mapper = String::valueOf;

    List<String> result = CollectionUtils.mapToList(mapper, empty);

    Assert.assertNotNull(result);
    Assert.assertTrue(result.isEmpty());
  }

  @Test
  public void testMapToList_WithNullMapper_ThrowsException() {
    List<Integer> numbers = Arrays.asList(1, 2, 3);

    try {
      CollectionUtils.mapToList(null, numbers);
      Assert.fail("Should have thrown NullPointerException");
    } catch (NullPointerException e) {
      // Expected
    }
  }

  @Test
  public void testReduce_WithValidCollection_ReturnsReducedValue() {
    List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);
    BinaryOperator<Integer> sum = Integer::sum;

    Integer result = CollectionUtils.reduce(0, sum, numbers);

    Assert.assertEquals(result, Integer.valueOf(15));
  }

  @Test
  public void testReduce_WithEmptyCollection_ReturnsInitial() {
    List<Integer> empty = Collections.emptyList();
    BinaryOperator<Integer> sum = Integer::sum;

    Integer result = CollectionUtils.reduce(10, sum, empty);

    Assert.assertEquals(result, Integer.valueOf(10));
  }

  @Test
  public void testReduce_WithMultiplication_ReturnsProduct() {
    List<Integer> numbers = Arrays.asList(2, 3, 4);
    BinaryOperator<Integer> multiply = (a, b) -> a * b;

    Integer result = CollectionUtils.reduce(1, multiply, numbers);

    Assert.assertEquals(result, Integer.valueOf(24));
  }

  @Test
  public void testIndexBy_WithValidCollection_ReturnsIndexedMap() {
    List<String> items = Arrays.asList("apple", "banana", "cherry");
    Function<String, Character> keyMapper = s -> s.charAt(0);

    Map<Character, String> result = CollectionUtils.indexBy(keyMapper, items);

    Assert.assertNotNull(result);
    Assert.assertEquals(result.size(), 3);
    Assert.assertEquals(result.get('a'), "apple");
    Assert.assertEquals(result.get('b'), "banana");
    Assert.assertEquals(result.get('c'), "cherry");
  }

  @Test
  public void testIndexBy_WithDuplicateKeys_ThrowsException() {
    List<String> items = Arrays.asList("apple", "apricot", "banana");
    Function<String, Character> keyMapper = s -> s.charAt(0);

    try {
      CollectionUtils.indexBy(keyMapper, items);
      Assert.fail("Should have thrown IllegalStateException for duplicate keys");
    } catch (IllegalStateException e) {
      Assert.assertNotNull(e);
      Assert.assertTrue(e.getMessage().contains("Duplicate key"));
    }
  }

  @Test
  public void testIndexBy_WithEmptyCollection_ReturnsEmptyMap() {
    List<String> empty = Collections.emptyList();
    Function<String, Character> keyMapper = s -> s.charAt(0);

    Map<Character, String> result = CollectionUtils.indexBy(keyMapper, empty);

    Assert.assertNotNull(result);
    Assert.assertTrue(result.isEmpty());
  }

  @Test
  public void testAny_WithMatchingPredicate_ReturnsTrue() {
    List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);
    Predicate<Integer> isEven = n -> n % 2 == 0;

    Boolean result = CollectionUtils.any(isEven, numbers);

    Assert.assertTrue(result);
  }

  @Test
  public void testAny_WithNoMatchingPredicate_ReturnsFalse() {
    List<Integer> numbers = Arrays.asList(1, 3, 5, 7, 9);
    Predicate<Integer> isEven = n -> n % 2 == 0;

    Boolean result = CollectionUtils.any(isEven, numbers);

    Assert.assertFalse(result);
  }

  @Test
  public void testAny_WithEmptyCollection_ReturnsFalse() {
    List<Integer> empty = Collections.emptyList();
    Predicate<Integer> isEven = n -> n % 2 == 0;

    Boolean result = CollectionUtils.any(isEven, empty);

    Assert.assertFalse(result);
  }

  @Test
  public void testAll_WithAllMatchingPredicate_ReturnsTrue() {
    List<Integer> numbers = Arrays.asList(2, 4, 6, 8, 10);
    Predicate<Integer> isEven = n -> n % 2 == 0;

    Boolean result = CollectionUtils.all(isEven, numbers);

    Assert.assertTrue(result);
  }

  @Test
  public void testAll_WithSomeNonMatching_ReturnsFalse() {
    List<Integer> numbers = Arrays.asList(2, 4, 5, 8, 10);
    Predicate<Integer> isEven = n -> n % 2 == 0;

    Boolean result = CollectionUtils.all(isEven, numbers);

    Assert.assertFalse(result);
  }

  @Test
  public void testAll_WithEmptyCollection_ReturnsTrue() {
    List<Integer> empty = Collections.emptyList();
    Predicate<Integer> isEven = n -> n % 2 == 0;

    Boolean result = CollectionUtils.all(isEven, empty);

    Assert.assertTrue(result);
  }

  @Test
  public void testZipToMap_WithEqualSizedCollections_ReturnsZippedMap() {
    List<String> keys = Arrays.asList("a", "b", "c");
    List<Integer> values = Arrays.asList(1, 2, 3);

    LinkedHashMap<String, Integer> result = CollectionUtils.zipToMap(keys, values);

    Assert.assertNotNull(result);
    Assert.assertEquals(result.size(), 3);
    Assert.assertEquals(result.get("a"), Integer.valueOf(1));
    Assert.assertEquals(result.get("b"), Integer.valueOf(2));
    Assert.assertEquals(result.get("c"), Integer.valueOf(3));
  }

  @Test
  public void testZipToMap_WithDifferentSizedCollections_UsesMinimumSize() {
    List<String> keys = Arrays.asList("a", "b", "c", "d");
    List<Integer> values = Arrays.asList(1, 2);

    LinkedHashMap<String, Integer> result = CollectionUtils.zipToMap(keys, values);

    Assert.assertNotNull(result);
    Assert.assertEquals(result.size(), 2);
    Assert.assertEquals(result.get("a"), Integer.valueOf(1));
    Assert.assertEquals(result.get("b"), Integer.valueOf(2));
  }

  @Test
  public void testZipToMap_WithEmptyCollections_ReturnsEmptyMap() {
    List<String> emptyKeys = Collections.emptyList();
    List<Integer> emptyValues = Collections.emptyList();

    LinkedHashMap<String, Integer> result = CollectionUtils.zipToMap(emptyKeys, emptyValues);

    Assert.assertNotNull(result);
    Assert.assertTrue(result.isEmpty());
  }
}
