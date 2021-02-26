package fr.openent.presences.common.bus;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
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
}
