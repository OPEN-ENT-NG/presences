package fr.openent.presences.common.helper;
import fr.wseduc.webutils.Either;
import fr.openent.presences.core.constants.Field;
import io.vertx.core.*;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class FutureHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(FutureHelper.class);

    private FutureHelper() {
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
                String message = String.format("[PresencesCommon@%s::handlerEitherPromise]: %s",
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



}