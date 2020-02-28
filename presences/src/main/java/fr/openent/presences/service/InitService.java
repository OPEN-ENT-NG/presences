package fr.openent.presences.service;

import io.vertx.core.Future;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;

public interface InitService {
    void getReasonsStatement(HttpServerRequest request, String structure, Future<JsonObject> future);

    void getActionsStatement(HttpServerRequest request, String structure, Future<JsonObject> future);

    void getSettingsStatement(String structure, Future<JsonObject> future);

    void getPresencesDisciplinesStatement(HttpServerRequest request, String structure, Future<JsonObject> future);
}
