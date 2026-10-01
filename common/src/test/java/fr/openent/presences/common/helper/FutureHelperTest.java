package fr.openent.presences.common.helper;

import fr.wseduc.webutils.Either;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.*;

import static fr.openent.presences.common.helper.FutureHelper.handlerEitherPromise;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

public class FutureHelperTest {

    private String error = "This is an error message";

    @Test
    @DisplayName("FutureHelper.handlerEitherPromise should succeed the future when succeeding the handler")
    public void handlerEitherPromise_JsonArray_should_succeed_the_future_when_succeeding_the_handler() {
        Promise<JsonArray> promise = Promise.promise();
        Handler<Either<String, JsonArray>> handler = handlerEitherPromise(promise);
        handler.handle(new Either.Right<>(new JsonArray()));
        assertTrue(promise.future().succeeded() && promise.future().result() != null && promise.future().result().size() == 0);
    }

    @Test
    @DisplayName("FutureHelper.handlerEitherPromise should failed the future when failing the handler")
    public void handlerEitherPromise_JsonArray_should_failed_the_future_when_failing_the_handler() {
        Promise<JsonArray> promise = Promise.promise();
        Handler<Either<String, JsonArray>> handler = handlerEitherPromise(promise);
        handler.handle(new Either.Left<>(error));
        assertTrue(promise.future().failed() && promise.future().cause() != null && error.equals(promise.future().cause().getMessage()));
    }


    @Test
    @DisplayName("FutureHelper.handlerEitherPromise should succeed the future when succeeding the handler")
    public void handlerEitherPromise_JsonObject_should_succeed_the_future_when_succeeding_the_handler() {
        Promise<JsonObject> promise = Promise.promise();
        Handler<Either<String, JsonObject>> handler = handlerEitherPromise(promise);
        handler.handle(new Either.Right<>(new JsonObject().put("status", "ok")));
        assertTrue(promise.future().succeeded() && promise.future().result() != null && "ok".equals(promise.future().result().getString("status")));
    }

    @Test
    @DisplayName("FutureHelper.handlerEitherPromise should failed the future when failing the handler")
    public void handlerEitherPromise_JsonObject_the_future_when_failing_the_handler() {
        Promise<JsonObject> promise = Promise.promise();
        Handler<Either<String, JsonObject>> handler = handlerEitherPromise(promise);
        handler.handle(new Either.Left<>(error));
        assertTrue(promise.future().failed() && promise.future().cause() != null && error.equals(promise.future().cause().getMessage()));
    }

    @Test
    @DisplayName("handlerEitherPromise should deeply convert nested Maps in JsonArray to JsonObjects")
    public void handlerEitherPromise_should_deep_convert_nested_maps_in_JsonArray() {
        // Simulate clustered deployment: nested structures are LinkedHashMaps
        LinkedHashMap<String, Object> nestedSlot = new LinkedHashMap<>();
        nestedSlot.put("id", "slot-1");
        nestedSlot.put("startHour", "08:00");
        nestedSlot.put("endHour", "09:00");

        List<Object> slotsList = new ArrayList<>();
        slotsList.add(nestedSlot);

        LinkedHashMap<String, Object> timeslot = new LinkedHashMap<>();
        timeslot.put("_id", "timeslot-1");
        timeslot.put("slots", slotsList);

        List<Object> resultList = new ArrayList<>();
        resultList.add(timeslot);

        JsonArray rawArray = new JsonArray(resultList);

        Promise<JsonArray> promise = Promise.promise();
        Handler<Either<String, JsonArray>> handler = handlerEitherPromise(promise);
        handler.handle(new Either.Right<>(rawArray));

        assertTrue(promise.future().succeeded());
        JsonArray result = promise.future().result();
        assertNotNull(result);
        assertEquals(1, result.size());

        // Verify top-level is JsonObject
        Object topLevel = result.getValue(0);
        assertTrue(topLevel instanceof JsonObject);

        JsonObject timeslotObj = (JsonObject) topLevel;
        // Verify nested 'slots' array elements are also JsonObjects (deep copy)
        JsonArray slots = timeslotObj.getJsonArray("slots");
        assertNotNull(slots);
        assertEquals(1, slots.size());

        Object slotElement = slots.getValue(0);
        assertTrue(slotElement instanceof JsonObject);
        assertEquals("slot-1", ((JsonObject) slotElement).getString("id"));
        assertEquals("08:00", ((JsonObject) slotElement).getString("startHour"));
    }

