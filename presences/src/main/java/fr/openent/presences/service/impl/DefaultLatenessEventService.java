package fr.openent.presences.service.impl;

import fr.openent.presences.Presences;
import fr.openent.presences.common.helper.DateHelper;
import fr.openent.presences.common.helper.FutureHelper;
import fr.openent.presences.common.service.GroupService;
import fr.openent.presences.common.service.impl.DefaultGroupService;
import fr.openent.presences.helper.CourseHelper;
import fr.openent.presences.model.Event.EventBody;
import fr.openent.presences.service.EventService;
import fr.openent.presences.service.LatenessEventService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DefaultLatenessEventService implements LatenessEventService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultLatenessEventService.class);
    private final EventService eventService;
    private final GroupService groupService;
    private final CourseHelper courseHelper;

    public DefaultLatenessEventService(EventBus eb) {

        eventService = new DefaultEventService(eb);
        groupService = new DefaultGroupService(eb);
        courseHelper = new CourseHelper(eb);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void create(EventBody eventBody, UserInfos userInfos, String structureId, Handler<Either<String, JsonObject>> handler) {

        if (eventBody.getRegisterId() != -1) return;

        JsonObject registerProcess = new JsonObject()
                .put("body", eventBody.toJSON().put("structure_id", structureId))
                .put("res", new JsonObject());


        getStudentGroups(registerProcess)
                .compose(this::getCourseIdsFromGroups)
                .compose((this::getRegisterIdFromCourseIds))
                .setHandler(ar -> {
                    if (ar.failed()) {
                        LOGGER.error("[Presences@LatenessEventService::create] Failed to fetch register id", ar.cause());
                        handler.handle(new Either.Left<>("Failed to fetch register id"));
                    } else {
                        if (ar.result().isEmpty()) {
                            handler.handle(new Either.Left<>("No register id(s) fetched, sending error"));
                        } else {
                            List<Integer> registerIds = ((List<JsonObject>) ar.result().getList())
                                    .stream()
                                    .filter(registerId -> registerId.getInteger("id") != -1)
                                    .map(registerId -> registerId.getInteger("id"))
                                    .collect(Collectors.toList());
                            if (registerIds.isEmpty()) {
                                handler.handle(new Either.Left<>("No register id(s) fetched, sending error"));
                            } else {
                                createEventsFromRegisterIds(eventBody, userInfos, handler, registerIds);
                            }
                        }
                    }
                });
    }

    private Future<JsonObject> getStudentGroups(JsonObject registerProcess) {
        Future<JsonObject> future = Future.future();
        JsonObject body = registerProcess.getJsonObject("body");
        JsonObject res = registerProcess.getJsonObject("res");

        List<String> studentId = new ArrayList<>();
        studentId.add(body.getString("student_id"));


        groupService.getUserGroups(studentId, body.getString("structure_id"), groups -> {
            if (groups.isLeft() || groups.right().getValue().isEmpty()) {
                LOGGER.error("[Presences@LatenessEventService::getStudentGroups] Failed to fetch student groups",
                        groups.left().getValue());
                future.fail(groups.left().getValue());
            } else {
                JsonArray groupNames = new JsonArray();

                for (int i = 0; i < groups.right().getValue().size(); i++) {
                    groupNames.add(groups.right().getValue().getJsonObject(i).getString("name"));
                }

                res.put("groupNames", groupNames);

                future.complete(registerProcess);
            }
        });

        return future;
    }

    @SuppressWarnings("unchecked")
    private Future<JsonObject> getCourseIdsFromGroups(JsonObject registerProcess) {
        Future<JsonObject> future = Future.future();
        JsonObject body = registerProcess.getJsonObject("body");
        JsonObject res = registerProcess.getJsonObject("res");

        String startDate = DateHelper.getDateString(body.getString("start_date"), DateHelper.YEAR_MONTH_DAY);
        String startTime = DateHelper.fetchTimeString(body.getString("start_date"), DateHelper.MONGO_FORMAT);

        courseHelper.getCourses(body.getString("structure_id"), new ArrayList<>(),
                (List<String>) res.getJsonArray("groupNames").getList(), startDate, startDate, startTime, startTime,
                "true", courses -> {

                    if (courses.isLeft()) {
                        LOGGER.error("[Presences@LatenessEventService::getCourseIdsFromGroups] " +
                                "Failed to fetch course ids", courses.left().getValue());
                        future.fail(courses.left().getValue());
                    } else if (courses.right().getValue().isEmpty()) {
                        future.fail("[Presences@LatenessEventService::getCourseIdsFromGroups] " +
                                "Student group has no courses");
                    } else {
                        JsonArray courseIds = new JsonArray();

                        for (int i = 0; i < courses.right().getValue().size(); i++) {
                            courseIds.add(courses.right().getValue().getJsonObject(i).getString("_id"));
                        }

                        res.put("courseIds", courseIds);
                        future.complete(registerProcess);
                    }
                });

        return future;
    }

    private Future<JsonArray> getRegisterIdFromCourseIds(JsonObject registerProcess) {

        Future<JsonArray> future = Future.future();
        JsonObject body = registerProcess.getJsonObject("body");
        JsonObject res = registerProcess.getJsonObject("res");

        String query = "SELECT r.id FROM " + Presences.dbSchema + ".register r WHERE r.course_id IN " +
                Sql.listPrepared(res.getJsonArray("courseIds")) + " AND r.structure_id = ? AND r.start_date = ?";


        JsonArray params = new JsonArray()
                .addAll(res.getJsonArray("courseIds"))
                .add(body.getString("structure_id"))
                .add(body.getString("start_date"));

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(either -> {
            if (either.isLeft()) {
                LOGGER.error("[Presences@LatenessEventService::getRegisterIdFromCourseIds] " +
                        "Failed to fetch register ids", either.left().getValue());
                future.fail(either.left().getValue());
            } else future.complete(either.right().getValue());
        }));
        return future;
    }


    private void createEventsFromRegisterIds(EventBody eventBody, UserInfos userInfos, Handler<Either<String, JsonObject>> handler, List<Integer> registerIds) {

        List<Future<JsonObject>> futures = new ArrayList<>();

        for (Integer id : registerIds) {

            Future<JsonObject> eventFuture = Future.future();
            futures.add(eventFuture);

            eventBody.setRegisterId(id);
            eventService.create(eventBody.toJSON(), userInfos, FutureHelper.handlerJsonObject(eventFuture));
        }

        FutureHelper.all(futures).setHandler(compositeEvent -> {
            if (compositeEvent.failed()) {
                LOGGER.error("[Presences@DefaultLatenessEventService::createEventsFromRegisterIds] " +
                        "Failed to create lateness event ", compositeEvent.cause());
                handler.handle(new Either.Left<>(compositeEvent.cause().toString()));
            } else {
                JsonArray res = new JsonArray();
                for (int i = 0; i < compositeEvent.result().size(); i++) {
                    res.add((JsonObject) compositeEvent.result().resultAt(i));
                }
                handler.handle(new Either.Right<>(new JsonObject().put("events", res)));
            }
        });
    }

    @Override
    public void update(Integer eventId, EventBody eventBody, Handler<Either<String, JsonObject>> handler) {
        eventService.updateEvent(eventId, eventBody.toJSON(), event -> {
            if (event.isLeft()) {
                LOGGER.error("[Presences@DefaultLatenessEventService::update] Failed to update lateness event ", event.left().getValue());
                handler.handle(new Either.Left<>(event.left().getValue()));
            } else {
                handler.handle(new Either.Right<>(event.right().getValue()));
            }
        });
    }

}
