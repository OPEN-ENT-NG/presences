package fr.openent.incidents.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public interface SeriousnessService {
    /**
     * get seriousnesses
     *
     * @param structureId   Structure Identifier
     * @param handler       Function handler returning data
     */
    void get(String structureId, Handler<Either<String, JsonArray>> handler);

    /**
     * create seriousness
     *
     * @param seriousnessBody       seriousnessBody fetched
     * @param handler               Function handler returning data
     */
    void create(JsonObject seriousnessBody, Handler<Either<String, JsonObject>> handler);

    /**
     * update seriousness
     *
     * @param seriousnessBody   seriousnessBody fetched
     * @param handler           Function handler returning data
     */
    void put(JsonObject seriousnessBody, Handler<Either<String, JsonObject>> handler);

    /**
     * delete seriousness
     *
     * @param seriousnessId seriousness Identifier
     * @param handler       Function handler returning data
     */
    void delete(Integer seriousnessId, Handler<Either<String, JsonObject>> handler);
}
