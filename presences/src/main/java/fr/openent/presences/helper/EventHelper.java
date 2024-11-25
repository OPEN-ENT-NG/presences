package fr.openent.presences.helper;

import fr.openent.presences.Presences;
import fr.openent.presences.common.helper.*;
import fr.openent.presences.common.service.UserService;
import fr.openent.presences.common.service.impl.DefaultUserService;
import fr.openent.presences.core.constants.Field;
import fr.openent.presences.enums.Events;
import fr.openent.presences.enums.WorkflowActions;
import fr.openent.presences.model.Absence;
import fr.openent.presences.model.Event.Event;
import fr.openent.presences.model.Event.EventType;
import fr.openent.presences.model.Event.RegisterEvent;
import fr.openent.presences.model.Person.Student;
import fr.openent.presences.model.Person.User;
import fr.openent.presences.model.Reason;
import fr.openent.presences.model.Slot;
import fr.openent.presences.service.ActionService;
import fr.openent.presences.service.ReasonService;
import fr.openent.presences.service.impl.DefaultActionService;
import fr.openent.presences.service.impl.DefaultReasonService;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.user.UserInfos;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class EventHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(EventHelper.class);
    private final EventTypeHelper eventTypeHelper;
    private final ReasonService reasonService;
    private final RegisterHelper registerHelper;
    private final PersonHelper personHelper;
    private final ActionService actionService;
    private final UserService userService;

    public EventHelper(EventBus eb) {
        this.eventTypeHelper = new EventTypeHelper();
        this.personHelper = new PersonHelper();
        this.reasonService = new DefaultReasonService();
        this.registerHelper = new RegisterHelper(eb, Presences.dbSchema);
        this.actionService = new DefaultActionService();
        this.userService = new DefaultUserService();
    }

    public JsonObject getCreationStatement(JsonObject event, UserInfos user) {
        String query = "INSERT INTO " + Presences.dbSchema + ".event (start_date, end_date, comment, counsellor_input, student_id, register_id, type_id, owner, reason_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "RETURNING id, start_date, end_date, comment, counsellor_input, student_id, register_id, type_id, reason_id;";
        JsonArray params = new JsonArray()
                .add(event.getString("start_date"))
                .add(event.getString("end_date"))
                .add(event.containsKey("comment") ? event.getString("comment") : "")
                .add(WorkflowHelper.hasRight(user, WorkflowActions.MANAGE.toString()))
                .add(event.getString("student_id"))
                .add(event.getInteger("register_id"))
                .add(event.getInteger("type_id"))
                .add(user.getUserId());

        if ((event.getInteger(Field.REASON_ID) != null && event.getInteger(Field.REASON_ID) != -1)) {
            params.add(event.getInteger(Field.REASON_ID));
        } else {
            params.addNull();
        }

        return new JsonObject()
                .put("action", "prepared")
                .put("statement", query)
                .put("values", params);
    }

    public JsonObject getDeletionEventStatement(JsonObject event) {
        String query = "DELETE FROM " + Presences.dbSchema + ".event WHERE type_id IN (2, 3) AND register_id = ? AND student_id = ?";
        JsonArray params = new JsonArray()
                .add(event.getInteger("register_id"))
                .add(event.getString("student_id"));

        return new JsonObject()
                .put("action", "prepared")
                .put("statement", query)
                .put("values", params);
    }

    @SuppressWarnings("unchecked")
    public void addLastActionAbbreviation(List<Event> events, Promise<JsonObject> promise) {
        List<Integer> ids = this.getAllEventsIds(events);
        actionService.getLastAbbreviations(ids, res -> {
            if (res.isLeft()) {
                promise.fail(res.left().getValue());
                return;
            }

            JsonArray result = res.right().getValue();
            Map<Integer, String> map = ((List<JsonObject>) result.getList())
                    .stream()
                    .collect(Collectors.toMap(abbr -> abbr.getInteger("event_id"), abbr -> abbr.getString("abbreviation")));


            events.forEach(event -> {
                List<JsonObject> dayHistory = event.getStudent().getDayHistory().getList();
                dayHistory.forEach(slot ->
                        ((List<JsonObject>) slot.getJsonArray("events").getList()).forEach(eventHistory ->
                                eventHistory.put("actionAbbreviation", map.getOrDefault(eventHistory.getInteger("id"), null))
                        )
                );

                List<String> actionAbbreviations = dayHistory.stream()
                        .flatMap(slot -> ((List<JsonObject>) slot.getJsonArray("events").getList())
                                .stream()
                                .map(eventHistory -> eventHistory.getString("actionAbbreviation"))
                        )
                        .filter(Objects::nonNull)
                        .distinct()
                        .collect(Collectors.toList());

                if (actionAbbreviations.size() == 1) event.setActionAbbreviation(actionAbbreviations.get(0));
                else if (actionAbbreviations.size() > 1) event.setActionAbbreviation("MULTIPLES");

            });
            promise.complete();
        });
    }

    @SuppressWarnings("unchecked")
    private List<Integer> getAllEventsIds(List<Event> events) {
        List<Integer> ids = new ArrayList<>();
        events.forEach(event -> {
            ((List<JsonObject>) event.getStudent().getDayHistory().getList()).forEach(dayHistory ->
                    ((List<JsonObject>) dayHistory.getJsonArray("events").getList()).forEach(eventHistory -> {
                        if (!eventHistory.getString("type").equalsIgnoreCase(Events.ABSENCE.toString())) {
                            ids.add(eventHistory.getInteger("id"));
                        }
                    })
            );
        });
        ids.removeAll(Collections.singletonList(null));
        return ids;
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
                        if (!slotEvents.contains(absence.toJSON().getInteger("id"))) {
                            if (!containsId(slotEvents, absence.toJSON())) {
                                slotEvents.add(absence.toJSON());
                            }
                        }
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

    public static List<Event> getEventListFromJsonArray(JsonArray events) {
        return events.stream().map(event -> new Event((JsonObject) event)).collect(Collectors.toList());
    }

    public static List<JsonObject> getEventListToJsonArray(List<Event> events) {
        return events.stream().map(Event::toJSON).collect(Collectors.toList());
    }

    public static JsonArray getMainEventsJsonArrayFromEventList(List<Event> events) {
        return new JsonArray(events.stream()
                .map(event -> new JsonObject()
                        .put("date", event.getDate())
                        .put("student", event.getStudent().toJSON())
                        .put("reason", event.getReason().toJSON())
                        .put("created", event.getCreated())
                        .put("counsellor_regularisation", event.isCounsellorRegularisation())
                        .put("massmailed", event.isMassmailed())
                        .put("type", event.getType())
                        .put("action_abbreviation", event.getActionAbbreviation()))
                .collect(Collectors.toList()));
    }

    @SuppressWarnings("unchecked")
    public void addReasonsToEvents(List<Event> events, List<Integer> reasonIds, Promise<JsonObject> promise) {
        if (reasonIds.isEmpty()) {
            promise.complete();
            return;
        }
        reasonService.getReasons(reasonIds, reasonsResult -> {
            if (reasonsResult.isRight()) {
                List<Reason> reasons = ReasonHelper.getReasonListFromJsonArray(
                        new JsonArray(((List<JsonObject>) reasonsResult.right().getValue().getList()).stream()
                                .map(reason -> new JsonObject(reason.getString("reason")))
                                .collect(Collectors.toList())),
                        Reason.MANDATORY_ATTRIBUTE
                );

                for (Event event : events) {
                    if (event.getReason().getId() != null) {
                        event.setReason(
                                reasons.stream()
                                        .filter(reason -> event.getReason().getId().equals(reason.getId()))
                                        .findFirst()
                                        .orElse(null)
                        );
                    }
                }
                promise.complete();
            } else {
                String message = String.format("[Presences@EventHelper::addReasonsToEvents] Failed to query reason info : %s", reasonsResult.left().getValue());
                LOGGER.error(message);
                promise.fail(reasonsResult.left().getValue());
            }
        });
    }

    public void addEventTypeToEvents(List<Event> events, List<Integer> eventTypeIds, Promise<JsonObject> eventTypePromise) {
        if (eventTypeIds.isEmpty()) {
            eventTypePromise.complete();
            return;
        }
        eventTypeHelper.getEventType(eventTypeIds, eventTypeResult -> {
            if (eventTypeResult.isRight()) {
                List<EventType> eventTypes = EventTypeHelper
                        .getEventTypeListFromJsonArray(eventTypeResult.right().getValue(), Reason.MANDATORY_ATTRIBUTE);

                for (Event event : events) {
                    if (event.getEventType().getId() != null) {
                        event.setEventType(
                                eventTypes.stream()
                                        .filter(eventType -> event.getEventType().getId().equals(eventType.getId()))
                                        .findFirst()
                                        .orElse(null)
                        );
                    }
                }
                eventTypePromise.complete();
            } else {
                eventTypePromise.fail("Failed to query event type info");
            }
        });
    }

    public void addStudentsToEvents(String structureId, List<Event> events, List<String> studentIds,
                                    List<String> restrictedClasses, String startDate, String endDate,
                                    String startTime, String endTime, List<String> typeIds, List<String> reasonIds,
                                    Boolean noAbsenceReason, Boolean noLatenessReason, Boolean regularized, Boolean followed, JsonArray absences,
                                    JsonObject slots, Promise<JsonObject> studentPromise) {

        Promise<JsonArray> registerEventPromise = Promise.promise();
        Promise<JsonArray> studentsInfosPromise = Promise.promise();

        RegisterPresenceHelper.getEventHistory(structureId, startDate, endDate, startTime, endTime,
                studentIds, typeIds, reasonIds, noAbsenceReason, noLatenessReason, regularized,
                followed, registerEventPromise);

        personHelper.getStudentsInfo(structureId, studentIds, restrictedClasses, FutureHelper.handlerEitherPromise(studentsInfosPromise));

        Future.all(registerEventPromise.future(), studentsInfosPromise.future()).onComplete(eventResult -> {
            if (eventResult.failed()) {
                String message = "[Presences@EventHelper] Failed to retrieve registerEvent or student info";
                LOGGER.error(message);
                studentPromise.fail(message);
            } else {
                List<RegisterEvent> registerEvents = RegisterPresenceHelper.getRegisterEventListFromJsonArray(registerEventPromise.future().result());
                List<Student> students = personHelper.getStudentListFromJsonArray(
                        studentsInfosPromise.future().result()
                );

                for (Event event : events) {
                    // add student info
                    for (Student student : students) {
                        if (event.getStudent().getId().equals(student.getId())) {
                            event.setStudent(student.clone());
                        }
                    }
                    // add dayHistory to student
                    for (RegisterEvent register : registerEvents) {
                        if (register.getStudentId().equals(event.getStudent().getId())) {
                            event.getStudent().setDayHistory(filterEvents(register.getEvents(), event.getDate()));
                        }
                    }
                }
                // Filter events w/ empty students => works only for infinite scroll paging
                events.removeIf(event -> event.getStudent().getName() == null);
                matchSlots(slots, events, absences, studentPromise);
            }
        });
    }

    public void addStudentsToEvents(List<Event> events, List<String> studentIds, List<String> restrictedClasses, String structureId,
                                    Promise<JsonObject> studentPromise) {
        personHelper.getStudentsInfo(structureId, studentIds, restrictedClasses, studentResp -> {
            if (studentResp.isLeft()) {
                String message = "[Presences@EventHelper::addStudentsToEvents] Failed to retrieve students info";
                LOGGER.error(message);
                studentPromise.fail(message);
            } else {
                List<Student> students = personHelper.getStudentListFromJsonArray(studentResp.right().getValue());
                // for some reason, we still manage to find some "duplicate" data so we use mergeFunction (see collectors.toMap)
                Map<String, Student> studentsMap = students.stream().collect(Collectors.toMap(Student::getId, Student::clone,
                        (student1, student2) -> student1));
                events.forEach(event ->
                        event.setStudent(
                                studentsMap.getOrDefault(event.getStudent().getId(),
                                        new Student(event.getStudent().getId()))
                        )
                );
                studentPromise.complete();
            }
        });
    }

    public void addOwnerToEvents(List<Event> events, List<String> ownerIds, Promise<JsonObject> ownerPromise) {
        userService.getUsers(ownerIds, ownerRes -> {
            if (ownerRes.isLeft()) {
                String message = "[Presences@EventHelper::addOwnerToEvents] Failed to retrieve owner info";
                LOGGER.error(message);
                ownerPromise.fail(message);
            } else {
                List<User> owners = personHelper.getUserListFromJsonArray(ownerRes.right().getValue());
                Map<String, User> ownersMap = owners.stream().collect(Collectors.toMap(User::getId, User::clone));
                events.forEach(
                        event -> event.setOwner(ownersMap.getOrDefault(event.getOwner().getId(), new User(event.getOwner().getId())))
                );
                ownerPromise.complete();
            }
        });
    }


    @SuppressWarnings("unchecked")
    public void addOwnerToEvents(List<Event> events, Promise<JsonObject> promise) {
        List<String> userIds = this.getAllOwnerIds(events);
        userService.getUsers(userIds, result -> {
            if (result.isLeft()) {
                String message = "[Presences@EventHelper] Failed to retrieve users info";
                LOGGER.error(message);
                promise.fail(message);
            }
            List<User> owners = personHelper.getUserListFromJsonArray(result.right().getValue());
            Map<String, User> userMap = new HashMap<>();
            owners.forEach(owner -> userMap.put(owner.getId(), owner));
            events.forEach(event -> {
                ((List<JsonObject>) event.getStudent().getDayHistory().getList()).forEach(dayHistory ->
                        ((List<JsonObject>) dayHistory.getJsonArray("events").getList()).forEach(eventHistory -> {
                            if (!eventHistory.getString("type").toUpperCase().equals(Events.ABSENCE.toString()) && eventHistory.getValue("owner") instanceof String) {
                                JsonObject owner = userMap.getOrDefault(eventHistory.getString("owner"), new User(eventHistory.getString("owner"))).toJSON();
                                eventHistory.put("owner", owner);
                            }
                        }));
            });
            promise.complete();
        });
    }

    @SuppressWarnings("unchecked")
    private List<String> getAllOwnerIds(List<Event> events) {
        List<String> userIds = new ArrayList<>();
        events.forEach(event -> {
            userIds.add(event.getOwner().getId());
            ((List<JsonObject>) event.getStudent().getDayHistory().getList()).forEach(dayHistory ->
                    ((List<JsonObject>) dayHistory.getJsonArray("events").getList()).forEach(eventHistory ->
                            userIds.add(eventHistory.getString("owner"))
                    )
            );
        });
        userIds.removeAll(Collections.singletonList(null));
        return userIds;
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
     * @param promise   Promise handler returning data
     */
    private void matchSlots(JsonObject slotsBody, List<Event> events, JsonArray absences, Promise<JsonObject> promise) {
        List<Slot> slots = SlotHelper.getSlotListFromJsonArray(slotsBody.getJsonArray("slots", new JsonArray()));
        List<Absence> absencesList = AbsenceHelper.getAbsenceListFromJsonArray(absences, Absence.MANDATORY_ATTRIBUTE);
        for (Event event : events) {
            try {
                String eventDate = DateHelper.getDateString(event.getDate(), DateHelper.SQL_DATE_FORMAT, DateHelper.SQL_FORMAT);
                JsonArray clone = registerHelper.cloneSlots(SlotHelper.getSlotJsonArrayFromList(slots), eventDate);
                Student student = event.getStudent();
                JsonArray history = student.getDayHistory();
                JsonArray userSlots = clone.copy();

                if (history == null || history.size() == 0) {
                    student.setDayHistory(userSlots);
                } else {
                    List<Absence> filteredAbsenceList = absencesList.stream()
                            .filter(absence -> absence.getStudentId().equals(student.getId()))
                            .collect(Collectors.toList());
                    student.setDayHistory(
                            mergeAbsencesSlots(
                                    registerHelper.mergeEventsSlots(student.getDayHistory(), userSlots), filteredAbsenceList
                            )
                    );
                }
                if (event.getType().toUpperCase().equals(Events.ABSENCE.toString())) {
                    matchAbsencesSlot(event);
                }
            } catch (Exception e) {
                String message = "[Presences@EventHelper] Failed to parse slots";
                LOGGER.error(message, e);
                promise.fail(message);
                return;
            }
        }
        promise.complete();
    }

    /**
     * Squash event (absence only) into student event history
     *
     * @param event Event element
     */
    private void matchAbsencesSlot(Event event) throws ParseException {
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
                        .put("followed", event.isFollowed())
                        .put("type", event.getType());
                if (!containsId(history.getJsonArray("events"), absenceEvent)) {
                    history.getJsonArray("events").add(absenceEvent);
                }
            }
        }
    }

    private boolean containsId(JsonArray slotEvents, JsonObject absence) {
        for (int i = 0; i < slotEvents.size(); i++) {
            JsonObject slotEvent = slotEvents.getJsonObject(i);
            if (slotEvent.getInteger("id").equals(absence.getInteger("id"))) {
                return true;
            }
        }
        return false;
    }
}