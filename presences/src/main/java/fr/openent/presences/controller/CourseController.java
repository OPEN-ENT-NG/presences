package fr.openent.presences.controller;

import fr.openent.presences.Presences;
import fr.openent.presences.common.helper.DateHelper;
import fr.openent.presences.common.helper.FutureHelper;
import fr.openent.presences.common.service.GroupService;
import fr.openent.presences.common.service.impl.DefaultGroupService;
import fr.openent.presences.constants.Actions;
import fr.openent.presences.enums.RegisterStatus;
import fr.openent.presences.export.RegisterCSVExport;
import fr.openent.presences.helper.CourseHelper;
import fr.openent.presences.helper.MapHelper;
import fr.openent.presences.helper.SquashHelper;
import fr.openent.presences.helper.SubjectHelper;
import fr.openent.presences.service.CourseService;
import fr.openent.presences.service.RegisterService;
import fr.openent.presences.service.impl.DefaultCourseService;
import fr.openent.presences.service.impl.DefaultRegisterService;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.Trace;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;
import org.entcore.common.user.UserInfos;
import org.entcore.common.user.UserUtils;

import java.text.ParseException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;
import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;

public class CourseController extends ControllerHelper {

    private EventBus eb;
    private RegisterService registerService;
    private GroupService groupService;
    private SubjectHelper subjectHelper;
    private CourseHelper courseHelper;
    private CourseService courseService = new DefaultCourseService();

