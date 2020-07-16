package fr.openent.incidents.service;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonObject;

public interface EventStudentService {

    /**
     * Get events for dashboard parent
     *
     * @param body              data to filter
     * @param handler           Function handler returning data
     */
    void get(MultiMap body, Handler<AsyncResult<JsonObject>> handler);
}
