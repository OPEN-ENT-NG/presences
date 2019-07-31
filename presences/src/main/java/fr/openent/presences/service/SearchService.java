package fr.openent.presences.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;

public interface SearchService {

    /**
     * Search for a user or a group
     *
     * @param query       query search
     * @param structureId Structure identifier
     * @param handler     Function handler returning data
     */
    void search(String query, String structureId, Handler<Either<String, JsonArray>> handler);
}
