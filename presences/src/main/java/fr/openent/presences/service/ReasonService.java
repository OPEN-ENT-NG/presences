package fr.openent.presences.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;

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

    void delete(Integer reasonId, Handler<Either<String, JsonObject>> handler);

    void fetchReason(String structureId, Handler<Either<String, JsonArray>> handler);

    Future<JsonArray> fetchReason(String structureId);

    /**
     * get reasons
     *
     * @param reasonIds Reason Identifier
     * @param handler   Function handler returning data
     */
    void getReasons(List<Integer> reasonIds, Handler<Either<String, JsonArray>> handler);
}
