package fr.openent.presences.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.user.UserInfos;

import java.util.List;

public interface EventStudentService {

    /**
     * Get events for dashboard parent
     *
     * @param body              data to filter
     * @param handler           Function handler returning data
     */
    void get(MultiMap body, Handler<AsyncResult<JsonObject>> handler);
}
