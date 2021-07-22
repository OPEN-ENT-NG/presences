package fr.openent.presences.service.impl;


import fr.openent.presences.common.helper.DateHelper;
import fr.openent.presences.common.viescolaire.Viescolaire;
import fr.openent.presences.core.constants.Field;
import fr.openent.presences.db.DBService;
import fr.openent.presences.export.EventsCSVExport;
import fr.openent.presences.model.Event.Event;
import fr.openent.presences.service.ArchiveService;

import fr.openent.presences.service.CommonPresencesServiceFactory;
import fr.openent.presences.service.EventService;
import fr.openent.presences.service.ReasonService;
import fr.openent.presences.worker.EventExportWorker;
import fr.wseduc.webutils.I18n;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;

public class DefaultArchiveService extends DBService implements ArchiveService {
    private final EventBus eb;
    private final EventService eventService;
    private final ReasonService reasonService;

    private final Logger log = LoggerFactory.getLogger(DefaultArchiveService.class);

    public DefaultArchiveService(CommonPresencesServiceFactory commonPresencesServiceFactory) {
        this.eventService = commonPresencesServiceFactory.eventService();
        this.reasonService = commonPresencesServiceFactory.reasonService();
        this.eb = commonPresencesServiceFactory.eventBus();
    }

    /**
     * process archiving event (will fetch necessary data before write and return all csv files)
     *
     * @param structures        List of structure data info
     * @return {@link Future} of {@link JsonArray} containing list of all files written from all structures
     */
    @Override
    public Future<JsonArray> archiveEventsExport(JsonArray structures, String domain, String locale) {
        log.info("[" + this.getClass().getSimpleName() + "] - archiveEventsExport");

        Promise<JsonArray> promise = Promise.promise();
        if (structures.isEmpty()) {
            String message = "[Presences@DefaultArchiveService::archiveEventsExport] Structures empty";
            promise.fail(message);
        } else {
            List<Future<JsonObject>> futures = new ArrayList<>();
            Promise<JsonObject> init = Promise.promise();
            Future<JsonObject> current = init.future();
            for (int i = 0; i < structures.size() ; i++) {
                int indice = i;
                current = current.compose(v -> {
                    Future<JsonObject> next = processStructure(structures.getJsonObject(indice), domain, locale);
                    futures.add(next);
                    return next;
                });
            }
            current
                    .onSuccess(ar -> extractAllFilesFromStructures(promise, futures))
                    .onFailure(ar -> {
                        String message = "[Presences@DefaultArchiveService::archiveEventsExport] An error has occured " +
                                "during process structure: " + ar.getMessage();
                        log.error(message);
                        promise.fail(ar.getCause().getMessage());
                    });
            init.complete();
        }
        return promise.future();
    }

    /**
     * extract all files from array of exportProcess (each structure completed data)
     *
     * @param promise        Promise of {@link JsonArray} to fill with all extracted file
     * @param futures        List of {@link Future<JsonObject>} futures handled via each processStructure
     */
    private void extractAllFilesFromStructures(Promise<JsonArray> promise, List<Future<JsonObject>> futures) {
        JsonArray extractedFiles = new JsonArray();

        for (Future<JsonObject> handler : futures) {
            JsonObject exportProcessResult = handler.result();
            extractedFiles.addAll(exportProcessResult.getJsonArray(Field.FILES));
        }
        promise.complete(extractedFiles);
    }

    /**
     * process structure step
     *
     * @param structure     structure data info
     * @param domain        domain host
     * @param locale        locale accepted lang
     * @return {@link Future} of {@link JsonObject} containing exportProcess completed
     */
    private Future<JsonObject> processStructure(JsonObject structure, String domain, String locale) {
        log.info("[" + this.getClass().getSimpleName() + "] processStructure " + structure.getString(Field.NAME));

        Promise<JsonObject> promise = Promise.promise();

        // Object sequentially built as :

        // structure -> JsonObject Structure Data
        // schoolYear -> JsonObject SchoolYear Data
        // reason -> JsonArray of reason Data
        // separatedDateBySchoolYear -> (JsonArray) List<JsonObject> separated dates from school year;
        // files -> (JsonArray) with each Object (id: string, name: string);
        JsonObject exportProcess = new JsonObject()
                .put("structure", structure);

        getStructureSettings(exportProcess)
                .compose(this::splitSchoolYearByMonth)
                .compose(ar -> processEvents(exportProcess, domain, locale))
                .onSuccess(succeed -> promise.complete(exportProcess))
                .onFailure(err -> {
                    String message = "[Presences@DefaultArchiveService::processStructure] An error has occurred during process structure "
                            + structure.getString(Field.NAME) + ": " + err.getMessage();
                    log.error(message);
                    promise.fail(err.getMessage());
                });

        return promise.future();
    }

