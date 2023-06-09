package fr.openent.presences.service;

import fr.openent.presences.model.Settings;
import fr.wseduc.webutils.Either;
import io.vertx.core.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.Map;

public interface SettingsService {
    void retrieve(String structureId, Handler<Either<String, JsonObject>> handler);

    /**
     * @deprecated Replaced by {@link #retrieveSettings(String)}
     */
    Future<JsonObject> retrieve(String structureId);

    Future<Settings> retrieveSettings(String structureId);

    /**
     * Retrieve multiple slot setting.
     *
     * @param structureId Structure identifier
     * @return Future {@link Future<JsonObject>}
     */
    Future<JsonObject> retrieveMultipleSlots(String structureId);

    Future<JsonObject> put(String structureId, JsonObject settings);
}
