package fr.openent.presences.service.impl;

import fr.openent.presences.common.helper.EventsHelper;
import fr.openent.presences.common.helper.FutureHelper;
import fr.openent.presences.common.viescolaire.Viescolaire;
import fr.openent.presences.core.constants.Field;
import fr.openent.presences.model.Settings;
import fr.openent.presences.model.TimeslotModel;
import fr.openent.presences.service.*;
import io.vertx.core.*;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;


public class DefaultEventStudentService implements EventStudentService {

    private static final Logger log = LoggerFactory.getLogger(DefaultEventStudentService.class);

    private final String NO_REASON = "NO_REASON";
    private final String UNREGULARIZED = "UNREGULARIZED";
    private final String REGULARIZED = "REGULARIZED";
    private final String LATENESS = "LATENESS";
    private final String DEPARTURE = "DEPARTURE";

    private final String MORNING = "MORNING";
    private final String HALF_DAY = "HALF_DAY";
    private final String HOUR = "HOUR";
    private final String DAY = "DAY";

    private final String AFTERNOON = "AFTERNOON";

    private final EventService eventService;
    private final ReasonService reasonService;
    private final SettingsService settingsService;
    private final EventBus eb;

    public DefaultEventStudentService(CommonPresencesServiceFactory commonPresencesServiceFactory) {
        super();
        this.eb = commonPresencesServiceFactory.eventBus();
        this.eventService = commonPresencesServiceFactory.eventService();
        this.reasonService = commonPresencesServiceFactory.reasonService();
        this.settingsService = commonPresencesServiceFactory.settingsService();
    }

    @Override
    public Future<JsonObject> get(String structureId, String studentId, List<String> types, String start, String end, String limit, String offset) {
        Promise<JsonObject> promise = Promise.promise();

        JsonObject settings = new JsonObject();
        Map<Integer, JsonObject> reasons = new HashMap<>();
        setSettingsAndReasons(structureId, types, settings, reasons)
                .compose(settingsResult -> setStudentTimeslot(settings, Collections.singletonList(studentId), structureId))
                .compose(res -> getEventsData(types, structureId, Collections.singletonList(studentId), reasons, settings, start, end, limit, offset))
                .onFailure(promise::fail)
                .onSuccess(result -> {
                    JsonObject response = result.get(studentId)
                            .put(Field.LIMIT, limit)
                            .put(Field.OFFSET, offset)
                            .put(Field.RECOVERY_METHOD, settings.getString(Field.EVENT_RECOVERY_METHOD));
                    promise.complete(response);
                });

        return promise.future();
    }

    @Override
    public Future<JsonObject> get(String structureId, List<String> studentIds, List<String> types, String start, String end, String limit, String offset) {
        Promise<JsonObject> promise = Promise.promise();

        JsonObject settings = new JsonObject();
        Map<Integer, JsonObject> reasons = new HashMap<>();
        setSettingsAndReasons(structureId, types, settings, reasons)
                .compose(settingsResult -> setStudentTimeslot(settings, studentIds, structureId))
                .compose(res -> getEventsData(types, structureId, studentIds, reasons, settings, start, end, limit, offset))
                .onFailure(promise::fail)
                .onSuccess(result -> {
                    JsonObject response = new JsonObject()
                            .put(Field.LIMIT, limit)
                            .put(Field.OFFSET, offset)
                            .put(Field.STUDENTS_EVENTS, result)
                            .put(Field.RECOVERY_METHOD, settings.getString(Field.EVENT_RECOVERY_METHOD));
                    promise.complete(response);
                });

        return promise.future();
    }

