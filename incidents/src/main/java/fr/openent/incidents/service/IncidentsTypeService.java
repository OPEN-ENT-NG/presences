package fr.openent.incidents.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public interface IncidentsTypeService {

    /**
     * get incidents type
     *
     * @param structureId   Structure Identifier
     * @param handler       Function handler returning data
     */
    void get(String structureId, Handler<Either<String, JsonArray>> handler);

    /**
     * create incident type
     *
     * @param incidentTypeBody  incidentTypeBody fetched
     * @param handler           Function handler returning data
     */
    void create(JsonObject incidentTypeBody, Handler<Either<String, JsonObject>> handler);

    /**
     * update incident type
     *
     * @param incidentTypeBody  incidentTypeBody fetched
     * @param handler           Function handler returning data
     */
    void put(JsonObject incidentTypeBody, Handler<Either<String, JsonObject>> handler);

    /**
     * delete incident type
     *
     * @param incidentTypeId    Reason Identifier
     * @param handler           Function handler returning data
     */
    void delete(Integer incidentTypeId, Handler<Either<String, JsonObject>> handler);
}
