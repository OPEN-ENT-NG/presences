package fr.openent.presences.controller;

import fr.openent.presences.Presences;
import fr.openent.presences.common.helper.DateHelper;
import fr.openent.presences.constants.Actions;
import fr.openent.presences.export.RegisterCSVExport;
import fr.openent.presences.helper.CourseHelper;
import fr.openent.presences.helper.SubjectHelper;
import fr.openent.presences.model.Course;
import fr.openent.presences.service.CourseService;
import fr.openent.presences.service.RegisterService;
import fr.openent.presences.service.impl.DefaultCourseService;
import fr.openent.presences.service.impl.DefaultRegisterService;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.MultiMap;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.Trace;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;

public class CourseController extends ControllerHelper {

    private EventBus eb;
    private RegisterService registerService;
    private SubjectHelper subjectHelper;
    private CourseHelper courseHelper;
    private CourseService courseService;

    public CourseController(EventBus eb) {
        super();
        this.eb = eb;
        this.registerService = new DefaultRegisterService(eb);
        this.courseHelper = new CourseHelper(eb);
        this.courseService = new DefaultCourseService(eb);
        this.subjectHelper = new SubjectHelper(eb);
    }

    @Get("/courses")
    @ApiDoc("Get courses")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getCourses(HttpServerRequest request) {
        MultiMap params = request.params();
        if (!courseHelper.checkParams(params)) {
            badRequest(request);
            return;
        }
        String userDate = request.getParam("_t");
        boolean forgottenFilter = params.contains("forgotten_registers") && Boolean.parseBoolean(request.getParam("forgotten_registers"));
        boolean multipleSlot = params.contains("multiple_slot") && Boolean.parseBoolean(request.getParam("multiple_slot"));

        courseService.listCourses(params.get("structure"), params.getAll("teacher"), params.getAll("group"),
                params.get("start"), params.get("end"), null, null, forgottenFilter, multipleSlot, userDate,
                params.get("limit"), params.get("offset"), request.getParam("descendingDate"), event -> {
                    if (event.isLeft()) {
                        renderError(request);
                    } else {
                        List<Course> courses = event.right().getValue().getList();
                        // second ternary checks if we choose limit or our courses size
                        renderJson(request, new JsonArray(courses));
                    }
                });
    }

    @Get("/courses/export")
    @ApiDoc("Export courses")
    @SecuredAction(Presences.EXPORT)
    public void exportCourses(HttpServerRequest request) {
        MultiMap params = request.params();
        if (!courseHelper.checkParams(params)) {
            badRequest(request);
            return;
        }

        String userDate = request.getParam("_t");
        boolean forgottenFilter = params.contains("forgotten_registers") && Boolean.parseBoolean(request.getParam("forgotten_registers"));
        boolean multipleSlot = params.contains("multiple_slot") && Boolean.parseBoolean(request.getParam("multiple_slot"));
        courseService.listCourses(params.get("structure"), params.getAll("teacher"), params.getAll("group"),
                params.get("start"), params.get("end"), null, null, forgottenFilter, multipleSlot, userDate, event -> {
                    if (event.isLeft()) {
                        log.error("[Presences@CourseController] Failed to list courses", event.left().getValue());
                        renderError(request);
                        return;
                    }

                    JsonArray courses = event.right().getValue();
                    courses.getList().sort(Comparator.comparing(Course::getTimestamp));
                    Collections.reverse(courses.getList());
                    List<String> csvHeaders = Arrays.asList(
                            "presences.exemptions.dates",
                            "presences.hour",
                            "presences.register.csv.header.teacher",
                            "presences.register.csv.header.groups",
                            "presences.exemptions.csv.header.subject",
                            "presences.register.forgotten");
                    RegisterCSVExport rce = new RegisterCSVExport(courses, forgottenFilter);
                    rce.setRequest(request);
                    rce.setHeader(csvHeaders);
                    rce.export();
                });
    }

