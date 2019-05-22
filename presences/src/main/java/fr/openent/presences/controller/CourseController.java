package fr.openent.presences.controller;

import fr.openent.presences.common.helper.DateHelper;
import fr.openent.presences.common.helper.FutureHelper;
import fr.openent.presences.enums.RegisterStatus;
import fr.openent.presences.helper.SquashHelper;
import fr.openent.presences.service.GroupService;
import fr.openent.presences.service.RegisterService;
import fr.openent.presences.service.impl.DefaultGroupService;
import fr.openent.presences.service.impl.DefaultRegisterService;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import io.vertx.core.*;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;

import java.text.ParseException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;

public class CourseController extends ControllerHelper {

    private EventBus eb;
    private RegisterService registerService;
    private GroupService groupService;

    public CourseController(EventBus eb) {
        super();
        this.eb = eb;
        this.registerService = new DefaultRegisterService(eb);
        this.groupService = new DefaultGroupService(eb);
    }

    @Get("/courses")
    @ApiDoc("Get courses")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getCourses(HttpServerRequest request) {
        MultiMap params = request.params();
        if (!params.contains("structure")
                || !params.contains("start") || !params.contains("end")
                || !params.get("start").matches("\\d{4}-\\d{2}-\\d{2}")
                || !params.get("end").matches("\\d{4}-\\d{2}-\\d{2}")) {
            badRequest(request);
            return;
        }
        boolean forgottenFilter = request.params().contains("forgotten_registers") ? Boolean.parseBoolean(request.getParam("forgotten_registers")) : false;

        getCourses(params, event -> {
            if (event.isLeft()) {
                renderError(request, new JsonObject().put("error", event.left().getValue()));
            } else {
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
                        renderError(request);
                        return;
                    }

                    JsonArray subjects = subjectsFuture.result();
                    JsonArray teachers = teachersFuture.result();
                    JsonObject subjectMap = transformToMap(subjects, "id");
                    JsonObject teacherMap = transformToMap(teachers, "id");
                    JsonObject object;
                    for (int i = 0; i < courses.size(); i++) {
                        object = courses.getJsonObject(i);
                        object.remove("startCourse");
                        object.remove("endCourse");
                        object.remove("is_periodic");
                        object.remove("is_recurrent");
                        object.put("subjectName", subjectMap.getJsonObject(object.getString("subjectId")).getString("externalId"));
                        JsonArray courseTeachers = new JsonArray();
                        JsonArray teacherIds = object.getJsonArray("teacherIds");
                        for (int j = 0; j < teacherIds.size(); j++) {
                            courseTeachers.add(teacherMap.getJsonObject(teacherIds.getString(j)));
                        }
                        object.put("teachers", courseTeachers);
                        object.remove("teacherIds");
                    }

                    SquashHelper squashHelper = new SquashHelper(eb);
                    squashHelper.squash(params.get("structure"), params.get("start") + " 00:00:00", params.get("end") + " 23:59:59", courses, squashEvent -> {
                        renderJson(request, forgottenFilter ? filterForgottenCourses(squashEvent.right().getValue()) : squashEvent.right().getValue());
                    });
                });

                getSubjects(subjectIds, FutureHelper.handlerJsonArray(subjectsFuture));
                getCourseTeachers(teachersIds, FutureHelper.handlerJsonArray(teachersFuture));
            }
        });
    }

    private void getCourseTeachers(JsonArray teachers, Handler<Either<String, JsonArray>> handler) {
        String teacherQuery = "MATCH (u:User) WHERE u.id IN {teacherIds} RETURN u.id as id, u.displayName as displayName";
        Neo4j.getInstance().execute(teacherQuery, new JsonObject().put("teacherIds", teachers), Neo4jResult.validResultHandler(handler));
    }

    private JsonArray filterForgottenCourses(JsonArray courses) {
        JsonArray forgottenRegisters = new JsonArray();
        for (int i = 0; i < courses.size(); i++) {
            try {
                //TODO Fix timezone trick
                long timeDifference = ZoneId.of("Europe/Paris").getRules().getOffset(Instant.now()).getTotalSeconds();
                Date forgottenBeforeThatDate = new Date(System.currentTimeMillis() + (15 * 60000) + (timeDifference * 1000));

                JsonObject course = courses.getJsonObject(i);
                if (forgottenBeforeThatDate.after(DateHelper.parse(course.getString("startDate")))) {
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

    private JsonObject transformToMap(JsonArray objects, String key) {
        JsonObject object;
        JsonObject map = new JsonObject();
        for (int i = 0; i < objects.size(); i++) {
            object = objects.getJsonObject(i);
            map.put(object.getString(key), object);
        }

        return map;
    }

    private void getCourses(MultiMap params, Handler<Either<String, JsonArray>> handler) {
        JsonObject action = new JsonObject()
                .put("action", "course.getCoursesOccurences")
                .put("structureId", params.get("structure"))
                .put("teacherId", new JsonArray(params.getAll("teacher")))
                .put("group", new JsonArray(params.getAll("group")))
                .put("begin", params.get("start"))
                .put("end", params.get("end"));

        eb.send("viescolaire", action, getEvtBusHandler("Failed to recover courses", handler));
    }

    private void getSubjects(JsonArray subjects, Handler<Either<String, JsonArray>> handler) {
        JsonObject action = new JsonObject()
                .put("action", "matiere.getMatieres")
                .put("idMatieres", subjects);

        eb.send("viescolaire", action, getEvtBusHandler("Failed to recover subjects", handler));
    }

    private Handler<AsyncResult<Message<JsonObject>>> getEvtBusHandler(String errorMessage, Handler<Either<String, JsonArray>> handler) {
        return event -> {
            JsonObject body = event.result().body();
            if (event.failed() || "error".equals(body.getString("status"))) {
                log.error("[Presences@CourseController] " + errorMessage);
                handler.handle(new Either.Left<>(errorMessage));
            } else {
                handler.handle(new Either.Right<>(body.getJsonArray("results")));
            }
        };
    }
}
