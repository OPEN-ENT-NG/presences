package fr.openent.presences.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;

import java.util.List;

public interface RegistryService {

    /**
     * Retrieve register summary based on given month, class identifiers and event types
     *
     * @param month                 Month needed, format is : yyyy-MM
     * @param groups                class identifiers list
     * @param eventTypes            types identifiers list.
     *                              Warning: Contrary to other types in register service, this list is a String list because it
     *                              includes incident events.
     * @param structureId           structure identifier
     * @param forgottenNotebook     forgottenNotebook optional filter (true or false)
     * @param handler               Function handler returning data. Returns a JsonArray
     */
    void get(String month, List<String> groups, List<String> eventTypes,
             String structureId, boolean forgottenNotebook, Handler<Either<String, JsonArray>> handler);

    /**
     * Retrieve register events for the CSV export
     * @param groups                class identifiers list
     * @param eventTypes            types identifiers list.
     *                              Warning: Contrary to other types in register service, this list is a String list because it
     *                              includes incident events.
     * @param structureId           structure identifier
     * @param forgottenNotebook     forgottenNotebook optional filter (true or false)
     * @param handler               Function handler returning data. Returns a JsonArray
     */
    void getCSV(String month, List<String> groups, List<String> eventTypes,
             String structureId, boolean forgottenNotebook, Handler<Either<String, JsonArray>> handler);

    /**
     * Retrieve register summary enriched with morning/afternoon schedule data, for the board CSV export.
     * Only absences are fetched. The half-day schedule per weekday is derived from the Présences
     * register table, scoped to the exported groups (see DefaultRegistryService#fetchScheduleFromRegisters).
     *
     * @param month       Month needed, format is : yyyy-MM
     * @param groups      class identifiers list
     * @param structureId structure identifier
     * @param handler     Function handler returning enriched students JsonArray
     */
    void getForExport(String month, List<String> groups, String structureId,
                      Handler<Either<String, JsonArray>> handler);
}
