package fr.openent.presences.service.impl;

import fr.openent.presences.common.helper.DateHelper;
import fr.openent.presences.common.helper.FutureHelper;
import fr.openent.presences.service.EventService;
import fr.openent.presences.service.EventStudentService;
import fr.openent.presences.service.ReasonService;
import fr.openent.presences.service.SettingsService;
import fr.wseduc.webutils.Either;
import io.vertx.core.*;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;


public class DefaultEventStudentService implements EventStudentService {

    private static final Logger log = LoggerFactory.getLogger(DefaultEventStudentService.class);

    private final String NO_REASON = "NO_REASON";
    private final String UNREGULARIZED = "UNREGULARIZED";
    private final String REGULARIZED = "REGULARIZED";
    private final String LATENESS = "LATENESS";
    private final String DEPARTURE = "DEPARTURE";

    private final String HALF_DAY = "HALF_DAY";
    private final String HOUR = "HOUR";
    private final String DAY = "DAY";

    private final String AFTERNOON = "AFTERNOON";

    private final EventService eventService;
    private final ReasonService reasonService = new DefaultReasonService();
    private final SettingsService settingsService = new DefaultSettingsService();
    private final EventBus eb;

    public DefaultEventStudentService(EventBus eventBus) {
        super();
        eb = eventBus;
        eventService = new DefaultEventService(eventBus);
    }

    @Override
    public void get(MultiMap body, Handler<AsyncResult<JsonObject>> handler) {
        String student = body.get("id");
        String structure = body.get("structure_id");
        String limit = body.get("limit");
        String offset = body.get("offset");
        String start = body.get("start_at");
        String end = body.get("end_at");
        List<String> types = body.getAll("type");

        if (!validTypes(types)) {
            String message = "[Presences@DefaultEventStudentService::get] Types are not valid.";
            handler.handle(Future.failedFuture(message));
            return;
        }

        Future<Map<Integer, JsonObject>> reasonFuture = Future.future();
        Future<JsonObject> viscoSettingsFuture = Future.future();
        Future<JsonObject> settingsFuture = Future.future();
        Future<JsonObject> timeSlotsFuture = Future.future();

        CompositeFuture.all(reasonFuture, viscoSettingsFuture, settingsFuture, timeSlotsFuture).setHandler(settingsResult -> {
            if (settingsResult.failed()) {
                log.error(settingsResult.cause().getMessage());
                handler.handle(Future.failedFuture(settingsResult.cause().getMessage()));
                return;
            }
            Map<Integer, JsonObject> reasons = reasonFuture.result();
            JsonObject settings = viscoSettingsFuture.result();
            settings.mergeIn(settingsFuture.result());
            settings.mergeIn(timeSlotsFuture.result());

            getEvents(types, structure, student, reasons, settings, start, end, limit, offset, result -> {
                if (result.failed()) {
                    handler.handle(Future.failedFuture(result.cause()));
                    return;
                }

                Future<JsonObject> future = Future.future();
                getTotals(student, structure, limit, offset, start, end, types, reasons, settings, result, future);

                CompositeFuture.all(Collections.singletonList(future)).setHandler(resultTotals -> {
                    if (resultTotals.failed()) {
                        String message = "[Presences@DefaultEventStudentService::get] Failed to get totals from events.";
                        handler.handle(Future.failedFuture(message + " " + resultTotals.cause()));
                        return;
                    }

                    JsonObject response = new JsonObject()
                            .put("limit", limit)
                            .put("offset", offset)
                            .put("all", result.result())
                            .put("totals", future.result())
                            .put("recovery_method", settings.getString("event_recovery_method"));
                    handler.handle(Future.succeededFuture(response));
                });
            });
        });

        // Get reasons types to map it thanks reason_id on absences justified events results
        getReasons(structure, reasonFuture);
        // Get viscolaire settings to get Half Day Date configured by the current structure
        //  (if absences events are configured by Half Days).
        getHalfDay(structure, viscoSettingsFuture);
        // Get presence settings to get recovery method (for absences events configuration retrieving)
        getSettings(structure, settingsFuture);
        // Get timeSlots settings configured by the current structure, to configure absences events times
        // (if they are retrieved by Hald Days or by Days)
        getTimeSlots(structure, timeSlotsFuture);
    }

