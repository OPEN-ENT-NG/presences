package fr.openent.presences.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

public interface SettingsService {
    void retrieve(String structureId, Handler<Either<String, JsonObject>> handler);

    void put(String structureId, JsonObject settings, Handler<Either<String, JsonObject>> handler);
}
