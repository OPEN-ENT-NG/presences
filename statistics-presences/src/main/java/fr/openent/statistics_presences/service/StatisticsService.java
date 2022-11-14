package fr.openent.statistics_presences.service;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;

public interface StatisticsService {
    /**
     * Delete all old statistics and save the new for students
     *
     * @param structureId Structure identifier
     * @param students List of student
     * @param values List of statistics value
     * @param handler handler
     * @deprecated Replaced by {@link #save(String, JsonArray, List, String, String, Handler)}
     */
    @Deprecated
    void save(String structureId, JsonArray students, List<JsonObject> values, Handler<AsyncResult<Void>> handler);

    /**
     * Delete all old statistics in range date and save the new for students
     *
     * @param structureId Structure identifier
     * @param students List of student
     * @param values List of statistics value
     * @param handler handler
     * @param startDate Start range date filter for delete old statistics
     * @param endDate End range date filter for delete old statistics
     */
    void save(String structureId, JsonArray students, List<JsonObject> values, String startDate, String endDate,
              Handler<AsyncResult<Void>> handler);

    /**
     * Override old statistics for one student by new value
     *
     * @param structureId Structure identifier
     * @param studentId Student identifier
     * @param values New statistics value
     * @param startDate Start range date filter for delete old statistics
     * @param endDate End range date filter for delete old statistics
     * @return Future
     */
    Future<List<JsonObject>> overrideStatisticsStudent(String structureId, String studentId, List<JsonObject> values, String startDate,
                                                       String endDate);
}
