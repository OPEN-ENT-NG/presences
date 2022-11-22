package fr.openent.statistics_presences.service;

import fr.openent.presences.model.StatisticsUser;
import fr.openent.presences.model.StructureStatisticsUser;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;

public interface StatisticsPresencesService {
    /**
     * Put user in the queue for calculating stats
     *
     * @param studentIds users identifiers identifier
     * @param handler    function handler returning data
     * @deprecated Uses the start date of the school period as the user recalculation date. Replaced by {@link #createWithModifiedDate(String, List, Handler)}
     */
    @Deprecated
    void create(String structureId, List<String> studentIds, Handler<AsyncResult<JsonObject>> handler);

    /**
     * Put user in the queue for calculating stats
     *
     * @param studentIdModifiedDateMap studentId modifiedDate map
     * @param handler    function handler returning data
     */
    void createWithModifiedDate(String structureId, List<StatisticsUser> studentIdModifiedDateMap, Handler<AsyncResult<JsonObject>> handler);


    /**
     * process statistics task for a structure/student(s)
     * If we set isWaitingEndprocess 'true', this will simply call a method that will reply at the end
     * if false, this will call a worker that does the same thing but without replying
     *
     * @param structure             list structure identifier
     * @param studentIds            list student identifiers
     * @param isWaitingEndProcess   state to enable worker mode or not
     * @return Future JsonObject completing process
     * @deprecated Uses the start date of the school period as the user recalculation date. Replaced by {@link #processStatisticsPrefetch(List, Boolean)}
     */
    @Deprecated
    Future<JsonObject> processStatisticsPrefetch(List<String> structure, List<String> studentIds, Boolean isWaitingEndProcess);

    /**
     * process statistics task for a structure/student(s)
     * If we set isWaitingEndprocess 'true', this will simply call a method that will reply at the end
     * if false, this will call a worker that does the same thing but without replying
     *
     * @param structureStatisticsUserList
     * @param isWaitingEndProcess   state to enable worker mode or not
     * @return Future JsonObject completing process
     */
    Future<JsonObject> processStatisticsPrefetch(List<StructureStatisticsUser> structureStatisticsUserList, Boolean isWaitingEndProcess);

    /**
     * Create a Structure Statistics User from each structure id.
     * In this method we will look for all the students of the structure to create a {@link StatisticsUser}
     * Each {@link StatisticsUser} have modified date to start school date
     *
     * @param structureIdList structure identifier list
     * @return Future of result
     */
    Future<List<StructureStatisticsUser>> fetchUsers(List<String> structureIdList);

    /**
     * Create a Structure Statistics User from the structure id.
     * For each student in studentIdList, we create a {@link StatisticsUser}
     * Each {@link StatisticsUser} have modified date to start school date
     *
     * @param structureId structure identifier
     * @param studentIdList student identifier list
     * @return Future of result
     */
    Future<StructureStatisticsUser> fetchUsers(String structureId, List<String> studentIdList);


    /**
     * Fetch user list in database. The list contains all users identifier that need to be proceeded.
     *
     * @return Future handling result
     */
    Future<List<StructureStatisticsUser>> fetchUsers();

    /**
     * Delete each student for user table
     *
     * @param studentIdList student identifier list
     * @return Future succeeded when success
     */
    Future<Void> clearWaitingList(List<String> studentIdList);

    /**
     * Truncate user table
     *
     * @return Future succeeded when success
     */
    Future<Void> clearWaitingList();
}
