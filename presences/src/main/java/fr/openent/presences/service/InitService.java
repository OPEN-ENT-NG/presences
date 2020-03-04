package fr.openent.presences.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;

public interface InitService {

    void retrieveInitializationStatus(String structure, Handler<Either<String, JsonObject>> handler);

    void getReasonsStatement(HttpServerRequest request, String structure, Future<JsonObject> future);

    void getActionsStatement(HttpServerRequest request, String structure, Future<JsonObject> future);

    void getSettingsStatement(String structure, Future<JsonObject> future);

    void getPresencesDisciplinesStatement(HttpServerRequest request, String structure, Future<JsonObject> future);
}
