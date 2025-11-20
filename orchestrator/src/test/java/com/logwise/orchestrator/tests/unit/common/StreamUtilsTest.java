package com.logwise.orchestrator.tests.unit.common;

import com.logwise.orchestrator.common.util.StreamUtils;
import com.logwise.orchestrator.setup.BaseTest;
import java.util.*;
import java.util.stream.Stream;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Unit tests for StreamUtils. */
public class StreamUtilsTest extends BaseTest {

  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
  }

  @AfterClass
  public static void tearDownClass() {
    BaseTest.cleanup();
  }

  @Test
  public void testToLinkedHashMap_WithEntries_ReturnsLinkedHashMap() {
    Map<String, Integer> map = new LinkedHashMap<>();
    map.put("a", 1);
    map.put("b", 2);
    map.put("c", 3);

    LinkedHashMap<String, Integer> result =
        map.entrySet().stream().collect(StreamUtils.toLinkedHashMap());

    Assert.assertNotNull(result);
    Assert.assertEquals(result.size(), 3);
    List<String> keys = new ArrayList<>(result.keySet());
    Assert.assertEquals(keys.get(0), "a");
    Assert.assertEquals(keys.get(1), "b");
    Assert.assertEquals(keys.get(2), "c");
  }

  @Test
  public void testToLinkedHashMap_WithKeyAndValueMappers_ReturnsLinkedHashMap() {
    List<String> items = Arrays.asList("apple", "banana", "cherry");

    LinkedHashMap<Character, String> result =
        items.stream().collect(StreamUtils.toLinkedHashMap(s -> s.charAt(0), s -> s.toUpperCase()));

    Assert.assertNotNull(result);
    Assert.assertEquals(result.size(), 3);
    Assert.assertEquals(result.get('a'), "APPLE");
    Assert.assertEquals(result.get('b'), "BANANA");
    Assert.assertEquals(result.get('c'), "CHERRY");
  }

  @Test
  public void testToLinkedHashMap_WithDuplicateKeys_ThrowsException() {
    List<String> items = Arrays.asList("apple", "apricot", "banana");

    try {
      items.stream().collect(StreamUtils.toLinkedHashMap(s -> s.charAt(0), s -> s));
      Assert.fail("Should have thrown IllegalStateException");
    } catch (IllegalStateException e) {
      Assert.assertNotNull(e);
      Assert.assertTrue(e.getMessage().contains("Duplicate key"));
    }
  }

  @Test
  public void testToLinkedHashMap_WithEmptyStream_ReturnsEmptyMap() {
    Stream<Map.Entry<String, Integer>> emptyStream = Stream.empty();

    LinkedHashMap<String, Integer> result = emptyStream.collect(StreamUtils.toLinkedHashMap());

    Assert.assertNotNull(result);
    Assert.assertTrue(result.isEmpty());
  }

  @Test
  public void testToLinkedHashMap_PreservesInsertionOrder() {
    List<String> items = Arrays.asList("zebra", "apple", "banana", "cherry");

    LinkedHashMap<Character, String> result =
        items.stream().collect(StreamUtils.toLinkedHashMap(s -> s.charAt(0), String::toUpperCase));

    Assert.assertNotNull(result);
    List<Character> keys = new ArrayList<>(result.keySet());
    Assert.assertEquals(keys.get(0), Character.valueOf('z'));
    Assert.assertEquals(keys.get(1), Character.valueOf('a'));
    Assert.assertEquals(keys.get(2), Character.valueOf('b'));
    Assert.assertEquals(keys.get(3), Character.valueOf('c'));
  }
}