    private void getTotals(String student, String structure, String limit, String offset, String start, String end,
                           List<String> types, Map<Integer, JsonObject> reasons, JsonObject settings,
                           AsyncResult<JsonObject> result, Future<JsonObject> future) {
        if (limit == null && offset == null) { // If we get all result, we just need to get array size to get total results
            future.complete(getTotalsByTypes(types, result.result()));
        } else { // Else, we use same queries with a count result
            getEvents(types, structure, student, reasons, settings, start, end, null, null, resultAllEvents -> {
                if (resultAllEvents.failed()) {
                    future.fail(resultAllEvents.cause());
                    return;
                }
                future.complete(getTotalsByTypes(types, resultAllEvents.result()));
            });
        }
    }

    private void getTimeSlots(String structure, Future<JsonObject> timeSlotsFuture) {
        getTimeSlots(structure, timeslotsResult -> {
            if (timeslotsResult.failed()) {
                timeSlotsFuture.fail(timeslotsResult.cause().getMessage());
                return;
            }

            timeSlotsFuture.complete(timeslotsResult.result());
        });
    }

    private void getSettings(String structure, Future<JsonObject> settingsFuture) {
        settingsService.retrieve(structure, settingsResult -> {
            if (settingsResult.isLeft()) {
                settingsFuture.fail(settingsResult.left().getValue());
                return;
            }
            settingsFuture.complete(settingsResult.right().getValue());
        });
    }

    private void getHalfDay(String structure, Future<JsonObject> viscoSettingsFuture) {
        getViscoSettings(structure, viscoResult -> {
            if (viscoResult.failed()) {
                viscoSettingsFuture.fail(viscoResult.cause().getMessage());
                return;
            }

            viscoSettingsFuture.complete(viscoResult.result());
        });
    }

