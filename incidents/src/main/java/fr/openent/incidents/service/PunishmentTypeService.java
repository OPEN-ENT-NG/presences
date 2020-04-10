package fr.openent.incidents.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public interface PunishmentTypeService {

    /**
     * get punishment type
     *
     * @param structure_id Structure Identifier
     * @param handler      Function handler returning data
     */
    void get(String structure_id, Handler<Either<String, JsonArray>> handler);

    /**
     * create punishment type
     *
     * @param punishmentTypeBody punishmentTypeBody fetched
     * @param handler            Function handler returning data
     */
    void create(JsonObject punishmentTypeBody, Handler<Either<String, JsonObject>> handler);

    /**
     * put punishment type
     *
     * @param punishmentTypeBody punishmentTypeBody fetched
     * @param handler            Function handler returning data
     */
    void put(JsonObject punishmentTypeBody, Handler<Either<String, JsonObject>> handler);

    /**
     * delete punishment type
     *
     * @param punishmentTypeId punishment_type identifier
     * @param handler          Function handler returning data
     */
    void delete(Integer punishmentTypeId, Handler<Either<String, JsonObject>> handler);
}