    public CourseController(EventBus eb) {
        super();
        this.eb = eb;
        this.registerService = new DefaultRegisterService(eb);
        this.groupService = new DefaultGroupService(eb);
        this.courseHelper = new CourseHelper(eb);
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
        boolean forgottenFilter = params.contains("forgotten_registers") && Boolean.parseBoolean(request.getParam("forgotten_registers"));
        listCourses(params.get("structure"), params.getAll("teacher"), params.getAll("group"),
                params.get("start"), params.get("end"), forgottenFilter, arrayResponseHandler(request));
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

        boolean forgottenFilter = params.contains("forgotten_registers") && Boolean.parseBoolean(request.getParam("forgotten_registers"));
        listCourses(params.get("structure"), params.getAll("teacher"), params.getAll("group"), params.get("start"), params.get("end"), forgottenFilter, event -> {
            if (event.isLeft()) {
                log.error("[Presences@CourseController] Failed to list courses", event.left().getValue());
                renderError(request);
                return;
            }

            JsonArray courses = event.right().getValue();
            List<String> csvHeaders = Arrays.asList("presences.register.csv.header.date", "presences.register.csv.header.teacher",
                    "presences.register.csv.header.groups", "presences.register.csv.header.subject");
            RegisterCSVExport rce = new RegisterCSVExport(courses);
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
                            subjectName = subjects.getJsonObject(0).getString("externalId");
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

    /**
     * List courses
     *
     * @param structureId  Structure identifier
     * @param teachersList Teachers list identifiers
     * @param groupsList   Groups list identifiers
     * @param start        Start date
     * @param end          End date
     * @param handler      Function handler returning data
     */
    private void listCourses(String structureId, List<String> teachersList, List<String> groupsList, String start, String end, boolean forgottenFilter, Handler<Either<String, JsonArray>> handler) {
        courseHelper.getCourses(structureId, teachersList, groupsList, start, end, event -> {
            if (event.isLeft()) {
                handler.handle(new Either.Left<>(event.left().getValue()));
                return;
            }
            JsonArray courses = event.right().getValue();
            JsonArray subjectIds = new JsonArray();
            JsonArray teachersIds = new JsonArray();
            JsonObject course;
            for (int i = 0; i < courses.size(); i++) {
                course = courses.getJsonObject(i);
                if (!subjectIds.contains(course.getString("subjectId"))) {
                    subjectIds.add(course.getString("subjectId"));
                }

                JsonArray teachers = course.getJsonArray("teacherIds");
                for (int j = 0; j < teachers.size(); j++) {
                    if (!teachersIds.contains(teachers.getString(j))) {
                        teachersIds.add(teachers.getString(j));
                    }
                }
            }

            Future<JsonArray> subjectsFuture = Future.future();
            Future<JsonArray> teachersFuture = Future.future();

            CompositeFuture.all(subjectsFuture, teachersFuture).setHandler(asyncHandler -> {
                if (asyncHandler.failed()) {
                    handler.handle(new Either.Left<>(asyncHandler.cause().toString()));
                    return;
                }

                JsonArray subjects = subjectsFuture.result();
                JsonArray teachers = teachersFuture.result();
                JsonObject subjectMap = MapHelper.transformToMap(subjects, "id");
                JsonObject teacherMap = MapHelper.transformToMap(teachers, "id");
                JsonObject object;
                for (int i = 0; i < courses.size(); i++) {
                    object = courses.getJsonObject(i);
                    object.remove("startCourse");
                    object.remove("endCourse");
                    object.remove("is_periodic");
                    object.remove("is_recurrent");
                    object.put("subjectName", subjectMap.getJsonObject(object.getString("subjectId"), new JsonObject()).getString("externalId", object.getString("exceptionnal", "")));
                    JsonArray courseTeachers = new JsonArray();
                    JsonArray teacherIds = object.getJsonArray("teacherIds");
                    for (int j = 0; j < teacherIds.size(); j++) {
                        courseTeachers.add(teacherMap.getJsonObject(teacherIds.getString(j)));
                    }
                    object.put("teachers", courseTeachers);
                    object.remove("teacherIds");
                }

                SquashHelper squashHelper = new SquashHelper(eb);
                squashHelper.squash(structureId, start + " 00:00:00", end + " 23:59:59", courses, squashEvent -> {
                    handler.handle(new Either.Right<>(forgottenFilter ? filterForgottenCourses(squashEvent.right().getValue()) : squashEvent.right().getValue()));
                });
            });

            subjectHelper.getSubjects(subjectIds, FutureHelper.handlerJsonArray(subjectsFuture));
            getCourseTeachers(teachersIds, FutureHelper.handlerJsonArray(teachersFuture));
        });
    }

    private void getCourseTeachers(JsonArray teachers, Handler<Either<String, JsonArray>> handler) {
        String teacherQuery = "MATCH (u:User) WHERE u.id IN {teacherIds} RETURN u.id as id, (u.lastName + ' ' + u.firstName) as displayName";
        Neo4j.getInstance().execute(teacherQuery, new JsonObject().put("teacherIds", teachers), Neo4jResult.validResultHandler(handler));
    }

    private JsonArray filterForgottenCourses(JsonArray courses) {
        JsonArray forgottenRegisters = new JsonArray();
        for (int i = 0; i < courses.size(); i++) {
            try {
                //FIXME Fix timezone trick
                long timeDifference = ZoneId.of("Europe/Paris").getRules().getOffset(Instant.now()).getTotalSeconds();
                Date currentDate = new Date(System.currentTimeMillis() + (timeDifference * 1000));

                JsonObject course = courses.getJsonObject(i);
                Date forgottenStartDateCourse = new Date(DateHelper.parse(course.getString("startDate")).getTime() + (15 * 60000));
                if (currentDate.after(forgottenStartDateCourse)) {
                    if (!course.containsKey("register_id")) {
                        forgottenRegisters.add(course);
                        continue;
                    }
                    Integer registerState = course.getInteger("register_state_id");

                    if (!registerState.equals(RegisterStatus.DONE.getStatus())) {
                        forgottenRegisters.add(course);
                    }
                }
            } catch (ParseException e) {
                log.error("[Presences@CourseController] Failed to parse date", e);
            }
        }

        return forgottenRegisters;
    }
}