    private void getReasons(String structure, Future<Map<Integer, JsonObject>> reasonFuture) {
        reasonService.fetchReason(structure, resultReason -> {
            if (resultReason.isLeft()) {
                String message = "[Presences@DefaultEventStudentService::get] Failed to get structure absence reasons.";
                reasonFuture.fail(message + " " + resultReason.left().getValue());
                return;
            }
            Map<Integer, JsonObject> reasons = new HashMap<>();
            for (Object o : resultReason.right().getValue()) {
                JsonObject reason = (JsonObject) o;
                reasons.put(reason.getInteger("id"), reason);
            }
            reasonFuture.complete(reasons);
        });
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
    private void getEvents(List<String> types, String structure, String student, Map<Integer, JsonObject> reasons, JsonObject settings,
                           String start, String end, String limit, String offset, Handler<AsyncResult<JsonObject>> handler) {
        List<Integer> reasonsIds = new ArrayList<>(reasons.keySet());
        List<Future> futures = new ArrayList<>();
        for (String type : types) {
            Future<JsonArray> future = Future.future();
            futures.add(future);
            getEventsByStudent(type, structure, reasonsIds, student, start, end, limit, offset, FutureHelper.handlerJsonArray(future));
        }

        CompositeFuture.all(futures).setHandler(result -> {
            if (result.failed()) {
                String message = "[Presences@DefaultEventStudentService::getEvents] Failed to retrieve events info.";
                handler.handle(Future.failedFuture(message + " " + result.cause()));
                return;
            }

            handler.handle(Future.succeededFuture(getEventsFromFutures(types, reasons, settings, futures, handler)));
        });
    }

    // Get events for a student, corresponding to the type mentioned
    private void getEventsByStudent(String type, String structureId, List<Integer> reasonsIds, String studentId, String start, String end, String limit,
                                    String offset, Handler<Either<String, JsonArray>> handler) {
        switch (type) {
            case NO_REASON:
                eventService.getEventsByStudent(1, Collections.singletonList(studentId), structureId, null,
                        new ArrayList<>(), null, start, end, true, null, false, handler);
                break;
            case UNREGULARIZED:
                eventService.getEventsByStudent(1, Collections.singletonList(studentId), structureId, null,
                        reasonsIds, null, start, end, false, null, false, handler);
                break;
            case REGULARIZED:
                eventService.getEventsByStudent(1, Collections.singletonList(studentId), structureId, null,
                        reasonsIds, null, start, end, false, null, true, handler);
                break;
            case LATENESS:
                eventService.getEventsByStudent(2, Collections.singletonList(studentId), structureId,
                        null, new ArrayList<>(), null, start, end, true, null,
                        limit, offset, null, handler);
                break;
            case DEPARTURE:
                eventService.getEventsByStudent(3, Collections.singletonList(studentId), structureId,
                        null, new ArrayList<>(), null, start, end, true, null,
                        limit, offset, null, handler);
                break;
            default:
                //There is no default case
                String message = "There is no case for value: " + type + ".";
                log.error(message);
                handler.handle(new Either.Left<>(message));
        }
    }

    //Map events result (resultFutures) by types {{ NO_REASON / UNREGULARIZED / REGULARIZED / LATENESS / DEPARTURE }}
    private JsonObject getEventsFromFutures(List<String> types, Map reasons, JsonObject settings,
                                            List<Future> resultFutures, Handler<AsyncResult<JsonObject>> handler) {
        JsonObject sorted_events = new JsonObject();
        String recovery = settings.getString("event_recovery_method");
        for (int i = 0; i < types.size(); i++) {
            String type = types.get(i);
            sorted_events.put(type, new JsonArray());

            JsonArray values = (JsonArray) resultFutures.get(i).result();
            if (values.isEmpty()) continue;
            ((List<JsonObject>) values.getList()).forEach(dataType -> {
                if (isAbsence(type) && (recovery.equals(HALF_DAY) || recovery.equals(DAY))) {
                    try {
                        addAbsencesByRecovery(sorted_events, dataType, settings, type, recovery, reasons);
                    } catch (ParseException e) {
                        handler.handle(Future.failedFuture(e.getMessage()));
                    }
                } else {
                    dataType.getJsonArray("events").forEach(o -> {
                        JsonObject event = (JsonObject) o;
                        event.put("reason", reasons.get(event.getInteger("reason_id")));
                        sorted_events.getJsonArray(type).add(event);
                    });
                }


            });
        }
        return sorted_events;
    }

    // If absences events are recovered by Half Days or Days, map start_date and end_date events to correspond to it.
    private void addAbsencesByRecovery(JsonObject sorted_events, JsonObject eventsData, JsonObject settings,
                                       String type, String recovery, Map reasons) throws ParseException {
        JsonArray slots = settings.getJsonArray("slots");

        String halfDay = settings.getString("end_of_half_day");
        String startFirstSlot = slots.getJsonObject(0).getString("startHour");
        String endLastSlot = slots.getJsonObject(slots.size() - 1).getString("endHour");

        Date start = DateHelper.parse(eventsData.getString("start_date"), DateHelper.SQL_DATE_FORMAT);
        Date end = DateHelper.parse(eventsData.getString("end_date"), DateHelper.SQL_DATE_FORMAT);

        String startDateResult;
        String endDateResult;

        if (recovery.equals(HALF_DAY)) {
            String MORNING = "MORNING";
            if (eventsData.getString("period").equals(MORNING)) {
                startDateResult = formatDateAndTime(start, startFirstSlot);
                endDateResult = formatDateAndTime(end, halfDay);
            } else { // For afternoon start_date is half day and end_date is end of day
                startDateResult = formatDateAndTime(start, halfDay);
                endDateResult = formatDateAndTime(end, endLastSlot);
            }
            eventsData.put("start_date", startDateResult);
            eventsData.put("end_date", endDateResult);
        } else { // For DAY start_date is start of day and end_date is end of day
            startDateResult = formatDateAndTime(start, startFirstSlot);
            endDateResult = formatDateAndTime(end, endLastSlot);

            eventsData.put("start_date", startDateResult);
            eventsData.put("end_date", endDateResult);
        }
        addReasonToRecoveredAbsence(eventsData, reasons);
        eventsData.remove("events");
        sorted_events.getJsonArray(type).add(eventsData);
    }

    private String formatDateAndTime(Date date, String time) throws ParseException {
        return DateHelper.getDateString(date, DateHelper.YEAR_MONTH_DAY) + " " +
                DateHelper.getTimeString(time, DateHelper.HOUR_MINUTES);
    }

    // Add reasons on justified absences
    private void addReasonToRecoveredAbsence(JsonObject eventsData, Map reasons) {
        ArrayList<Integer> distinctReasons = (ArrayList<Integer>) eventsData.getJsonArray("events")
                .stream().map(event -> ((JsonObject) event).getInteger("reason_id"))
                .distinct().collect(Collectors.toList());

        if (distinctReasons.size() > 1) eventsData.put("reason", reasons.get(-1)); // multiple reasons
        else eventsData.put("reason", reasons.get(distinctReasons.get(0)));
    }

    // Retrieve totals by types
    private JsonObject getTotalsByTypes(List<String> types, JsonObject sorted_events) {
        JsonObject totals = new JsonObject();
        for (String type : types) {
            totals.put(type, sorted_events.getJsonArray(type).size());
        }
        return totals;
    }

    // Absence are events {{NO_REASON}} {{REGULARIZED}} and {{UNREGULARIZED}}
    private Boolean isAbsence(String type) {
        return type.equals(NO_REASON) || type.equals(REGULARIZED) || type.equals(UNREGULARIZED);
    }

}