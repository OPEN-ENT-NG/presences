package fr.openent.incidents.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public interface ProtagonistTypeService {
    /**
     * get protagonistsType
     *
     * @param structureId   Structure Identifier
     * @param handler       Function handler returning data
     */
    void get(String structureId, Handler<Either<String, JsonArray>> handler);

    /**
     * create protagonistType
     *
     * @param protagonistTypeBody   protagonistTypeBody fetched
     * @param handler               Function handler returning data
     */
    void create(JsonObject protagonistTypeBody, Handler<Either<String, JsonObject>> handler);

    /**
     * update protagonistType
     *
     * @param protagonistTypeBody   protagonistTypeBody fetched
     * @param handler               Function handler returning data
     */
    void put(JsonObject protagonistTypeBody, Handler<Either<String, JsonObject>> handler);

    /**
     * delete protagonistType
     *
     * @param protagonistTypeId protagonistType Identifier
     * @param handler           Function handler returning data
     */
    void delete(Integer protagonistTypeId, Handler<Either<String, JsonObject>> handler);
}
