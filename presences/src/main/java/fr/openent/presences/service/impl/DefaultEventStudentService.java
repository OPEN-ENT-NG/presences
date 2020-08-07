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

    private final String JUSTIFIED = "JUSTIFIED";
    private final String UNJUSTIFIED = "UNJUSTIFIED";
    private final String LATENESS = "LATENESS";
    private final String DEPARTURE = "DEPARTURE";

    private final String HALF_DAY = "HALF_DAY";
    private final String HOUR = "HOUR";
    private final String DAY = "DAY";

    private final String AFTERNOON = "AFTERNOON";
    private final String MORNING = "MORNING";

    private EventService eventService;
    private ReasonService reasonService = new DefaultReasonService();
    private SettingsService settingsService = new DefaultSettingsService();
    private EventBus eb;

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

        // Get reasons types to map it thanks reason_id on absences justified events results
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

        // Get viscolaire settings to get Half Day Date configured by the current structure
        //  (if absences events are configured by Half Days).
        getViscoSettings(structure, viscoResult -> {
            if (viscoResult.failed()) {
                viscoSettingsFuture.fail(viscoResult.cause().getMessage());
                return;
            }

            viscoSettingsFuture.complete(viscoResult.result());
        });

        // Get presence settings to get recovery method (for absences events configuration retrieving)
        settingsService.retrieve(structure, settingsResult -> {
            if (settingsResult.isLeft()) {
                settingsFuture.fail(settingsResult.left().getValue());
                return;
            }
            settingsFuture.complete(settingsResult.right().getValue());
        });

        // Get timeSlots settings configured by the current structure, to configure absences events times
        // (if they are retrieved by Hald Days or by Days)
        getTimeSlots(structure, timeslotsResult -> {
            if (timeslotsResult.failed()) {
                timeSlotsFuture.fail(timeslotsResult.cause().getMessage());
                return;
            }

            timeSlotsFuture.complete(timeslotsResult.result());
        });

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

            getSortedEventsByTypes(types, structure, student, reasons, settings, start, end, limit, offset, result -> {
                if (result.failed()) {
                    handler.handle(Future.failedFuture(result.cause()));
                    return;
                }

                Future<JsonObject> future = Future.future();
                if (limit == null && offset == null) { // If we get all result, we just need to get array size to get total results
                    future.complete(getTotalsByTypes(types, result.result()));
                } else { // Else, we use same queries with a count result
                    getSortedEventsByTypes(types, structure, student, reasons, settings, start, end, null, null, resultAllEvents -> {
                        if (resultAllEvents.failed()) {
                            future.fail(resultAllEvents.cause());
                            return;
                        }
                        future.complete(getTotalsByTypes(types, resultAllEvents.result()));
                    });
                }

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
    }

    // check if types are in {{ JUSTIFIED / UNJUSTIFIED / LATENESS / DEPARTURE }}
    private boolean validTypes(List<String> types) {
        boolean valid = true;
        for (String type : types) {
            switch (type) {
                case JUSTIFIED:
                case UNJUSTIFIED:
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


    // Send request to get each events (thanks the great method corresponding to type {{ JUSTIFIED / UNJUSTIFIED / LATENESS / DEPARTURE }}).
    // Handler will return them mapped by type.
    private void getSortedEventsByTypes(List<String> types, String structure, String student, Map reasons, JsonObject settings, String start, String end,
                                        String limit, String offset, Handler<AsyncResult<JsonObject>> handler) {
        List<Future> futures = new ArrayList<>();
        for (String type : types) {
            Future<JsonArray> future = Future.future();
            futures.add(future);
            getTypedEventsByStudent(type, structure, student, start, end, limit, offset, FutureHelper.handlerJsonArray(future));
        }

        CompositeFuture.all(futures).setHandler(result -> {
            if (result.failed()) {
                String message = "[Presences@DefaultEventStudentService::getSortedEventsByTypes] Failed to retrieve events info.";
                handler.handle(Future.failedFuture(message + " " + result.cause()));
                return;
            }

            handler.handle(Future.succeededFuture(getSortedByTypesEventsFromFutures(types, reasons, settings, futures, handler)));
        });
    }

    // Get events for a student, corresponding to the type mentioned
    private void getTypedEventsByStudent(String type, String structureId, String studentId, String start, String end, String limit, String offset, Handler<Either<String, JsonArray>> handler) {
        switch (type) {
            case UNJUSTIFIED:
                eventService.getEventsByStudent(1, Collections.singletonList(studentId), structureId,
                        false, new ArrayList<>(), null, start, end, true, null, limit, offset, null, handler);
                break;
            case JUSTIFIED:
                eventService.getEventsByStudent(1, Collections.singletonList(studentId), structureId,
                        true, new ArrayList<>(), null, start, end, true, null, limit, offset, null, handler);
                break;
            case LATENESS:
                eventService.getEventsByStudent(2, Collections.singletonList(studentId), structureId,
                        null, new ArrayList<>(), null, start, end, true, null, limit, offset, null, handler);
                break;
            case DEPARTURE:
                eventService.getEventsByStudent(3, Collections.singletonList(studentId), structureId,
                        null, new ArrayList<>(), null, start, end, true, null, limit, offset, null, handler);
                break;
            default:
                //There is no default case
                String message = "There is no case for value: " + type + ".";
                log.error(message);
                handler.handle(new Either.Left(message));
        }
    }

    //Map events result (resultFutures) by types {{ JUSTIFIED / UNJUSTIFIED / LATENESS / DEPARTURE }}
    private JsonObject getSortedByTypesEventsFromFutures(List<String> types, Map reasons, JsonObject settings, List<Future> resultFutures, Handler<AsyncResult<JsonObject>> handler) {
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

        long halfTime = DateHelper.parse(halfDay, halfDay.length() > 5 ? DateHelper.HOUR_MINUTES_SECONDS : DateHelper.HOUR_MINUTES).getTime();
        long startFirstSlotTime = DateHelper.parse(startFirstSlot, startFirstSlot.length() > 5 ? DateHelper.HOUR_MINUTES_SECONDS : DateHelper.HOUR_MINUTES).getTime();
        long endLastSlotTime = DateHelper.parse(endLastSlot, endLastSlot.length() > 5 ? DateHelper.HOUR_MINUTES_SECONDS : DateHelper.HOUR_MINUTES).getTime();

        Date start = DateHelper.parse(eventsData.getString("start_date"), DateHelper.SQL_DATE_FORMAT);
        Date end = DateHelper.parse(eventsData.getString("end_date"), DateHelper.SQL_DATE_FORMAT);

        if (recovery.equals(HALF_DAY)) {
            // For HALF_DAY morning start_date is start of day and end_date is half day
            if (eventsData.getString("period").equals(MORNING)) {
                start.setTime(start.getTime() + startFirstSlotTime);
                end.setTime(end.getTime() + halfTime);
            } else { // For afternoon start_date is half day and end_date is end of day
                start.setTime(start.getTime() + halfTime);
                end.setTime(end.getTime() + endLastSlotTime);
            }
            eventsData.put("start_date", DateHelper.getDateString(start.toInstant().toString(), DateHelper.MONGO_FORMAT));
            eventsData.put("end_date", DateHelper.getDateString(end.toInstant().toString(), DateHelper.MONGO_FORMAT));
        } else { // For DAY start_date is start of day and end_date is end of day
            start.setTime(start.getTime() + startFirstSlotTime);
            end.setTime(end.getTime() + endLastSlotTime);
            eventsData.put("start_date", DateHelper.getDateString(start.toInstant().toString(), DateHelper.MONGO_FORMAT));
            eventsData.put("end_date", DateHelper.getDateString(end.toInstant().toString(), DateHelper.MONGO_FORMAT));
        }
        addReasonToRecoveredAbsence(eventsData, reasons);
        eventsData.remove("events");
        sorted_events.getJsonArray(type).add(eventsData);
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

    // Absence are events {{JUSTIFIED}} and {{UNJUSTIFIED}}
    private Boolean isAbsence(String type) {
        return type.equals(JUSTIFIED) || type.equals(UNJUSTIFIED);
    }

}