    private Future<JsonObject> setStudentTimeslot(JsonObject settings, List<String> studentIdList, String structureId) {
        Promise<JsonObject> promise = Promise.promise();

        Viescolaire.getInstance().getStudentTimeslot(studentIdList, structureId)
                .onSuccess(studentTimeSlotList -> {
                    settings.put(Field.STUDENT_TIMESLOT, studentTimeSlotList);
                    promise.complete(settings);
                })
                .onFailure(error -> {
                    String message = String.format("[Presences@%s::setStudentTimeslot] Fail to set settings student timeslot.",
                                    this.getClass().getSimpleName());
                    log.error(String.format("%s %s", message, error.getMessage()));
                    promise.fail(message);
                });

        return promise.future();
    }

    private Future<JsonObject> setSettingsAndReasons(String structureId, List<String> types,
                                                     JsonObject settings, Map<Integer, JsonObject> reasons) {
        Promise<JsonObject> promise = Promise.promise();

        if (!validTypes(types)) {
            String message =
                    String.format("[Presences@%s::setSettingsAndReasons] Types are not valid.",
                            this.getClass().getSimpleName());
            log.error(message);
            promise.fail(message);
            return promise.future();
        }

        // Get reasons types to map it thanks reason_id on absences justified events results
        Future<Map<Integer, JsonObject>> reasonFuture = getReasons(structureId);
        // Get presence settings to get recovery method (for absences events configuration retrieving)
        Future<Settings> presencesSettingsFuture = settingsService.retrieveSettings(structureId);
        // Get viscolaire settings to get Half Day Date configured by the current structure
        //  (if absences events are configured by Half Days).
        Future<JsonObject> viscoSettingsFuture = getHalfDay(structureId);
        // Get timeSlots settings configured by the current structure, to configure absences events times
        // (if they are retrieved by Half Days or by Days)
        Future<JsonObject> timeSlotsFuture = getTimeSlots(structureId);

        CompositeFuture.all(reasonFuture, presencesSettingsFuture, viscoSettingsFuture, timeSlotsFuture)
                .onFailure(error -> {
                    String message =
                            String.format("[Presences@%s::setSettingsAndReasons] Fail to get settings.",
                                    this.getClass().getSimpleName());
                    log.error(String.format("%s %s", message, error.getMessage()));
                    promise.fail(message);
                })
                .onSuccess(settingsResult -> {
                    Settings presencesSettings = presencesSettingsFuture.result();
                    reasons.putAll(reasonFuture.result());
                    settings.mergeIn(viscoSettingsFuture.result());
                    settings.mergeIn(timeSlotsFuture.result());
                    settings.put(Field.EVENT_RECOVERY_METHOD, presencesSettings.recoveryMethod());
                    promise.complete(settings);
                });
        return promise.future();
    }

    private Future<Map<String, JsonObject>> getEventsData(List<String> types, String structureId, List<String> studentIds, Map<Integer, JsonObject> reasons,
                                                          JsonObject settings, String start, String end, String limit, String offset) {
        Promise<Map<String, JsonObject>> promise = Promise.promise();

        Map<String, JsonObject> studentsEvents = studentIds.stream()
                .collect(Collectors.toMap(
                        studentId -> studentId,
                        studentId -> new JsonObject()
                                .put(Field.ALL, new JsonObject(types.stream().collect(Collectors.toMap(type -> type, type -> new JsonArray()))))
                                .put(Field.TOTALS, new JsonObject(types.stream().collect(Collectors.toMap(type -> type, type -> 0))))
                ));


        Future<Map<String, JsonObject>> eventsCountFuture = getCountEvents(types, structureId, studentIds, reasons, start,
                end, studentsEvents);
        Future<Map<String, JsonObject>> eventsFuture = getEvents(settings, types, structureId, studentIds, reasons, start,
                end, limit, offset, studentsEvents);

        CompositeFuture.all(eventsCountFuture, eventsFuture)
                .onSuccess(result -> promise.complete(studentsEvents))
                .onFailure(error -> {
                    String message =
                            String.format("[Presences@%s::getEventsData] Fail to get events or totals events.",
                                    this.getClass().getSimpleName());
                    log.error(String.format("%s %s", message, error.getMessage()));
                    promise.fail(message);
                });

        return promise.future();
    }

