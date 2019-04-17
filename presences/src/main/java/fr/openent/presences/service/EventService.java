package fr.openent.presences.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import org.entcore.common.user.UserInfos;

public interface EventService {
    /**
     * Create event
     *
     * @param event   event
     * @param user    user that create event
     * @param handler function handler returning data
     */
    void create(JsonObject event, UserInfos user, Handler<Either<String, JsonObject>> handler);

    /**
     * Update given event
     *
     * @param id      event identifier
     * @param event   event
     * @param handler Function handler returning data
     */
    void update(Integer id, JsonObject event, Handler<Either<String, JsonObject>> handler);

    /**
     * Delete given identifier
     *
     * @param id      event identifier
     * @param handler function handler returning data
     */
    void delete(Integer id, Handler<Either<String, JsonObject>> handler);
}
