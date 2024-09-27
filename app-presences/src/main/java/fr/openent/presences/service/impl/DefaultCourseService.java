package fr.openent.presences.service.impl;

import fr.openent.presences.common.helper.FutureHelper;
import fr.openent.presences.common.service.*;
import fr.openent.presences.common.service.impl.*;
import fr.openent.presences.common.viescolaire.Viescolaire;
import fr.openent.presences.core.constants.*;
import fr.openent.presences.db.*;
import fr.openent.presences.helper.*;
import fr.openent.presences.model.*;
import fr.openent.presences.service.*;
import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.webutils.Either;
import io.vertx.core.*;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.mongodb.MongoDbResult;
import org.apache.commons.lang3.BooleanUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DefaultCourseService extends DBService implements CourseService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultCourseService.class);

    private final EventBus eb;
    private final CourseHelper courseHelper;
    private final RegisterService registerService;
    private final GroupService groupService;

    public DefaultCourseService(CommonPresencesServiceFactory commonPresencesServiceFactory) {
        this.eb = commonPresencesServiceFactory.eventBus();
        this.courseHelper = commonPresencesServiceFactory.courseHelper();
        this.registerService = commonPresencesServiceFactory.registerService();
        this.groupService = new DefaultGroupService(eb);
    }

    @Override
    public void getCourse(String courseId, Handler<Either<String, JsonObject>> handler) {
        JsonObject courseQuery = new JsonObject()
                .put("_id", courseId);

        MongoDb.getInstance().findOne("courses", courseQuery, MongoDbResult.validActionResultHandler(courseAsync -> {
            if (courseAsync.isLeft()) {
                LOGGER.error("[Presences@DefaultCourseService::getCourse] Failed to retrieve course ");
                handler.handle(new Either.Left<>(courseAsync.left().getValue()));
            } else {
                JsonObject course = courseAsync.right().getValue().getJsonObject("result");
                setSubjectToCourse(handler, course);
            }
        }));
    }

    @SuppressWarnings("unchecked")
    private void setSubjectToCourse(Handler<Either<String, JsonObject>> handler, JsonObject course) {
        JsonObject action = new JsonObject()
                .put("action", "matiere.getSubjectsAndTimetableSubjects")
                .put("idMatieres", new JsonArray()
                        .add(course.getString("subjectId", course.getString("timetableSubjectId", ""))));

        eb.request("viescolaire", action, subjectsAsync -> {
            if (subjectsAsync.failed() || subjectsAsync.result() == null ||
                    "error".equals(((JsonObject) subjectsAsync.result().body()).getString("status"))) {
                handler.handle(new Either.Left<>(subjectsAsync.cause().getMessage()));
            } else {
                JsonArray subjects = ((JsonObject) subjectsAsync.result().body()).getJsonArray("results");
                Map<String, JsonObject> subjectsMap = ((List<JsonObject>) subjects.getList())
                        .stream()
                        .collect(Collectors.toMap(subject -> subject.getString("id"), Function.identity()));
                course.put("subject", subjectsMap.getOrDefault(course.getString("subjectId", ""), new JsonObject()));
                handler.handle(new Either.Right<>(course));
            }
        });
    }

    @Override
    public void listCourses(String structureId, List<String> teachersList, List<String> groupsList,
                            String start, String end, String startTime, String endTime,
                            boolean forgottenFilter, boolean multipleSlot,
                            Handler<Either<String, JsonArray>> handler) {
        this.listCourses(structureId, teachersList, groupsList, start, end, startTime, endTime,
                forgottenFilter, multipleSlot, null, null, null, handler);
    }

    @Override
    public void listCourses(String structureId, List<String> teachersList, List<String> groupsList,
                            String start, String end, String startTime, String endTime,
                            boolean forgottenFilter, boolean multipleSlot,
                            String limit, String offset, String descendingDate, Handler<Either<String, JsonArray>> handler) {
        this.listCourses(structureId, teachersList, groupsList, start, end, startTime, endTime,
                forgottenFilter, multipleSlot, null, null, null, null, handler);
    }

    @Override
    public void listCourses(String structureId, List<String> teachersList, List<String> groupsList,
                            String start, String end, String startTime, String endTime,
                            boolean forgottenFilter, boolean multipleSlot,
                            String limit, String offset, String descendingDate,
                            String searchTeacher, Handler<Either<String, JsonArray>> handler) {
        this.listCourses(structureId, teachersList, groupsList, start, end, startTime, endTime,
                forgottenFilter, multipleSlot, limit, offset, descendingDate, searchTeacher, null, handler);
    }

    @Override
    public void listCourses(String structureId, List<String> teachersList, List<String> groupsList,
                            String start, String end, String startTime, String endTime,
                            boolean forgottenFilter, boolean multipleSlot,
                            String limit, String offset, String descendingDate,
                            String searchTeacher, String crossDateFilter, Handler<Either<String, JsonArray>> handler) {

        Future<JsonArray> getCoursesFuture = forgottenFilter ?
                listCoursesWithForgottenRegisters(structureId, teachersList, groupsList, start, end, multipleSlot, limit,
                        offset, searchTeacher) :
                listRegistersWithCourses(structureId, teachersList,
                        groupsList, start, end, startTime, endTime, multipleSlot, limit, offset,
                        descendingDate, crossDateFilter, searchTeacher);

        getCoursesFuture
                .onFailure(fail -> handler.handle(new Either.Left<>(fail.getMessage())))
                .onSuccess(courses -> handler.handle(new Either.Right<>(courses)));
    }

    @Override
    @SuppressWarnings("unchecked")
    public Future<JsonArray> listRegistersWithCourses(String structureId, List<String> teacherIds,
                                                      List<String> groupsList, String start, String end, String startTime,
                                                      String endTime, boolean multipleSlot, String limit,
                                                      String offset, String descendingDate, String crossDateFilter,
                                                      String searchTeacher) {

        Promise<JsonArray> promise = Promise.promise();

        courseHelper.getCourses(structureId, teacherIds, groupsList, start, end, startTime,
                endTime, limit, offset, descendingDate, crossDateFilter, searchTeacher, event -> {
                    if (event.isLeft()) {
                        promise.fail(event.left().getValue());
                        return;
                    }
                    JsonArray courses = event.right().getValue();

                    List<String> coursesIds = ((List<JsonObject>) courses.getList())
                            .stream()
                            .map(course -> course.getString(Field._ID))
                            .distinct()
                            .collect(Collectors.toList());

                    Future<JsonArray> teachersFuture = courseHelper.formatCourseTeachersSubjectsAndTags(courses,
                            structureId);
                    Promise<JsonArray> slotsFuture = Promise.promise();
                    Promise<JsonArray> registerEventFuture = Promise.promise();


                    CompositeFuture.all(teachersFuture, slotsFuture.future(), registerEventFuture.future())
                            .onFailure(fail -> promise.fail(fail.getMessage()))
                            .onSuccess(ar ->
                                    promise.complete(new JsonArray(getFormattedCourses(slotsFuture.future().result(),
                                            courses, registerEventFuture.future().result(), multipleSlot, searchTeacher))));


                    Viescolaire.getInstance().getSlotsFromProfile(structureId, FutureHelper.handlerJsonArray(slotsFuture));
                    registerService.list(structureId, coursesIds, FutureHelper.handlerJsonArray(registerEventFuture));
                });
        return promise.future();
    }


    @Override
    @SuppressWarnings("unchecked")
    public Future<JsonArray> listCoursesWithForgottenRegisters(String structureId, List<String> teacherIds,
                                                               List<String> groupsList, String startDate, String endDate,
                                                               boolean multipleSlot, String limit, String offset,
                                                               String isWithTeacherFilter) {

        Promise<JsonArray> promise = Promise.promise();

        Future<List<String>> noTeacherFuture = getCourseIdsWithoutTeacher(structureId, startDate, endDate);
        Future<List<String>> groupIdsFuture = groupService.getGroupsIdList(structureId, groupsList);

        List<Future<List<String>>> futures = new ArrayList<>();

        futures.add(groupIdsFuture);

        if ((isWithTeacherFilter != null) &&
                BooleanUtils.toBooleanObject(isWithTeacherFilter).equals(Boolean.FALSE)) {
            futures.add(noTeacherFuture);
        }

        Future.all(futures)
                .onFailure(fail -> promise.fail(fail.getCause().getMessage()))
                .onSuccess(ar -> {
                    if ((isWithTeacherFilter != null) && BooleanUtils.toBooleanObject(isWithTeacherFilter).equals(Boolean.FALSE)
                            && (noTeacherFuture.result() == null || noTeacherFuture.result().isEmpty())) {
                        promise.complete(new JsonArray());
                    } else {
                        registerService.list(structureId, startDate, endDate, noTeacherFuture.result(),
                                        teacherIds, groupIdsFuture.result(),
                                        true, BooleanUtils.toBooleanObject(isWithTeacherFilter), limit, offset)
                                .onFailure(fail -> promise.fail(fail.getMessage()))
                                .onSuccess(registers -> {

                                    List<String> courseIds = ((List<JsonObject>) registers.getList())
                                            .stream()
                                            .map(register -> register.getString(Field.COURSE_ID))
                                            .filter(Objects::nonNull)
                                            .distinct()
                                            .collect(Collectors.toList());


                                    courseHelper.getCoursesByIds(new JsonArray(courseIds), courseRes -> {
                                        if (courseRes.isLeft()) {
                                            promise.fail(courseRes.left().getValue());
                                        } else {
                                            JsonArray courses = courseRes.right().getValue();

                                            Future<JsonArray> teachersFuture = courseHelper.formatCourseTeachersSubjectsAndTags(courses,
                                                    structureId);
                                            Promise<JsonArray> slotsFuture = Promise.promise();

                                            CompositeFuture.all(teachersFuture, slotsFuture.future())
                                                    .onFailure(fail -> promise.fail(fail.getMessage()))
                                                    .onSuccess(success -> promise.complete(new JsonArray(
                                                            getFormattedCourses(slotsFuture.future().result(), courses,
                                                                    registers, multipleSlot, isWithTeacherFilter).stream()
                                                                    .filter(course -> course.getRegisterId() != null)
                                                                    .collect(Collectors.toList())
                                                    )));

                                            Viescolaire.getInstance().getSlotsFromProfile(structureId,
                                                    FutureHelper.handlerJsonArray(slotsFuture));
                                        }
                                    });
                                });
                    }
                });

        return promise.future();
    }

    /**
     * Format courses from timeslots and multiple slot settings
     *
     * @param slots         time slots {@link JsonArray}
     * @param courses       courses {@link JsonArray}
     * @param registers     registers {@link JsonArray}
     * @param multipleSlot  multiple slot settings
     * @param searchTeacher true -> fetch courses with teachers;
     *                      false -> fetch courses without teachers
     * @return {@link List<Course>} of formatted courses
     */
    private List<Course> getFormattedCourses(JsonArray slots, JsonArray courses,
                                             JsonArray registers, boolean multipleSlot,
                                             String searchTeacher) {
        List<Slot> slotsList = SlotHelper.getSlotListFromJsonArray(slots, Slot.MANDATORY_ATTRIBUTE);
        List<Course> coursesEvent = CourseHelper.getCourseListFromJsonArray(courses, Course.MANDATORY_ATTRIBUTE);
        List<Course> splitCoursesEvent = CourseHelper.splitCoursesFromSlot(coursesEvent, slotsList);
        List<Course> squashCourses = SquashHelper.squash(coursesEvent, splitCoursesEvent,
                registers, multipleSlot);

        return CourseHelper.formatCourses(squashCourses, slotsList, BooleanUtils.toBooleanObject(searchTeacher));
    }

    @Override
    public Future<List<String>> getCourseIdsWithoutTeacher(String structureId, String startDate, String endDate) {
        Promise<List<String>> promise = Promise.promise();

        courseHelper.getCourses(structureId, new ArrayList<>(), new ArrayList<>(), startDate, endDate, null,
                null, null, null, null, "false",
                courses -> {
                    if (courses.isLeft()) {
                        LOGGER.error(courses.left().getValue());
                        promise.fail(courses.left().getValue());
                    } else {
                        promise.complete(courses.right().getValue().stream().map(course ->
                                ((JsonObject) course).getString(Field._ID)).distinct().collect(Collectors.toList()));
                    }
                });
        return promise.future();
    }
}
