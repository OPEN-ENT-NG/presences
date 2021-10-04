package fr.openent.presences.service.impl;

import fr.openent.presences.common.helper.FutureHelper;
import fr.openent.presences.common.viescolaire.Viescolaire;
import fr.openent.presences.core.constants.*;
import fr.openent.presences.enums.RegisterStatus;
import fr.openent.presences.helper.CourseHelper;
import fr.openent.presences.helper.MapHelper;
import fr.openent.presences.helper.SlotHelper;
import fr.openent.presences.helper.SquashHelper;
import fr.openent.presences.model.*;
import fr.openent.presences.service.CourseService;
import fr.openent.presences.service.RegisterService;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DefaultCourseService implements CourseService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultCourseService.class);

    private final EventBus eb;
    private final CourseHelper courseHelper;
    private final RegisterService registerService;

    public DefaultCourseService(EventBus eb) {
        this.eb = eb;
        this.courseHelper = new CourseHelper(eb);
        this.registerService = new DefaultRegisterService(eb);
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

        eb.send("viescolaire", action, subjectsAsync -> {
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
                            boolean forgottenFilter, MultipleSlotSettings multipleSlot,
                            Handler<Either<String, JsonArray>> handler) {
        this.listCourses(structureId, teachersList, groupsList, start, end, startTime, endTime,
                forgottenFilter, multipleSlot, null, null, null, handler);
    }

    @Override
    public void listCourses(String structureId, List<String> teachersList, List<String> groupsList,
                            String start, String end, String startTime, String endTime,
                            boolean forgottenFilter, MultipleSlotSettings multipleSlot,
                            String limit, String offset, String descendingDate, Handler<Either<String, JsonArray>> handler) {
        this.listCourses(structureId, teachersList, groupsList, start, end, startTime, endTime,
                forgottenFilter, multipleSlot, null, null, null, null, handler);
    }

    @Override
    public void listCourses(String structureId, List<String> teachersList, List<String> groupsList,
                            String start, String end, String startTime, String endTime,
                            boolean forgottenFilter, MultipleSlotSettings multipleSlot,
                            String limit, String offset, String descendingDate, String searchTeacher, Handler<Either<String, JsonArray>> handler) {
        courseHelper.getCourses(structureId, teachersList, groupsList, start, end, startTime, endTime, limit, offset, descendingDate,
                searchTeacher, event -> {
                    if (event.isLeft()) {
                        handler.handle(new Either.Left<>(event.left().getValue()));
                        return;
                    }
                    JsonArray courses = event.right().getValue();
                    JsonArray teachersIds = new JsonArray();
                    CourseHelper.setTeachersCourses(courses, teachersIds);

                    List<String> coursesIds = ((List<JsonObject>) courses.getList())
                            .stream()
                            .map(course -> course.getString("_id")).collect(Collectors.toList());

                    Promise<JsonArray> teachersFuture = Promise.promise();
                    Promise<JsonArray> slotsFuture = Promise.promise();
                    Promise<JsonArray> registerEventFuture = Promise.promise();

                    CompositeFuture.all(teachersFuture.future(), slotsFuture.future(), registerEventFuture.future())
                            .onFailure(fail ->  handler.handle(new Either.Left<>(fail.getCause().getMessage())))
                            .onSuccess(ar -> {
                                JsonArray teachers = teachersFuture.future().result();
                                JsonObject teacherMap = MapHelper.transformToMap(teachers, Field.ID);

                                CourseHelper.formatCourse(courses, teacherMap);

                                List<Slot> slots = SlotHelper.getSlotListFromJsonArray(slotsFuture.future().result(),
                                        Slot.MANDATORY_ATTRIBUTE);
                                List<Course> coursesEvent = CourseHelper.getCourseListFromJsonArray(courses,
                                        Course.MANDATORY_ATTRIBUTE);
                                List<Course> splitCoursesEvent = CourseHelper.splitCoursesFromSlot(coursesEvent, slots);

                                List<Course> squashCourses = SquashHelper.squash(coursesEvent, splitCoursesEvent,
                                        registerEventFuture.future().result(), multipleSlot);

                                handler.handle(new Either.Right<>(forgottenFilter ?
                                        new JsonArray(filterForgottenCourses(CourseHelper.formatCourses(squashCourses,
                                                multipleSlot, slots, BooleanUtils.toBooleanObject(searchTeacher)))) :
                                        new JsonArray(CourseHelper.formatCourses(squashCourses, multipleSlot,
                                                slots, BooleanUtils.toBooleanObject(searchTeacher)))
                                ));
                            });
                    courseHelper.getCourseTeachers(teachersIds, FutureHelper.handlerJsonArray(teachersFuture));
                    Viescolaire.getInstance().getSlotsFromProfile(structureId, FutureHelper.handlerJsonArray(slotsFuture));
                    registerService.list(structureId, coursesIds, FutureHelper.handlerJsonArray(registerEventFuture));
                });
    }

    private List<Course> filterForgottenCourses(List<Course> courses) {
        List<Course> forgottenRegisters = new ArrayList<>();
        for (Course course : courses) {
            Course newCourse = course.clone();
            if (newCourse.getRegisterId() == null) {
                forgottenRegisters.add(course);
                continue;
            }
            Integer registerState = newCourse.getRegisterStateId();

            if (!registerState.equals(RegisterStatus.DONE.getStatus())) {
                forgottenRegisters.add(course);
            }
        }

        return forgottenRegisters;
    }
}
