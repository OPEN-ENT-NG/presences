package fr.openent.presences.controller;

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
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;

import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;

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
        if (!params.contains("teacher") || !params.contains("structure")
                || !params.contains("start") || !params.contains("end")
                || !params.get("start").matches("\\d{4}-\\d{2}-\\d{2}")
                || !params.get("end").matches("\\d{4}-\\d{2}-\\d{2}")) {
            badRequest(request);
            return;
        }

        getCourses(params, event -> {
            if (event.isLeft()) {
                renderError(request, new JsonObject().put("error", event.left().getValue()));
            } else {
                JsonArray courses = event.right().getValue();
                JsonArray subjectIds = new JsonArray();
                JsonObject course;
                for (int i = 0; i < courses.size(); i++) {
                    course = courses.getJsonObject(i);
                    if (!subjectIds.contains(course.getString("subjectId"))) {
                        subjectIds.add(course.getString("subjectId"));
                    }
                }

                getSubjects(subjectIds, subjectEvent -> {
                    if (event.isLeft()) {
                        renderError(request);
                        return;
                    }

                    JsonArray subjects = subjectEvent.right().getValue();
                    JsonObject subjectMap = transformSubjectsToMap(subjects);
                    JsonObject object;
                    for (int i = 0; i < courses.size(); i++) {
                        object = courses.getJsonObject(i);
                        object.remove("startCourse");
                        object.remove("endCourse");
                        object.remove("is_periodic");
                        object.remove("is_recurrent");
                        object.put("subjectName", subjectMap.getJsonObject(object.getString("subjectId")).getString("externalId"));

                    }

                    SquashHelper squashHelper = new SquashHelper(eb);
                    squashHelper.squash(params.get("structure"), params.get("start") + " 00:00:00", params.get("end") + " 23:59:59", courses, arrayResponseHandler(request));
                });
            }
        });
    }

    private JsonObject transformSubjectsToMap(JsonArray subjects) {
        JsonObject subject;
        JsonObject map = new JsonObject();
        for (int i = 0; i < subjects.size(); i++) {
            subject = subjects.getJsonObject(i);
            map.put(subject.getString("id"), subject);
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
