package fr.openent.presences.controller;

import fr.openent.presences.common.helper.DateHelper;
import fr.openent.presences.common.helper.FutureHelper;
import fr.openent.presences.common.security.SearchRight;
import fr.openent.presences.common.service.GroupService;
import fr.openent.presences.common.service.impl.DefaultGroupService;
import fr.openent.presences.helper.CourseHelper;
import fr.openent.presences.helper.MapHelper;
import fr.openent.presences.helper.SubjectHelper;
import fr.openent.presences.service.AbsenceService;
import fr.openent.presences.service.EventService;
import fr.openent.presences.service.ExemptionService;
import fr.openent.presences.service.impl.DefaultAbsenceService;
import fr.openent.presences.service.impl.DefaultEventService;
import fr.openent.presences.service.impl.DefaultExemptionService;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.security.Md5;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;

import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;

import static fr.openent.presences.common.helper.DateHelper.*;
import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;

public class CalendarController extends ControllerHelper {

    private final EventBus eb;
    private AbsenceService absenceService;
    private CourseHelper courseHelper;
    private SubjectHelper subjectHelper;
    private EventService eventService;
    private GroupService groupService;
    private ExemptionService exemptionService;

    public CalendarController(EventBus eb) {
        super();
        this.eb = eb;
        this.absenceService = new DefaultAbsenceService(eb);
        this.courseHelper = new CourseHelper(eb);
        this.subjectHelper = new SubjectHelper(eb);
        this.eventService = new DefaultEventService(eb);
        this.groupService = new DefaultGroupService(eb);
        this.exemptionService = new DefaultExemptionService(eb);
    }

    @Get("/calendar/courses")
    @ApiDoc("Retrieve all courses and events")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(SearchRight.class)
    public void getCalendarCourses(HttpServerRequest request) {
        MultiMap params = request.params();
        if (!courseHelper.checkParams(params)) {
            badRequest(request);
            return;
        }

        List<String> users = params.getAll("user");
        String structure = params.get("structure");
        groupService.getUserGroups(users, structure, groupEvent -> {
            if (groupEvent.isLeft()) {
                log.error("[CalendarController@getCalendarCourses] Failed to retrieve users groups", groupEvent.left().getValue());
                renderError(request);
                return;
            }
            List<String> groups = getGroupsName(groupEvent.right().getValue());
            courseHelper.getCourses(structure, params.getAll("teacher"), groups,
                    params.get("start"), params.get("end"), either -> {
                        if (either.isLeft()) {
                            log.error("[CalendarController@getCalendarCourses] Failed to retrieve courses", either.left().getValue());
                            renderError(request);
                            return;
                        }
                        JsonArray courses = either.right().getValue();
                        List<String> subjects = new ArrayList<>();
                        HashMap<String, JsonArray> eventList = new HashMap<>();
                        for (int i = 0; i < courses.size(); i++) {
                            JsonObject course = courses.getJsonObject(i);
                            if (!subjects.contains(course.getString("subjectId")) && !course.containsKey("exceptionnal")) {
                                subjects.add(course.getString("subjectId"));
                            }
                            String hash = hash(course.getString("_id") + course.getString("startDate") + course.getString("endDate"));
                            eventList.put(hash, new JsonArray());
                        }

                        List<Integer> eventTypes = Arrays.asList(1, 2, 3, 4);
                        Future<JsonArray> subjectsFuture = Future.future();
                        Future<JsonArray> eventsFuture = Future.future();
                        Future<JsonArray> exemptionsFuture = Future.future();
                        Future<JsonArray> incidentsFuture = Future.future();
                        Future<JsonArray> absentFuture = Future.future();
                        CompositeFuture.all(subjectsFuture, eventsFuture, exemptionsFuture, incidentsFuture, absentFuture).setHandler(futureEvent -> {
                            if (futureEvent.failed()) {
                                log.error("[CalendarController@getCalendarCourses] Failed to retrieve information", futureEvent.cause());
                                renderError(request);
                                return;
                            }
                            JsonArray subjectList = subjectsFuture.result();
                            JsonArray events = eventsFuture.result();
                            JsonObject exemptionsMap = MapHelper.transformToMapMultiple(exemptionsFuture.result(), "subject_id");
                            JsonObject subjectMap = MapHelper.transformToMap(subjectList, "id");
                            JsonArray incidents = incidentsFuture.result();
                            JsonArray absents = absentFuture.result();
                            for (int i = 0; i < events.size(); i++) {
                                JsonObject event = events.getJsonObject(i);
                                String eventHash = hash(event.getString("course_id") + event.getString("course_start_date") + event.getString("course_end_date"));
                                if (eventList.containsKey(eventHash)) {
                                    event.remove("course_id");
                                    event.remove("course_start_date");
                                    event.remove("course_end_date");
                                    eventList.get(eventHash).add(event);
                                }
                            }

                            for (int i = 0; i < courses.size(); i++) {
                                JsonObject course = courses.getJsonObject(i);
                                String subjectId = course.getString("subjectId");
                                if (course.containsKey("exceptionnal")) {
                                    course.put("subject_name", course.getString("exceptionnal"));
                                } else {
                                    course.put("subject_name", subjectMap.containsKey(subjectId)
                                            ? subjectMap.getJsonObject(subjectId).getString("externalId")
                                            : "");
                                }
                                formatCourse(course);
                                String courseHash = hash(course.getString("_id") + course.getString("startDate") + course.getString("endDate"));
                                course.put("events", eventList.containsKey(courseHash) ? eventList.get(courseHash) : new JsonArray());
                                if (exemptionsMap.containsKey(subjectId)) {
                                    JsonObject exemption = exempted(course, exemptionsMap.getJsonArray(subjectId));
                                    if (exemption != null) {
                                        course.put("exempted", true)
                                                .put("exemption", new JsonObject()
                                                        .put("start_date", exemption.getString("start_date"))
                                                        .put("end_date", exemption.getString("end_date"))
                                                        .put("attendance", exemption.getBoolean("attendance")));
                                    } else {
                                        course.put("exempted", false);
                                    }
                                }

                                JsonObject incident = incident(course, incidents);
                                if (incident != null) {
                                    course.put("incident", new JsonObject().put("description", incident.getString("description")).put("date", incident.getString("date")));
                                }
                            }

                            if (absents != null) {
                                for (int i = 0; i < absents.size(); i++) {
                                    if (calendarMatchDate(absents.getJsonObject(i), params.get("start"), params.get("end"))) {
                                        courses.addAll(addNewCourse(absents.getJsonObject(i), params));
                                    }
                                }
                            }
                            renderJson(request, courses);
                        });

                        getSubjects(subjects, subjectsFuture);
                        getEvents(params.get("structure"), params.get("start") + " 00:00:00", params.get("end") + " 23:59:59", eventTypes, Arrays.asList(params.get("user")), eventsFuture);
                        getExemptions(params.get("structure"), params.get("start") + " 00:00:00", params.get("end") + " 23:59:59", params.get("user"), exemptionsFuture);
                        getIncidents(params.get("structure"), params.get("start") + " 00:00:00", params.get("end") + " 23:59:59", params.get("user"), incidentsFuture);
                        getAbsences(params.get("start"), params.get("end"), params.get("user"), absentFuture);
                    });
        });
    }

