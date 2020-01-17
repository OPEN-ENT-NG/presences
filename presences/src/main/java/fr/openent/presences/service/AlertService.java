package fr.openent.presences.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;

public interface AlertService {
    /**
     * Delete alerts based on given identifiers.
     * Alert deletion trigger plsql trigger that create a new row in history table.
     *
     * @param alerts  alert identifiers
     * @param handler function handler returning data
     */
    void delete(List<String> alerts, Handler<Either<String, JsonObject>> handler);

    /**
     * Get alerts count
     *
     * @param structureId structure identifier
     * @param handler     function handler returning data
     */
    void getSummary(String structureId, Handler<Either<String, JsonObject>> handler);

    /**
     * Get alerts for all students
     *
     * @param structureId structure identifier
     * @param types alert types
     * @param handler function handler returning data
     */
    void getAlertsStudents(String structureId, List<String> types, Handler<Either<String, JsonArray>> handler);
}