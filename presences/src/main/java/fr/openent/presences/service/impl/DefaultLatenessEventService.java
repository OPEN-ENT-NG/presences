package fr.openent.presences.service.impl;

import fr.openent.presences.Presences;
import fr.openent.presences.common.helper.DateHelper;
import fr.openent.presences.common.helper.FutureHelper;
import fr.openent.presences.common.service.GroupService;
import fr.openent.presences.common.service.impl.DefaultGroupService;
import fr.openent.presences.common.viescolaire.*;
import fr.openent.presences.core.constants.*;
import fr.openent.presences.db.*;
import fr.openent.presences.helper.*;
import fr.openent.presences.model.*;
import fr.openent.presences.model.Event.EventBody;
import fr.openent.presences.service.*;
import fr.wseduc.webutils.Either;
import io.vertx.core.*;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;

import java.text.*;
import java.util.*;
import java.util.stream.Collectors;

public class DefaultLatenessEventService extends DBService implements LatenessEventService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultLatenessEventService.class);
    private final EventService eventService;
    private final GroupService groupService;
    private final RegisterService registerService;
    private final SettingsService settingsService;
    private final CourseHelper courseHelper;

    public DefaultLatenessEventService(EventBus eb) {

        eventService = new DefaultEventService(eb);
        groupService = new DefaultGroupService(eb);
        registerService = new DefaultRegisterService(eb);
        settingsService = new DefaultSettingsService();
        courseHelper = new CourseHelper(eb);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void create(EventBody eventBody, UserInfos userInfos, String structureId, Handler<Either<String, JsonObject>> handler) {

        if (eventBody.getRegisterId() != -1) return;

        JsonObject registerProcess = new JsonObject()
                .put("body", eventBody.toJSON().put(Field.STRUCTURE_ID, structureId))
                .put("res", new JsonObject());

        getStudentGroups(registerProcess)
                .compose(this::getCourseIdsFromGroups)
                .compose(process -> getRegisterIdFromCourses(process, userInfos))
                .onFailure(fail -> {
                    String message = String.format("[Presences@%s::create] Failed to fetch register id : %s",
                            this.getClass().getSimpleName(), fail.getMessage());
                    LOGGER.error(message, fail.getMessage());
                    handler.handle(new Either.Left<>(message));
                })
                .onSuccess(ar -> {
                    String message = String.format("[Presences@%s::create] No register id(s) fetched, sending error",
                            this.getClass().getSimpleName());
                    if (ar.isEmpty()) {
                        handler.handle(new Either.Left<>(message));
                    } else {
                        List<Integer> registerIds = ((List<JsonObject>) ar.getList())
                                .stream()
                                .filter(registerId -> registerId.getInteger(Field.ID) != -1)
                                .map(registerId -> registerId.getInteger(Field.ID))
                                .collect(Collectors.toList());
                        if (registerIds.isEmpty()) {
                            handler.handle(new Either.Left<>(message));
                        } else {
                            createEventsFromRegisterIds(eventBody, userInfos, handler, registerIds);
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
        Promise<JsonObject> promise = Promise.promise();
        JsonObject body = registerProcess.getJsonObject("body");
        JsonObject res = registerProcess.getJsonObject("res");

        String endDate = DateHelper.getDateString(body.getString(Field.END_DATE), DateHelper.YEAR_MONTH_DAY);
        String endTime = DateHelper.fetchTimeString(body.getString(Field.END_DATE), DateHelper.MONGO_FORMAT);

        courseHelper.getCourses(body.getString(Field.STRUCTURE_ID), new ArrayList<>(),
                res.getJsonArray(Field.GROUPNAMES).getList(), endDate, endDate, endTime, endTime,
                "true", courses -> {

                    if (courses.isLeft()) {
                        String message = String.format("[Presences@%s::getCourseIdsFromGroups] " +
                                "Failed to fetch course ids : %s", this.getClass().getSimpleName(),
                                courses.left().getValue());
                        LOGGER.error(message, courses.left().getValue());
                        promise.fail(courses.left().getValue());
                    } else if (courses.right().getValue().isEmpty()) {
                        promise.fail(String.format("[Presences@%s::getCourseIdsFromGroups] " +
                                "Student group has no courses", this.getClass().getSimpleName()));
                    } else {
                        res.put(Field.COURSES, courses.right().getValue());
                        promise.complete(registerProcess);
                    }
                });

        return promise.future();
    }

    @SuppressWarnings("unchecked")
    private Future<JsonArray> getRegisterIdFromCourses(JsonObject registerProcess, UserInfos userInfos) {
        Promise<JsonArray> promise = Promise.promise();
        JsonObject body = registerProcess.getJsonObject("body");
        JsonObject res = registerProcess.getJsonObject("res");

        JsonArray courses = res.getJsonArray(Field.COURSES);

        JsonArray courseIds = new JsonArray(
                courses.stream()
                        .map(course -> ((JsonObject) course).getString(Field._ID))
                        .collect(Collectors.toList()));

        String query = "SELECT r.id FROM " + Presences.dbSchema + ".register r WHERE r.course_id IN " +
                Sql.listPrepared(courseIds) + " AND r.structure_id = ? AND r.start_date > ?" +
                " AND r.start_date < ?";

        JsonArray params = new JsonArray()
                .addAll(courseIds)
                .add(body.getString(Field.STRUCTURE_ID));

        try {
            params.add(DateHelper.getDateString(DateHelper.add(DateHelper.parse(body.getString(Field.START_DATE),
                            DateHelper.MONGO_FORMAT),
                    Calendar.MINUTE, -15), DateHelper.MONGO_FORMAT));
            params.add(DateHelper.getDateString(DateHelper.add(DateHelper.parse(body.getString(Field.START_DATE),
                            DateHelper.MONGO_FORMAT),
                    Calendar.MINUTE, 15), DateHelper.MONGO_FORMAT));
        } catch (ParseException e) {
            promise.fail(e.getMessage());
        }

        sql.prepared(query, params, SqlResult.validResultHandler(event -> {
            if (event.isLeft()) {
                promise.fail(event.left().getValue());
            } else {
                JsonArray registerIds = event.right().getValue();

                if (registerIds.isEmpty()) {

                    fetchCoursesWithMultipleSlotSetting(body.getString(Field.STRUCTURE_ID), courses)
                            .onFailure(fail -> promise.fail(fail.getMessage()))
                            .onSuccess(coursesArray -> {
                                Promise<JsonArray> init = Promise.promise();
                                Future<JsonArray> createFuture = init.future();

                                List<JsonObject> coursesList = coursesArray.getList();

                                for (JsonObject course : coursesList) {
                                    createFuture = createFuture.compose(r ->
                                            addCourseRegister(body.getString(Field.STRUCTURE_ID),
                                                    body.getString(Field.END_DATE), course, r, userInfos));
                                }

                                createFuture
                                        .onFailure(fail -> promise.fail(fail.getMessage()))
                                        .onSuccess(promise::complete);

                                init.complete();
                            });
                } else {
                    promise.complete(registerIds);
                }
            }
        }));

        return promise.future();
    }


    private Future<JsonArray> fetchCoursesWithMultipleSlotSetting(String structureId, JsonArray courses) {
        Promise<JsonArray> promise = Promise.promise();

        settingsService.retrieveMultipleSlots(structureId)
                .onFailure(fail -> promise.fail(fail.getMessage()))
                .onSuccess(ar -> {
                    boolean multipleSlot = ar.getBoolean(Field.ALLOW_MULTIPLE_SLOTS, true);

                    Viescolaire.getInstance().getSlotsFromProfile(structureId, event -> {
                        if (event.isLeft()) {
                            promise.fail(event.left().getValue());
                        } else {
                            List<Slot> slotsList = SlotHelper.getSlotListFromJsonArray(event.right().getValue(),
                                    Slot.MANDATORY_ATTRIBUTE);
                            List<Course> coursesEvent = CourseHelper.getCourseListFromJsonArray(courses,
                                    Course.MANDATORY_ATTRIBUTE);
                            JsonArray splitCourses = new JsonArray(CourseHelper.splitCoursesFromSlot(coursesEvent,
                                    slotsList).stream().map(Course::toJSON).collect(Collectors.toList()));

                            promise.complete(multipleSlot ? splitCourses : courses);
                        }
                    });
                });

        return promise.future();
    }

    /**
     * Create course registers and return register id corresponding to lateness hour
     * @param structureId   structure identifier
     * @param endDate       lateness time
     * @param course        course object
     * @param registers     list of created registers
     * @param userInfos     user infos
     * @return {@link Future} array of created registers
     */
    private Future<JsonArray> addCourseRegister(String structureId, String endDate,
                                                JsonObject course, JsonArray registers, UserInfos userInfos) {
        Promise<JsonArray> promise = Promise.promise();

        JsonObject register = new JsonObject()
                .put(Field.STRUCTURE_ID, structureId)
                .put(Field.COURSE_ID, course.getString(Field._ID))
                .put(Field.START_DATE, course.getString(Field.STARTDATE))
                .put(Field.END_DATE, course.getString(Field.ENDDATE))
                .put(Field.SUBJECT_ID, course.getString(Field.SUBJECTID))
                .put(Field.SPLIT_SLOT, course.getBoolean(Field.SPLIT_SLOT, true))
                .put(Field.GROUPS, course.getJsonArray(Field.GROUPS, new JsonArray()))
                .put(Field.CLASSES, course.getJsonArray(Field.CLASSES, new JsonArray()));

        registerService.create(register, userInfos, createEvt -> {
            if (createEvt.isLeft()) {
                promise.fail(createEvt.left().getValue());
            } else {
                JsonObject createdRegister = createEvt.right().getValue();
                JsonArray createdRegisters = (registers != null) ? registers : new JsonArray();

                promise.complete(DateHelper.isDateBeforeOrEqual(createdRegister.getString(Field.START_DATE), endDate)
                        && DateHelper.isDateBeforeOrEqual(endDate, createdRegister.getString(Field.END_DATE))
                        ? createdRegisters.add(createdRegister)
                        : createdRegisters);
            }
        });

        return  promise.future();
    }

    private void createEventsFromRegisterIds(EventBody eventBody, UserInfos userInfos, Handler<Either<String, JsonObject>> handler, List<Integer> registerIds) {

        Promise<JsonArray> init = Promise.promise();
        Future<JsonArray> createFuture = init.future();

        for (Integer id : registerIds) {
            createFuture = createFuture.compose(eventList -> {
                eventBody.setRegisterId(id);
                return createAndAddEvent(eventList, eventBody.toJSON(), userInfos);
            });
        }

        createFuture.onFailure(fail -> {
                    LOGGER.error(String.format("[Presences@%s::createEventsFromRegisterIds] " +
                            "Failed to create lateness event : %s", this.getClass().getSimpleName(), fail.getMessage()),
                            fail.getMessage());
                    handler.handle(new Either.Left<>(fail.getMessage()));
                })
                .onSuccess(ar ->
                    handler.handle(new Either.Right<>(new JsonObject().put("events", ar))));

        init.complete();
    }

    private Future<JsonArray> createAndAddEvent(JsonArray eventList, JsonObject event, UserInfos user) {
        Promise<JsonArray> promise = Promise.promise();
        eventService.create(event, user, res -> {
           if (res.isLeft()) {
               promise.fail(res.left().getValue());
           } else {
               promise.complete((eventList != null) ? eventList.add(res.right().getValue())
                       : new JsonArray().add(res.right().getValue()));
           }
        });

        return promise.future();
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
