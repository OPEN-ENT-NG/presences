package fr.openent.presences.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.user.UserInfos;

import java.util.List;

public interface RegisterService {

    /**
     * List registers based on given structure identifier, start date and end date
     *
     * @param structureId structure identifier
     * @param start       start date filter (format YYY-MM-DD)
     * @param end         end date filter (format YYY-MM-DD)
     * @param handler     function {@link Handler} returning data
     */
    void list(String structureId, String start, String end, Handler<Either<String, JsonArray>> handler);

    /**
     *  List registers based on given parameters
     * @param structureId           structure identifier
     * @param start                 start date filter (format YYY-MM-DD)
     * @param end                   end date filter (format YYY-MM-DD)
     * @param courseIds             {@link List} of course identifier
     * @param teacherIds            {@link List} of teacher identifiers
     * @param groupIds              {@link List} of group identifiers
     * @param forgottenFilter       true -> fetch only forgotten registers;
     *                              false/null -> fetch all registers
     * @param isWithTeacherFilter   true -> only retrieve registers with teachers
     * @param limit                 limit of registers
     * @param offset                offset to get registers
     * @param handler               function {@link Handler} returning data
     */
    void list(String structureId, String start, String end, List<String> courseIds,
              List<String> teacherIds, List<String> groupIds, boolean forgottenFilter, Boolean isWithTeacherFilter,
              String limit, String offset, Handler<Either<String, JsonArray>> handler);

    /**
     *  List registers from course identifiers
     * @param structureId       structure identifier
     * @param courseIds         {@link List} of course identifier
     * @param handler           function {@link Handler} returning data
     */
    void list(String structureId, List<String> courseIds, Handler<Either<String, JsonArray>> handler);

    /**
     *  List registers based on given parameters
     * @param structureId           structure identifier
     * @param start                 start date filter (format YYY-MM-DD)
     * @param end                   end date filter (format YYY-MM-DD)
     * @param courseIds             {@link List} of course identifier
     * @param teacherIds            {@link List} of teacher identifiers
     * @param groupIds              {@link List} of group identifiers
     * @param forgottenFilter       true -> fetch only forgotten registers;
     *                              false/null -> fetch all registers
     * @param isWithTeacherFilter   true -> only retrieve registers with teachers
     * @param limit                 limit of registers
     * @param offset                offset to get registers
     * @return {@link Future} of {@link List}
     */
    Future<JsonArray> list(String structureId, String start, String end, List<String> courseIds,
                           List<String> teacherIds, List<String> groupIds, boolean forgottenFilter,
                           Boolean isWithTeacherFilter, String limit, String offset);

    /**
     * Create register
     *
     * @param register register
     * @param user     current user
     * @param handler  function handler returning data
     */
    void create(JsonObject register, UserInfos user, Handler<Either<String, JsonObject>> handler);

    /**
     * Create multiple registers in given time period
     * @param structureId   structure identifier
     * @param startDate     start date
     * @param endDate       end date
     * @return  {@link Future} of {@link JsonObject}
     */
    Future<JsonObject> createMultipleRegisters(String structureId, String startDate, String endDate);


    /**
     * Create not existing registers based on courses for given structure and dates
     *
     * @param startDate             start date filter
     * @param endDate               end date filter
     * @param startTime             start time filter
     * @param endTime               end time filter
     * @param result                result object for worker
     * @param structureId           structure identifier
     * @param crossDateFilter       cross date filter (true : get courses beginning < start date and finishing end date)
     * @param promise               promise handling data
     */
    void createStructureCoursesRegisterFuture(String startDate, String endDate, String startTime, String endTime, JsonObject result,
                                              String structureId, String crossDateFilter, Promise<JsonObject> promise);

    /**
     * Update register status
     *
     * @param registerId Register identifier
     * @param status     new register status
     * @param handler    Function handler returning data
     */
    void updateStatus(Integer registerId, Integer status, Handler<Either<String, JsonObject>> handler);

    /**
     * Retrieve given register
     *
     * @param id      register identifier
     * @param handler Function handler returning data
     */
    void get(Integer id, Handler<Either<String, JsonObject>> handler);

    /**
     * get register only without extra data
     *
     * @param id      register identifier
     * @return Register from SQL
     */
    Future<JsonObject> fetchRegister(Integer id);

    /**
     * Check if a register exists for given information
     *
     * @param courseId  Course identifier
     * @param startDate Start date
     * @param endDate   End date
     * @param handler   Function handler returning data
     */
    void exists(String courseId, String startDate, String endDate, Handler<Either<String, JsonObject>> handler);

    /**
     * Check if given course exists. It use course identifier (_id), start date course (startDate) and end date course (endDate).
     *
     * @param registerId Register identifier
     * @param handler    Function handler returning data
     */
    void setNotified(Long registerId, Handler<Either<String, JsonObject>> handler);

    /**
     * List the last 16 courses with forgotten registers before the given date.
     * @param structureId       Structure identifier
     * @param teacherIds        Teacher identifiers
     * @param groupNames        Group names
     * @param startDate         Start date
     * @param endDate           End date
     * @param multipleSlot      Multiple slot admin setting value
     * @param handler           Function handler returning data
     */
    void getLastForgottenRegistersCourses(String structureId, List<String> teacherIds, List<String> groupNames,
                                          String startDate, String endDate, boolean multipleSlot, Handler<AsyncResult<JsonArray>> handler);
}
