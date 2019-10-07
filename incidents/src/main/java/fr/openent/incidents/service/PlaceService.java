package fr.openent.incidents.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public interface PlaceService {
    /**
     * get places
     *
     * @param structureId   Structure Identifier
     * @param handler       Function handler returning data
     */
    void get(String structureId, Handler<Either<String, JsonArray>> handler);

    /**
     * create place
     *
     * @param placeBody     placeBody fetched
     * @param handler       Function handler returning data
     */
    void create(JsonObject placeBody, Handler<Either<String, JsonObject>> handler);

    /**
     * update place
     *
     * @param placeBody     placeBody fetched
     * @param handler       Function handler returning data
     */
    void put(JsonObject placeBody, Handler<Either<String, JsonObject>> handler);

    /**
     * delete place
     *
     * @param placeId   place Identifier
     * @param handler   Function handler returning data
     */
    void delete(Integer placeId, Handler<Either<String, JsonObject>> handler);
}