    @Test
    @DisplayName("handlerJsonArray(Promise) should deeply convert nested Maps")
    public void handlerJsonArray_promise_should_deep_convert_nested_maps() {
        LinkedHashMap<String, Object> child = new LinkedHashMap<>();
        child.put("key", "value");

        LinkedHashMap<String, Object> parent = new LinkedHashMap<>();
        parent.put("id", "1");
        parent.put("child", child);

        List<Object> list = new ArrayList<>();
        list.add(parent);

        JsonArray rawArray = new JsonArray(list);

        Promise<JsonArray> promise = Promise.promise();
        Handler<Either<String, JsonArray>> handler = FutureHelper.handlerJsonArray(promise);
        handler.handle(new Either.Right<>(rawArray));

        assertTrue(promise.future().succeeded());
        JsonArray result = promise.future().result();
        JsonObject parentObj = result.getJsonObject(0);
        Object childVal = parentObj.getValue("child");
        assertTrue(childVal instanceof JsonObject);
        assertEquals("value", ((JsonObject) childVal).getString("key"));
    }

    @Test
    @DisplayName("handlerJsonArray(Handler) should deeply convert nested Maps")
    public void handlerJsonArray_handler_should_deep_convert_nested_maps() {
        LinkedHashMap<String, Object> nested = new LinkedHashMap<>();
        nested.put("name", "nested");

        LinkedHashMap<String, Object> item = new LinkedHashMap<>();
        item.put("id", "item-1");
        item.put("nested", nested);

        List<Object> list = new ArrayList<>();
        list.add(item);

        JsonArray rawArray = new JsonArray(list);

        final JsonArray[] captured = new JsonArray[1];
        Handler<AsyncResult<JsonArray>> asyncHandler = ar -> {
            assertTrue(ar.succeeded());
            captured[0] = ar.result();
        };

        Handler<Either<String, JsonArray>> handler = FutureHelper.handlerJsonArray(asyncHandler);
        handler.handle(new Either.Right<>(rawArray));

        assertNotNull(captured[0]);
        JsonObject itemObj = captured[0].getJsonObject(0);
        Object nestedVal = itemObj.getValue("nested");
        assertTrue(nestedVal instanceof JsonObject);
        assertEquals("nested", ((JsonObject) nestedVal).getString("name"));
    }

    @Test
    @DisplayName("handlerJsonArray / handlerEitherPromise should keep non Json values (e.g. model instances) as is")
    public void handlers_should_keep_non_json_values() {
        InnerDTO model = new InnerDTO("one", "two");

        Promise<JsonArray> promise = Promise.promise();
        FutureHelper.handlerJsonArray(promise).handle(new Either.Right<>(arrayWithNonJsonValues(model)));
        assertTrue(promise.future().succeeded());
        assertNonJsonValuesKept(promise.future().result(), model);

        final JsonArray[] captured = new JsonArray[1];
        FutureHelper.handlerJsonArray((Handler<AsyncResult<JsonArray>>) ar -> {
            assertTrue(ar.succeeded());
            captured[0] = ar.result();
        }).handle(new Either.Right<>(arrayWithNonJsonValues(model)));
        assertNonJsonValuesKept(captured[0], model);

        Promise<JsonArray> eitherPromise = Promise.promise();
        Handler<Either<String, JsonArray>> handler = handlerEitherPromise(eitherPromise);
        handler.handle(new Either.Right<>(arrayWithNonJsonValues(model)));
        assertTrue(eitherPromise.future().succeeded());
        assertNonJsonValuesKept(eitherPromise.future().result(), model);
    }

    /**
     * Builds [model, {id, model, models: [model]}] with raw Map / List, as received in a clustered deployment,
     * and with the model at top level (as in DefaultCourseService#listRegistersWithCourses) and nested.
     */
    private static JsonArray arrayWithNonJsonValues(Object model) {
        LinkedHashMap<String, Object> item = new LinkedHashMap<>();
        item.put("id", "item-1");
        item.put("model", model);
        item.put("models", new ArrayList<>(Collections.singletonList(model)));

        return new JsonArray(new ArrayList<>(Arrays.asList(model, item)));
    }

    private static void assertNonJsonValuesKept(JsonArray result, Object model) {
        assertNotNull(result);
        assertEquals(2, result.size());
        assertSame(model, result.getValue(0));

        // getValue rather than getJsonObject / getJsonArray, which would wrap a raw Map / List on their own
        Object item = result.getValue(1);
        assertTrue(item instanceof JsonObject);
        assertEquals("item-1", ((JsonObject) item).getString("id"));
        assertSame(model, ((JsonObject) item).getValue("model"));

        Object models = ((JsonObject) item).getValue("models");
        assertTrue(models instanceof JsonArray);
        assertSame(model, ((JsonArray) models).getValue(0));
    }

    private static class InnerDTO {
        private final String fieldOne;
        private final String fieldTwo;

        InnerDTO(String fieldOne, String fieldTwo) {
            this.fieldOne = fieldOne;
            this.fieldTwo = fieldTwo;
        }
    }

}