    private Future<JsonObject> getTimeSlots(String structure) {
        Promise<JsonObject> promise = Promise.promise();
        getTimeSlots(structure, promise);
        return promise.future();
    }

    private Future<JsonObject> getHalfDay(String structure) {
        Promise<JsonObject> promise = Promise.promise();
        getViscoSettings(structure, promise);
        return promise.future();
    }

    private Future<Map<Integer, JsonObject>> getReasons(String structure) {
        Promise<Map<Integer, JsonObject>> promise = Promise.promise();
        reasonService.fetchAbsenceReason(structure, resultReason -> {
            if (resultReason.isLeft()) {
                String message =
                        String.format("[Presences@%s::getReasons] Fail to get structure absence reasons.",
                                this.getClass().getSimpleName());
                log.error(String.format("%s %s", message, resultReason.left().getValue()));
                promise.fail(message);
                return;
            }
            Map<Integer, JsonObject> reasons = new HashMap<>();
            for (Object o : resultReason.right().getValue()) {
                JsonObject reason = (JsonObject) o;
                reasons.put(reason.getInteger(Field.ID), reason);
            }
            promise.complete(reasons);
        });

        return promise.future();
    }

    // check if types are in {{ NO_REASON / UNREGULARIZED / REGULARIZED / LATENESS / DEPARTURE }}
    private boolean validTypes(List<String> types) {
        boolean valid = true;
        for (String type : types) {
            switch (type) {
                case NO_REASON:
                case UNREGULARIZED:
                case REGULARIZED:
                case LATENESS:
                case DEPARTURE:
                    valid = true;
                    break;
                default:
                    valid = false;
            }
        }

        return valid;
    }

    private void getViscoSettings(String structure, Handler<AsyncResult<JsonObject>> handler) {
        JsonObject action = new JsonObject()
                .put("action", "timeslot.getSlotProfileSettings")
                .put("structureId", structure);

        eb.send("viescolaire", action, event -> {
            if (event.failed() || event.result() == null || "error".equals(((JsonObject) event.result().body()).getString("status"))) {
                String err = "[Presences@DefaultEventStudentService::getViscoSettings] Failed to retrieve courses";
                log.error(err);
                handler.handle(Future.failedFuture(err));
                return;
            }
            handler.handle(Future.succeededFuture(((JsonObject) event.result().body()).getJsonObject("result")));
        });
    }

    private void getTimeSlots(String structure, Handler<AsyncResult<JsonObject>> handler) {
        JsonObject action = new JsonObject()
                .put("action", "timeslot.getSlotProfiles")
                .put("structureId", structure);

        eb.send("viescolaire", action, event -> {
            if (event.failed() || event.result() == null || "error".equals(((JsonObject) event.result().body()).getString("status"))) {
                String err = "[Presences@DefaultEventStudentService::getTimeSlots] Failed to retrieve courses";
                log.error(err);
                handler.handle(Future.failedFuture(err));
                return;
            }
            handler.handle(Future.succeededFuture(((JsonObject) event.result().body()).getJsonObject("result")));
        });
    }

    // Send request to get each events (thanks the great method corresponding to type {{ NO_REASON / UNREGULARIZED / REGULARIZED / LATENESS / DEPARTURE }}).
    // Handler will return them mapped by type.
    private Future<Map<String, JsonObject>> getEvents(JsonObject settings, List<String> types, String structure, List<String> studentIds, Map<Integer, JsonObject> reasons,
                                                      String start, String end, String limit, String offset, Map<String, JsonObject> studentsEvents) {
        Promise<Map<String, JsonObject>> promise = Promise.promise();
        List<Integer> reasonsIds = new ArrayList<>(reasons.keySet());
        List<Future<JsonArray>> futures = new ArrayList<>();
        for (String type : types)
            futures.add(getEventsByStudent(type, structure, reasonsIds, studentIds, start, end, limit, offset));

        FutureHelper.all(futures)
                .onFailure(error -> {
                    String message =
                            String.format("[Presences@%s::getEvents] Fail to retrieve events info.",
                                    this.getClass().getSimpleName());
                    log.error(String.format("%s %s", message, error.getMessage()));
                    promise.fail(message);
                })
                .onSuccess(result -> promise.complete(setEventsByStudents(types, reasons, settings, futures, studentsEvents)));

        return promise.future();
    }

