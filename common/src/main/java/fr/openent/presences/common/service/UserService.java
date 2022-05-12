package fr.openent.presences.common.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;

public interface UserService {

    /**
     * Get list of user info (id, displayName)
     *
     * @param userIds userIds
     * @param handler Function handler returning data
     * @return void || Future returning data
     */
    void getUsers(List<String> userIds, Handler<Either<String, JsonArray>> handler);

    Future<JsonArray> getUsers(List<String> userIds);

    /**
     * Get list of students info (id, displayName, className)
     *
     * @param students/studentIds List of student identifiers
     * @param handler             Function handler returning data
     * @return void || Future returning data
     */
    void getStudents(List<String> students, Handler<Either<String, JsonArray>> handler);

    Future<JsonArray> getStudents(List<String> studentIds);

    /**
     * Get list of students info (id, displayName, className)
     *
     * @param structureId structure identifier
     * @param studentIds  list of student identifiers
     * @param halfBoarder if we want (or no) half boarder students only
     * @param internal    if we want (or no) internal students only
     * @return Future returning data
     */
    Future<JsonArray> getStudents(String structureId, List<String> studentIds, Boolean halfBoarder, Boolean internal);

    void getStudentsWithAudiences(String structureId, List<String> studentIds, Handler<AsyncResult<JsonArray>> handler);

    Future<List<String>> getStudentsFromTeacher(String teacherId, String structureId);

    /**
     * Get every student ids from the structure with their accommodation.
     *
     * @param structureId structure identifier
     * @param handler     function handler returning data
     */
    void getAllStudentsIdsWithAccommodation(String structureId, Handler<Either<String, JsonArray>> handler);

    /**
     * Get all students from their structures mapped
     *
     * @param structures list of structure identifier
     * @return Future JsonArray of JsonObject(Map format) {String structure: List<String> student identifier}
     */
    Future<JsonArray> fetchAllStudentsFromStructure(List<String> structures);

    /**
     * List children upon relative's data
     *
     * @param relativeId relative identifier
     * @param handler    handler returning list of children from relative
     */
    void getChildren(String relativeId, Handler<Either<String, JsonArray>> handler);

    /**
     * get child own info data
     *
     * @param id      user identifier
     * @param handler handler returning list of children from relative
     */
    void getChildInfo(String id, Handler<Either<String, JsonObject>> handler);


}
