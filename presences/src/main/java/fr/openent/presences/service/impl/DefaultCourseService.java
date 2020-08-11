package fr.openent.presences.service.impl;

import fr.openent.presences.common.helper.DateHelper;
import fr.openent.presences.common.helper.FutureHelper;
import fr.openent.presences.common.viescolaire.Viescolaire;
import fr.openent.presences.enums.RegisterStatus;
import fr.openent.presences.helper.*;
import fr.openent.presences.model.Course;
import fr.openent.presences.model.Slot;
import fr.openent.presences.service.CourseService;
import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.webutils.Either;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.mongodb.MongoDbResult;

import java.text.ParseException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DefaultCourseService implements CourseService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultCourseService.class);
    private EventBus eb;
    private CourseHelper courseHelper;
    private SubjectHelper subjectHelper;

    public DefaultCourseService(EventBus eb) {
        this.eb = eb;
        this.courseHelper = new CourseHelper(eb);
        this.subjectHelper = new SubjectHelper(eb);
    }

    @Override
    public void getCourse(String courseId, Handler<Either<String, JsonObject>> handler) {
        JsonObject courseQuery = new JsonObject()
                .put("_id", courseId);

        MongoDb.getInstance().findOne("courses", courseQuery, message -> handler.handle(MongoDbResult.validResult(message)));
    }


    @Override
    public void listCourses(String structureId, List<String> teachersList, List<String> groupsList,
                            String start, String end, boolean forgottenFilter, boolean multipleSlot, String userDate,
                            Handler<Either<String, JsonArray>> handler) {
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
                if (course.containsKey("subjectId") && !subjectIds.contains(course.getString("subjectId"))) {
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
                    try {

                        object = courses.getJsonObject(i);
                        object.remove("startCourse");
                        object.remove("endCourse");
                        object.remove("is_periodic");
                        object.remove("is_recurrent");
                        if (object.containsKey("subjectId")) {
                            object.put("subjectName", subjectMap.getJsonObject(object.getString("subjectId"), new JsonObject()).getString("name", object.getString("exceptionnal", "")));
                        } else object.put("subjectName", "");
                        object.put("timestamp", DateHelper.parse(object.getString("startDate")).getTime());
                        JsonArray courseTeachers = new JsonArray();
                        JsonArray teacherIds = object.getJsonArray("teacherIds");
                        for (int j = 0; j < teacherIds.size(); j++) {
                            if (!teacherMap.containsKey(teacherIds.getString(j))) continue;
                            courseTeachers.add(teacherMap.getJsonObject(teacherIds.getString(j)));
                        }
                        object.put("teachers", courseTeachers);
                        object.remove("teacherIds");
                    } catch (ParseException e) {
                        LOGGER.error("[Presences@DefaultCourseService] Failed to cast date to timestamp", e);
                    }
                }

                Viescolaire.getInstance().getDefaultSlots(structureId, slotsResult -> {
                    List<Slot> slots = SlotHelper.getSlotListFromJsonArray(slotsResult.right().getValue(), Slot.MANDATORY_ATTRIBUTE);
                    List<Course> coursesEvent = CourseHelper.getCourseListFromJsonArray(courses, Course.MANDATORY_ATTRIBUTE);
                    List<Course> splitCoursesEvent = CourseHelper.splitCoursesFromSlot(coursesEvent, slots);

                    SquashHelper squashHelper = new SquashHelper(eb);
                    squashHelper.squash(structureId, start + " 00:00:00", end + " 23:59:59",
                            coursesEvent, splitCoursesEvent, squashEvent ->
                                    handler.handle(new Either.Right<>(forgottenFilter ? new JsonArray(filterForgottenCourses(
                                            CourseHelper.formatCourses(squashEvent.right().getValue(), multipleSlot, slots),
                                            userDate
                                    )) : new JsonArray(CourseHelper.formatCourses(squashEvent.right().getValue(), multipleSlot, slots))))
                    );
                });
            });

            subjectHelper.getSubjects(subjectIds, FutureHelper.handlerJsonArray(subjectsFuture));
            courseHelper.getCourseTeachers(teachersIds, FutureHelper.handlerJsonArray(teachersFuture));
        });
    }

    private List<Course> filterForgottenCourses(List<Course> courses, String userDate) {
        List<Course> forgottenRegisters = new ArrayList<>();
        //FIXME Fix timezone trick
        Date currentDate;
        try {
            if (userDate != null) {
                currentDate = DateHelper.parse(userDate);
            } else {
                long timeDifference = ZoneId.of("Europe/Paris").getRules().getOffset(Instant.now()).getTotalSeconds();
                currentDate = new Date(System.currentTimeMillis() + (timeDifference * 1000));
            }
        } catch (ParseException e) {
            long timeDifference = ZoneId.of("Europe/Paris").getRules().getOffset(Instant.now()).getTotalSeconds();
            currentDate = new Date(System.currentTimeMillis() + (timeDifference * 1000));
        }
        for (Course course : courses) {
            try {
                Course newCourse = course.clone();
                Date forgottenStartDateCourse = new Date(DateHelper.parse(newCourse.getStartDate()).getTime() + (15 * 60000));
                if (currentDate.after(forgottenStartDateCourse)) {
                    if (newCourse.getRegisterId() == null) {
                        forgottenRegisters.add(course);
                        continue;
                    }
                    Integer registerState = newCourse.getRegisterStateId();

                    if (!registerState.equals(RegisterStatus.DONE.getStatus())) {
                        forgottenRegisters.add(course);
                    }
                }
            } catch (ParseException e) {
                LOGGER.error("[Presences@CourseController] Failed to parse date", e);
            }
        }

        return forgottenRegisters;
    }
}
