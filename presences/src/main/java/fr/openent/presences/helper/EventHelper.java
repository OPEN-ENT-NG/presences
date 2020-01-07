package fr.openent.presences.helper;

import fr.openent.presences.Presences;
import fr.openent.presences.common.helper.DateHelper;
import fr.openent.presences.common.helper.FutureHelper;
import fr.openent.presences.common.helper.RegisterHelper;
import fr.openent.presences.model.Absence;
import fr.openent.presences.model.Event.Event;
import fr.openent.presences.model.Event.EventType;
import fr.openent.presences.model.Event.RegisterEvent;
import fr.openent.presences.model.Person.Student;
import fr.openent.presences.model.Reason;
import fr.openent.presences.model.Slot;
import fr.openent.presences.service.AbsenceService;
import fr.openent.presences.service.ReasonService;
import fr.openent.presences.service.impl.DefaultAbsenceService;
import fr.openent.presences.service.impl.DefaultReasonService;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class EventHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(EventHelper.class);
    private final EventBus eb;
    private AbsenceService absenceService;
    private CourseHelper courseHelper;
    private EventTypeHelper eventTypeHelper;
    private ReasonService reasonService;
    private RegisterHelper registerHelper;
    private PersonHelper personHelper;

    public EventHelper(EventBus eb) {
        this.eb = eb;
        this.absenceService = new DefaultAbsenceService(eb);
        this.courseHelper = new CourseHelper(eb);
        this.eventTypeHelper = new EventTypeHelper();
        this.personHelper = new PersonHelper();
        this.reasonService = new DefaultReasonService();
        this.registerHelper = new RegisterHelper(eb, Presences.dbSchema);
    }

    public JsonArray mergeAbsencesSlots(JsonArray slots, List<Absence> absences) {
        for (int i = 0; i < slots.size(); i++) {
            JsonObject slot = slots.getJsonObject(i);
            JsonArray slotEvents = slot.getJsonArray("events");

            try {
                for (Absence absence : absences) {
                    if (DateHelper.isBetween(
                            absence.getStartDate(),
                            absence.getEndDate(),
                            slot.getString("start"),
                            slot.getString("end"))) {
                        slotEvents.add(absence.toJSON());
                    }
                }
            } catch (ParseException e) {
                LOGGER.error("[Presences@EventHelper] Failed to parse date", e);
                return slots;
            }
        }
        return slots;
    }

    /**
     * Convert JsonArray into event list
     *
     * @param array               JsonArray response
     * @param mandatoryAttributes List of mandatory attributes
     * @return new list of events
     */
    public static List<Event> getEventListFromJsonArray(JsonArray array, List<String> mandatoryAttributes) {
        List<Event> eventList = new ArrayList<>();
        for (Object o : array) {
            if (!(o instanceof JsonObject)) continue;
            Event event = new Event((JsonObject) o, mandatoryAttributes);
            eventList.add(event);
        }
        return eventList;
    }

    public void addReasonsToEvents(List<Event> events, List<Integer> reasonIds, Future<JsonObject> future) {
        if (reasonIds.size() == 0) {
            future.complete();
            return;
        }
        reasonService.getReasons(reasonIds, reasonsResult -> {
            if (reasonsResult.isRight()) {
                JsonArray formatReasonsResult = new JsonArray();
                for (int i = 0; i < reasonsResult.right().getValue().size(); i++) {
                    formatReasonsResult.add(new JsonObject(reasonsResult.right().getValue().getJsonObject(i).getString("reason")));
                }
                List<Reason> reasons = ReasonHelper.getReasonListFromJsonArray(formatReasonsResult, Reason.MANDATORY_ATTRIBUTE);
                for (Event event : events) {
                    for (Reason reason : reasons) {
                        if (event.getReason().getId() != null) {
                            if (event.getReason().getId().equals(reason.getId())) {
                                event.setReason(reason);
                            }
                        }
                    }
                }
                future.complete();
            } else {
                future.fail("Failed to query reason info");
            }
        });
    }

    public void addEventTypeToEvents(List<Event> events, List<Integer> eventTypeIds, Future<JsonObject> eventTypeFuture) {
        if (eventTypeIds.size() == 0) {
            eventTypeFuture.complete();
            return;
        }
        eventTypeHelper.getEventType(eventTypeIds, eventTypeResult -> {
            if (eventTypeResult.isRight()) {
                List<EventType> eventTypes = EventTypeHelper
                        .getEventTypeListFromJsonArray(eventTypeResult.right().getValue(), Reason.MANDATORY_ATTRIBUTE);
                for (Event event : events) {
                    for (EventType eventType : eventTypes) {
                        if (event.getEventType().getId() != null) {
                            if (event.getEventType().getId().equals(eventType.getId())) {
                                event.setEventType(eventType);
                            }
                        }
                    }
                }
                eventTypeFuture.complete();
            } else {
                eventTypeFuture.fail("Failed to query event type info");
            }
        });
    }

    public void addStudentsToEvents(List<Event> events, List<String> studentIds, String startDate, String endDate,
                                    String structureId, JsonArray absences, JsonObject slots, Future<JsonObject> studentFuture) {

        Future<JsonArray> registerEventFuture = Future.future();
        Future<JsonArray> studentsInfosFuture = Future.future();

        registerHelper.getRegisterEventHistory(startDate, endDate, new JsonArray(studentIds),
                FutureHelper.handlerJsonArray(registerEventFuture));
        personHelper.getStudentsInfo(structureId, studentIds, FutureHelper.handlerJsonArray(studentsInfosFuture));

        CompositeFuture.all(registerEventFuture, studentsInfosFuture).setHandler(eventResult -> {
            if (eventResult.failed()) {
                String message = "[Presences@EventHelper] Failed to retrieve registerEvent or student info";
                LOGGER.error(message);
                studentFuture.fail(message);
            } else {
                List<RegisterEvent> registerEvents = fr.openent.presences.helper.RegisterHelper
                        .getRegisterEventListFromJsonArray(registerEventFuture.result());
                List<Student> students = personHelper.getStudentListFromJsonArray(
                        studentsInfosFuture.result(), Student.MANDATORY_ATTRIBUTE
                );
                for (Event event : events) {
                    for (Student student : students) {
                        if (event.getStudent().getId().equals(student.getId())) {
                            event.setStudent(student.clone());
                        }
                    }
                    for (RegisterEvent register : registerEvents) {
                        if (register.getStudentId().equals(event.getStudent().getId())) {
                            event.getStudent().getDayHistory()
                                    .addAll(filterEvents(register.getEvents(), event.getStartDate()));
                        }
                    }
                }
                matchSlots(slots, events, absences, studentFuture);
            }
        });
    }

    private JsonArray filterEvents(JsonArray events, String eventStartDate) {
        JsonArray filteredEvents = new JsonArray();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        try {
            Date startDate = sdf.parse(eventStartDate);
            for (int i = 0; i < events.size(); i++) {
                JsonObject event = events.getJsonObject(i);
                Date studentStartDate = sdf.parse(event.getString("start_date"));
                if (startDate.equals(studentStartDate)) {
                    filteredEvents.add(event);
                }
            }
        } catch (ParseException e) {
            String message = "[Presences@EventHelper] Failed to filter events";
            LOGGER.error(message, e);
            return filteredEvents;
        }

        return filteredEvents;
    }

    /**
     * Squash events student event history and structure slot profile.
     *
     * @param slotsBody Time slots object
     * @param events    Events list
     * @param absences  Absences list
     * @param future    Function handler returning data
     */
    private void matchSlots(JsonObject slotsBody, List<Event> events, JsonArray absences, Future<JsonObject> future) {
        List<Slot> slots = SlotHelper.getSlotListFromJsonArray(slotsBody.getJsonArray("slots"));
        List<Absence> absencesList = AbsenceHelper.getAbsenceListFromJsonArray(absences, Absence.MANDATORY_ATTRIBUTE);
        for (Event event : events) {
            try {
                JsonArray clone = registerHelper.cloneSlots(SlotHelper.getSlotJsonArrayFromList(slots), event.getStartDate());
                Student student = event.getStudent();
                JsonArray history = student.getDayHistory();
                JsonArray userSlots = clone.copy();

                if (history.size() == 0) {
                    student.setDayHistory(userSlots);
                } else {
                    if (student.getDayHistory().size() != 9) {
                        List<Absence> filteredAbsenceList = absencesList.stream()
                                .filter(absence -> absence.getStudentId().equals(student.getId()))
                                .collect(Collectors.toList());
                        student.setDayHistory(
                                mergeAbsencesSlots(
                                        registerHelper.mergeEventsSlots(student.getDayHistory(), userSlots), filteredAbsenceList
                                )
                        );
                    }
                }
                matchAbsencesSlot(events);
            } catch (Exception e) {
                String message = "[Presences@EventHelper] Failed to parse slots";
                LOGGER.error(message, e);
                future.fail(message);
                return;
            }
        }
        future.complete();
    }

    /**
     * Squash events (absence only) into student event history
     *
     * @param events Events list
     */
    private void matchAbsencesSlot(List<Event> events) throws ParseException {
        for (Event event : events) {
            if (event.getType().equals("absence")) {
                for (int i = 0; i < event.getStudent().getDayHistory().size(); i++) {
                    JsonObject history = event.getStudent().getDayHistory().getJsonObject(i);
                    if (DateHelper.isBetween(
                            event.getStartDate(),
                            event.getEndDate(),
                            history.getString("start"),
                            history.getString("end"))) {
                        JsonObject absenceEvent = new JsonObject()
                                .put("id", event.getId())
                                .put("start_date", event.getStartDate())
                                .put("end_date", event.getEndDate())
                                .put("reason_id", event.getReason().getId())
                                .put("counsellor_regularisation", event.isCounsellorRegularisation())
                                .put("type", event.getType());
                        history.getJsonArray("events").add(absenceEvent);
                    }
                }
            }
        }
    }
}