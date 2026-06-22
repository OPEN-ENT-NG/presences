package fr.openent.presences.common.message;

import fr.wseduc.webutils.Either;
import fr.openent.presences.core.constants.Field;
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

public class MessageResponseHandlerTest {

    /**
     * Simulates a clustered deployment where nested JsonObjects are deserialized as LinkedHashMaps.
     * Creates a JsonArray with nested Maps (like what happens when Vert.x deserializes in cluster mode).
     */
    private JsonObject buildClusteredResponseWithNestedMaps() {
        // Simulate nested structure: timeslot with slots array containing maps instead of JsonObjects
        LinkedHashMap<String, Object> slot1 = new LinkedHashMap<>();
        slot1.put("id", "slot-1");
        slot1.put("name", "Slot 1");
        slot1.put("startHour", "08:00");
        slot1.put("endHour", "09:00");

        LinkedHashMap<String, Object> slot2 = new LinkedHashMap<>();
        slot2.put("id", "slot-2");
        slot2.put("name", "Slot 2");
        slot2.put("startHour", "09:00");
        slot2.put("endHour", "10:00");

        List<Object> slotsList = new ArrayList<>();
        slotsList.add(slot1);
        slotsList.add(slot2);

        LinkedHashMap<String, Object> timeslot = new LinkedHashMap<>();
        timeslot.put("_id", "timeslot-1");
        timeslot.put("schoolId", "structure-1");
        timeslot.put("audienceId", "audience-1");
        timeslot.put("slots", slotsList);

        List<Object> resultList = new ArrayList<>();
        resultList.add(timeslot);

        // Build the body as it would be received from the event bus in cluster mode
        JsonObject body = new JsonObject()
                .put(Field.STATUS, Field.OK)
                .put(Field.RESULT, new JsonArray(resultList));

        return body;
    }

    @Test
    @DisplayName("messageJsonArrayHandler should deeply convert nested Maps to JsonObjects")
    public void messageJsonArrayHandler_should_deeply_convert_nested_maps() {
        JsonObject body = buildClusteredResponseWithNestedMaps();

        // Create a mock AsyncResult<Message<JsonObject>>
        final JsonArray[] captured = new JsonArray[1];
        Handler<Either<String, JsonArray>> resultHandler = event -> {
            assertTrue(event.isRight());
            captured[0] = event.right().getValue();
        };

        Handler<AsyncResult<Message<JsonObject>>> handler = MessageResponseHandler.messageJsonArrayHandler(resultHandler);

        // Simulate a succeeded event
        handler.handle(Future.succeededFuture(new FakeMessage(body)));

        assertNotNull(captured[0]);
        assertEquals(1, captured[0].size());

        // Verify top-level element is a JsonObject
        Object topLevel = captured[0].getValue(0);
        assertTrue("Top-level element should be a JsonObject, was: " + topLevel.getClass().getName(),
                topLevel instanceof JsonObject);

        JsonObject timeslot = (JsonObject) topLevel;

        // Verify nested 'slots' array elements are JsonObjects (not LinkedHashMaps)
        JsonArray slots = timeslot.getJsonArray("slots");
        assertNotNull("slots field should be a JsonArray", slots);
        assertEquals(2, slots.size());

        Object firstSlot = slots.getValue(0);
        assertTrue("Nested slot should be a JsonObject, was: " + firstSlot.getClass().getName(),
                firstSlot instanceof JsonObject);

        JsonObject slotObj = (JsonObject) firstSlot;
        assertEquals("slot-1", slotObj.getString("id"));
        assertEquals("08:00", slotObj.getString("startHour"));
    }

    @Test
    @DisplayName("messageJsonArrayHandler should handle null result gracefully")
    public void messageJsonArrayHandler_should_handle_null_result() {
        JsonObject body = new JsonObject()
                .put(Field.STATUS, Field.OK);
        // No RESULT or RESULTS field

        final JsonArray[] captured = new JsonArray[1];
        Handler<Either<String, JsonArray>> resultHandler = event -> {
            assertTrue(event.isRight());
            captured[0] = event.right().getValue();
        };

        Handler<AsyncResult<Message<JsonObject>>> handler = MessageResponseHandler.messageJsonArrayHandler(resultHandler);
        handler.handle(Future.succeededFuture(new FakeMessage(body)));

        assertNull(captured[0]);
    }

    @Test
    @DisplayName("messageJsonArrayHandler should return error on failure")
    public void messageJsonArrayHandler_should_return_error_on_failure() {
        final String[] captured = new String[1];
        Handler<Either<String, JsonArray>> resultHandler = event -> {
            assertTrue(event.isLeft());
            captured[0] = event.left().getValue();
        };

        Handler<AsyncResult<Message<JsonObject>>> handler = MessageResponseHandler.messageJsonArrayHandler(resultHandler);
        handler.handle(Future.failedFuture("connection timeout"));

        assertEquals("connection timeout", captured[0]);
    }

    @Test
    @DisplayName("messageJsonObjectHandler should return result JsonObject")
    public void messageJsonObjectHandler_should_return_result() {
        JsonObject resultObj = new JsonObject().put("key", "value");
        JsonObject body = new JsonObject()
                .put(Field.STATUS, Field.OK)
                .put(Field.RESULT, resultObj);

        final JsonObject[] captured = new JsonObject[1];
        Handler<Either<String, JsonObject>> resultHandler = event -> {
            assertTrue(event.isRight());
            captured[0] = event.right().getValue();
        };

        Handler<AsyncResult<Message<JsonObject>>> handler = MessageResponseHandler.messageJsonObjectHandler(resultHandler);
        handler.handle(Future.succeededFuture(new FakeMessage(body)));

        assertNotNull(captured[0]);
        assertEquals("value", captured[0].getString("key"));
    }

    /**
     * Minimal Message implementation for testing.
     */
    private static class FakeMessage implements Message<JsonObject> {
        private final JsonObject body;

        FakeMessage(JsonObject body) {
            this.body = body;
        }

        @Override public JsonObject body() { return body; }
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
