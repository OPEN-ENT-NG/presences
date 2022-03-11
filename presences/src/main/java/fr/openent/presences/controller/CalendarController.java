package fr.openent.presences.controller;

import fr.openent.presences.*;
import fr.openent.presences.common.helper.FutureHelper;
import fr.openent.presences.common.service.GroupService;
import fr.openent.presences.common.service.impl.DefaultGroupService;
import fr.openent.presences.common.viescolaire.Viescolaire;
import fr.openent.presences.helper.*;
import fr.openent.presences.model.Course;
import fr.openent.presences.model.Exemption.ExemptionView;
import fr.openent.presences.model.Slot;
import fr.openent.presences.security.*;
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
import java.util.stream.Collectors;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;

public class CalendarController extends ControllerHelper {

    private final EventBus eb;
    private final AbsenceService absenceService;
    private final CourseHelper courseHelper;
    private final EventService eventService;
    private final GroupService groupService;
    private final ExemptionService exemptionService;

    public CalendarController(EventBus eb) {
        super();
        this.eb = eb;
        this.absenceService = new DefaultAbsenceService(eb);
        this.courseHelper = new CourseHelper(eb);
        this.eventService = new DefaultEventService(eb);
        this.groupService = new DefaultGroupService(eb);
        this.exemptionService = new DefaultExemptionService(eb);
    }

    @Get("/calendar/courses")
    @ApiDoc("Retrieve all courses and events")
    @SecuredAction(Presences.CALENDAR_VIEW)
    @SuppressWarnings("unchecked")
    public void getCalendarCourses(HttpServerRequest request) {
        MultiMap params = request.params();
        if (!courseHelper.checkParams(params)) {
            badRequest(request);
            return;
        }

        List<String> users = params.getAll("user");
        String user = params.get("user");
        String structure = params.get("structure");
        String start = params.get("start");
        String end = params.get("end");
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
                Future<JsonArray> eventsFuture = Future.future();
                Future<JsonArray> exemptionsFuture = Future.future();
                Future<JsonArray> incidentsFuture = Future.future();
                Future<JsonArray> punishmentsFuture = Future.future();
                Future<JsonArray> absentFuture = Future.future();

                CompositeFuture.all(eventsFuture, exemptionsFuture, incidentsFuture, punishmentsFuture, absentFuture)
                        .setHandler(futureEvent -> {
                            if (futureEvent.failed()) {
                                log.error("[CalendarController@getCalendarCourses] Failed to retrieve information", futureEvent.cause());
                                renderError(request);
                                return;
                            }

                            JsonArray punishmentsResult = punishmentsFuture.result() != null ? punishmentsFuture.result() : new JsonArray();
                            List<JsonObject> punishments = (List<JsonObject>) ((List<JsonObject>) punishmentsResult.getList()).stream()
                                    .flatMap(punishment -> punishment.getJsonArray("punishments").getList().stream())
                                    .collect(Collectors.toList());

                            renderJson(request, formatCalendar(punishments, eventsFuture.result().getList(), eventList, courses,
                                    exemptionsFuture.result(), slots, incidentsFuture.result())
                            );
                        });

                String startTime = " 00:00:00";
                String endTime = " 23:59:59";
                getEvents(structure, start + startTime, end + endTime, eventTypes, users, eventsFuture);
                getExemptions(structure, start + startTime, end + endTime, user, exemptionsFuture);
                getIncidents(structure, start + startTime, end + endTime, user, incidentsFuture);
                getPunishments(structure, start + startTime, end + endTime, users, punishmentsFuture);
                getAbsences(start, end, user, absentFuture);
            });
            courseHelper.getCoursesList(structure, params.getAll("teacher"), groups,
                    start, end, coursesFuture);
            Viescolaire.getInstance().getDefaultSlots(structure, FutureHelper.handlerJsonArray(slotsFuture));
        });
    }

    private JsonArray formatCalendar(List<JsonObject> punishments, List<JsonObject> events, HashMap<String, Map<String, Course>> eventList,
                                     List<Course> courses, JsonArray exemptions, List<Slot> slots, JsonArray incidents) {
        for (JsonObject event : events) {
            groupHashedEvents(event, eventList);
        }

        for (Course course : courses) {
            formatCourse(course, eventList, exemptions, slots, incidents, punishments);
        }
        return new JsonArray().addAll(new JsonArray(courses));
    }

    private void groupHashedEvents(JsonObject event, HashMap<String, Map<String, Course>> eventList) {
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

    private void formatCourse(Course course, HashMap<String, Map<String, Course>> eventList, JsonArray exemptions,
                              List<Slot> slots, JsonArray incidents, List<JsonObject> punishments) {
        setCourseSubjectLabel(course);
        CalendarHelper.formatCourse(course);
        setEvents(course, eventList);
        course.setSplitCourses(CourseHelper.splitCoursesWithOneCourse(course, slots));
        setCourseExempted(course, exemptions);
        setIncident(course, incidents);
        course.setPunishments(new JsonArray(CalendarHelper.getMatchingPunishments(course, punishments)));
    }

    private void setCourseSubjectLabel(Course course) {
        if (!course.getExceptionnal().isEmpty()) course.setSubjectName(course.getExceptionnal());
        else course.setSubjectName(course.getSubject().getName());
    }

    private void setEvents(Course course, HashMap<String, Map<String, Course>> eventList) {
        String courseHash = CalendarHelper.hash(course.getId() + course.getStartDate() + course.getEndDate());
        eventList.forEach((key, map) -> {
            if (map.containsKey(courseHash)) {
                course.setEvents(map.get(courseHash).getEvents());
            }
        });
    }

    private void setCourseExempted(Course course, JsonArray exemptions) {
        JsonObject exemptionsMap = MapHelper.transformToMapMultiple(exemptions, "subject_id");
        List<ExemptionView> exemptionView = ExemptionHelper.getExemptionListFromJsonArray(exemptions);
        String subjectId = course.getSubjectId();

        if (exemptionsMap.containsKey(subjectId) && !course.getSubjectId().isEmpty()) {
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
        } else {
            course.setExempted(CalendarHelper.isExemptionRecursiveExempted(course, exemptionView));
        }
    }

    private void setIncident(Course course, JsonArray incidents) {
        JsonObject incident = CalendarHelper.incident(course, incidents);
        if (incident != null) {
            course.setIncident(new JsonObject().put("description", incident.getString("description")).put("date", incident.getString("date")));
        }
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

    private void getPunishments(String structureId, String startDate, String endDate, List<String> students, Future<JsonArray> future) {
        JsonObject action = new JsonObject()
                .put("action", "get-punishment-by-student")
                .put("structure", structureId)
                .put("students", students)
                .put("start_at", startDate)
                .put("end_at", endDate);

        eb.send("fr.openent.incidents", action, handlerToAsyncHandler(event -> {
            if (!"ok".equals(event.body().getString("status"))) {
                log.error("[Presences@CalendarController] Failed to retrieve punishments", event.body().getString("error"));
                future.fail(event.body().getString("error"));
                return;
            }

            future.complete(event.body().getJsonArray("result"));
        }));
    }

    private void getExemptions(String structureId, String startDate, String endDate, String users, Future<JsonArray> future) {
        exemptionService.get(structureId, startDate, endDate, users, null, FutureHelper.handlerJsonArray(future));
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
    @ResourceFilter(CalendarViewRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void getGroupStudents(HttpServerRequest request) {
        String groupIdentifier = request.getParam("id");
        groupService.getGroupStudents(groupIdentifier, arrayResponseHandler(request));
    }

}