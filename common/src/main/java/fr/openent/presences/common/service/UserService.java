package fr.openent.presences.common.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;

import java.util.List;

public interface UserService {

    /**
     * Get list of user info (id, displayName)
     *
     * @param userIds userIds
     * @param handler Function handler returning data
     */
    void getUsers(List<String> userIds, Handler<Either<String, JsonArray>> handler);
}