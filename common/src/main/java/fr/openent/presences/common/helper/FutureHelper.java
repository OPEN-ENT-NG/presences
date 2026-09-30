package fr.openent.presences.common.helper;
import fr.wseduc.webutils.Either;
import fr.openent.presences.core.constants.Field;
import io.vertx.core.*;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.List;
import java.util.Map;

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
                // In a clustered deployment results are not JsonObject but Map so we need to "transform" them back to
                // JsonObject so downstream process do not get cast errors (see normalize).
                final JsonArray formatedArray = normalize(event.right().getValue());
                promise.complete(formatedArray);
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

    @SuppressWarnings("unchecked")
    public static <L, R> Handler<Either<L, R>> handlerEitherPromise(Promise<R> promise) {
        return event -> {
            if (event.isRight()) {
                R value = event.right().getValue();
                // In a clustered deployment results are not JsonObject but Map so we need to "transform" them back to
                // JsonObject so downstream process do not get cast errors (see normalize)
                if (value instanceof JsonArray) {
                    value = (R) normalize((JsonArray) value);
                }
                promise.complete(value);
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
                // In a clustered deployment results are not JsonObject but Map so we need to "transform" them back to
                // JsonObject so downstream process do not get cast errors (see normalize).
                JsonArray formatedArray = null;
                if(event.right().getValue() != null) {
                    formatedArray = normalize(event.right().getValue());
                }
                handler.handle(Future.succeededFuture(formatedArray));
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

    /**
     * Deeply rebuild a {@link JsonArray}, converting nested {@link Map} into {@link JsonObject} and {@link List} into
     * {@link JsonArray} (clustered event bus results). Unlike {@link JsonArray#copy()}, any other value (e.g. a model
     * instance such as a Course) is kept as is instead of throwing "Illegal type in Json".
     *
     * @param array array to normalize
     * @return a new normalized {@link JsonArray}
     */
    public static JsonArray normalize(JsonArray array) {
        return (JsonArray) normalizeValue(array);
    }

    @SuppressWarnings("unchecked")
    private static Object normalizeValue(Object value) {
        if (value instanceof JsonObject) {
            value = ((JsonObject) value).getMap();
        } else if (value instanceof JsonArray) {
            value = ((JsonArray) value).getList();
        }

        if (value instanceof Map) {
            JsonObject object = new JsonObject();
            ((Map<String, Object>) value).forEach((key, val) -> object.put(key, normalizeValue(val)));
            return object;
        } else if (value instanceof List) {
            JsonArray array = new JsonArray();
            ((List<Object>) value).forEach(val -> array.add(normalizeValue(val)));
            return array;
        }
        return value;
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