    private Future<JsonObject> getStructureSettings(JsonObject exportProcess) {
        Promise<JsonObject> promise = Promise.promise();

        String structure = exportProcess.getJsonObject(Field.STRUCTURE).getString(Field.ID);

        Future<JsonObject> schoolYearFuture = Viescolaire.getInstance().getSchoolYear(structure);
        Future<JsonArray> reasonFuture = this.reasonService.fetchReason(structure);

        CompositeFuture.all(schoolYearFuture, reasonFuture)
                .onSuccess(unused -> {
                    exportProcess
                            .put(Field.SCHOOL_YEAR, schoolYearFuture.result())
                            .put(Field.REASON, reasonFuture.result());
                    promise.complete(exportProcess);
                })
                .onFailure(promise::fail);

        return promise.future();
    }

    private Future<JsonObject> splitSchoolYearByMonth(JsonObject exportProcess) {
        Promise<JsonObject> promise = Promise.promise();
        List<LocalDate> daysFetched = DateHelper.getListOfDateBasedOnDates(
                exportProcess.getJsonObject(Field.SCHOOL_YEAR).getString(Field.START_DATE),
                exportProcess.getJsonObject(Field.SCHOOL_YEAR).getString(Field.END_DATE),
                DateHelper.YEAR_MONTH_DAY,
                "",
                false
        );

        splitDays(exportProcess, daysFetched);
        promise.complete(exportProcess);
        return promise.future();
    }

    private void splitDays(JsonObject exportProcess, List<LocalDate> daysFetched) {
        List<JsonObject> separatedDateBySchoolYear = new ArrayList<>();

        int numberDays = daysFetched.size();
        numberDays = numberDays / 3;

        int reliquat = numberDays % 3;

        List<LocalDate> splitDaysChunk1;
        List<LocalDate> splitDaysChunk2;
        List<LocalDate> splitDaysChunk3;

        // Since we split our days by 3 we might not have the whole number with modulo 3
        // this case occurs if we have 1 days left with our modulo (to which we add to our missing chunk)
        if (reliquat == 1) {
            splitDaysChunk1 = daysFetched.subList(0, numberDays + 1);
            splitDaysChunk2 = daysFetched.subList((numberDays + 1), (numberDays * 2) + 1);
            splitDaysChunk3 = daysFetched.subList((numberDays * 2) + 1, daysFetched.size());
        } else if (reliquat == 2) {
            // this case occurs if we have 2 days left with our modulo (to which we add to our missing chunk)
            splitDaysChunk1 = daysFetched.subList(0, numberDays + 1);
            splitDaysChunk2 = daysFetched.subList(numberDays + 1, (numberDays * 2) + 2);
            splitDaysChunk3 = daysFetched.subList((numberDays * 2) + 2, daysFetched.size());
        } else {
            // this case occurs if we have 0 days left with our modulo (to which we do not need to add to our missing chunk)
            splitDaysChunk1 = daysFetched.subList(0, numberDays);
            splitDaysChunk2 = daysFetched.subList(numberDays, (numberDays * 2));
            splitDaysChunk3 = daysFetched.subList((numberDays * 2), daysFetched.size());
        }

        // first chunk
        separatedDateBySchoolYear.add(new JsonObject()
                .put(Field.START_DATE, splitDaysChunk1.get(0).toString())
                .put(Field.END_DATE, splitDaysChunk1.get(splitDaysChunk1.size() - 1).toString()));
        // second chunk
        separatedDateBySchoolYear.add(new JsonObject()
                .put(Field.START_DATE, splitDaysChunk2.get(0).toString())
                .put(Field.END_DATE, splitDaysChunk2.get(splitDaysChunk2.size() - 1).toString()));
        // third chunk
        separatedDateBySchoolYear.add(new JsonObject()
                .put(Field.START_DATE, splitDaysChunk3.get(0).toString())
                .put(Field.END_DATE, splitDaysChunk3.get(splitDaysChunk3.size() - 1).toString()));
        exportProcess.put(Field.SPLITTED_DATE_SCHOOL_YEAR, separatedDateBySchoolYear);
    }

