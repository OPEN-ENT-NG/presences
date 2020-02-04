package fr.openent.presences.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public interface ActionService {
    /**
     * get actions
     *
     * @param structureId Structure Identifier
     * @param handler     Function handler returning data
     */
    void get(String structureId, Handler<Either<String, JsonArray>> handler);

    /**
     * create action
     *
     * @param actionBody actionBody fetched
     * @param handler    Function handler returning data
     */
    void create(JsonObject actionBody, Handler<Either<String, JsonObject>> handler);

    /**
     * put reason
     *
     * @param actionBody actionBody fetched
     * @param handler    Function handler returning data
     */
    void put(JsonObject actionBody, Handler<Either<String, JsonObject>> handler);

    /**
     * delete reason
     *
     * @param actionId action identifier
     * @param handler  Function handler returning data
     */
    void delete(Integer actionId, Handler<Either<String, JsonObject>> handler);
}
