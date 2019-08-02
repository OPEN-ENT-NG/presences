package fr.openent.presences.service;

import fr.wseduc.webutils.Either;
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
     * @param userId            userId userId neo4j
     * @param userIdFromClasses userId fetched from classes neo4j
     * @param classes           classes list
     * @param regularized       regularized filter
     * @param page              page
     * @param handler           function handler returning data
     */
    void get(String structureId, String startDate, String endDate,
             List<String> eventType, List<String> userId, JsonArray userIdFromClasses,
             List<String> classes, Boolean regularized, Integer page, Handler<Either<String, JsonArray>> handler);

    /**
     * Get events page number
     *
     * @param structureId       structure identifier
     * @param startDate         startDate start date
     * @param endDate           endDate end date
     * @param eventType         event type
     * @param userId            userId userId neo4j
     * @param regularized       filter regularized absence
     * @param userIdFromClasses userId fetched from classes neo4j
     * @param handler           function handler returning data
     */
    void getPageNumber(String structureId, String startDate, String endDate, List<String> eventType,
                       List<String> userId, Boolean regularized, JsonArray userIdFromClasses,
                       Handler<Either<String, JsonObject>> handler);

    /**
     * Get events reason type
     *
     * @param structureId structure identifier
     * @param handler     function handler returning data
     */
    void getEventsReasonType(String structureId, Handler<Either<String, JsonArray>> handler);

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
    void changeReasonEvents(JsonObject eventBody, Handler<Either<String, JsonObject>> handler);

    /**
     * Update regularized for each event
     *
     * @param event   Event body that can contain list of id's events and its reason_id retrieved
     * @param handler Function handler returning data
     */
    void changeRegularizedEvents(JsonObject event, Handler<Either<String, JsonObject>> handler);


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
}
