package fr.openent.presences.common.helper;

import fr.openent.presences.common.presences.Presences;
import fr.openent.presences.core.constants.Field;
import fr.openent.presences.model.IModel;
import fr.openent.presences.model.SharedDataModel;
import io.vertx.core.*;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.AsyncMap;

public class SharedDataHelper {

    public static <T extends IModel<T>> Future<T> loadPresenceData(Vertx vertx, SharedDataModel<T> sharedDataModel) {
        Promise<T> promise = Promise.promise();

        getObjectFromAsyncMap(vertx, Field.PRESENCES_SHARED_DATA_MAP_NAME, sharedDataModel.getKey(), JsonObject.class, false)
                .onFailure(promise::fail)
                .onSuccess(res -> promise.complete(res == null ? (T) sharedDataModel : sharedDataModel.load(res)));

        return promise.future();
    }

    public static <T> Future<T> getObjectFromAsyncMap(Vertx vertx, String map, Object key, Class<T> tClass, boolean local) {
        return getObjectFromAsyncMap(vertx, map, key, tClass, null, local);
    }

    public static <T> Future<T> getObjectFromAsyncMap(Vertx vertx, String map, Object key, Class<T> tClass, T defaultValue, boolean local) {
        Promise<T> promise = Promise.promise();

        Handler<AsyncResult<AsyncMap<Object, Object>>> handler = mapAsyncResult -> {
            if (mapAsyncResult.succeeded()) {
                mapAsyncResult.result().get(key, objectAsyncResult -> {
                    if (objectAsyncResult.failed()) {
                        promise.fail(objectAsyncResult.cause());
                    } else if (objectAsyncResult.result() == null) {
                        promise.complete(defaultValue);
                    } else if (!tClass.isInstance(objectAsyncResult.result())) {
                        promise.fail(String.format("[Common@%s::getObjectFromAsyncMap] %s is not assignable to %s.",
                                SharedDataHelper.class.getName(), objectAsyncResult.result().getClass().getName(), tClass.getName()));
                    } else {
                        promise.complete(tClass.cast(objectAsyncResult.result()));
                    }
                });
            } else {
                promise.fail(mapAsyncResult.cause());
            }
        };
        if (local) {
            vertx.sharedData().getLocalAsyncMap(map, handler);
        } else {
            vertx.sharedData().getAsyncMap(map, handler);
        }

        return promise.future();
    }

    public static Future<Void> setObjectFromAsyncMap(Vertx vertx, String map, Object key, Object value, boolean local) {
        Promise<Void> promise = Promise.promise();

        Handler<AsyncResult<AsyncMap<Object, Object>>> handler = mapAsyncResult -> {
            if (mapAsyncResult.succeeded()) {
                mapAsyncResult.result().put(key, value, objectAsyncResult -> {
                    if (objectAsyncResult.failed()) {
                        promise.fail(objectAsyncResult.cause());
                    } else {
                        promise.complete();
                    }
                });
            } else {
                promise.fail(mapAsyncResult.cause());
            }
        };
        if (local) {
            vertx.sharedData().getLocalAsyncMap(map, handler);
        } else {
            vertx.sharedData().getAsyncMap(map, handler);
        }

        return promise.future();
    }
}
