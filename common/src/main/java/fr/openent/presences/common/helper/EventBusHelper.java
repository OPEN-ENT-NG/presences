package fr.openent.presences.common.helper;

import fr.openent.presences.common.message.*;
import io.vertx.core.*;
import io.vertx.core.eventbus.*;
import io.vertx.core.json.*;

public class EventBusHelper {

    private EventBusHelper() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Call event bus with action
     * @param address   EventBus address
     * @param eb        EventBus
     * @param action    The action to perform
     * @return          Future with the body of the response from the eb
     */
    public static Future<JsonObject> requestJsonObject(String address, EventBus eb, JsonObject action) {
        Promise<JsonObject> promise = Promise.promise();
        eb.request(address, action, MessageResponseHandler.messageJsonObjectHandler(FutureHelper.handlerJsonObject(promise)));
        return promise.future();
    }

    public static Future<JsonArray> requestJsonArray(String address, EventBus eb, JsonObject action) {
        Promise<JsonArray> promise = Promise.promise();
        eb.request(address, action, MessageResponseHandler.messageJsonArrayHandler(FutureHelper.handlerJsonArray(promise)));
        return promise.future();
    }


}
