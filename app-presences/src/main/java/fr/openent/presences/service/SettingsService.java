package fr.openent.presences.service;

import fr.openent.presences.model.Settings;
import fr.wseduc.webutils.Either;
import io.vertx.core.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.Map;

public interface SettingsService {

    /**
     * Retrieve presences settings with a handler.
     *
     * @param structureId Structure identifier
     * @param handler handler
     * @deprecated Replaced by {@link #retrieveSettings(String)}
     */
    @Deprecated
    void retrieve(String structureId, Handler<Either<String, JsonObject>> handler);

    /**
     * Retrieve presences settings with a future of JsonObject.
     *
     * @param structureId Structure identifier
     * @deprecated Replaced by {@link #retrieveSettings(String)}
     */
    @Deprecated
    Future<JsonObject> retrieve(String structureId);

    /**
     * Retrieve presences settings.
     *
     * @param structureId Structure identifier
     */
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
