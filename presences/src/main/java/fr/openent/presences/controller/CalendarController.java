package fr.openent.presences.controller;

import fr.openent.presences.common.helper.FutureHelper;
import fr.openent.presences.common.security.SearchRight;
import fr.openent.presences.common.service.GroupService;
import fr.openent.presences.common.service.impl.DefaultGroupService;
import fr.openent.presences.common.viescolaire.Viescolaire;
import fr.openent.presences.helper.*;
import fr.openent.presences.model.Course;
import fr.openent.presences.model.Slot;
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
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;

import java.util.*;

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
            List<String> groups = CalendarHelper.getGroupsName(groupEvent.right().getValue());
            Future<List<Course>> coursesFuture = Future.future();
            Future<JsonArray> slotsFuture = Future.future();

            CompositeFuture.all(coursesFuture, slotsFuture).setHandler(futureCourses -> {
                if (futureCourses.failed()) {
                    log.error("[CalendarController@getCalendarCourses] Failed to retrieve courses", futureCourses.cause());
                    renderError(request);
                    return;
                }
                List<Course> courses = coursesFuture.result();
                List<Slot> slots = SlotHelper.getSlotListFromJsonArray(slotsFuture.result(), Slot.MANDATORY_ATTRIBUTE);
                List<String> subjects = new ArrayList<>();

                HashMap<String, Map<String, Course>> eventList = CalendarHelper.hashCourses(courses, slots, subjects);

                List<Integer> eventTypes = Arrays.asList(1, 2, 3, 4);
                Future<JsonArray> subjectsFuture = Future.future();
                Future<JsonArray> eventsFuture = Future.future();
                Future<JsonArray> exemptionsFuture = Future.future();
                Future<JsonArray> incidentsFuture = Future.future();
                Future<JsonArray> absentFuture = Future.future();

                CompositeFuture.all(subjectsFuture, eventsFuture, exemptionsFuture, incidentsFuture, absentFuture)
                        .setHandler(futureEvent -> {
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
                                String eventHash = CalendarHelper.hash(event.getString("course_id")
                                        + event.getString("course_start_date")
                                        + event.getString("course_end_date"));
                                if (eventList.containsKey(eventHash)) {
                                    event.remove("course_id");
                                    event.remove("course_start_date");
                                    event.remove("course_end_date");
                                    eventList.get(eventHash).entrySet().iterator().next().getValue().getEvents().add(event);
                                }
                            }

                            for (Course course : courses) {
                                String subjectId = course.getSubjectId();
                                if (!course.getExceptionnal().isEmpty()) {
                                    course.setSubjectName(course.getExceptionnal());
                                } else {
                                    course.setSubjectName(subjectMap.containsKey(subjectId)
                                            ? subjectMap.getJsonObject(subjectId).getString("externalId")
                                            : "");
                                }
                                CalendarHelper.formatCourse(course);
                                String courseHash = CalendarHelper.hash(course.getId() + course.getStartDate() + course.getEndDate());
                                eventList.forEach((key, map) -> {
                                    if (map.containsKey(courseHash)) {
                                        course.setEvents(map.get(courseHash).getEvents());
                                    }
                                });
                                if (exemptionsMap.containsKey(subjectId)) {
                                    JsonObject exemption = CalendarHelper.exempted(course, exemptionsMap.getJsonArray(subjectId));
                                    if (exemption != null) {
                                        course.setExempted(true);
                                        course.setExemption(new JsonObject()
                                                .put("start_date", exemption.getString("start_date"))
                                                .put("end_date", exemption.getString("end_date"))
                                                .put("attendance", exemption.getBoolean("attendance")));
                                    } else {
                                        course.setExempted(false);
                                    }
                                }

                                JsonObject incident = CalendarHelper.incident(course, incidents);
                                if (incident != null) {
                                    course.setIncident(new JsonObject().put("description", incident.getString("description")).put("date", incident.getString("date")));
                                }
                            }

                            JsonArray absencesCourses = new JsonArray();
                            if (absents != null) {
                                for (int i = 0; i < absents.size(); i++) {
                                    if (CalendarHelper.calendarMatchDate(absents.getJsonObject(i), params.get("start"), params.get("end"))) {
                                        absencesCourses.addAll(CalendarHelper.addAbsencesCourses(absents.getJsonObject(i), params));
                                    }
                                }
                            }
                            JsonArray eventsCalendar = new JsonArray().addAll(new JsonArray(courses)).addAll(absencesCourses);
                            renderJson(request, eventsCalendar);
                        });

                getSubjects(subjects, subjectsFuture);
                getEvents(params.get("structure"), params.get("start") + " 00:00:00", params.get("end") + " 23:59:59", eventTypes, Arrays.asList(params.get("user")), eventsFuture);
                getExemptions(params.get("structure"), params.get("start") + " 00:00:00", params.get("end") + " 23:59:59", params.get("user"), exemptionsFuture);
                getIncidents(params.get("structure"), params.get("start") + " 00:00:00", params.get("end") + " 23:59:59", params.get("user"), incidentsFuture);
                getAbsences(params.get("start"), params.get("end"), params.get("user"), absentFuture);
            });
            courseHelper.getCoursesList(structure, params.getAll("teacher"), groups,
                    params.get("start"), params.get("end"), coursesFuture);
            Viescolaire.getInstance().getDefaultSlots(structure, FutureHelper.handlerJsonArray(slotsFuture));
        });
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