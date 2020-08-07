package fr.openent.presences.common.message;

import fr.wseduc.webutils.Either;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class MessageResponseHandler {

    private MessageResponseHandler() {
    }

    public static Handler<AsyncResult<Message<JsonObject>>> messageJsonArrayHandler(Handler<Either<String, JsonArray>> handler) {
        return event -> {
            if (event.succeeded() && "ok".equals(event.result().body().getString("status"))) {
                handler.handle(new Either.Right<>(event.result().body().getJsonArray("result")));
            } else {
                handler.handle(new Either.Left<>(event.result().body().getString("message")));
            }
        };
    }

    public static Handler<AsyncResult<Message<JsonObject>>> messageJsonObjectHandler(Handler<Either<String, JsonObject>> handler) {
        return event -> {
            if (event.succeeded() && "ok".equals(event.result().body().getString("status"))) {
                handler.handle(new Either.Right<>(event.result().body().getJsonObject("result")));
            } else {
                handler.handle(new Either.Left<>(event.cause().getMessage()));
            }
        };
    }
}
