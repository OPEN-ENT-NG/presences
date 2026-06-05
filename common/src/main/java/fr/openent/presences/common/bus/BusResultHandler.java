package fr.openent.presences.common.bus;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class BusResultHandler {

    private BusResultHandler() {
        throw new IllegalStateException("Utility class");
    }

    public static <T>Handler<AsyncResult<T>> busResponseHandler(final Message<T> message) {
        return event -> {
            if (event.succeeded()) {
                message.reply((new JsonObject()).put("status", "ok").put("result", event.result()));
            } else {
                JsonObject error = (new JsonObject()).put("status", "error").put("message", event.cause().getMessage());
                message.reply(error);
            }
        };
    }

    /**
     * Helper to retrieve results from bus response (when the result field is named "results"), as in a clustered deployment results are not JsonObject but Map so we need to "transform" them back to
     * JsonObject so downstream process do not get cast errors.
     * @param event The bus response event
     * @return The results as JsonArray, or null if the result field is not present in the response or null
     */
    public static JsonArray getResults(AsyncResult<Message<Object>> event) {
        return getResults(event, "results");
    }


    /**
     * Helper to retrieve results from bus response, as in a clustered deployment results are not JsonObject but Map so we need to "transform" them back to
     * JsonObject so downstream process do not get cast errors.
     * @param event The bus response event
     * @param resultFieldName The name of the field containing the results in the bus response
     * @return The results as JsonArray, or null if the result field is not present in the response or null
     */
    public static JsonArray getResults(AsyncResult<Message<Object>> event, final String resultFieldName) {
        // In a clustered deployment results are not JsonObject but Map so we need to "transform" them back to
        // JsonObject so downstream process do not get cast errors
        final JsonArray raw = ((JsonObject) event.result().body()).getJsonArray(resultFieldName);
        if (raw == null) {
            return null;
        }
        final JsonArray results = new JsonArray();
        raw.stream().forEach(results::add);
        return results;
    }
}
