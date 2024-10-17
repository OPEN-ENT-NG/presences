package fr.openent.presences.service;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

import java.util.List;

public interface EventStudentService {

    /**
     * Get events for dashboard parent
     *
     * @param structureId structure identifier
     * @param studentId   student identifier
     * @param types       event types needed
     * @param start       start date to get events
     * @param end         end date to get events
     * @param limit       data to filter
     * @param offset      data to filter
     * @return future returning data
     */
    Future<JsonObject> get(String structureId, String studentId, List<String> types, String start, String end, String limit, String offset);

    Future<JsonObject> get(String structureId, List<String> studentIds, List<String> types, String start, String end, String limit, String offset);
}