    private boolean calendarMatchDate(JsonObject events, String startDate, String endDate) {
        try {
            return DateHelper.isBetween(
                    events.getString("start_date"),
                    events.getString("end_date"),
                    startDate + "T00:00:00.000",
                    endDate + "T00:00:00.000"
            );
        } catch (ParseException e) {
            log.error("[CalendarController@calendarMatchDate] Failed to parse date", e);
            return false;
        }
    }

    private JsonArray addNewCourse(JsonObject absent, MultiMap params) {
        try {
            String startDateTime = DateHelper.getTimeString(absent.getString("start_date"), SQL_FORMAT);
            String endDateTime = DateHelper.getTimeString(absent.getString("end_date"), SQL_FORMAT);

            JsonArray coursesAdded = new JsonArray();

            List<LocalDate> totalDates = DateHelper.getDatesBetweenTwoDates(params.get("start"), params.get("end"));

            LocalDate absentsStart = LocalDate.parse(DateHelper.getDateString(absent.getString("start_date"), YEAR_MONTH_DAY));
            LocalDate absentsEnd = LocalDate.parse(DateHelper.getDateString(absent.getString("end_date"), YEAR_MONTH_DAY));
            for (LocalDate totalDate : totalDates) {
                if ((totalDate.isAfter(absentsStart) || totalDate.isEqual(absentsStart)) && (totalDate.isBefore(absentsEnd) || totalDate.isEqual(absentsEnd))) {
                    String startDate = totalDate.isEqual(absentsStart) ? totalDate.toString() : totalDate.toString();
                    String startTime = (totalDate.isEqual(absentsStart) ? startDateTime : "00:00");
                    String endDate = (totalDate.isEqual(absentsEnd) ? totalDate.toString() : totalDate.toString());
                    String endTime = (totalDate.isEqual(absentsEnd) ? endDateTime : "23:59");

                    coursesAdded.add(new JsonObject()
                            .put("_id", "0")
                            .put("dayOfWeek", DateHelper.getDayOfWeek(DateHelper.parse(totalDate.toString(), YEAR_MONTH_DAY)))
                            .put("is_periodic", false)
                            .put("absence", true)
                            .put("absenceId", absent.getLong("id"))
                            .put("absenceReason", absent.getInteger("reason_id") != null ? absent.getInteger("reason_id") : 0)
                            .put("structureId", params.get("structure"))
                            .put("events", new JsonArray())
                            .put("startDate", startDate + " " + startTime)
                            .put("startMomentDate", startDate + " " + startTime)
                            .put("startMomentTime", startTime)
                            .put("endDate", endDate + " " + endTime)
                            .put("endMomentDate", endDate + " " + endTime)
                            .put("endMomentTime", startTime)
                    );
                }
            }
            return coursesAdded;
        } catch (ParseException e) {
            log.error("[CalendarController@absent] Failed to parse date", e);
            return new JsonArray();
        }
    }

