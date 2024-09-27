package fr.openent.presences.service.impl;

import fr.openent.presences.common.helper.DateHelper;
import fr.openent.presences.common.helper.FutureHelper;
import fr.openent.presences.common.incidents.Incidents;
import fr.openent.presences.common.service.GroupService;
import fr.openent.presences.common.service.impl.DefaultGroupService;
import fr.openent.presences.common.viescolaire.Viescolaire;
import fr.openent.presences.core.constants.Field;
import fr.openent.presences.enums.EventTypeEnum;
import fr.openent.presences.enums.Events;
import fr.openent.presences.helper.CalendarHelper;
import fr.openent.presences.service.EventService;
import fr.openent.presences.service.NotebookService;
import fr.openent.presences.service.RegistryService;
import fr.wseduc.webutils.Either;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

public class DefaultRegistryService implements RegistryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultRegistryService.class);
    private EventService eventService;
    private GroupService groupService;
    private NotebookService notebookService;

    public DefaultRegistryService(EventBus eb) {
        this.eventService = new DefaultEventService(eb);
        this.groupService = new DefaultGroupService(eb);
        this.notebookService = new DefaultNotebookService();
    }

    @Override
    public void get(String month, List<String> groups, List<String> eventTypes,
                    String structureId, boolean forgottenBooleanFilter, Handler<Either<String, JsonArray>> handler) {
        getStudents(month, groups, eventTypes, structureId, forgottenBooleanFilter, handler);
    }

    private void getStudents(String month, List<String> groups, List<String> eventTypes, String structureId, boolean forgottenBooleanFilter, Handler<Either<String, JsonArray>> handler) {
        String startDate;
        String endDate;
        Date monthDate;
        try {
            monthDate = DateHelper.parse(month, DateHelper.YEAR_MONTH);
            startDate = DateHelper.getFirstDayOfMonth(monthDate);
            endDate = DateHelper.getLastDayOfMonth(monthDate);
        } catch (ParseException e) {
            String message = "[Presences@DefaultRegisterService] Failed to parse month date";
            LOGGER.error(message, e);
            handler.handle(new Either.Left<>(message));
            return;
        }

        Promise<JsonArray> daysPromise = Promise.promise();
        Future<JsonArray> studentFuture = groupService.getGroupStudents(structureId, groups);

        generateMonthDaysArray(monthDate, structureId, daysPromise);

        Future.all(daysPromise.future(), studentFuture).onComplete(event -> {
            if (event.failed()) {
                String message = "[Presences@DefaultRegistryService] Failed to retrieve user groups or generate month days for register summary";
                LOGGER.error(message);
                handler.handle(new Either.Left<>(message));
                return;
            }

            JsonArray users = studentFuture.result();
            JsonArray days = daysPromise.future().result();

            List<String> userIds = new ArrayList<>();
            for (int i = 0; i < users.size(); i++) {
                userIds.add(users.getJsonObject(i).getString("id"));
            }

            notebookService.get(userIds, DateHelper.getDateString(startDate, DateHelper.YEAR_MONTH_DAY),
                    DateHelper.getDateString(endDate, DateHelper.YEAR_MONTH_DAY), resultNotebook -> {
                        if (resultNotebook.isLeft()) {
                            String message = "[Presences@DefaultRegistryService] Failed to retrieve forgotten notebook for all students";
                            LOGGER.error(message, resultNotebook.left().getValue());
                            handler.handle(new Either.Left<>(message));
                            return;
                        }
                        JsonArray studentsForgottenNotebook = resultNotebook.right().getValue();
                        JsonArray finalUserList = formatUsers(users, days, studentsForgottenNotebook, forgottenBooleanFilter);
                        processEvents(startDate, endDate, users, eventTypes, eventsEvent -> {
                            if (eventsEvent.isLeft()) {
                                String message = "[Presences@DefaultRegistryService] Failed to retrieve events";
                                LOGGER.error(message, eventsEvent.left().getValue());
                                handler.handle(new Either.Left<>(message));
                                return;
                            }

                            // Events contains presences events and incidents
                            JsonArray events = eventsEvent.right().getValue();
                            HashMap<String, List<JsonObject>> eventsMap = mapEventsByUserIdentifier(events);
                            handler.handle(new Either.Right<>(mergeUsersEvents(finalUserList, eventsMap)));
                        });
                    });
        });
    }

    @Override
    public void getCSV(String month, List<String> groups, List<String> eventTypes,
                       String structureId, boolean forgottenBooleanFilter, Handler<Either<String, JsonArray>> handler) {

        getStudents(month, groups, eventTypes, structureId, forgottenBooleanFilter, result -> {

            if (result.isLeft()) {
                handler.handle(new Either.Left<>("[Presences@DefaultRegistry::getCSV] Failed to export Registry"));
            } else {
                JsonArray students = result.right().getValue();

                JsonArray events = new JsonArray();
                for (int studentIndex = 0; studentIndex < students.size(); studentIndex++) {
                    JsonObject student = students.getJsonObject(studentIndex);
                    JsonArray days = student.getJsonArray("days");

                    for (int dayIndex = 0; dayIndex < days.size(); dayIndex++) {
                        JsonObject day = days.getJsonObject(dayIndex);
                        JsonArray ev = day.getJsonArray("events");

                        for (int eventIndex = 0; eventIndex < ev.size(); eventIndex++) {
                            JsonObject event = ev.getJsonObject(eventIndex);
                            event.put(Field.LASTNAME, student.getString(Field.LASTNAME));
                            event.put(Field.FIRSTNAME, student.getString(Field.FIRSTNAME));
                            event.put(Field.CLASSNAME, getClassNames(student));
                            events.add(event);
                        }

                        if (day.getBoolean(Field.FORGOTTENNOTEBOOK)) {
                            JsonObject event = new JsonObject();
                            event.put(Field.LASTNAME, student.getString(Field.LASTNAME));
                            event.put(Field.FIRSTNAME, student.getString(Field.FIRSTNAME));
                            event.put(Field.CLASSNAME, getClassNames(student));
                            event.put(Field.START_DATE, day.getString(Field.DATE));
                            event.put(Field.END_DATE, (byte[]) null);
                            event.put(Field.TYPE, EventTypeEnum.FORGOTTEN_NOTEBOOK.toString());
                            events.add(event);
                        }
                    }
                }
                handler.handle(new Either.Right<>(events));
            }
        });
    }

    private String getClassNames(JsonObject student) {
        return student.getJsonArray(Field.CLASSES, new JsonArray()).stream()
                .map(classesObject -> ((JsonObject)classesObject).getString(Field.NAME, ""))
                .filter(className -> !"".equals(className))
                .collect(Collectors.joining(", "));
    }

    /**
     * Format users as renderer list
     *
     * @param users                   User list
     * @param daysResult              Month days list
     * @param forgottenNotebookFilter forgotten notebook filter
     * @return User                         list that contains formatted users
     */
    @SuppressWarnings("unchecked")
    private JsonArray formatUsers(JsonArray users, JsonArray daysResult, JsonArray studentsForgottenNotebook,
                                  boolean forgottenNotebookFilter) {
        return new JsonArray(((List<JsonObject>) users.getList())
                .stream()
                .map(user -> {
                    user.put(Field.DAYS, copyDays(daysResult, user, studentsForgottenNotebook, forgottenNotebookFilter));
                    return user;
                })
                .collect(Collectors.toList()));
    }

    /**
     * Copy new days object
     *
     * @param daysResults               Month days list
     * @param user                      user info
     * @param studentsForgottenNotebook list of all student forgotten notebook
     * @param forgottenNotebookFilter   forgotten notebook filter
     * @return days                         New List with specific value for each day
     * and add forgottenNotebook if current student has forgotten its notebook
     */
    private JsonArray copyDays(JsonArray daysResults, JsonObject user, JsonArray studentsForgottenNotebook,
                               boolean forgottenNotebookFilter) {
        JsonArray days = new JsonArray();
        for (int i = 0; i < daysResults.size(); i++) {
            JsonObject date = new JsonObject()
                    .put("date", daysResults.getJsonObject(i).getString("date"))
                    .put("exclude", daysResults.getJsonObject(i).getBoolean("exclude"))
                    .put("forgottenNotebook", addForgottenNotebook(
                            daysResults.getJsonObject(i).getString("date"),
                            user, studentsForgottenNotebook, forgottenNotebookFilter));
            days.add(date.put("events", new JsonArray()));
        }
        return days;
    }

    private boolean addForgottenNotebook(String date, JsonObject user, JsonArray studentsForgottenNotebook,
                                         boolean forgottenNotebookFilter) {
        boolean hasForgottenNotebook = false;
        if (forgottenNotebookFilter) {
            for (int i = 0; i < studentsForgottenNotebook.size(); i++) {
                JsonObject studentForgottenNotebook = studentsForgottenNotebook.getJsonObject(i);
                if (studentForgottenNotebook.getString("student_id").contains(user.getString("id")) &&
                        studentForgottenNotebook.getString("date")
                                .equals(DateHelper.getDateString(date, DateHelper.YEAR_MONTH_DAY))) {
                    hasForgottenNotebook = true;
                }
            }
        }
        return hasForgottenNotebook;
    }

    /**
     * Get all events. Retrieve presences events and incidents
     *
     * @param startDate  Range start date. Define the min event date.
     * @param endDate    Range end date. Define the max event date.
     * @param users      User list. Define the user list search..
     * @param eventTypes Event type list. Define the event types search.
     * @param handler    Function handler returning data.
     */
    private void processEvents(String startDate, String endDate, JsonArray users, List<String> eventTypes, Handler<Either<String, JsonArray>> handler) {
        //TODO: Don't forget to retrieve schoolbook after its implementation
        boolean needsIncident = eventTypes.contains(Events.INCIDENT.toString());
        boolean needsEvents = eventTypes.contains(Events.ABSENCE.toString()) || eventTypes.contains(Events.LATENESS.toString()) || eventTypes.contains(Events.DEPARTURE.toString());
        List<String> userIds = new ArrayList<>();
        List<Number> types = mapStringTypesToNumberTypes(eventTypes);
        for (int i = 0; i < users.size(); i++) {
            userIds.add(users.getJsonObject(i).getString("id"));
        }

        List<Future<JsonArray>> futures = new ArrayList<>();
        Promise<JsonArray> eventsPromise = Promise.promise();
        Promise<JsonArray> incidentsPromise = Promise.promise();

        if (needsIncident) {
            futures.add(incidentsPromise.future());
            Incidents.getInstance().getIncidents(startDate, endDate, userIds, FutureHelper.handlerEitherPromise(incidentsPromise));
        }

        if (needsEvents) {
            futures.add(eventsPromise.future());
            eventService.get(startDate, endDate, types, userIds, FutureHelper.handlerEitherPromise(eventsPromise));
        }

        if (futures.isEmpty()) {
            handler.handle(new Either.Right<>(new JsonArray()));
        }

        Future.all(futures).onComplete(event -> {
            if (event.failed()) {
                String message = "[Presences@DefaultRegistryService] Failed to retrieve events or incidents for register summary";
                LOGGER.error(message, event.cause());
                handler.handle(new Either.Left<>(message));
                return;
            }

            JsonArray finalEvents = new JsonArray();
            finalEvents.addAll(needsEvents ? formatEvents(eventsPromise.future().result()) : new JsonArray());
            finalEvents.addAll(needsIncident ? formatIncidents(incidentsPromise.future().result()) : new JsonArray());

            handler.handle(new Either.Right<>(finalEvents));
        });
    }

    private JsonArray mergeUsersEvents(JsonArray users, HashMap<String, List<JsonObject>> eventMap) {
        for (int i = 0; i < users.size(); i++) {
            JsonObject user = users.getJsonObject(i);
            if (!eventMap.containsKey(user.getString("id"))) continue;
            List<JsonObject> userEventsList = eventMap.get(user.getString("id"));
            for (JsonObject event : userEventsList) {
                try {
                    int dayNumber = DateHelper.getDayOfMonthNumber(event.getString("start_date"));
                    user.getJsonArray("days").getJsonObject(dayNumber - 1).getJsonArray("events").add(event);
                } catch (ParseException e) {
                    LOGGER.error("[Presences@DefaultRegistryService] Failed to get day of month number, date : " + event.getString("start_date"), e);
                    continue;
                }
            }
        }
        return sort(users);
    }

    private JsonArray sort(JsonArray users) {
        List<JsonObject> list = users.getList();
        Collections.sort(list, (o1, o2) -> o1.getString("displayName").compareToIgnoreCase(o2.getString("displayName")));

        return new JsonArray(list);
    }

    /**
     * Map all events in a map containing user identifier as key and its events as list in value
     *
     * @param events All user events
     * @return Map containing all events
     */
    private HashMap<String, List<JsonObject>> mapEventsByUserIdentifier(JsonArray events) {
        HashMap<String, List<JsonObject>> map = new HashMap<>();
        for (int i = 0; i < events.size(); i++) {
            JsonObject e = events.getJsonObject(i);
            if (!map.containsKey(e.getString("student_id"))) {
                map.put(e.getString("student_id"), new ArrayList<>());
            }

            map.get(e.getString("student_id")).add(e);
        }
        return map;
    }

    private JsonArray formatEvents(JsonArray events) {
        JsonArray evts = new JsonArray();
        for (int i = 0; i < events.size(); i++) {
            JsonObject e = events.getJsonObject(i);
            JsonObject eventObject = new JsonObject()
                    .put("student_id", e.getString("student_id"))
                    .put("start_date", e.getString("start_date", ""))
                    .put("end_date", e.getString("end_date", ""))
                    .put("followed", e.getBoolean("followed", false))
                    .put("counsellor_regularisation", e.getBoolean("counsellor_regularisation", false))
                    .put("type", getEventTypeName(e.getInteger("type_id")));
            if (getEventTypeName(e.getInteger("type_id")).equals(Events.ABSENCE.toString())
                    && e.getLong("reason_id") != null) {
                eventObject.put("reason_id", e.getLong("reason_id"));
            }
            if (getEventTypeName(e.getInteger("type_id")).equals(Events.ABSENCE.toString())
                    && e.getString("reason") != null) {
                eventObject.put("reason", e.getString("reason"));
            }
            evts.add(eventObject);
        }

        return evts;
    }

    private JsonArray formatIncidents(JsonArray incidents) {
        JsonArray icdts = new JsonArray();
        for (int i = 0; i < incidents.size(); i++) {
            JsonObject idt = incidents.getJsonObject(i);
            icdts.add(
                    new JsonObject()
                            .put("student_id", idt.getString("student_id"))
                            .put("start_date", idt.getString("date"))
                            .put("end_date", idt.getString("date"))
                            .put("incident_type", idt.getString("incident_type"))
                            .put("place", idt.getString("place"))
                            .put("protagonist_type", idt.getString("protagonist_type"))
                            .put("type", Events.INCIDENT)
            );
        }
        return icdts;
    }

    private String getEventTypeName(int type) {
        for (EventTypeEnum t : EventTypeEnum.values()) {
            if (t.getType().equals(type)) return t.name();
        }

        return "";
    }

    /**
     * Generate an array of object containing each month day
     *
     * @param month       A date in the month we need to generate Array (We don't care of the day, we just want a
     *                    day in expected month)
     * @param structureId structure identifier used to ask all exclusions days in order
     *                    to position our day if it an exclusion day
     * @param promise      Promise to complete the process
     */
    private void generateMonthDaysArray(Date month, String structureId, Promise<JsonArray> promise) {

        Promise<JsonArray> exclusionDaysPromise = Promise.promise();
        Promise<JsonObject> saturdayCoursesCountPromise = Promise.promise();
        Promise<JsonObject> sundayCoursesCountPromise = Promise.promise();

        Viescolaire.getInstance().getExclusionDays(structureId, FutureHelper.handlerEitherPromise(exclusionDaysPromise));
        CalendarHelper.getWeekEndCourses(structureId, CalendarHelper.SATURDAY_OF_WEEK, FutureHelper.handlerEitherPromise(saturdayCoursesCountPromise));
        CalendarHelper.getWeekEndCourses(structureId, CalendarHelper.SUNDAY_OF_WEEK, FutureHelper.handlerEitherPromise(sundayCoursesCountPromise));

        Future.all(exclusionDaysPromise.future(), saturdayCoursesCountPromise.future(), sundayCoursesCountPromise.future()).onComplete(result -> {
            if (result.failed()) {
                String message = "[Presences@DefaultRegistryService] Failed to fetch exclusion days or week-end courses";
                LOGGER.error(message);
                promise.fail(message);
            } else {
                JsonArray days = new JsonArray();
                long saturdayCourses = saturdayCoursesCountPromise.future().result().getLong("count");
                long sundayCourses = sundayCoursesCountPromise.future().result().getLong("count");
                Calendar cal = CalendarHelper.resetDay(month);
                int min = cal.getActualMinimum(Calendar.DAY_OF_MONTH);
                int max = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
                for (int i = min; i <= max; i++) {
                    JsonObject day = new JsonObject();
                    String date = DateHelper.getPsqlSimpleDateFormat().format(cal.getTime());
                    day.put("date", date);
                    day.put("events", new JsonArray());
                    CalendarHelper.setExcludeDay(day, date, exclusionDaysPromise.future().result(), CalendarHelper.SATURDAY_OF_WEEK,
                            saturdayCourses, CalendarHelper.SUNDAY_OF_WEEK, sundayCourses);
                    days.add(day);
                    cal.add(Calendar.DAY_OF_MONTH, 1);
                }
                promise.complete(days);
            }
        });
    }

    private List<Number> mapStringTypesToNumberTypes(List<String> stringTypes) {
        List<Number> types = new ArrayList<>();
        for (String type : stringTypes) {
            if (!type.equals(Events.INCIDENT.toString())) {
                types.add(EventTypeEnum.valueOf(type).getType());
            }
        }

        return types;
    }

}
