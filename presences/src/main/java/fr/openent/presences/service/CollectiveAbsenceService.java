package fr.openent.presences.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.user.UserInfos;

import java.util.List;

public interface CollectiveAbsenceService {

    /**
     * get absences
     *
     * @param structureId Structure identifier
     * @param startDate   startDate
     * @param endDate     endDate
     * @param handler     Function handler returning data
     */
    void getCollectives(String structureId, String startDate, String endDate, Long reasonId, Boolean regularized,
                        List<String> audienceNames, Integer page, Handler<AsyncResult<JsonObject>> handler);

    void get(String structureId, Long collectiveId, Handler<AsyncResult<JsonObject>> handler);

    void getCollectiveFromAbsence(Long absenceId, Handler<AsyncResult<JsonObject>> handler);

    void getAbsencesStatus(String structureId, List<String> studentIds, String startDate, String endDate,
                           Long collectiveId, Handler<AsyncResult<JsonObject>> handler);

    /**
     * create absence
     *
     * @param collectiveBody collectiveBody fetched
     * @param user        userInfo
     * @param handler     Function handler returning data
     */
    void create(JsonObject collectiveBody, UserInfos user, String structureId, Handler<AsyncResult<JsonObject>> handler);

    /**
     * update absence
     *
     * @param handler     Function handler returning data
     */
    void update(JsonObject collectiveBody, UserInfos user, String structureId, Long collectiveId, Handler<AsyncResult<JsonObject>> handler);


    /**
     * Delete absence from collective absence.
     * @param studentIds    list of student identifiers
     * @param structureId   structure identifier
     * @param collectiveId  collective absence identifier
     * @param handler       Function handler returning data
     */
    void removeAbsenceFromCollectiveAbsence(JsonObject studentIds, String structureId, Long collectiveId, Handler<AsyncResult<JsonObject>> handler);

    /**
     *
     * @param structureId   structure identifier
     * @param collectiveId  collective identifier
     * @param handler       Function handler returning data
     */
    void removeAudiencesRelation(String structureId, Long collectiveId, Handler<AsyncResult<JsonObject>> handler);

    /**
     * delete absence
     *
     * @param handler   Function handler returning data
     */
    void delete(Long id, String structureId, Handler<AsyncResult<JsonObject>> handler);


    /**
     * Retrieve collective absences for CSV export
     *
     * @param structureId   structure identifier
     * @param startDate     start date range
     * @param endDate       end date range
     * @param handler       Function handler returning data. Returns a JsonArray
     */
    void getCSV(String structureId, String startDate, String endDate, Handler<AsyncResult<JsonArray>> handler);
}
