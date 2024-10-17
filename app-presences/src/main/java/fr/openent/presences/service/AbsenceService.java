package fr.openent.presences.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.*;
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

    Future<JsonArray> get(String structureId, String startDate, String endDate, List<String> users);

    /**
     * get absences / count with paginate
     *
     * @param structureId Structure identifier
     * @param teacherId   teacher identifier
     * @param audienceIds audience identifiers
     * @param studentIds  student identifiers
     * @param reasonIds   reason identifiers
     * @param startAt     date from which we want to retrieve absences
     * @param regularized filter on regularized or not
     * @param followed    filter on followed or not
     * @param halfBoarder filter on half boarder students or not
     * @param internal    filter on internal students or not
     * @param page        page number
     * @return Future returning data with page data / Future returning count
     */
    Future<JsonObject> get(String structureId, String teacherId, List<String> audienceIds, List<String> studentIds,
                           List<Integer> reasonIds, String startAt, String endAt,
                           Boolean regularized, Boolean noReason, Boolean followed, Boolean halfBoarder, Boolean internal, Integer page);

    Future<JsonObject> count(String structure, List<String> students, String start, String end, Boolean regularized,
                             Boolean noReason, Boolean followed, List<Integer> reasons);

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

    void getAbsencesBetweenDates(String startDate, String endDate, List<String> users, String structureId, Handler<Either<String, JsonArray>> handler);

    Future<JsonArray> getAbsencesBetweenDates(String startDate, String endDate, List<String> users, String structureId);

    void getAbsencesFromCollective(String structureId, Long collectiveId, Handler<Either<String, JsonArray>> handler);

    /**
     * create absence
     *
     * @param absenceBody  absenceBody fetched
     * @param user         userInfo
     * @param collectiveId absence collective identifier (nullable)
     * @param handler      Function handler returning data
     */
    void create(JsonObject absenceBody, List<String> studentIds, UserInfos user, Long collectiveId, Handler<AsyncResult<JsonArray>> handler);

    void create(JsonObject absenceBody, List<String> studentIds, String userId, Long collectiveId, Handler<AsyncResult<JsonArray>> handler);

    void create(JsonObject absenceBody, UserInfos user, boolean editEvents, Handler<Either<String, JsonObject>> handler);

    /**
     * @param collectiveId collective absence identifier
     * @param structureId  structure identifier
     * @param userInfoId   user logged identifier
     * @param editEvents   if events corresponding to absences have to be edited
     * @param handler      response handler
     */
    void afterPersistCollective(Long collectiveId, String structureId, String userInfoId, boolean editEvents, Handler<AsyncResult<JsonObject>> handler);

    void afterPersist(List<String> studentIds, String structureId, String startDate, String endDate, String userInfoId, boolean editEvents, Handler<AsyncResult<JsonObject>> handler);

    /**
     * update absence
     *
     * @param absenceId   absence identifier used to update absence
     * @param absenceBody absenceBody fetched
     * @param handler     Function handler returning data
     */
    void update(Long absenceId, JsonObject absenceBody, UserInfos user, boolean editEvents, Handler<Either<String, JsonObject>> handler);

    void update(Long absenceId, JsonObject absenceBody, String userInfoId, boolean editEvents, Handler<Either<String, JsonObject>> handler);

    void updateFromCollective(JsonObject absenceBody, UserInfos user, Long collectiveId, boolean editEvents, Handler<AsyncResult<JsonObject>> handler);

    /**
     * update absence reason
     *
     * @param absence Absence object from client to update absence reasons
     *                JsonObject : {Ids: JsonArray, reasonId: Integer}
     * @param user    userInfo
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
     * Update absence followed state
     *
     * @param absenceIds absences identifiers
     * @param followed   followed status
     * @param handler    Function handler returning data
     */
    void followAbsence(JsonArray absenceIds, Boolean followed, Handler<Either<String, JsonObject>> handler);

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
     * @param regularized
     * @param followed
     * @param reasons
     * @param page
     * @return
     */
    Future<JsonArray> retrieve(String structure, List<String> students, String start, String end, Boolean regularized,
                               Boolean followed, Boolean noReason, List<Integer> reasons, Integer page);


    /**
     * Get all student ids for absent students from the structure
     * in a given period
     *
     * @param structureId structure identifier
     * @param studentIds  student identifiers to filter
     * @param startAt     date from which we want to count students
     * @param endAt       date until which we want to count students
     * @return future that contain count result.
     */
    Future<JsonObject> countAbsentStudents(String structureId, List<String> studentIds, String startAt, String endAt);


    /**
     * Restore absences with bad structure with the correct one
     *
     * @param structureId structure identifier that own absences to restore
     * @param startAt     date from which we need to retrieve absences to restore
     * @param endAt       date until which we need to retrieve absences to restore
     * @return result if restore worked
     */
    Future<JsonObject> restoreAbsences(String structureId, String startAt, String endAt);
}
