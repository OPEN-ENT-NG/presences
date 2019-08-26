package fr.openent.presences.controller;

import fr.openent.presences.common.helper.DateHelper;
import fr.openent.presences.common.helper.FutureHelper;
import fr.openent.presences.common.security.SearchRight;
import fr.openent.presences.helper.CourseHelper;
import fr.openent.presences.helper.MapHelper;
import fr.openent.presences.helper.SubjectHelper;
import fr.openent.presences.service.AbsenceService;
import fr.openent.presences.service.EventService;
import fr.openent.presences.service.ExemptionService;
import fr.openent.presences.service.GroupService;
import fr.openent.presences.service.impl.DefaultAbsenceService;
import fr.openent.presences.service.impl.DefaultEventService;
import fr.openent.presences.service.impl.DefaultExemptionService;
import fr.openent.presences.service.impl.DefaultGroupService;
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
import java.util.*;

import static fr.openent.presences.common.helper.DateHelper.DAY_MONTH_YEAR;
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
                        CompositeFuture.all(subjectsFuture, eventsFuture, exemptionsFuture, incidentsFuture).setHandler(futureEvent -> {
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
                                                .put("exemption", new JsonObject().put("start_date", exemption.getString("start_date")).put("end_date", exemption.getString("end_date")));
                                    } else {
                                        course.put("exempted", false);
                                    }
                                }

                                JsonObject incident = incident(course, incidents);
                                if (incident != null) {
                                    course.put("incident", new JsonObject().put("description", incident.getString("description")).put("date", incident.getString("date")));
                                }
                            }

                            for (int i = 0; i < absents.size() ; i++) {
                                courses.add(addNewCourse(absents.getJsonObject(i), params.get("structure")));
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

    private JsonObject addNewCourse(JsonObject absent, String structure) {
        try {

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date date = sdf.parse(absent.getString("start_date"));
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1;

            return new JsonObject()
                    .put("_id", "0")
                    .put("dayOfWeek", dayOfWeek == 0 ? 7 : dayOfWeek)
                    .put("locked", true)
                    .put("is_periodic", false)
                    .put("is_recurrent", true)
                    .put("absence", true)
                    .put("structureId", structure)
                    .put("events", new JsonArray())
                    .put("startDate", absent.getString("start_date"))
                    .put("endDate", absent.getString("end_date"))
                    .put("roomLabels", new JsonArray())
                    .put("subjectId", "")
                    .put("subject_name", "")
                    .put("startMomentDate", DateHelper.getDateString(absent.getString("start_date"), DAY_MONTH_YEAR))
                    .put("startMomentTime", DateHelper.getDateString(absent.getString("start_date"), DateHelper.HOUR_MINUTES))
                    .put("endMomentDate", DateHelper.getDateString(absent.getString("end_date"), DAY_MONTH_YEAR))
                    .put("endMomentTime", DateHelper.getDateString(absent.getString("end_date"), DateHelper.HOUR_MINUTES));
        } catch (ParseException e) {
            log.error("[CalendarController@absent] Failed to parse date", e);
            return new JsonObject();
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
        String courseStartDate = course.getString("startDate");
        String courseEndDate = course.getString("endDate");
        for (int i = 0; i < exemptions.size(); i++) {
            JsonObject exemption = exemptions.getJsonObject(i);
            try {
                if (DateHelper.isAfterOrEquals(courseStartDate, exemption.getString("start_date")) && DateHelper.isBeforeOrEquals(courseEndDate, exemption.getString("end_date"))) {
                    return exemption;
                }
            } catch (ParseException e) {
                log.error("[CalendarController@exempted] Failed to parse date", e);
            }
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
        absenceService.get(start, end, users, FutureHelper.handlerJsonArray(future));
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
