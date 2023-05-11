package fr.openent.presences.service;

import fr.openent.presences.enums.InitTypeEnum;
import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;

import java.util.*;

public interface InitService {

    Future<JsonObject> initPresences(HttpServerRequest request, String structureId, String userId,
                                     Optional<InitTypeEnum> initTypeEnum);
    void retrieveInitializationStatus(String structure, Handler<Either<String, JsonObject>> handler);

    Future<Boolean> retrieveInitializationStatus(String structureId);

    void getReasonsStatement(HttpServerRequest request, String structure, InitTypeEnum initTypeEnum, Promise<JsonObject> promise);

    void getActionsStatement(HttpServerRequest request, String structure, InitTypeEnum initTypeEnum, Promise<JsonObject> promise);

    void getSettingsStatement(String structure, InitTypeEnum initTypeEnum, Promise<JsonObject> promise);

    void getPresencesDisciplinesStatement(HttpServerRequest request, String structure, InitTypeEnum initTypeEnum, Promise<JsonObject> promise);
}
