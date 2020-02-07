package fr.openent.presences.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;


public interface DisciplineService {

    /**
     * get disciplines
     *
     * @param structureId Structure Identifier
     * @param handler     Function handler returning data
     */
    void get(String structureId, Handler<Either<String, JsonArray>> handler);

    /**
     * create discipline
     *
     * @param disciplineBody disciplineBody fetched
     * @param handler        Function handler returning data
     */
    void create(JsonObject disciplineBody, Handler<Either<String, JsonObject>> handler);

    /**
     * put discipline
     *
     * @param disciplineBody disciplineBody fetched
     * @param handler        Function handler returning data
     */
    void put(JsonObject disciplineBody, Handler<Either<String, JsonObject>> handler);

    /**
     * delete discipline
     *
     * @param disciplineId discipline identifier
     * @param handler      Function handler returning data
     */
    void delete(Integer disciplineId, Handler<Either<String, JsonObject>> handler);
}
