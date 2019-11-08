package fr.openent.presences.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public interface ReasonService {

    /**
     * get reasons
     *
     * @param structureId Structure Identifier
     * @param handler     Function handler returning data
     */
    void get(String structureId, Handler<Either<String, JsonArray>> handler);

    /**
     * create reason
     *
     * @param reasonBody reasonBody fetched
     * @param handler    Function handler returning data
     */
    void create(JsonObject reasonBody, Handler<Either<String, JsonObject>> handler);

    /**
     * put reason
     *
     * @param reasonBody reasonBody fetched
     * @param handler    Function handler returning data
     */
    void put(JsonObject reasonBody, Handler<Either<String, JsonObject>> handler);

    /**
     * get reasons
     *
     * @param reasonId Reason Identifier
     * @param handler  Function handler returning data
     */
    void delete(Integer reasonId, Handler<Either<String, JsonObject>> handler);

    void fetchReason(String structureId, Handler<Either<String, JsonArray>> handler);

}
