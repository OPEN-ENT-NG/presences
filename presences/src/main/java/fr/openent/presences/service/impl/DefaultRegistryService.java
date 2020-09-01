package fr.openent.presences.service.impl;

import fr.openent.presences.common.helper.DateHelper;
import fr.openent.presences.common.helper.FutureHelper;
import fr.openent.presences.common.incidents.Incidents;
import fr.openent.presences.common.service.GroupService;
import fr.openent.presences.common.service.impl.DefaultGroupService;
import fr.openent.presences.common.viescolaire.Viescolaire;
import fr.openent.presences.enums.EventType;
import fr.openent.presences.enums.Events;
import fr.openent.presences.helper.CalendarHelper;
import fr.openent.presences.service.EventService;
import fr.openent.presences.service.NotebookService;
import fr.openent.presences.service.RegistryService;
import fr.wseduc.webutils.Either;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.text.ParseException;
import java.util.*;

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

        Future<JsonArray> daysFuture = Future.future();
        Future<JsonArray> studentFuture = Future.future();

        groupService.getGroupStudents(groups, FutureHelper.handlerJsonArray(studentFuture));
        generateMonthDaysArray(monthDate, structureId, daysFuture);

        CompositeFuture.all(daysFuture, studentFuture).setHandler(event -> {
            if (event.failed()) {
                String message = "[Presences@DefaultRegistryService] Failed to retrieve user groups or generate month days for register summary";
                LOGGER.error(message);
                handler.handle(new Either.Left<>(message));
                return;
            }

            JsonArray users = studentFuture.result();
            JsonArray days = daysFuture.result();

            //Todo : Process events
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
                            event.put("lastName",student.getString("lastName"));
                            event.put("firstName",student.getString("firstName"));
                            event.put("className", student.getString("className"));
                            events.add(event);
                        }

                        if(day.getBoolean("forgottenNotebook")) {
                            JsonObject event = new JsonObject();
                            event.put("lastName",student.getString("lastName"));
                            event.put("firstName",student.getString("firstName"));
                            event.put("className", student.getString("className"));
                            event.put("start_date", day.getString("date"));
                            event.put("end_date", (byte[]) null);
                            event.put("type", EventType.FORGOTTEN_NOTEBOOK.toString());
                            events.add(event);
                        }
                    }
                }
                handler.handle(new Either.Right<>(events));
            }
        });
    }

    /**
     * Format users as renderer list
     *
     * @param users                         User list
     * @param daysResult                    Month days list
     * @param forgottenNotebookFilter       forgotten notebook filter
     * @return User                         list that contains formatted users
     */
    private JsonArray formatUsers(JsonArray users, JsonArray daysResult, JsonArray studentsForgottenNotebook,
                                  boolean forgottenNotebookFilter) {
        JsonArray userList = new JsonArray();
        for (int i = 0; i < users.size(); i++) {
            JsonObject u = users.getJsonObject(i);
            JsonArray days = copyDays(daysResult, u, studentsForgottenNotebook, forgottenNotebookFilter);
            JsonObject user = new JsonObject();
            user.put("id", u.getString("id", ""))
                    .put("displayName", u.getString("displayName", ""))
                    .put("lastName", u.getString("lastName", ""))
                    .put("firstName", u.getString("firstName", ""))
                    .put("className", u.getString("groupName", ""))
                    .put("days", days);

            userList.add(user);
        }

        return userList;
    }

    /**
     * Copy new days object
     *
     * @param daysResults                   Month days list
     * @param user                          user info
     * @param studentsForgottenNotebook     list of all student forgotten notebook
     * @param forgottenNotebookFilter       forgotten notebook filter
     * @return days                         New List with specific value for each day
     *                                      and add forgottenNotebook if current student has forgotten its notebook
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

        List<Future> futures = new ArrayList<>();
        Future<JsonArray> eventsFuture = Future.future();
        Future<JsonArray> incidentsFuture = Future.future();

        if (needsIncident) {
            futures.add(incidentsFuture);
            Incidents.getInstance().getIncidents(startDate, endDate, userIds, FutureHelper.handlerJsonArray(incidentsFuture));
        }

        if (needsEvents) {
            futures.add(eventsFuture);
            eventService.get(startDate, endDate, types, userIds, FutureHelper.handlerJsonArray(eventsFuture));
        }

        if (futures.isEmpty()) {
            handler.handle(new Either.Right<>(new JsonArray()));
        }

        CompositeFuture.all(futures).setHandler(event -> {
            if (event.failed()) {
                String message = "[Presences@DefaultRegistryService] Failed to retrieve events or incidents for register summary";
                LOGGER.error(message, event.cause());
                handler.handle(new Either.Left<>(message));
                return;
            }

            JsonArray finalEvents = new JsonArray();
            finalEvents.addAll(needsEvents ? formatEvents(eventsFuture.result()) : new JsonArray());
            finalEvents.addAll(needsIncident ? formatIncidents(incidentsFuture.result()) : new JsonArray());

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
        for (EventType t : EventType.values()) {
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
     * @param future      Future to complete the process
     */
    private void generateMonthDaysArray(Date month, String structureId, Future<JsonArray> future) {

        Future<JsonArray> exclusionDays = Future.future();
        Future<JsonObject> saturdayCoursesCount = Future.future();
        Future<JsonObject> sundayCoursesCount = Future.future();

        Viescolaire.getInstance().getExclusionDays(structureId, FutureHelper.handlerJsonArray(exclusionDays));
        CalendarHelper.getWeekEndCourses(structureId, CalendarHelper.SATURDAY_OF_WEEK, FutureHelper.handlerJsonObject(saturdayCoursesCount));
        CalendarHelper.getWeekEndCourses(structureId, CalendarHelper.SUNDAY_OF_WEEK, FutureHelper.handlerJsonObject(sundayCoursesCount));

        CompositeFuture.all(exclusionDays, saturdayCoursesCount, sundayCoursesCount).setHandler(result -> {
            if (result.failed()) {
                String message = "[Presences@DefaultRegistryService] Failed to fetch exclusion days or week-end courses";
                LOGGER.error(message);
                future.fail(message);
            } else {
                JsonArray days = new JsonArray();
                long saturdayCourses = saturdayCoursesCount.result().getLong("count");
                long sundayCourses = sundayCoursesCount.result().getLong("count");
                Calendar cal = CalendarHelper.resetDay(month);
                int min = cal.getActualMinimum(Calendar.DAY_OF_MONTH);
                int max = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
                for (int i = min; i <= max; i++) {
                    JsonObject day = new JsonObject();
                    String date = DateHelper.getPsqlSimpleDateFormat().format(cal.getTime());
                    day.put("date", date);
                    day.put("events", new JsonArray());
                    CalendarHelper.setExcludeDay(day, date, exclusionDays.result(), CalendarHelper.SATURDAY_OF_WEEK,
                            saturdayCourses, CalendarHelper.SUNDAY_OF_WEEK, sundayCourses);
                    days.add(day);
                    cal.add(Calendar.DAY_OF_MONTH, 1);
                }
                future.complete(days);
            }
        });
    }

    private List<Number> mapStringTypesToNumberTypes(List<String> stringTypes) {
        List<Number> types = new ArrayList<>();
        for (String type : stringTypes) {
            if (!type.equals(Events.INCIDENT.toString())) {
                types.add(EventType.valueOf(type).getType());
            }
        }

        return types;
    }

}
