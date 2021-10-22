package fr.openent.presences.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.*;
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
     * @param types       alert types
     * @param handler     function handler returning data
     */
    void getAlertsStudents(String structureId, List<String> types, List<String> students, Handler<Either<String, JsonArray>> handler);

    /**
     * Get student alert number by given type with the corresponding threshold
     *
     * @param structureId structure identifier
     * @param studentId   student identifier
     * @param type        alert type
     * @param handler     function handler returning data
     */
    void getStudentAlertNumberWithThreshold(String structureId, String studentId, String type, Handler<Either<String, JsonObject>> handler);

    /**
     * Delete alerts of given type for a student
     * @param structureId   structure identifier
     * @param studentId     student identifier
     * @param type          alert type
     * @return  {@link Future} of {@link JsonObject}
     */
    Future<JsonObject> resetStudentAlertsCount(String structureId, String studentId, String type);
}