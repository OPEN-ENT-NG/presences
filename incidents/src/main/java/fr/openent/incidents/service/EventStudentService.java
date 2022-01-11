package fr.openent.incidents.service;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

import java.util.List;

public interface EventStudentService {

    /**
     * Get events for dashboard parent
     */
    Future<JsonObject> get(String structureId, String studentId, List<String> types, String start, String end, String limit, String offset);

    Future<JsonObject> get(String structureId, List<String> studentIds, List<String> types, String startAt, String endAt, String limit, String offset);
}