    private JsonObject incident(JsonObject course, JsonArray incidents) {
        for (int j = 0; j < incidents.size(); j++) {
            JsonObject incident = incidents.getJsonObject(j);
            try {
                if (DateHelper.isBeforeOrEquals(course.getString("startDate"), incident.getString("date")) && DateHelper.isAfterOrEquals(course.getString("endDate"), incident.getString("date"))) {
                    return incident;
                }
            } catch (ParseException e) {
                log.error("[CalendarController@incident] Failed to parse date", e);
            }
        }

        return null;
    }

    private JsonObject exempted(JsonObject course, JsonArray exemptions) {
        String courseStartDate = DateHelper.getDateString(course.getString("startDate"), YEAR_MONTH_DAY);
        String courseEndDate = DateHelper.getDateString(course.getString("endDate"), YEAR_MONTH_DAY);
        SimpleDateFormat sdf = new SimpleDateFormat(YEAR_MONTH_DAY);

        try {
            Date dateCourseStartDate = sdf.parse(courseStartDate);
            Date dateCourseEndDate = sdf.parse(courseEndDate);

            for (int i = 0; i < exemptions.size(); i++) {
                JsonObject exemption = exemptions.getJsonObject(i);

                Date exemptionStartDate = sdf.parse(DateHelper.getDateString(exemption.getString("start_date"), YEAR_MONTH_DAY));
                Date exemptionEndDate = sdf.parse(DateHelper.getDateString(exemption.getString("end_date"), YEAR_MONTH_DAY));

                if ((dateCourseStartDate.after(exemptionStartDate) || dateCourseStartDate.equals(exemptionStartDate)) &&
                        (dateCourseEndDate.before(exemptionEndDate) || dateCourseEndDate.equals(exemptionEndDate))) {
                    return exemption;
                }

            }
        } catch (ParseException e) {
            log.error("[CalendarController@exempted] Failed to parse date", e);
        }

        return null;
    }

    private void formatCourse(JsonObject course) {
        course.remove("color");
        course.remove("startCourse");
        course.remove("endCourse");
        course.remove("teacherIds");
        course.remove("manual");
        course.put("locked", true);
        course.put("startMomentDate", DateHelper.getDateString(course.getString("startDate"), DAY_MONTH_YEAR));
        course.put("startMomentTime", DateHelper.getDateString(course.getString("startDate"), DateHelper.HOUR_MINUTES));
        course.put("endMomentDate", DateHelper.getDateString(course.getString("endDate"), DAY_MONTH_YEAR));
        course.put("endMomentTime", DateHelper.getDateString(course.getString("endDate"), DateHelper.HOUR_MINUTES));
    }

    private List<String> getGroupsName(JsonArray groups) {
        List<String> names = new ArrayList<>();
        for (int i = 0; i < groups.size(); i++) {
            JsonObject group = groups.getJsonObject(i);
            if (group.containsKey("name")) names.add(group.getString("name"));
        }

        return names;
    }

    private String hash(String value) {
        String hash = "";
        try {
            hash = Md5.hash(value);
        } catch (NoSuchAlgorithmException e) {
            log.error("[CalendarController@hash] Failed to hash " + value, e);
        }

        return hash;
    }

    private void getIncidents(String structureId, String startDate, String endDate, String userId, Future<JsonArray> future) {
        JsonObject action = new JsonObject()
                .put("action", "getUserIncident")
                .put("structureId", structureId)
                .put("userId", userId)
                .put("start_date", startDate)
                .put("end_date", endDate);

        eb.send("fr.openent.incident", action, handlerToAsyncHandler(event -> {
            if (!"ok".equals(event.body().getString("status"))) {
                log.error("[Presences@CalendarController] Failed to retrieve incidents", event.body().getString("error"));
                future.fail(event.body().getString("error"));
                return;
            }

            future.complete(event.body().getJsonArray("results"));
        }));
    }

    private void getExemptions(String structureId, String startDate, String endDate, String users, Future<JsonArray> future) {
        exemptionService.get(structureId, startDate, endDate, users, null, FutureHelper.handlerJsonArray(future));
    }

    private void getSubjects(List<String> subjects, Future<JsonArray> future) {
        subjectHelper.getSubjects(subjects, FutureHelper.handlerJsonArray(future));
    }

    private void getEvents(String structureId, String startDate, String endDate, List<Integer> eventType, List<String> users, Future<JsonArray> future) {
        eventService.list(structureId, startDate, endDate, eventType, users, FutureHelper.handlerJsonArray(future));
    }

    private void getAbsences(String start, String end, String userId, Future<JsonArray> future) {
        List<String> users = new ArrayList<>();
        users.add(userId);
        absenceService.getAbsencesBetween(start, end, users, FutureHelper.handlerJsonArray(future));
    }

    @Get("/calendar/groups/:id/students")
    @ApiDoc("Retrieve students in given group")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(SearchRight.class)
    public void getGroupStudents(HttpServerRequest request) {
        String groupIdentifier = request.getParam("id");
        groupService.getGroupStudents(groupIdentifier, arrayResponseHandler(request));
    }

}