    @SuppressWarnings("unchecked")
    private Future<JsonObject> processEvents(JsonObject exportProcess, String domain, String locale) {
        Promise<JsonObject> promise = Promise.promise();
        JsonObject structure = exportProcess.getJsonObject(Field.STRUCTURE);
        List<String> eventType = new ArrayList<>(Arrays.asList("1", "2", "3"));
        List<String> reasonIds = ((List<JsonObject>) exportProcess.getJsonArray(Field.REASON).getList())
                .stream()
                .map(reason -> reason.getLong(Field.ID).toString())
                .collect(Collectors.toList());

        List<JsonObject> splittedDateSchoolYears = exportProcess.getJsonArray(Field.SPLITTED_DATE_SCHOOL_YEAR).getList();

        Promise<JsonObject> init = Promise.promise();
        Future<JsonObject> current = init.future();

        for (int i = 0; i < splittedDateSchoolYears.size() ; i++) {
            int indice = i;
            current = current.compose(v -> interactEvent(structure, splittedDateSchoolYears.get(indice),
                    eventType, reasonIds, exportProcess, domain, locale));
        }
        current
                .onSuccess(ar -> promise.complete(exportProcess))
                .onFailure(ar -> promise.fail(ar.getCause()));
        init.complete();

        return promise.future();
    }

    private Future<JsonObject> interactEvent(JsonObject structure, JsonObject splittedDateSchoolYear, List<String> eventType,
                                             List<String> reasonIds, JsonObject exportProcess, String domain, String locale) {
        Promise<JsonObject> promise = Promise.promise();
        String start = splittedDateSchoolYear.getString(Field.START_DATE);
        String end = splittedDateSchoolYear.getString(Field.END_DATE);
        eventService.getCsvData(structure.getString(Field.ID), start, end, eventType, reasonIds, true, new ArrayList<>(),
                new JsonArray(), null, null, null)
                .compose(events -> writeCSV(events, start, end, exportProcess, domain, locale))
                .onSuccess(res -> promise.complete(exportProcess))
                .onFailure(err -> {
                    String message = "[Presences@DefaultArchiveService::interactEvent] An error has occured during" +
                            " interact event: " + err.getMessage();
                    log.error(message);
                    promise.fail(err.getCause());
                });
        return promise.future();
    }


    private Future<JsonObject> writeCSV(List<Event> events, String start, String end, JsonObject exportProcess,
                                        String domain, String locale) {
        Promise<JsonObject> promise = Promise.promise();

        List<String> csvHeaders = Arrays.asList(
                translate("presences.csv.header.student.lastName", domain, locale),
                translate("presences.csv.header.student.firstName", domain, locale),
                translate("presences.exemptions.csv.header.audiance", domain, locale),
                translate("presences.event.type", domain, locale),
                translate("presences.absence.reason", domain, locale),
                translate("presences.created.by", domain, locale),
                translate("presences.exemptions.dates", domain, locale),
                translate("presences.hour", domain, locale),
                translate("presences.exemptions.csv.header.comment", domain, locale),
                translate("presences.widgets.absences.regularized", domain, locale),
                translate( "presences.id",domain, locale));
        EventsCSVExport ece = new EventsCSVExport(events, domain, locale);

        ece.setHeader(csvHeaders);
        ece.generate();

        String structureName = exportProcess.getJsonObject(Field.STRUCTURE).getString(Field.NAME, "");
        String structureFileName = structureName + "_" + start + "-" + end + ".csv";

        JsonObject fileData = new JsonObject()
                .put(Field.NAME, structureFileName)
                .put(Field.CONTENTS, ece.value.toString());

        if (!exportProcess.containsKey(Field.FILES)) {
            exportProcess.put(Field.FILES, new JsonArray().add(fileData));
        } else {
            exportProcess.getJsonArray(Field.FILES).add(fileData);
        }
        promise.complete(exportProcess);
        return promise.future();
    }

    private String translate(String key, String domain, String locale) {
        return I18n.getInstance().translate(key, domain, locale);
    }

    @Override
    public Future<JsonObject> processEventExportWorker(JsonArray structures, String domain, String locale) {
        Promise<JsonObject> promise = Promise.promise();
        if (structures.isEmpty()) {
            promise.fail("[Presences@DefaultArchiveService::processEventExportWorker] No structure(s) identifier given");
        } else {
            JsonObject params = new JsonObject()
                    .put(Field.STRUCTURE, structures)
                    .put(Field.DOMAIN, domain)
                    .put(Field.LOCALE, locale);
            eb.request(EventExportWorker.class.getName(), params,
                    new DeliveryOptions().setSendTimeout(1000 * 1000L), handlerToAsyncHandler(eventExport -> {
                                if (!eventExport.body().getString("status").equals("ok")) {
                                    processEventExportWorker(structures, domain, locale);
                                }
                            }
                    ));
            promise.complete(new JsonObject().put("status", "ok"));
        }
        return promise.future();
    }
}
