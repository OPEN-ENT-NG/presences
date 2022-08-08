package fr.openent.presences.common.helper;
import fr.wseduc.webutils.Either;
import fr.openent.presences.core.constants.Field;
import io.vertx.core.*;
import io.vertx.core.eventbus.Message;
import io.vertx.core.impl.CompositeFutureImpl;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.util.List;
public class FutureHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(FutureHelper.class);
    private FutureHelper() {
    }
    public static Handler<Either<String, JsonArray>> handlerJsonArray(Future<JsonArray> future) {
        return event -> {
            if (event.isRight()) {
                future.complete(event.right().getValue());
            } else {
                LOGGER.error(event.left().getValue());
                future.fail(event.left().getValue());
            }
        };
    }
    /**
     * @deprecated  Replaced by {@link #handlerEitherPromise(Promise)}
     */
    @Deprecated
    public static Handler<Either<String, JsonArray>> handlerJsonArray(Promise<JsonArray> promise) {
        return event -> {
            if (event.isRight()) {
                promise.complete(event.right().getValue());
            } else {
                String message = String.format("[PresencesCommon@%s::handlerJsonArray]: %s",
                        FutureHelper.class.getSimpleName(), event.left().getValue());
                LOGGER.error(message);
                promise.fail(event.left().getValue());
            }
        };
    }
    /**
     * @deprecated  Replaced by {@link #handlerEitherPromise(Promise)}
     */
    @Deprecated
    public static Handler<Either<String, JsonObject>> handlerJsonObject(Promise<JsonObject> promise) {
        return event -> {
            if (event.isRight()) {
                promise.complete(event.right().getValue());
            } else {
                String message = String.format("[PresencesCommon@%s::handlerJsonObject]: %s",
                        FutureHelper.class.getSimpleName(), event.left().getValue());
                LOGGER.error(message);
                promise.fail(event.left().getValue());
            }
        };
    }
    public static <L, R> Handler<Either<L, R>> handlerEitherPromise(Promise<R> promise) {
        return event -> {
            if (event.isRight()) {
                promise.complete(event.right().getValue());
            } else {
                String message = String.format("[PresencesCommon@%s::handlerFuture]: %s",
                        FutureHelper.class.getSimpleName(), event.left().getValue());
                LOGGER.error(message);
                promise.fail(event.left().getValue().toString());
            }
        };
    }
    public static Handler<Either<String, JsonArray>> handlerJsonArray(Handler<AsyncResult<JsonArray>> handler) {
        return event -> {
            if (event.isRight()) {
                handler.handle(Future.succeededFuture(event.right().getValue()));
            } else {
                LOGGER.error(event.left().getValue());
                handler.handle(Future.failedFuture(event.left().getValue()));
            }
        };
    }
    public static Handler<AsyncResult<JsonArray>> handlerAsyncJsonArray(Future<JsonArray> future) {
        return event -> {
            if (event.succeeded()) {
                future.complete(event.result());
            } else {
                LOGGER.error(event.cause().getMessage());
                future.fail(event.cause().getMessage());
            }
        };
    }
    public static Handler<Either<String, JsonObject>> handlerJsonObject(Future<JsonObject> future) {
        return event -> {
            if (event.isRight()) {
                future.complete(event.right().getValue());
            } else {
                LOGGER.error(event.left().getValue());
                future.fail(event.left().getValue());
            }
        };
    }
    public static Handler<Either<String, JsonObject>> handlerJsonObject(Handler<AsyncResult<JsonObject>> handler) {
        return event -> {
            if (event.isRight()) {
                handler.handle(Future.succeededFuture(event.right().getValue()));
            } else {
                LOGGER.error(event.left().getValue());
                handler.handle(Future.failedFuture(event.left().getValue()));
            }
        };
    }
    public static void busArrayHandler(Future<JsonArray> future, Message<JsonObject> message) {
        future
                .onSuccess(result -> message.reply((new JsonObject()).put(Field.STATUS, Field.OK).put(Field.RESULT, result)))
                .onFailure(error -> message.reply((new JsonObject()).put(Field.STATUS, Field.ERROR).put(Field.MESSAGE, error.getMessage())));
    }
    public static void busObjectHandler(Future<JsonObject> future, Message<JsonObject> message) {
        future
                .onSuccess(result -> message.reply((new JsonObject()).put(Field.STATUS, Field.OK).put(Field.RESULT, result)))
                .onFailure(error -> message.reply((new JsonObject()).put(Field.STATUS, Field.ERROR).put(Field.MESSAGE, error.getMessage())));
    }
    public static void handleObjectResult(JsonObject messageBody, Promise<JsonObject> promise) {
        if (Field.OK.equals(messageBody.getString(Field.STATUS)))
            promise.complete(messageBody.getJsonObject(Field.RESULT, new JsonObject()));
        else {
            LOGGER.error(messageBody.getString(Field.MESSAGE));
            promise.fail(messageBody.getString(Field.MESSAGE));
        }
    }
    public static <T> CompositeFuture all(List<Future<T>> futures) {
        return CompositeFutureImpl.all(futures.toArray(new Future[futures.size()]));
    }
    public static <T> CompositeFuture join(List<Future<T>> futures) {
        return CompositeFutureImpl.join(futures.toArray(new Future[futures.size()]));
    }
}