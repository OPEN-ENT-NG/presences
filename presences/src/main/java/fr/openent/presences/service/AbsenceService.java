package fr.openent.presences.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.user.UserInfos;

import java.util.List;

public interface AbsenceService {

    /**
     * get absences
     *
     * @param structureId Structure identifier
     * @param startDate   startDate
     * @param endDate     endDate
     * @param users       List of users
     * @param handler     Function handler returning data
     */
    void get(String structureId, String startDate, String endDate, List<String> users, Handler<Either<String, JsonArray>> handler);


    /**
     * get absences in events
     *
     * @param structureId Structure identifier
     * @param startDate   startDate
     * @param endDate     endDate
     * @param users       List of users
     * @param handler     Function handler returning data
     */
    void getAbsenceInEvents(String structureId, String startDate, String endDate, List<String> users, Handler<Either<String, JsonArray>> handler);

    /**
     * get absence identifier
     *
     * @param absenceId absence identifier to fetch
     * @param handler   Function handler returning data
     */
    void getAbsenceId(Integer absenceId, Handler<Either<String, JsonObject>> handler);


    /**
     * fetch absences between two dates chosen
     *
     * @param handler Function handler returning data
     */
    void getAbsencesBetween(String startDate, String endDate, List<String> users, Handler<Either<String, JsonArray>> handler);
    void getAbsencesBetweenDates(String startDate, String endDate, List<String> users, Handler<Either<String, JsonArray>> handler);

    void getAbsencesFromCollective(String structureId, Long collectiveId, Handler<Either<String, JsonArray>> handler);

    /**
     * create absence
     *
     * @param absenceBody   absenceBody fetched
     * @param user          userInfo
     * @param collectiveId  absence collective identifier (nullable)
     * @param handler       Function handler returning data
     */
    void create(JsonObject absenceBody, List<String> studentIds, UserInfos user, Long collectiveId, Handler<AsyncResult<JsonArray>> handler);
    void create(JsonObject absenceBody, UserInfos user, boolean editEvents, Handler<Either<String, JsonObject>> handler);

    /**
     *
     * @param collectiveId collective absence identifier
     * @param structureId  structure identifier
     * @param userInfoId   user logged identifier
     * @param editEvents   if events corresponding to absences have to be edited
     * @param handler      response handler
     */
    void afterPersistCollective(Long collectiveId, String structureId, String userInfoId, boolean editEvents, Handler<AsyncResult<JsonObject>> handler);

    /**
     * update absence
     *
     * @param absenceId   absence identifier used to update absence
     * @param absenceBody absenceBody fetched
     * @param handler     Function handler returning data
     */
    void update(Long absenceId, JsonObject absenceBody, UserInfos user, boolean editEvents, Handler<Either<String, JsonObject>> handler);
    void updateFromCollective(JsonObject absenceBody, UserInfos user, Long collectiveId, boolean editEvents, Handler<AsyncResult<JsonObject>> handler);

    /**
     * update absence reason
     *
     * @param absence Absence object from client to update absence reasons
     *                JsonObject : {Ids: JsonArray, reasonId: Integer}
     * @param user        userInfo
     * @param handler Function handler returning data
     */
    void changeReasonAbsences(JsonObject absence, UserInfos user, Handler<Either<String, JsonObject>> handler);

    /**
     * update absence regularisation
     *
     * @param absence Absence object from client to update absence regularized
     *                JsonObject : {Ids: JsonArray, regularized: Boolean}
     * @param user    userInfo
     * @param handler Function handler returning data
     */
    void changeRegularizedAbsences(JsonObject absence, UserInfos user, Handler<Either<String, JsonObject>> handler);

    void changeRegularizedAbsences(JsonObject absence, UserInfos user, boolean editEvents, Handler<Either<String, JsonObject>> handler);

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
     * @param handler Function handler returning data
     */
    void absenceRemovalTask(Handler<Either<String, JsonObject>> handler);

    /**
     * Retrieve absences based on parameters.
     * It returns:
     * - identifier
     * - start date, end date
     * - student object containing student identifier and student name
     * - reason identifier
     * - counsellor regularisation
     *
     * @param structure
     * @param students
     * @param start
     * @param end
     * @param justified
     * @param regularized
     * @param reasons
     * @param handler
     */
    void retrieve(String structure, List<String> students, String start, String end, Boolean justified, Boolean regularized, List<Integer> reasons, Handler<Either<String, JsonArray>> handler);

    /**
     * Get all student ids for absent students from the structure
     * in a given period
     *
     * @param structureId       structure identifier
     * @param currentDate       current date and hour
     * @param handler           function handler returning data
     */
    void getAbsentStudentIds(String structureId, String currentDate, Handler<Either<String, JsonArray>> handler);
}
