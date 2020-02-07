package fr.openent.presences.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.user.UserInfos;

import java.util.List;

public interface PresenceService {

    /**
     * Fetch presences
     *
     * @param structureId structure identifier
     * @param startDate   start date
     * @param endDate     end date
     * @param userIds     userId neo
     * @param ownerIds    ownerIds (teacher...personal..)
     * @param handler     function handler returning data
     */
    void get(String structureId, String startDate, String endDate, List<String> userIds,
             List<String> ownerIds, Handler<Either<String, JsonArray>> handler);

    /**
     * Create presence
     *
     * @param user         user infos
     * @param presenceBody presenceBody Object
     * @param handler      function handler returning data
     */
    void create(UserInfos user, JsonObject presenceBody, Handler<Either<String, JsonObject>> handler);

    /**
     * Update an presence
     *
     * @param presenceBody presence object
     * @param handler      function handler returning data
     */
    void update(JsonObject presenceBody, Handler<Either<String, JsonObject>> handler);

    /**
     * Delete presence
     *
     * @param presenceId presence identifier
     * @param handler    function handler returning data
     */
    void delete(String presenceId, Handler<Either<String, JsonObject>> handler);
}
