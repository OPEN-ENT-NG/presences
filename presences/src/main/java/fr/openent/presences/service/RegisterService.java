package fr.openent.presences.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.user.UserInfos;

public interface RegisterService {

    /**
     * List register based on given structure identifier, start date and end date
     *
     * @param structureId structure identifier
     * @param start       start date
     * @param end         end date
     * @param handler     function handler returning data
     */
    void list(String structureId, String start, String end, Handler<Either<String, JsonArray>> handler);

    /**
     * Create register
     *
     * @param register register
     * @param user     current user
     * @param handler  function handler returning data
     */
    void create(JsonObject register, UserInfos user, Handler<Either<String, JsonObject>> handler);

    /**
     * Update register status
     *
     * @param registerId Register identifier
     * @param status     new register status
     * @param handler    Function handler returning data
     */
    void updateStatus(Integer registerId, Integer status, Handler<Either<String, JsonObject>> handler);

    /**
     * Retrieve given register
     *
     * @param id      register identifier
     * @param handler Function handler returning data
     */
    void get(Integer id, Handler<Either<String, JsonObject>> handler);
}
