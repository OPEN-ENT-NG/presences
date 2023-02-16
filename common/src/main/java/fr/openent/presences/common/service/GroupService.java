package fr.openent.presences.common.service;

import fr.openent.presences.enums.GroupType;
import fr.wseduc.webutils.Either;
import io.vertx.core.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;

public interface GroupService {

    /**
     * Get all group identifiers
     *
     * @param structureId  structure identifier
     * @param groups       group list
     * @param classes      class list
     * @param handler      Function handler returning data
     */
    void getGroupsId(String structureId, JsonArray groups, JsonArray classes, Handler<Either<String, JsonObject>> handler);

    /**
     * Get group/class identifiers from their names
     * @param structureId          structure identifier
     * @param groupClassNames      group and class names
     * @return {@link Future} of {@link List}
     */
    Future<List<String>> getGroupsIdList(String structureId, List<String> groupClassNames);

    /**
     * Get group users
     *
     * @param id      group identifier
     * @param type    group type
     * @param handler Function handler returning data
     */
    void getGroupUsers(String id, GroupType type, Handler<Either<String, JsonArray>> handler);

    /**
     * Get user groups
     *
     * @param users       User identifiers
     * @param structureId User structure identifier
     * @param handler     Function handler returning data
     */
    void getUserGroups(List<String> users, String structureId, Handler<Either<String, JsonArray>> handler);

    /**
     * Get group students
     *
     * @param groupIdentifier Group identifier
     * @param handler         Function handler returning data
     */
    void getGroupStudents(String groupIdentifier, Handler<Either<String, JsonArray>> handler);

    /**
     * get groups students
     *
     * @param groups  groups identifiers
     * @param handler FUnction handler returning data
     */
    void getGroupStudents(List<String> groups, Handler<Either<String, JsonArray>> handler);
    Future<JsonArray> getGroupStudents(String structureId, List<String> groups);

    Future<JsonArray> getGroupStudents(List<String> groups);

    void getAudiences(String structureId, List<String> audienceIds, Handler<AsyncResult<JsonArray>> handler);
    void getAudiencesFromNames(String structureId, List<String> audienceNames, Handler<AsyncResult<JsonArray>> handler);

    // Define this new service to fix some performance issues. Maybe refactor later ?
    void getFunctionalAndManualGroupsStudents(List<String> groups, Handler<Either<String, JsonArray>> handler);

    Future<List<String>> getGroupsAndClassesFromTeacherId(String userId, String structureId);

    void getClassesStudents(List<String> classes, Handler<Either<String, JsonArray>> handler);
}
