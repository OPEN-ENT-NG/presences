package fr.openent.statistics_presences.service;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
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
     * Add user in deleted table.
     *
     * @param structure list structure identifier
     * @return Future JsonObject completing process
     */
    Future<JsonObject> processStatisticsPrefetch(List<String> structure);
}