    @Post("/courses/:id/notify")
    @ApiDoc("Notify user that it does not made register in time")
    @SecuredAction(Presences.NOTIFY)
    @Trace(Actions.NOTIFY_REGISTER_TEACHER)
    public void notify(HttpServerRequest request) {
        RequestUtils.bodyToJson(request, requestBody -> {
            if (!requestBody.containsKey("start") || !requestBody.containsKey("end")) {
                badRequest(request);
                return;
            }

            String courseId = request.getParam("id");
            String start = requestBody.getString("start");
            String end = requestBody.getString("end");
            UserUtils.getUserInfos(this.eb, request, user -> {
                courseService.getCourse(courseId, either -> {
                    if (either.isLeft()) {
                        log.error("[Presences@CourseController] Failed to retrieve course", either.left().getValue());
                        return;
                    }

                    JsonObject course = either.right().getValue();
                    subjectHelper.getSubjects(new JsonArray().add(course.getString("subjectId")), event -> {
                        if (event.isLeft()) {
                            log.error("[Presences@CourseController] Failed to retrieve course subject");
                        }
                        JsonArray subjects = event.right().getValue();
                        String subjectName;
                        if (subjects.isEmpty()) {
                            subjectName = course.containsKey("exceptionnal") ? course.getString("exceptionnal") : course.getString("subjectId");
                        } else {
                            subjectName = subjects.getJsonObject(0).getString("name");
                        }
                        String startHour = DateHelper.getDateString(start, "kk'h'mm");
                        String endHour = DateHelper.getDateString(end, "kk'h'mm");
                        String subjectDate = DateHelper.getDateString(start, "dd/MM");
                        JsonArray teachers = course.getJsonArray("teacherIds");
                        String structureName = getStructureName(user, course.getString("structureId"));
                        String groups = getGroupsName(course.getJsonArray("classes"), course.getJsonArray("groups"));
                        String subject = I18n.getInstance().translate("presences.register.notify.subject", Renders.getHost(request), I18n.acceptLanguage(request), subjectDate);
                        String body = I18n.getInstance().translate("presences.register.notify.body", Renders.getHost(request), I18n.acceptLanguage(request), subjectName, groups, startHour, endHour, structureName);
                        JsonObject message = new JsonObject()
                                .put("subject", subject)
                                .put("body", body)
                                .put("to", teachers);

                        JsonObject action = new JsonObject()
                                .put("action", "send")
                                .put("userId", user.getUserId())
                                .put("username", user.getUsername())
                                .put("message", message);

                        eb.send("org.entcore.conversation", action, handlerToAsyncHandler(messageEvent -> {
                            if (!"ok".equals(messageEvent.body().getString("status"))) {
                                log.error("[Presences@CourseController] Failed to send message", messageEvent.body().getString("error"));
                                renderError(request);
                                return;
                            }

                            setRegisterNotified(course, start, end, user, request);
                        }));
                    });
                });
            });
        });
    }

    /**
     * Set given course as notified. First, it looks if given course is already registered. If not, it create an empty register
     *
     * @param course  Course that needs to be nset
     * @param start   Start occurrence course
     * @param end     End occurrence dourse
     * @param user    User that notify
     * @param request Server http request
     */
    private void setRegisterNotified(JsonObject course, String start, String end, UserInfos user, HttpServerRequest request) {
        registerService.exists(course.getString("_id"), start, end, existsEither -> {
            if (existsEither.isLeft()) {
                log.error("[Presences@CourseController] Failed to retrieve course register");
                renderError(request);
                return;
            }
            JsonObject res = existsEither.right().getValue();
            if (res.containsKey("exists") && res.getBoolean("exists")) {
                registerService.setNotified(res.getLong("id"), defaultResponseHandler(request));
            } else {
                JsonObject register = new JsonObject()
                        .put("course_id", course.getString("_id"))
                        .put("structure_id", course.getString("structureId"))
                        .put("start_date", start)
                        .put("end_date", end)
                        .put("subject_id", course.getString("subjectId"))
                        .put("classes", course.getJsonArray("classes"))
                        .put("groups", course.getJsonArray("groups"));
                registerService.create(register, user, either -> {
                    if (either.isLeft()) {
                        log.error("[Presences@CourseController] Failed to create register before notify");
                        renderError(request);
                    } else {
                        Long newRegisterId = either.right().getValue().getLong("id");
                        registerService.setNotified(newRegisterId, notifyEither -> {
                            if (notifyEither.isLeft()) {
                                log.error("[Presences@CourseController] Failed to notify given course");
                                renderError(request, new JsonObject().put("register_id", newRegisterId));
                            } else {
                                renderJson(request, new JsonObject().put("register_id", newRegisterId));
                            }
                        });
                    }
                });
            }
        });
    }

    private String getStructureName(UserInfos user, String structureId) {
        List<String> structures = user.getStructures();
        List<String> names = user.getStructureNames();
        for (int i = 0; i < structures.size(); i++) {
            if (structures.get(i).equals(structureId)) {
                return names.get(i);
            }
        }

        return "";
    }

    private String getGroupsName(JsonArray classes, JsonArray groups) {
        StringBuilder names = new StringBuilder();
        for (int i = 0; i < classes.size(); i++) {
            names.append(classes.getString(i))
                    .append("-");
        }

        for (int i = 0; i < groups.size(); i++) {
            names.append(groups.getString(i))
                    .append("-");
        }

        return names.toString().substring(0, names.length() - "-".length());
    }
}
