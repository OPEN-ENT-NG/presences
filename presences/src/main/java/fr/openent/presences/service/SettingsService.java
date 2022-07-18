package fr.openent.presences.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.*;
import io.vertx.core.json.JsonObject;

public interface SettingsService {
    void retrieve(String structureId, Handler<Either<String, JsonObject>> handler);


    /**
     * @param structureId Structure identifier
     * @return Future {@link Future<JsonObject>}
     */
    Future<JsonObject> retrieve(String structureId);

    /**
     * Retrieve multiple slot setting.
     *
     * @param structureId Structure identifier
     * @return Future {@link Future<JsonObject>}
     */
    Future<JsonObject> retrieveMultipleSlots(String structureId);

    /**
     * @param structureId Structure identifier
     * @param settings json representing the settings data
     * @return Future {@link Future<JsonObject>}
     */
    Future<JsonObject> put(String structureId, JsonObject settings);
}
