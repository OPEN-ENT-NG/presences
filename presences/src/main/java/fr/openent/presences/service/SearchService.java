package fr.openent.presences.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;

import java.util.List;

public interface SearchService {

    /**
     * Search for a user or a group
     *
     * @param query       query search
     * @param structureId Structure identifier
     * @param handler     Function handler returning data
     */
    void search(String query, String structureId, Handler<Either<String, JsonArray>> handler);

    /**
     * Search for classes/groups
     *
     * @param query        query string for research
     * @param fields       list of fields
     * @param structure_id structure identifier
     * @param handler      Function handler returning data
     */
    void searchGroups(String query, List<String> fields, String structure_id, Handler<Either<String, JsonArray>> handler);
}