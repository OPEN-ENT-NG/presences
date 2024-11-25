package fr.openent.presences.service;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public interface ArchiveService {

    /**
     * process archiving event (will fetch necessary data before write and return all csv files)
     *
     * @param structures    List of structure data info
     * @param domain        domain host
     * @param locale        locale accepted lang
     * @return {@link Future} of {@link JsonArray} containing list of all files written from all structures
     */
    Future<JsonArray> archiveEventsExport(JsonArray structures, String domain, String locale);


    /**
     * Call EventExportWorker class
     *
     * @param structures    list structure identifier
     * @param domain        domain host
     * @param locale        locale accepted lang
     * @return {@link Future} JsonObject completing process
     */
    Future<JsonObject> processEventExportWorker(JsonArray structures, String domain, String locale);
}
