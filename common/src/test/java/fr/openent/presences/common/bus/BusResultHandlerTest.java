package fr.openent.presences.common.bus;

import io.vertx.codegen.annotations.Nullable;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.*;

import static org.junit.Assert.*;

public class BusResultHandlerTest {

    @Test
    @DisplayName("getResults should deeply convert nested Maps to JsonObjects")
    public void getResults_should_deeply_convert_nested_maps() {
        // Simulate clustered deployment: nested structures are LinkedHashMaps
        LinkedHashMap<String, Object> nestedChild = new LinkedHashMap<>();
        nestedChild.put("childKey", "childValue");
        nestedChild.put("childNumber", 42);

        LinkedHashMap<String, Object> parentMap = new LinkedHashMap<>();
        parentMap.put("id", "parent-1");
        parentMap.put("name", "Parent");
        parentMap.put("nested", nestedChild);

        List<Object> resultList = new ArrayList<>();
        resultList.add(parentMap);

        JsonObject body = new JsonObject()
                .put("status", "ok")
                .put("results", new JsonArray(resultList));

        AsyncResult<Message<Object>> event = Future.succeededFuture(new FakeObjectMessage(body));

        JsonArray results = BusResultHandler.getResults(event);

        assertNotNull(results);
        assertEquals(1, results.size());

        // Verify top-level is JsonObject
        Object topLevel = results.getValue(0);
        assertTrue("Top-level element should be JsonObject, was: " + topLevel.getClass().getName(),
                topLevel instanceof JsonObject);

        JsonObject parentObj = (JsonObject) topLevel;
        assertEquals("parent-1", parentObj.getString("id"));

        // Verify nested object is also a JsonObject (deep copy)
        Object nested = parentObj.getValue("nested");
        assertTrue("Nested element should be JsonObject, was: " + nested.getClass().getName(),
                nested instanceof JsonObject);

        JsonObject nestedObj = (JsonObject) nested;
        assertEquals("childValue", nestedObj.getString("childKey"));
        assertEquals(Integer.valueOf(42), nestedObj.getInteger("childNumber"));
    }

    @Test
    @DisplayName("getResults with custom field name should deeply convert nested Maps")
    public void getResults_with_custom_field_should_deeply_convert() {
        LinkedHashMap<String, Object> innerList = new LinkedHashMap<>();
        innerList.put("slotId", "s1");

        List<Object> innerArray = new ArrayList<>();
        innerArray.add(innerList);

        LinkedHashMap<String, Object> item = new LinkedHashMap<>();
        item.put("id", "item-1");
        item.put("slots", innerArray);

        List<Object> resultList = new ArrayList<>();
        resultList.add(item);

        JsonObject body = new JsonObject()
                .put("status", "ok")
                .put("result", new JsonArray(resultList));

        AsyncResult<Message<Object>> event = Future.succeededFuture(new FakeObjectMessage(body));

        JsonArray results = BusResultHandler.getResults(event, "result");

        assertNotNull(results);
        assertEquals(1, results.size());

        JsonObject itemObj = results.getJsonObject(0);
        JsonArray slots = itemObj.getJsonArray("slots");
        assertNotNull(slots);
        assertEquals(1, slots.size());

        Object slotElement = slots.getValue(0);
        assertTrue("Nested array element should be JsonObject, was: " + slotElement.getClass().getName(),
                slotElement instanceof JsonObject);
        assertEquals("s1", ((JsonObject) slotElement).getString("slotId"));
    }

    @Test
    @DisplayName("getResults should return null when field is absent")
    public void getResults_should_return_null_when_field_absent() {
        JsonObject body = new JsonObject().put("status", "ok");
        AsyncResult<Message<Object>> event = Future.succeededFuture(new FakeObjectMessage(body));

        JsonArray results = BusResultHandler.getResults(event);
        assertNull(results);
    }

    /**
     * Minimal Message<Object> implementation for testing.
     */
    private static class FakeObjectMessage implements Message<Object> {
        private final JsonObject body;

        FakeObjectMessage(JsonObject body) {
            this.body = body;
        }

        @Override public Object body() { return body; }
        @Override public String address() { return null; }
        @Override public String replyAddress() { return null; }
        @Override public io.vertx.core.MultiMap headers() { return null; }
        @Override public boolean isSend() { return false; }
        @Override public void reply(Object message) {}
        @Override public void reply(Object message, io.vertx.core.eventbus.DeliveryOptions options) {}

        @Override
        public <R> void replyAndRequest(@Nullable Object message, Handler<AsyncResult<Message<R>>> replyHandler) {
            Message.super.replyAndRequest(message, replyHandler);
        }

        @Override
        public <R> Future<Message<R>> replyAndRequest(@Nullable Object message) {
            return Message.super.replyAndRequest(message);
        }

        @Override
        public <R> void replyAndRequest(@Nullable Object message, DeliveryOptions options, Handler<AsyncResult<Message<R>>> replyHandler) {
            Message.super.replyAndRequest(message, options, replyHandler);
        }

        @Override
        public <R> Future<Message<R>> replyAndRequest(@Nullable Object message, DeliveryOptions options) {
            return null;
        }

        @Override public void fail(int failureCode, String message) {}
    }
}
