package fr.openent.incidents.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;

public interface IncidentsService {
    /**
     * Fetch incidents
     *
     * @param structureId structure identifier structure identifier
     * @param startDate   start date
     * @param endDate     end date
     * @param userId      userId neo
     * @param page        page
     * @param handler     function handler returning da
     */
    void get(String structureId, String startDate, String endDate, List<String> userId,
             String page, boolean paginationMode, String field, boolean reverse, Handler<Either<String, JsonArray>> handler);

    /**
     * Retrieve user incidents
     *
     * @param structureId Structure identifier
     * @param startDate   Start date
     * @param endDate     End date
     * @param userId      User that needs to retrieve incidents
     * @param handler     Function handler returning data
     */
    void get(String structureId, String startDate, String endDate, String userId, Handler<Either<String, JsonArray>> handler);

    /**
     * Get Incidents count
     *
     * @param structureId structure identifier
     * @param startDate   start date
     * @param endDate     end date
     * @param userId      userId neo
     * @param page        page
     * @param handler     handler
     */
    void getPageNumber(String structureId, String startDate, String endDate, List<String> userId,
                       String page, String order, boolean reverse, Handler<Either<String, JsonObject>> handler);

    /**
     * Get all parameter type linked to incident (place, type of incident...)
     *
     * @param structureId structure identifier
     * @param handler     handler
     */
    void getIncidentParameter(String structureId, Handler<Either<String, JsonObject>> handler);

    /**
     * Create incident
     *
     * @param incident structure identifier
     * @param handler  function handler returning da
     */
    void create(JsonObject incident, Handler<Either<String, JsonArray>> handler);

    /**
     * Update an incident
     *
     * @param incidentId incident identifier
     * @param incident   incident object
     * @param handler    function handler returning da
     */
    void update(Number incidentId, JsonObject incident, Handler<Either<String, JsonObject>> handler);

    /**
     * Delete incident
     *
     * @param incidentId incident identifier
     * @param handler    function handler returning da
     */
    void delete(String incidentId, Handler<Either<String, JsonObject>> handler);
}
