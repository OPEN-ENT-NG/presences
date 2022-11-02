package fr.openent.statistics_presences.service;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;

public interface StatisticsPresencesService {
    /**
     * Create incident
     *
     * @param studentIds users identifiers identifier
     * @param handler    function handler returning data
     */
    void create(String structureId, List<String> studentIds, Handler<AsyncResult<JsonObject>> handler);


    /**
     * process statistics task for a structure/student(s)
     * If we set isWaitingEndprocess 'true', this will simply call a method that will reply at the end
     * if false, this will call a worker that does the same thing but without replying
     *
     * @param structure             list structure identifier
     * @param studentIds            list student identifiers
     * @param isWaitingEndProcess   state to enable worker mode or not
     * @return Future JsonObject completing process
     */
    Future<JsonObject> processStatisticsPrefetch(List<String> structure, List<String> studentIds, Boolean isWaitingEndProcess);

    /**
     * Truncate user queue
     *
     * @return
     */
    Future<JsonArray> truncateUserQueue();
}
