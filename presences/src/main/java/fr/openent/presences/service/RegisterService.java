package fr.openent.presences.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.user.UserInfos;

import java.util.List;

public interface RegisterService {

    /**
     * List register based on given structure identifier, start date and end date
     *
     * @param structureId structure identifier
     * @param start       start date
     * @param end         end date
     * @param handler     function handler returning data
     */
    void list(String structureId, String start, String end, Handler<Either<String, JsonArray>> handler);

    void list(String structureId, List<String> courseIds, Handler<Either<String, JsonArray>> handler);

    /**
     * Create register
     *
     * @param register register
     * @param user     current user
     * @param handler  function handler returning data
     */
    void create(JsonObject register, UserInfos user, Handler<Either<String, JsonObject>> handler);

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
