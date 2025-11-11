package com.logwise.orchestrator.tests.unit;

import com.logwise.orchestrator.common.util.*;
import com.logwise.orchestrator.setup.BaseTest;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.functions.Function;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.stream.Stream;
import me.escoffier.vertx.completablefuture.VertxCompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Unit tests for common/util package (CollectionUtils, MapUtils, StreamUtils, StackUtils,
 * JsonUtils, ListUtils, CompletableFutureUtils, MaybeUtils, SingleUtils).
 */
public class CommonUtilTest extends BaseTest {

  private static final Logger log = LoggerFactory.getLogger(CommonUtilTest.class);

  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
  }

  @Test
  public void testListUtils_Map_WithValidMapper_ReturnsMappedList() {

    List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
    java.util.function.Function<Integer, Integer> mapper = x -> x * 2;

    List<Integer> result = ListUtils.map(mapper, list);

    Assert.assertNotNull(result);
    Assert.assertEquals(result.size(), 5);
    Assert.assertEquals(result.get(0), Integer.valueOf(2));
    Assert.assertEquals(result.get(4), Integer.valueOf(10));
  }

  @Test
  public void testListUtils_Map_WithEmptyList_ReturnsEmptyList() {

    List<Integer> list = new ArrayList<>();
    java.util.function.Function<Integer, String> mapper = Object::toString;

    List<String> result = ListUtils.map(mapper, list);

    Assert.assertNotNull(result);
    Assert.assertTrue(result.isEmpty());
  }

  @Test
  public void testListUtils_Filter_WithValidPredicate_ReturnsFilteredList() {

    List<Integer> list = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    Predicate<Integer> selector = x -> x % 2 == 0;

    List<Integer> result = ListUtils.filter(selector, list);

    Assert.assertNotNull(result);
    Assert.assertEquals(result.size(), 5);
    Assert.assertEquals(result, Arrays.asList(2, 4, 6, 8, 10));
  }

  @Test
  public void testCollectionUtils_MapToList_WithValidCollection_ReturnsMappedList() {

    List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);

    List<String> result = CollectionUtils.mapToList(String::valueOf, numbers);

    Assert.assertNotNull(result);
    Assert.assertEquals(result.size(), 5);
    Assert.assertEquals(result.get(0), "1");
    Assert.assertEquals(result.get(4), "5");
  }

  @Test
  public void testCollectionUtils_Reduce_WithValidCollection_ReturnsReducedValue() {

    List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);

    Integer sum = CollectionUtils.reduce(0, Integer::sum, numbers);

    Assert.assertEquals(sum, Integer.valueOf(15));
  }

  @Test
  public void testCollectionUtils_IndexBy_WithValidCollection_ReturnsIndexedMap() {

    List<TestItem> items =
        Arrays.asList(
            new TestItem("key1", "value1"),
            new TestItem("key2", "value2"),
            new TestItem("key3", "value3"));

    Map<String, TestItem> result = CollectionUtils.indexBy(TestItem::getKey, items);

    Assert.assertNotNull(result);
    Assert.assertEquals(result.size(), 3);
    Assert.assertEquals(result.get("key1").getValue(), "value1");
    Assert.assertEquals(result.get("key2").getValue(), "value2");
  }

  @Test
  public void testCollectionUtils_Any_WithMatchingCondition_ReturnsTrue() {

    List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);

    Boolean result = CollectionUtils.any(n -> n > 3, numbers);

    Assert.assertTrue(result);
  }

  @Test
  public void testCollectionUtils_All_WithAllMatchingCondition_ReturnsTrue() {

    List<Integer> numbers = Arrays.asList(2, 4, 6, 8);

    Boolean result = CollectionUtils.all(n -> n % 2 == 0, numbers);

    Assert.assertTrue(result);
  }

  @Test
  public void testCollectionUtils_ZipToMap_WithValidCollections_ReturnsZippedMap() {

    List<String> keys = Arrays.asList("key1", "key2", "key3");
    List<String> values = Arrays.asList("value1", "value2", "value3");

    LinkedHashMap<String, String> result = CollectionUtils.zipToMap(keys, values);

    Assert.assertNotNull(result);
    Assert.assertEquals(result.size(), 3);
    Assert.assertEquals(result.get("key1"), "value1");
    Assert.assertEquals(result.get("key2"), "value2");
    Assert.assertEquals(result.get("key3"), "value3");
  }

  @Test
  public void testMapUtils_Filter_WithValidPredicate_ReturnsFilteredMap() {

    Map<String, Integer> map = new LinkedHashMap<>();
    map.put("one", 1);
    map.put("two", 2);
    map.put("three", 3);
    map.put("four", 4);
    Predicate<Map.Entry<String, Integer>> selector = entry -> entry.getValue() > 2;

    Map<String, Integer> result = MapUtils.filter(selector, map);

    Assert.assertNotNull(result);
    Assert.assertEquals(result.size(), 2);
    Assert.assertEquals(result.get("three"), Integer.valueOf(3));
    Assert.assertEquals(result.get("four"), Integer.valueOf(4));
  }

  @Test
  public void testMapUtils_Pick_WithValidKeys_ReturnsPickedMap() {

    Map<String, Integer> map = new LinkedHashMap<>();
    map.put("one", 1);
    map.put("two", 2);
    map.put("three", 3);
    map.put("four", 4);
    List<String> keys = Arrays.asList("two", "four", "five");

    Map<String, Integer> result = MapUtils.pick(keys, map);

    Assert.assertNotNull(result);
    Assert.assertEquals(result.size(), 2);
    Assert.assertEquals(result.get("two"), Integer.valueOf(2));
    Assert.assertEquals(result.get("four"), Integer.valueOf(4));
  }

  @Test
  public void testMapUtils_Values_WithMap_ReturnsValueList() {

    Map<String, Integer> map = new LinkedHashMap<>();
    map.put("one", 1);
    map.put("two", 2);
    map.put("three", 3);

    List<Integer> result = MapUtils.values(map);

    Assert.assertNotNull(result);
    Assert.assertEquals(result.size(), 3);
    Assert.assertTrue(result.contains(1));
    Assert.assertTrue(result.contains(2));
    Assert.assertTrue(result.contains(3));
  }

  @Test
  public void testMapUtils_Keys_WithMap_ReturnsKeyList() {

    Map<String, Integer> map = new LinkedHashMap<>();
    map.put("one", 1);
    map.put("two", 2);
    map.put("three", 3);

    List<String> result = MapUtils.keys(map);

    Assert.assertNotNull(result);
    Assert.assertEquals(result.size(), 3);
    Assert.assertTrue(result.contains("one"));
    Assert.assertTrue(result.contains("two"));
    Assert.assertTrue(result.contains("three"));
  }

  @Test
  public void testMapUtils_Map_WithMapper_ReturnsMappedMap() {

    Map<String, Integer> map = new LinkedHashMap<>();
    map.put("one", 1);
    map.put("two", 2);
    map.put("three", 3);
    java.util.function.Function<Integer, String> mapper = i -> "value" + i;

    Map<String, String> result = MapUtils.map(mapper, map);

    Assert.assertNotNull(result);
    Assert.assertEquals(result.size(), 3);
    Assert.assertEquals(result.get("one"), "value1");
    Assert.assertEquals(result.get("two"), "value2");
    Assert.assertEquals(result.get("three"), "value3");
  }

  @Test
  public void testMapUtils_Sort_WithComparator_ReturnsSortedMap() {

    Map<String, Integer> map = new LinkedHashMap<>();
    map.put("one", 1);
    map.put("three", 3);
    map.put("two", 2);
    Comparator<Map.Entry<String, Integer>> comparator = Comparator.comparing(Map.Entry::getValue);

    LinkedHashMap<String, Integer> result = MapUtils.sort(comparator, map);

    Assert.assertNotNull(result);
    Assert.assertEquals(result.size(), 3);
    List<String> keys = new ArrayList<>(result.keySet());
    Assert.assertEquals(keys.get(0), "one");
    Assert.assertEquals(keys.get(1), "two");
    Assert.assertEquals(keys.get(2), "three");
  }

  @Test
  public void testStreamUtils_ToLinkedHashMap_WithMapEntries_ReturnsLinkedHashMap() {

    Map<String, Integer> sourceMap = new LinkedHashMap<>();
    sourceMap.put("one", 1);
    sourceMap.put("two", 2);
    sourceMap.put("three", 3);

    LinkedHashMap<String, Integer> result =
        sourceMap.entrySet().stream().collect(StreamUtils.toLinkedHashMap());

    Assert.assertNotNull(result);
    Assert.assertEquals(result.size(), 3);
    Assert.assertEquals(result.get("one"), Integer.valueOf(1));
    String[] keys = result.keySet().toArray(new String[0]);
    Assert.assertEquals(keys[0], "one");
    Assert.assertEquals(keys[1], "two");
    Assert.assertEquals(keys[2], "three");
  }

  @Test
  public void testStreamUtils_ToLinkedHashMap_WithKeyValueMappers_ReturnsLinkedHashMap() {

    Stream<String> stream = Stream.of("one", "two", "three");

    LinkedHashMap<String, Integer> result =
        stream.collect(StreamUtils.toLinkedHashMap(s -> s, s -> s.length()));

    Assert.assertNotNull(result);
    Assert.assertEquals(result.size(), 3);
    Assert.assertEquals(result.get("one"), Integer.valueOf(3));
    Assert.assertEquals(result.get("two"), Integer.valueOf(3));
    Assert.assertEquals(result.get("three"), Integer.valueOf(5));
  }

  @Test
  public void testStackUtils_GetCallerName_ReturnsMethodName() {

    String callerName = StackUtils.getCallerName();

    Assert.assertNotNull(callerName);

    Assert.assertEquals(callerName, "invoke0");
  }

  @Test
  public void testStackUtils_GetCallerName_FromHelperMethod_ReturnsHelperName() {

    String callerName = helperMethod();

    Assert.assertNotNull(callerName);
    Assert.assertEquals(
        callerName, "testStackUtils_GetCallerName_FromHelperMethod_ReturnsHelperName");
  }

  private String helperMethod() {
    return StackUtils.getCallerName();
  }

  @Test
  public void testJsonUtils_JsonFrom_WithMap_ReturnsJsonObject() {

    Map<String, Object> map = new HashMap<>();
    JsonObject nestedObj = new JsonObject().put("nestedKey", "nestedValue");
    map.put("key1", nestedObj);
    map.put("key2", new JsonObject().put("anotherKey", 456));

    JsonObject result = JsonUtils.jsonFrom(map);

    Assert.assertNotNull(result);
    Assert.assertTrue(result.containsKey("key1"));
    Assert.assertTrue(result.containsKey("key2"));
    JsonObject nested1 = result.getJsonObject("key1");
    Assert.assertNotNull(nested1);
    Assert.assertEquals(nested1.getString("nestedKey"), "nestedValue");
  }

  @Test
  public void testJsonUtils_JsonFrom_WithKeyAndList_ReturnsJsonObjectWithArray() {

    List<Object> list = Arrays.asList("value1", "value2", "value3");

    JsonObject result = JsonUtils.jsonFrom("testKey", list);

    Assert.assertNotNull(result);
    Assert.assertTrue(result.containsKey("testKey"));
    JsonArray array = result.getJsonArray("testKey");
    Assert.assertEquals(array.size(), 3);
    Assert.assertEquals(array.getString(0), "value1");
  }

  @Test
  public void testJsonUtils_JsonMerge_WithListOfJsonObjects_ReturnsMergedJsonObject() {

    List<JsonObject> objects =
        Arrays.asList(
            new JsonObject().put("key1", "value1"),
            new JsonObject().put("key2", "value2"),
            new JsonObject().put("key3", "value3"));

    JsonObject result = JsonUtils.jsonMerge(objects);

    Assert.assertNotNull(result);
    Assert.assertEquals(result.getString("key1"), "value1");
    Assert.assertEquals(result.getString("key2"), "value2");
    Assert.assertEquals(result.getString("key3"), "value3");
  }

  @Test
  public void testJsonUtils_GetValueFromNestedJson_WithValidPath_ReturnsValue() {

    JsonObject json =
        new JsonObject()
            .put("level1", new JsonObject().put("level2", new JsonObject().put("level3", "value")));

    Object result = JsonUtils.getValueFromNestedJson(json, "level1.level2.level3");

    Assert.assertNotNull(result);
    Assert.assertEquals(result, "value");
  }

  @Test
  public void testCompletableFutureUtils_ToSingle_WithCompletableFuture_ReturnsSingle() {

    CompletableFuture<String> completableFuture = CompletableFuture.supplyAsync(() -> "test-value");
    io.vertx.reactivex.core.Vertx reactiveVertx = BaseTest.getReactiveVertx();

    VertxCompletableFuture<String> vertxFuture =
        VertxCompletableFuture.from(
            reactiveVertx.getDelegate().getOrCreateContext(), completableFuture);
    Single<String> single = CompletableFutureUtils.toSingle(vertxFuture);
    String result = single.blockingGet();

    Assert.assertNotNull(result);
    Assert.assertEquals(result, "test-value");
  }

  @Test
  public void testCompletableFutureUtils_ToSingle_WithVertxCompletableFuture_ReturnsSingle() {

    io.vertx.reactivex.core.Vertx reactiveVertx = BaseTest.getReactiveVertx();
    VertxCompletableFuture<String> vertxFuture =
        new VertxCompletableFuture<>(reactiveVertx.getDelegate());
    vertxFuture.complete("test-value");

    Single<String> single = CompletableFutureUtils.toSingle(vertxFuture);
    String result = single.blockingGet();

    Assert.assertNotNull(result);
    Assert.assertEquals(result, "test-value");
  }

  @Test
  public void testMaybeUtils_ReadThroughCache_WithCacheHit_ReturnsCachedValue() {

    Maybe<String> getFromCache = Maybe.just("cached-value");
    Maybe<String> getFromSource = Maybe.just("source-value");
    Function<String, Completable> saveToCache = value -> Completable.complete();

    Maybe<String> result = MaybeUtils.readThroughCache(getFromCache, getFromSource, saveToCache);
    String value = result.blockingGet();

    Assert.assertNotNull(value);
    Assert.assertEquals(value, "cached-value");
  }

  @Test
  public void testMaybeUtils_ReadThroughCache_WithCacheMiss_ReturnsSourceValue() {

    Maybe<String> getFromCache = Maybe.empty();
    Maybe<String> getFromSource = Maybe.just("source-value");
    Function<String, Completable> saveToCache = value -> Completable.complete();

    Maybe<String> result = MaybeUtils.readThroughCache(getFromCache, getFromSource, saveToCache);
    String value = result.blockingGet();

    Assert.assertNotNull(value);
    Assert.assertEquals(value, "source-value");
  }

  @Test
  public void testMaybeUtils_ApplyDebugLogs_WithLogPrefix_AddsLogging() {

    Maybe<String> maybe = Maybe.just("test-value");
    String logPrefix = "testMethod";

    Maybe<String> result = maybe.compose(MaybeUtils.applyDebugLogs(log, logPrefix));
    String value = result.blockingGet();

    Assert.assertNotNull(value);
    Assert.assertEquals(value, "test-value");
  }

  @Test
  public void testSingleUtils_ReadThroughCache_WithCacheHit_ReturnsCachedValue() {

    Maybe<String> getFromCache = Maybe.just("cached-value");
    Single<String> getFromSource = Single.just("source-value");
    Function<String, Completable> saveToCache = value -> Completable.complete();

    Single<String> result = SingleUtils.readThroughCache(getFromCache, getFromSource, saveToCache);
    String value = result.blockingGet();

    Assert.assertNotNull(value);
    Assert.assertEquals(value, "cached-value");
  }

  @Test
  public void testSingleUtils_ReadThroughCache_WithCacheMiss_ReturnsSourceValue() {

    Maybe<String> getFromCache = Maybe.empty();
    Single<String> getFromSource = Single.just("source-value");
    Function<String, Completable> saveToCache = value -> Completable.complete();

    Single<String> result = SingleUtils.readThroughCache(getFromCache, getFromSource, saveToCache);
    String value = result.blockingGet();

    Assert.assertNotNull(value);
    Assert.assertEquals(value, "source-value");
  }

  @Test
  public void testSingleUtils_ApplyDebugLogs_WithLogPrefix_AddsLogging() {

    Single<String> single = Single.just("test-value");
    String logPrefix = "testMethod";

    Single<String> result = single.compose(SingleUtils.applyDebugLogs(log, logPrefix));
    String value = result.blockingGet();

    Assert.assertNotNull(value);
    Assert.assertEquals(value, "test-value");
  }

  private static class TestItem {
    private String key;
    private String value;

    public TestItem(String key, String value) {
      this.key = key;
      this.value = value;
    }

    public String getKey() {
      return key;
    }

    public String getValue() {
      return value;
    }
  }
}
