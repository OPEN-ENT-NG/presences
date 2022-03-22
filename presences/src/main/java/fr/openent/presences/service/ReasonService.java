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
     * @param structureId  Structure Identifier
     * @param reasonTypeId Reason type Identifier
     * @param handler      Function handler returning data
     */
    void get(String structureId, Integer reasonTypeId, Handler<Either<String, JsonArray>> handler);

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
     * delete reason
     *
     * @param reasonId Reason Identifier
     * @param handler  Function handler returning data
     */
    void delete(Integer reasonId, Handler<Either<String, JsonObject>> handler);

    /**
     * get absence reasons from structure id and reason type id
     *
     * @param structureId Structure Identifier
     * @param handler     Function handler returning data
     */
    void fetchAbsenceReason(String structureId, Handler<Either<String, JsonArray>> handler);

    /**
     * get reasons from structure id and reason type id
     *
     * @param structureId  Structure Identifier
     * @param reasonTypeId Reason type Identifier
     * @return {@link Future} of {@link JsonObject}
     */
    Future<JsonArray> fetchReason(String structureId, Integer reasonTypeId);

    /**
     * get absence reasons from structure id and reason type id
     *
     * @param structureId Structure Identifier
     * @return {@link Future} of {@link JsonObject}
     */
    Future<JsonArray> fetchAbsenceReason(String structureId);

    /**
     * get absence reasons from structure i
     *
     * @param reasonIds Reason Identifier
     * @param handler   Function handler returning data
     */
    void getReasons(List<Integer> reasonIds, Handler<Either<String, JsonArray>> handler);
}
