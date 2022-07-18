package fr.openent.presences.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.*;
import io.vertx.core.json.JsonObject;

public interface SettingsService {
    void retrieve(String structureId, Handler<Either<String, JsonObject>> handler);

    Future<JsonObject> retrieve(String structureId);

    /**
     * Retrieve multiple slot setting.
     *
     * @param structureId Structure identifier
     * @return Future {@link Future<JsonObject>}
     */
    Future<JsonObject> retrieveMultipleSlots(String structureId);

    Future<JsonObject> put(String structureId, JsonObject settings);
}
