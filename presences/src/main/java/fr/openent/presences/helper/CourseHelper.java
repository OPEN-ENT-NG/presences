package fr.openent.presences.helper;

import fr.openent.presences.common.helper.DateHelper;
import fr.openent.presences.common.helper.FutureHelper;
import fr.openent.presences.model.Course;
import fr.openent.presences.model.Slot;
import fr.wseduc.webutils.Either;
import io.vertx.core.*;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class CourseHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(CourseHelper.class);
    private final EventBus eb;
    private SubjectHelper subjectHelper;

    public CourseHelper(EventBus eb) {
        this.eb = eb;
        this.subjectHelper = new SubjectHelper(eb);
    }

    /**
     * Parameters validation
     *
     * @param params parameters list
     * @return if parameters are valids or not
     */
    public boolean checkParams(MultiMap params) {
        if (!params.contains("structure")
                || !params.contains("start") || !params.contains("end")
                || !params.get("start").matches("\\d{4}-\\d{2}-\\d{2}")
                || !params.get("end").matches("\\d{4}-\\d{2}-\\d{2}")) {
            return false;
        }

        return true;
    }

    public void getCoursesList(String structure, List<String> teachers, List<String> groups,
                               String start, String end, Future<List<Course>> handler) {
        getCoursesList(structure, teachers, groups, start, end, result -> {
            if (result.failed()) {
                String err = "[CourseHelper@getCourses] Failed to retrieve list courses";
                LOGGER.error(err);
                handler.fail(err + " " + result.cause());
            } else {
                handler.complete(result.result());
            }
        });
    }

    public void getCoursesList(String structure, List<String> teachersList, List<String> groupsList,
                               String start, String end, Handler<AsyncResult<List<Course>>> handler) {
        getCourses(structure, teachersList, groupsList, start, end, courseResult -> {
            if (courseResult.isLeft()) {
                String message = "[CourseHelper@getCourses] Failed to retrieve list courses";
                LOGGER.error(message, courseResult.left().getValue());
                handler.handle(Future.failedFuture(message + " " + courseResult.left().getValue()));
            } else {
                JsonArray courses = courseResult.right().getValue();
                JsonArray subjectIds = new JsonArray();
                JsonArray teachersIds = new JsonArray();

                for (int i = 0; i < courses.size(); i++) {
                    JsonObject course = courses.getJsonObject(i);
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
                        String message = "[Presences@CourseHelper] Failed to retrieve subjects or teachers info";
                        LOGGER.error(message);
                        handler.handle(Future.failedFuture(message + " " + asyncHandler.cause().toString()));
                        return;
                    }
                    JsonArray subjects = subjectsFuture.result();
                    JsonArray teachers = teachersFuture.result();
                    JsonObject subjectMap = MapHelper.transformToMap(subjects, "id");
                    JsonObject teacherMap = MapHelper.transformToMap(teachers, "id");
                    for (int i = 0; i < courses.size(); i++) {
                        JsonObject object = courses.getJsonObject(i);
                        if (object.containsKey("subjectId")) {
                            object.put("subjectName", subjectMap.getJsonObject(object.getString("subjectId"), new JsonObject()).getString("name", object.getString("exceptionnal", "")));
                        } else object.put("subjectName", "");
                        JsonArray courseTeachers = new JsonArray();
                        JsonArray teacherIds = object.getJsonArray("teacherIds");
                        for (int j = 0; j < teacherIds.size(); j++) {
                            if (!teacherMap.containsKey(teacherIds.getString(j))) continue;
                            courseTeachers.add(teacherMap.getJsonObject(teacherIds.getString(j)));
                        }
                        object.put("teachers", courseTeachers);
                    }
                    handler.handle(Future.succeededFuture(CourseHelper.getCourseListFromJsonArray(courses, Course.MANDATORY_ATTRIBUTE)));
                });

                subjectHelper.getSubjects(subjectIds, FutureHelper.handlerJsonArray(subjectsFuture));
                getCourseTeachers(teachersIds, FutureHelper.handlerJsonArray(teachersFuture));
            }
        });
    }

    public void getCourses(String structure, List<String> teachers, List<String> groups, String start,
                           String end, Handler<Either<String, JsonArray>> handler) {
        JsonObject action = new JsonObject()
                .put("action", "course.getCoursesOccurences")
                .put("structureId", structure)
                .put("teacherId", new JsonArray(teachers))
                .put("group", new JsonArray(groups))
                .put("begin", start)
                .put("end", end);

        eb.send("viescolaire", action, event -> {
            if (event.failed() || event.result() == null || "error".equals(((JsonObject) event.result().body()).getString("status"))) {
                String err = "[CourseHelper@getCourses] Failed to retrieve courses";
                LOGGER.error(err);
                handler.handle(new Either.Left<>(err));
            } else {
                handler.handle(new Either.Right<>(((JsonObject) event.result().body()).getJsonArray("results")));
            }
        });
    }

    /**
     * Convert JsonArray into courses list
     *
     * @param array               JsonArray response
     * @param mandatoryAttributes List of mandatory attributes
     * @return new list of courses
     */
    public static List<Course> getCourseListFromJsonArray(JsonArray array, List<String> mandatoryAttributes) {
        List<Course> courseList = new ArrayList<>();
        for (Object o : array) {
            if (!(o instanceof JsonObject)) continue;
            Course course = new Course((JsonObject) o, mandatoryAttributes);
            courseList.add(course);
        }
        return courseList;
    }

    public static List<Course> formatCourses(List<Course> courses, boolean multipleSlot, List<Slot> slots) {
        // Case when slots are not defined from viesco.
        if (slots.isEmpty()) {
            return courses;
        }

        List<Course> formatCourses = new ArrayList<>();
        courses.stream()
                .collect(Collectors.groupingBy(Course::getId))
                .forEach((courseId, listCourses) -> {
                    if (listCourses.stream().anyMatch(listCourse -> listCourse.getRegisterId() != null)) {
                        boolean isSplit = Objects.requireNonNull(listCourses.stream()
                                .filter(listCourse -> listCourse.getRegisterId() != null)
                                .findAny().orElse(null)).isSplitSlot();
                        for (Course course : listCourses) {
                            if (course.isSplitSlot().equals(isSplit)) {
                                formatCourses.add(course);
                            }
                        }
                    } else {
                        for (Course course : listCourses) {
                            if (course.isSplitSlot().equals(multipleSlot)) {
                                formatCourses.add(course);
                            }
                        }
                    }
                });
        return formatCourses;
    }

    public static List<Course> splitCoursesWithOneCourse(Course course, List<Slot> slots) {
        List<Course> splitCourses = new ArrayList<>();
        try {
            SimpleDateFormat parser = new SimpleDateFormat(DateHelper.HOUR_MINUTES_SECONDS);
            Date startTime = parser.parse(DateHelper.getTimeString(course.getStartDate(), DateHelper.MONGO_FORMAT));
            Date endTime = parser.parse(DateHelper.getTimeString(course.getEndDate(), DateHelper.MONGO_FORMAT));
            for (Slot slot : slots) {
                Date slotStartHour = parser.parse(slot.getStartHour());
                Date slotEndHour = parser.parse(slot.getEndHour());
                if (((slotStartHour.after(startTime) || slotStartHour.equals(startTime)) || (startTime.before(slotEndHour)))
                        && ((slotEndHour.before(endTime) || slotEndHour.equals(endTime)) || (endTime.after(slotStartHour)))
                        && !(course.getRegisterId() != null && !course.isSplitSlot())) {
                    Course newCourse = treatingSplitSlot(course, slot, parser);
                    splitCourses.add(newCourse);
                }
            }
        } catch (ParseException e) {
            LOGGER.error("[Presences@CourseHelper] Failed to parse date [see DateHelper", e);
        }
        return splitCourses;
    }

    public void getCourseTeachers(JsonArray teachers, Handler<Either<String, JsonArray>> handler) {
        String teacherQuery = "MATCH (u:User) WHERE u.id IN {teacherIds} RETURN u.id as id, (u.lastName + ' ' + u.firstName) as displayName";
        Neo4j.getInstance().execute(teacherQuery, new JsonObject().put("teacherIds", teachers), Neo4jResult.validResultHandler(handler));
    }

    /**
     * Split the courses in x slots if multiple register is set true
     * (e.g Courses[09:00 - 12:00] would be split in [09:00-10:00], [09:00-11:00], [09:00-12:00])
     * based on slots set as parameter
     *
     * @param courses List of courses fetched
     * @param slots   list of slot fetched from default time slots
     * @return new split courses
     */
    public static List<Course> splitCoursesFromSlot(List<Course> courses, List<Slot> slots) {
        List<Course> splitCoursesEvent = new ArrayList<>();
        try {
            for (Course course : courses) {
                SimpleDateFormat parser = new SimpleDateFormat(DateHelper.HOUR_MINUTES_SECONDS);
                Date startTime = parser.parse(DateHelper.getTimeString(course.getStartDate(), DateHelper.MONGO_FORMAT));
                Date endTime = parser.parse(DateHelper.getTimeString(course.getEndDate(), DateHelper.MONGO_FORMAT));

                for (Slot slot : slots) {
                    Date slotStartHour = parser.parse(slot.getStartHour());
                    Date slotEndHour = parser.parse(slot.getEndHour());
                    if (((slotStartHour.after(startTime) || slotStartHour.equals(startTime)) || (startTime.before(slotEndHour)))
                            && ((slotEndHour.before(endTime) || slotEndHour.equals(endTime)) || (endTime.after(slotStartHour)))
                            && !(course.getRegisterId() != null && !course.isSplitSlot())) {
                        Course newCourse = treatingSplitSlot(course, slot, parser);
                        splitCoursesEvent.add(newCourse);
                    }
                }

                if (course.getRegisterId() != null && !course.isSplitSlot()) {
                    splitCoursesEvent.add(course.clone());
                }
            }
        } catch (ParseException e) {
            LOGGER.error("[Presences@CourseHelper] Failed to parse date [see DateHelper", e);
        }
        return splitCoursesEvent;
    }

    /**
     * Util function that compares the current course element and the current slot time
     * for treating split slot
     *
     * @param course    course element
     * @param slot      slot element
     * @param parser    Parse format
     * @return new course with new start and end time defined from the slot
     */
    public static Course treatingSplitSlot(Course course, Slot slot, SimpleDateFormat parser) throws ParseException {
        String newStartDate = DateHelper.getDateString(course.getStartDate(), DateHelper.YEAR_MONTH_DAY);
        String newStartTime;
        if (isCourseStartTimeAfterSlotStartTime(course, slot, parser)) {
            newStartTime = DateHelper.getTimeString(course.getStartDate(), DateHelper.MONGO_FORMAT);
        } else {
            newStartTime = DateHelper.getTimeString(slot.getStartHour(), DateHelper.HOUR_MINUTES_SECONDS);
        }
        String newEndDate = DateHelper.getDateString(course.getEndDate(), DateHelper.YEAR_MONTH_DAY);
        String newEndTime;
        if (isCourseEndTimeBeforeSlotEndTime(course, slot, parser)) {
            newEndTime = DateHelper.getTimeString(course.getEndDate(), DateHelper.MONGO_FORMAT);
        } else {
            newEndTime = DateHelper.getTimeString(slot.getEndHour(), DateHelper.HOUR_MINUTES_SECONDS);
        }
        Course newCourse = course.clone();
        newCourse.setStartDate(newStartDate + " " + newStartTime);
        newCourse.setEndDate(newEndDate + " " + newEndTime);
        return newCourse;
    }

    private static boolean isCourseStartTimeAfterSlotStartTime(Course course, Slot slot, SimpleDateFormat parser) throws ParseException {
        return parser.parse(DateHelper.getTimeString(course.getStartDate(), DateHelper.MONGO_FORMAT))
                .after(parser.parse(DateHelper.getTimeString(slot.getStartHour(), DateHelper.HOUR_MINUTES_SECONDS)))
                ||
                parser.parse(DateHelper.getTimeString(course.getStartDate(), DateHelper.MONGO_FORMAT))
                        .equals(parser.parse(DateHelper.getTimeString(slot.getStartHour(), DateHelper.HOUR_MINUTES_SECONDS)));
    }

    private static boolean isCourseEndTimeBeforeSlotEndTime(Course course, Slot slot, SimpleDateFormat parser) throws ParseException {
        return parser.parse(DateHelper.getTimeString(course.getEndDate(), DateHelper.MONGO_FORMAT))
                .before(parser.parse(DateHelper.getTimeString(slot.getEndHour(), DateHelper.HOUR_MINUTES_SECONDS)))
                ||
                parser.parse(DateHelper.getTimeString(course.getEndDate(), DateHelper.MONGO_FORMAT))
                        .equals(parser.parse(DateHelper.getTimeString(slot.getEndHour(), DateHelper.HOUR_MINUTES_SECONDS)));
    }
}
