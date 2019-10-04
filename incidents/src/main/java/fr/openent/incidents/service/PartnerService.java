package fr.openent.incidents.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public interface PartnerService {
    /**
     * get partners
     *
     * @param structureId   Structure Identifier
     * @param handler       Function handler returning data
     */
    void get(String structureId, Handler<Either<String, JsonArray>> handler);

    /**
     * create partner
     *
     * @param partnerBody  partnerBody fetched
     * @param handler      Function handler returning data
     */
    void create(JsonObject partnerBody, Handler<Either<String, JsonObject>> handler);

    /**
     * update partner
     *
     * @param partnerBody  partnerBody fetched
     * @param handler      Function handler returning data
     */
    void put(JsonObject partnerBody, Handler<Either<String, JsonObject>> handler);

    /**
     * delete partner
     *
     * @param partnerId partner Identifier
     * @param handler   Function handler returning data
     */
    void delete(Integer partnerId, Handler<Either<String, JsonObject>> handler);
}
