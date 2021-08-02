package fr.openent.presences.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.*;
import io.vertx.core.json.JsonObject;

public interface SettingsService {
    void retrieve(String structureId, Handler<Either<String, JsonObject>> handler);

    /**
     * Retrieve multiple slot setting.
     *
     * @param structureId Structure identifier
     * @return Future {@link Future<JsonObject>}
     */
    Future<JsonObject> retrieveMultipleSlots(String structureId);

    void put(String structureId, JsonObject settings, Handler<Either<String, JsonObject>> handler);
}
