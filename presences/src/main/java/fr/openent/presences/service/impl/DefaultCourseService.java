package fr.openent.presences.service.impl;

import fr.openent.presences.common.helper.DateHelper;
import fr.openent.presences.common.helper.FutureHelper;
import fr.openent.presences.common.viescolaire.Viescolaire;
import fr.openent.presences.enums.RegisterStatus;
import fr.openent.presences.helper.*;
import fr.openent.presences.model.Course;
import fr.openent.presences.model.Slot;
import fr.openent.presences.service.CourseService;
import fr.openent.presences.service.RegisterService;
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
import java.util.stream.Collectors;

public class DefaultCourseService implements CourseService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultCourseService.class);
    private EventBus eb;
    private CourseHelper courseHelper;
    private SubjectHelper subjectHelper;
    private RegisterService registerService;

    public DefaultCourseService(EventBus eb) {
        this.eb = eb;
        this.courseHelper = new CourseHelper(eb);
        this.subjectHelper = new SubjectHelper(eb);
        this.registerService = new DefaultRegisterService(eb);
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
            setSubjectsTeachersCourses(courses, subjectIds, teachersIds);

            List<String> coursesIds = ((List<JsonObject>) courses.getList())
                    .stream()
                    .map(course -> course.getString("_id")).collect(Collectors.toList());


            Future<JsonArray> subjectsFuture = Future.future();
            Future<JsonArray> teachersFuture = Future.future();
            Future<JsonArray> slotsFuture = Future.future();
            Future<JsonArray> registerEventFuture = Future.future();

            CompositeFuture.all(subjectsFuture, teachersFuture, slotsFuture, registerEventFuture).setHandler(asyncHandler -> {
                if (asyncHandler.failed()) {
                    handler.handle(new Either.Left<>(asyncHandler.cause().toString()));
                    return;
                }

                JsonArray subjects = subjectsFuture.result();
                JsonArray teachers = teachersFuture.result();
                JsonObject subjectMap = MapHelper.transformToMap(subjects, "id");
                JsonObject teacherMap = MapHelper.transformToMap(teachers, "id");

                CourseHelper.formatCourse(courses, subjectMap, teacherMap);

                List<Slot> slots = SlotHelper.getSlotListFromJsonArray(slotsFuture.result(), Slot.MANDATORY_ATTRIBUTE);
                List<Course> coursesEvent = CourseHelper.getCourseListFromJsonArray(courses, Course.MANDATORY_ATTRIBUTE);
                List<Course> splitCoursesEvent = CourseHelper.splitCoursesFromSlot(coursesEvent, slots);

                SquashHelper squashHelper = new SquashHelper();
                List<Course> squashCourses = squashHelper.squash(coursesEvent, splitCoursesEvent, registerEventFuture.result());

                handler.handle(new Either.Right<>(forgottenFilter ?
                        new JsonArray(filterForgottenCourses(CourseHelper.formatCourses(squashCourses, multipleSlot, slots), userDate)) :
                        new JsonArray(CourseHelper.formatCourses(squashCourses, multipleSlot, slots))));
            });

            subjectHelper.getSubjects(subjectIds, FutureHelper.handlerJsonArray(subjectsFuture));
            courseHelper.getCourseTeachers(teachersIds, FutureHelper.handlerJsonArray(teachersFuture));
            Viescolaire.getInstance().getDefaultSlots(structureId, FutureHelper.handlerJsonArray(slotsFuture));
            registerService.list(structureId, coursesIds, FutureHelper.handlerJsonArray(registerEventFuture));
        });
    }

    private void setSubjectsTeachersCourses(JsonArray courses, JsonArray subjectIds, JsonArray teachersIds) {
        JsonObject course;
        for (int i = 0; i < courses.size(); i++) {
            course = courses.getJsonObject(i);
            CourseHelper.treatSubjectAndTeacherInCourse(subjectIds, teachersIds, course);
        }
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
