package fr.openent.presences.service;

import fr.openent.presences.model.Event.EventBody;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import org.entcore.common.user.UserInfos;

public interface LatenessEventService {

    /**
     * create lateness event
     *
     * @param eventBody Event Body (using JsonObject to model)
     * @param userInfos userInfo
     * @param handler   function handler returning data JsonObject type
     */
    void create(EventBody eventBody, UserInfos userInfos, Handler<Either<String, JsonObject>> handler);

    /**
     * delete lateness event
     *
     * @param eventId   event identifier
     * @param eventBody Event Body (using JsonObject to model)
     * @param handler   function handler returning data JsonObject type
     */
    void update(Integer eventId, EventBody eventBody, Handler<Either<String, JsonObject>> handler);

}
