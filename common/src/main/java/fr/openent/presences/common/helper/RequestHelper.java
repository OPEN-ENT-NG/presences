package fr.openent.presences.common.helper;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;

public class RequestHelper {
    public static JsonObject getJsonRequest(HttpServerRequest request) {
        JsonObject headers = new JsonObject()
                .put("Host", request.getHeader("Host"))
                .put("Accept-Language", request.getHeader("Accept-Language"));
        return new JsonObject()
                .put("headers", headers);
    }
}