    private Future<Map<String, JsonObject>> getCountEvents(List<String> types, String structure, List<String> studentIds, Map<Integer, JsonObject> reasons,
                                                           String start, String end, Map<String, JsonObject> studentsEvents) {
        Promise<Map<String, JsonObject>> promise = Promise.promise();
        List<Integer> reasonsIds = new ArrayList<>(reasons.keySet());
        List<Future<JsonArray>> futures = new ArrayList<>();
        for (String type : types)
            futures.add(getCountEventsByStudent(type, structure, reasonsIds, studentIds, start, end));

        FutureHelper.all(futures)
                .onFailure(error -> {
                    String message =
                            String.format("[Presences@%s::getCountEvents] Fail to retrieve count events info.",
                                    this.getClass().getSimpleName());
                    log.error(String.format("%s %s", message, error.getMessage()));
                    promise.fail(message);
                })
                .onSuccess(result -> promise.complete(setCountEventsByStudents(types, futures, studentsEvents)));

        return promise.future();
    }

    // Get events for a student, corresponding to the type mentioned
    private Future<JsonArray> getEventsByStudent(String type, String structureId, List<Integer> reasonsIds, List<String> studentIds, String start, String end,
                                                 String limit, String offset) {
        Promise<JsonArray> promise = Promise.promise();
        switch (type) {
            case NO_REASON:
                eventService.getEventsByStudent(1, studentIds, structureId,
                        new ArrayList<>(), null, start, end, true, HOUR, false,
                        FutureHelper.handlerJsonArray(promise));
                break;
            case UNREGULARIZED:
                eventService.getEventsByStudent(1, studentIds, structureId,
                        reasonsIds, null, start, end, false, HOUR, false,
                        FutureHelper.handlerJsonArray(promise));
                break;
            case REGULARIZED:
                eventService.getEventsByStudent(1, studentIds, structureId,
                        reasonsIds, null, start, end, false, HOUR, true,
                        FutureHelper.handlerJsonArray(promise));
                break;
            case LATENESS:
                eventService.getEventsByStudent(2, studentIds, structureId,
                        new ArrayList<>(), null, start, end, true, null,
                        limit, offset, null, FutureHelper.handlerJsonArray(promise));
                break;
            case DEPARTURE:
                eventService.getEventsByStudent(3, studentIds, structureId,
                        new ArrayList<>(), null, start, end, true, null,
                        limit, offset, null, FutureHelper.handlerJsonArray(promise));
                break;
            default:
                //There is no default case
                String message =
                        String.format("[Presences@%s::getEventsByStudent] There is no case for value: %s.",
                                this.getClass().getSimpleName(), type);
                log.error(message);
                promise.fail(message);
        }
        return promise.future();
    }

