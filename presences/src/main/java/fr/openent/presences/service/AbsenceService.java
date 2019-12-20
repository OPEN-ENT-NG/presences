package fr.openent.presences.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.user.UserInfos;

import java.util.List;

public interface AbsenceService {

    /**
     * get absences
     *
     * @param handler       Function handler returning data
     */
    void get(String startDate, String endDate, List<String> users, Handler<Either<String, JsonArray>> handler);


    /**
     * get absence identifier
     *
     * @param absenceId     absence identifier to fetch
     * @param handler       Function handler returning data
     */
    void getAbsenceId(Integer absenceId, Handler<Either<String, JsonObject>> handler);


    /**
     * fetch absences between two dates chosen
     *
     * @param handler       Function handler returning data
     */
    void getAbsencesBetween(String startDate, String endDate, List<String> users, Handler<Either<String, JsonArray>> handler);

    /**
     * create absence
     *
     * @param absenceBody   absenceBody fetched
     * @param user          userInfo
     * @param handler       Function handler returning data
     */
    void create(JsonObject absenceBody, UserInfos user, Handler<Either<String, JsonObject>> handler);

    /**
     * update absence
     *
     * @param absenceId   absence identifier used to update absence
     * @param absenceBody absenceBody fetched
     * @param handler     Function handler returning data
     */
    void update(Integer absenceId, JsonObject absenceBody, UserInfos user, Handler<Either<String, JsonObject>> handler);

    /**
     * update absence reason
     *
     * @param absence Absence object from client to update absence reasons
     *                JsonObject : {Ids: JsonArray, reasonId: Integer}
     * @param handler Function handler returning data
     */
    void changeReasonAbsences(JsonObject absence, Handler<Either<String, JsonObject>> handler);

    /**
     * update absence regularisation
     *
     * @param absence Absence object from client to update absence regularized
     *                JsonObject : {Ids: JsonArray, regularized: Boolean}
     * @param handler Function handler returning data
     */
    void changeRegularizedAbsences(JsonObject absence, Handler<Either<String, JsonObject>> handler);

    /**
     * delete absence
     *
     * @param absenceId absence identifier used to delete absence
     * @param handler   Function handler returning data
     */
    void delete(Integer absenceId, Handler<Either<String, JsonObject>> handler);

    /**
     * CRON task that automatically removes absence that have expired 3 days (72 hours) before date.now()
     *
     * @param handler       Function handler returning data
     */
    void absenceRemovalTask(Handler<Either<String, JsonObject>> handler);
}
