package fr.openent.presences.service;

import fr.openent.presences.model.Event.Event;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;

public interface ExportEventService {


    /**
     * Get events. Will after export this into csv (or PDF)
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
     * @param restrictedClasses classes ids for restricted teachers
     * @param regularized       regularized filter
     * @param followed          followed filter
     * @param handler           Function handler returning data
     */
    void getCsvData(String structureId, String startDate, String endDate, List<String> eventType, List<String> listReasonIds,
                    Boolean noReason, List<String> userId, JsonArray userIdFromClasses, List<String> classes,
                    List<String> restrictedClasses, Boolean regularized, Boolean followed, Handler<AsyncResult<List<Event>>> handler);

    /**
     * Get events. Will after export this into csv (or PDF)
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
     * @param followed          followed filter
     * @return list of {@link Event}
     */
    Future<List<Event>> getCsvData(String structureId, String startDate, String endDate, List<String> eventType, List<String> listReasonIds,
                                   Boolean noReason, List<String> userId, JsonArray userIdFromClasses, List<String> classes,
                                   Boolean regularized, Boolean followed);

    /**
     * Get events. Will after export this into csv (or PDF)
     *
     * @param canSeeAllStudent  true if we have access to all student
     * @param domain            domain request sent
     * @param local             accepted langage
     * @param structureId       structure identifier
     * @param startDate         startDate start date
     * @param endDate           endDate end date
     * @param eventType         event type
     * @param listReasonIds     reasonId reason_id
     * @param noReason          noReason filter
     * @param userId            userId userId neo4j
     * @param userIdFromClasses userId fetched from classes neo4j
     * @param regularized       regularized filter
     * @return {@link JsonObject} will be build as
     * title -> {@link String}
     * event type -> {@link List} of {@link fr.openent.presences.model.Event.EventByStudent} as JsonObject
     * ... event type...
     */
    Future<JsonObject> getPdfData(Boolean canSeeAllStudent, String domain, String local, String structureId, String startDate, String endDate,
                                  List<String> eventType, List<String> listReasonIds, Boolean noReason, List<String> userId,
                                  JsonArray userIdFromClasses, Boolean regularized);

}