    private Future<JsonArray> getCountEventsByStudent(String type, String structureId, List<Integer> reasonsIds, List<String> studentIds, String start, String end) {
        Promise<JsonArray> promise = Promise.promise();
        switch (type) {
            case NO_REASON:
                eventService.getCountEventByStudent(1, studentIds, structureId, null, 0,
                        new ArrayList<>(), null, start, end, true, null, false, FutureHelper.handlerJsonArray(promise));
                break;
            case UNREGULARIZED:
                eventService.getCountEventByStudent(1, studentIds, structureId, null, 0,
                        reasonsIds, null, start, end, false, null, false, FutureHelper.handlerJsonArray(promise));
                break;
            case REGULARIZED:
                eventService.getCountEventByStudent(1, studentIds, structureId, null, 0,
                        reasonsIds, null, start, end, false, null, true, FutureHelper.handlerJsonArray(promise));
                break;
            case LATENESS:
                eventService.getCountEventByStudent(2, studentIds, structureId, null, 0,
                        new ArrayList<>(), null, start, end, true, null, null,
                        FutureHelper.handlerJsonArray(promise));
                break;
            case DEPARTURE:
                eventService.getCountEventByStudent(3, studentIds, structureId,
                        null, 0, new ArrayList<>(), null, start, end, true, null,
                        null, FutureHelper.handlerJsonArray(promise));
                break;
            default:
                //There is no default case
                String message =
                        String.format("[Presences@%s::getCountEventsByStudent] There is no case for value: %s.",
                                this.getClass().getSimpleName(), type);
                log.error(message);
                promise.fail(message);
        }
        return promise.future();
    }

    //Map events result (resultFutures) by types {{ NO_REASON / UNREGULARIZED / REGULARIZED / LATENESS / DEPARTURE }}
    @SuppressWarnings("unchecked")
    private Map<String, JsonObject> setEventsByStudents(List<String> types, Map<Integer, JsonObject> reasons, JsonObject settings,
                                                        List<Future<JsonArray>> resultFutures, Map<String, JsonObject> studentsEvents) {
        for (int i = 0; i < types.size(); i++) {
            String type = types.get(i);

            List<JsonObject> values = resultFutures.get(i).result().getList();
            if (values.isEmpty()) continue;

            isAbsence(type);
            List<JsonObject> events = (List<JsonObject>) values.stream()
                    .flatMap(dataType -> dataType.getJsonArray(Field.EVENTS).getList().stream())
                    .collect(Collectors.toList());

            Map<String, TimeslotModel> mapStudentIdTimeslot = settings.getJsonObject(Field.STUDENT_TIMESLOT).stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, stringObjectEntry -> new TimeslotModel((JsonObject) stringObjectEntry.getValue())));
            JsonArray mergedEvents = mapStudentIdTimeslot.isEmpty() ?
                    EventsHelper.mergeEventsByDates(new JsonArray(events), settings.getString(Field.END_OF_HALF_DAY)) :
                    EventsHelper.mergeEventsByDates(new JsonArray(events), settings.getString(Field.END_OF_HALF_DAY), mapStudentIdTimeslot);
            mergedEvents.forEach(o -> {
                        JsonObject event = (JsonObject) o;
                        event.put(Field.REASON, reasons.get(event.getInteger(Field.REASON_ID)));
                        JsonObject studentEvents = studentsEvents.get(event.getString(Field.STUDENT_ID));
                        if (studentEvents != null && studentEvents.containsKey(Field.ALL))
                            studentEvents.getJsonObject(Field.ALL).getJsonArray(type, new JsonArray()).add(event);
                    });
        }
        return studentsEvents;
    }

    @SuppressWarnings("unchecked")
    private Map<String, JsonObject> setCountEventsByStudents(List<String> types, List<Future<JsonArray>> resultFutures,
                                                             Map<String, JsonObject> studentsEvents) {
        for (int i = 0; i < types.size(); i++) {
            String type = types.get(i);
            List<JsonObject> values = resultFutures.get(i).result().getList();
            if (values.isEmpty()) continue;
            for (JsonObject countType : values) {
                JsonObject studentEvents = studentsEvents.get(countType.getString(Field.STUDENT_ID));
                if (studentEvents != null && studentEvents.containsKey(Field.TOTALS))
                    studentEvents.getJsonObject(Field.TOTALS).put(type, countType.getInteger(Field.COUNT));
            }
        }

        return studentsEvents;

    }

    // Absence are events {{NO_REASON}} {{REGULARIZED}} and {{UNREGULARIZED}}
    private Boolean isAbsence(String type) {
        return type.equals(NO_REASON) || type.equals(REGULARIZED) || type.equals(UNREGULARIZED);
    }

}