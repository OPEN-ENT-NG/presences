package fr.openent.presences.service;

import fr.openent.presences.model.Event.Event;
import fr.wseduc.webutils.Either;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.user.UserInfos;

import java.util.List;

public interface EventService {

    /**
     * Get events
     *
     * @param structureId       structure identifier
     * @param startDate         startDate start date
     * @param endDate           endDate end date
     * @param eventType         event type
     * @param listReasonIds     reasonId reason_id
     * @param noReason          noReason filter
     * @param userId            userId userId neo4j
     * @param userIdFromClasses userId fetched from classes neo4j
     * @param classes           classes list
     * @param regularized       regularized filter
     * @param page              page
     * @param handler           function handler returning data
     */
    void get(String structureId, String startDate, String endDate,
             List<String> eventType, List<String> listReasonIds, Boolean noReason, List<String> userId, JsonArray userIdFromClasses,
             List<String> classes, Boolean regularized, Integer page, Handler<Either<String, JsonArray>> handler);

    /**
     * Get events. Non paginated version. Used to display summary page
     *
     * @param startDate Start date
     * @param endDate   End date
     * @param eventType Event type list
     * @param users     User list
     * @param handler   Function handler returning data
     */
    void get(String startDate, String endDate, List<Number> eventType, List<String> users, Handler<Either<String, JsonArray>> handler);

    /**
     * Get events. Will after export this into csv
     *
     * @param structureId       structure identifier
     * @param startDate         startDate start date
     * @param endDate           endDate end date
     * @param eventType         event type
     * @param listReasonIds     reasonId reason_id
     * @param noReason          noReason filter
     * @param userId            userId userId neo4j
     * @param userIdFromClasses userId fetched from classes neo4j
     * @param classes           classes list
     * @param regularized       regularized filter
     * @param handler           Function handler returning data
     */
    void getCsvData(String structureId, String startDate, String endDate, List<String> eventType, List<String> listReasonIds,
                    Boolean noReason, List<String> userId, JsonArray userIdFromClasses, List<String> classes,
                    Boolean regularized, Handler<AsyncResult<List<Event>>> handler);

    /**
     * Get events page number
     *
     * @param structureId       structure identifier
     * @param startDate         startDate start date
     * @param endDate           endDate end date
     * @param eventType         event type
     * @param listReasonIds     reasonId reason_id
     * @param noReason          noReason filter
     * @param userId            userId userId neo4j
     * @param regularized       filter regularized absence
     * @param userIdFromClasses userId fetched from classes neo4j
     * @param handler           function handler returning data
     */
    void getPageNumber(String structureId, String startDate, String endDate, List<String> eventType,
                       List<String> listReasonIds, Boolean noReason, List<String> userId, Boolean regularized, JsonArray userIdFromClasses,
                       Handler<Either<String, JsonObject>> handler);


    /**
     * Create event
     *
     * @param event   event
     * @param user    user that create event
     * @param handler function handler returning data
     */
    void create(JsonObject event, UserInfos user, Handler<Either<String, JsonObject>> handler);

    /**
     * Update given event
     *
     * @param id      event identifier
     * @param event   event
     * @param handler Function handler returning data
     */
    void update(Integer id, JsonObject event, Handler<Either<String, JsonObject>> handler);


    /**
     * Update reason for each event
     *
     * @param eventBody Event body that can contain list of id's events and its reason_id retrieved
     * @param handler   Function handler returning data
     */
    void changeReasonEvents(JsonObject eventBody, UserInfos user, Handler<Either<String, JsonObject>> handler);

    /**
     * Update regularized for each event
     *
     * @param event   Event body that can contain list of id's events and its reason_id retrieved
     * @param handler Function handler returning data
     */
    void changeRegularizedEvents(JsonObject event, UserInfos user, Handler<Either<String, JsonObject>> handler);


    /**
     * Delete given identifier
     *
     * @param id      event identifier
     * @param handler function handler returning data
     */
    void delete(Integer id, Handler<Either<String, JsonObject>> handler);

    /**
     * List events as strict values based parameters
     *
     * @param structureId Structure identifier
     * @param startDate   Event start date
     * @param endDate     event end date
     * @param eventType   Event type
     * @param userId      User identifiers
     * @param handler     Function handler return data
     */
    void list(String structureId, String startDate, String endDate, List<Integer> eventType, List<String> userId, Handler<Either<String, JsonArray>> handler);

    /**
     * Get count event type group by user
     *
     * @param eventType  EVent type
     * @param students   Student list. Contains every students identifiers
     * @param structure  Structure identifier
     * @param justified  Justified events or not ? Can be null if justified event needs to be excluded
     * @param startAt    Start count at. Minimal number that student is retrieved
     * @param massmailed Massmailed ? Use by massmailing module. When null, column is excluded
     * @param startDate  Range start date
     * @param endDate    Range end date
     * @param reasonsId  Reasons identifiers. Can be sorted by reasons identifiers
     * @param noReasons  Should retrieve no reasons event
     * @param handler    Function handler returning data
     */
    void getCountEventByStudent(Integer eventType, List<String> students, String structure, Boolean justified, Integer startAt, List<Integer> reasonsId, Boolean massmailed,
                                String startDate, String endDate, boolean noReasons, Handler<Either<String, JsonArray>> handler);

    /**
     * Retrieve events by student
     *
     * @param eventType  Event Type list
     * @param students   Student list. Contains every students identifiers
     * @param structure  Structure identifier
     * @param justified  Justified events or not ? Can be null if justified event needs to be excluded
     * @param massmailed Massmailed ? Use by massmailing module. When null, column is excluded
     * @param startDate  Range start date
     * @param endDate    Range end date
     * @param reasonsId  Reasons identifiers. Can be sorted by reasons identifiers
     * @param noReasons  Should retrieve no reasons event
     * @param recoveryMethodUsed  method used to recover events, can be null if method in settings is wanted.
     * @param limit   corresponding to the limit of data rows wanted (optional)   method used to recover events, can be null if method in settings is wanted.
     * @param offset  corresponding to the offset data rows wanted (optional).
     * @param handler    Function handler returning data
     */
    void getEventsByStudent(Integer eventType, List<String> students, String structure, Boolean justified, List<Integer> reasonsId, Boolean massmailed,
                            String startDate, String endDate, boolean noReasons, String recoveryMethodUsed, String limit, String offset, Handler<Either<String, JsonArray>> handler);

    void getEventsByStudent(Integer eventType, List<String> students, String structure, Boolean justified, List<Integer> reasonsId, Boolean massmailed,
                            String startDate, String endDate, boolean noReasons, String recoveryMethodUsed, Handler<Either<String, JsonArray>> handler);

    /**
     * get event action
     *
     * @param eventId event identifier
     * @param handler Function handler returning data
     */
    void getActions(String eventId, Handler<Either<String, JsonArray>> handler);

    /**
     * create event action
     *
     * @param actionBody actionBody fetched
     * @param handler    Function handler returning data
     */
    void createAction(JsonObject actionBody, Handler<Either<String, JsonObject>> handler);


    /**
     * Retrieve student absence rate for given period
     *
     * @param student   student identifier
     * @param structure structure identifier
     * @param start     start date
     * @param end       end date
     * @param handler   function handler returning data
     */
    void getAbsenceRate(String student, String structure, String start, String end, Handler<Either<String, JsonObject>> handler);
}
