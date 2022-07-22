package fr.openent.presences.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.Map;

public interface AlertService {
    /**
     * Delete alerts based on filter required
     * Alert deletion trigger plsql trigger that create a new row in history table.
     *
     * @param structureId       Structure identifier
     * @param deletedAlertMap   Map with key the student id and in value the alert type witch must be deleted
     *                          null -> delete All, empty -> do nothing
     * @param startAt           start date from which we need remove alerts
     * @param endAt             end date until which we need remove alerts
     * @return request result Future
     */
    Future<JsonObject> delete(String structureId, Map<String, List<String>> deletedAlertMap, String startAt, String endAt);

    /**
     * Get alerts count
     *
     * @param structureId structure identifier
     */
    Future<JsonObject> getSummary(String structureId);

    /**
     * Get alerts for all students
     *
     * @param structureId structure identifier
     * @param types       alert types
     */
    Future<JsonArray> getAlertsStudents(String structureId, List<String> types, List<String> students, String startAt, String endAt);

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
     *
     * @param structureId structure identifier
     * @param studentId   student identifier
     * @param type        alert type
     * @return {@link Future} of {@link JsonObject}
     */
    Future<JsonObject> resetStudentAlertsCount(String structureId, String studentId, String type);
}