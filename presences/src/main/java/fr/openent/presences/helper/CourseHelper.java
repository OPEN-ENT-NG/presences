package fr.openent.presences.helper;

import fr.openent.presences.common.helper.*;
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
import java.util.*;
import java.util.stream.Collectors;

public class CourseHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(CourseHelper.class);
    private final EventBus eb;

    public CourseHelper(EventBus eb) {
        this.eb = eb;
    }

    /**
     * Parameters validation
     *
     * @param params parameters list
     * @return if parameters are valids or not
     */
    public boolean checkParams(MultiMap params) {
        return params.contains("structure")
                && params.contains("start") && params.contains("end")
                && params.get("start").matches("\\d{4}-\\d{2}-\\d{2}")
                && params.get("end").matches("\\d{4}-\\d{2}-\\d{2}");
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
        getCourses(structure, teachersList, groupsList, start, end, null, null, courseResult -> {
            if (courseResult.isLeft()) {
                String message = "[CourseHelper@getCourses] Failed to retrieve list courses";
                LOGGER.error(message, courseResult.left().getValue());
                handler.handle(Future.failedFuture(message + " " + courseResult.left().getValue()));
            } else {
                JsonArray courses = courseResult.right().getValue();
                JsonArray teachersIds = new JsonArray();

                for (int i = 0; i < courses.size(); i++) {
                    JsonObject course = courses.getJsonObject(i);
                    treatTeacherInCourse(teachersIds, course);
                }

                getCourseTeachers(teachersIds, teachersAsync -> {
                    if (teachersAsync.isLeft()) {
                        String message = "[Presences@CourseHelper::getCoursesList] Failed to retrieve teachers info";
                        LOGGER.error(message);
                        handler.handle(Future.failedFuture(message + " " + teachersAsync.left().getValue()));
                        return;
                    }
                    JsonArray teachers = teachersAsync.right().getValue();
                    JsonObject teacherMap = MapHelper.transformToMap(teachers, "id");
                    for (int i = 0; i < courses.size(); i++) {
                        JsonObject object = courses.getJsonObject(i);
                        if (object.containsKey("subjectId") || object.containsKey("timetableSubjectId")) {
                            object.put("subjectName", object.getJsonObject("subject", new JsonObject()).getString("name", object.getString("exceptionnal", "")));
                        } else object.put("subjectName", "");
                        setTeacherCourseObject(teacherMap, object);
                    }
                    handler.handle(Future.succeededFuture(CourseHelper.getCourseListFromJsonArray(courses, Course.MANDATORY_ATTRIBUTE)));
                });
            }
        });
    }

    public static void treatTeacherInCourse(JsonArray teachersIds, JsonObject course) {
        JsonArray teachers = course.getJsonArray("teacherIds", new JsonArray());
        for (int j = 0; j < teachers.size(); j++) {
            if (!teachersIds.contains(teachers.getString(j))) {
                teachersIds.add(teachers.getString(j));
            }
        }
    }

    public static void setTeachersCourses(JsonArray courses, JsonArray teachersIds) {
        for (int i = 0; i < courses.size(); i++) {
            JsonObject course = courses.getJsonObject(i);
            CourseHelper.treatTeacherInCourse(teachersIds, course);
        }
    }

    private static void setTeacherCourseObject(JsonObject teacherMap, JsonObject object) {
        JsonArray courseTeachers = new JsonArray();
        JsonArray teacherIds = object.getJsonArray("teacherIds", new JsonArray());
        for (int j = 0; j < teacherIds.size(); j++) {
            if (!teacherMap.containsKey(teacherIds.getString(j))) continue;
            courseTeachers.add(teacherMap.getJsonObject(teacherIds.getString(j)));
        }
        object.put("teachers", courseTeachers);
    }

    public void getCourses(String structure, List<String> teachers, List<String> groups, String start,
                           String end, String startTime, String endTime, String crossDateFilter, Handler<Either<String, JsonArray>> handler) {
        this.getCourses(structure, teachers, groups, start, end, startTime, endTime, null, null, null, crossDateFilter,
                null, handler);
    }

    public void getCourses(String structure, List<String> teachers, List<String> groups, String start,
                           String end, String startTime, String endTime, String limit, String offset,
                           String descendingDate, String disableWithoutTeacher, Handler<Either<String, JsonArray>> handler) {
        this.getCourses(structure, teachers, groups, start, end, startTime, endTime, limit, offset, descendingDate, null,
                disableWithoutTeacher, handler);
    }

    public void getCourses(String structure, List<String> teachers, List<String> groups, String start,
                           String end, String startTime, String endTime, Handler<Either<String, JsonArray>> handler) {
        this.getCourses(structure, teachers, groups, start, end, startTime, endTime, null, null, null, null,
                null, handler);
    }

    public void getCourses(String structure, List<String> teachers, List<String> groups, String start,
                           String end, String startTime, String endTime, String limit, String offset,
                           String descendingDate, String crossDateFilter, String disableWithoutTeacher, Handler<Either<String, JsonArray>> handler) {
        JsonObject action = new JsonObject()
                .put("action", "course.getCoursesOccurences")
                .put("structureId", structure)
                .put("teacherId", new JsonArray(teachers))
                .put("group", new JsonArray(groups))
                .put("begin", start)
                .put("end", end)
                .put("startTime", startTime)
                .put("endTime", endTime)
                .put("limit", limit)
                .put("offset", offset)
                .put("descendingDate", descendingDate)
                .put("disableWithoutTeacher", disableWithoutTeacher)
                .put("crossDateFilter", crossDateFilter);

        eb.send("viescolaire", action, event -> {
            if (event.failed() || event.result() == null || "error".equals(((JsonObject) event.result().body()).getString("status"))) {
                String err = "[CourseHelper@getCourses] Failed to retrieve courses " + event.cause().getMessage();
                LOGGER.error(err);
                handler.handle(new Either.Left<>(err));
            } else {
                handler.handle(new Either.Right<>(((JsonObject) event.result().body()).getJsonArray("results")));
            }
        });
    }

    public void getCourses(String structure, String start, String end, String startTime, String endTime,
                           String crossDateFilter, Handler<Either<String, JsonArray>> handler) {
        JsonObject action = new JsonObject()
                .put("action", "course.getCoursesOccurences")
                .put("structureId", structure)
                .put("teacherId", new JsonArray())
                .put("group", new JsonArray())
                .put("begin", start)
                .put("end", end)
                .put("startTime", startTime)
                .put("endTime", endTime)
                .put("crossDateFilter", crossDateFilter);

        eb.send("viescolaire", action, event -> {
            if (event.failed() || event.result() == null || "error".equals(((JsonObject) event.result().body()).getString("status"))) {
                String err = "[CourseHelper@getCourses] Failed to retrieve courses " + event.cause().getMessage();
                LOGGER.error(err);
                handler.handle(new Either.Left<>(err));
            } else {
                handler.handle(new Either.Right<>(((JsonObject) event.result().body()).getJsonArray("results")));
            }
        });
    }

    public void getCoursesByIds(JsonArray courseIds, Handler<Either<String, JsonArray>> handler) {
        JsonObject action = new JsonObject()
                .put("action", "course.getCoursesByIds")
                .put("courseIds", courseIds);

        eb.send("viescolaire", action, courseAsync -> {
            if (courseAsync.failed() || courseAsync.result() == null ||
                    "error".equals(((JsonObject) courseAsync.result().body()).getString("status"))) {

                String message = "[CourseHelper@getCoursesByIds] Failed to retrieve courses by ids.";
                handler.handle(new Either.Left<>(message));
            } else {
                handler.handle(new Either.Right<>(((JsonObject) courseAsync.result().body()).getJsonArray("result")));
            }
        });
    }

    /**
     * Format course fetched (must provide teacher data)
     *
     * @param courses    courses JsonArray
     * @param teacherMap Map of teacher fetched
     * @return new list of courses
     */
    public static void formatCourse(JsonArray courses, JsonObject teacherMap) {
        for (int i = 0; i < courses.size(); i++) {
            try {
                JsonObject object = courses.getJsonObject(i);
                object.remove("startCourse");
                object.remove("endCourse");
                object.remove("is_periodic");
                object.remove("is_recurrent");
                if (object.containsKey("subjectId") || object.containsKey("timetableSubjectId")) {
                    object.put("subjectName", object.getJsonObject("subject", new JsonObject()).getString("name", object.getString("exceptionnal", "")));
                } else object.put("subjectName", "");
                object.put("timestamp", DateHelper.parse(object.getString("startDate")).getTime());
                setTeacherCourseObject(teacherMap, object);
                object.remove("teacherIds");
            } catch (ParseException e) {
                LOGGER.error("[Presences@DefaultCourseService] Failed to cast date to timestamp", e);
            }
        }
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
            splitCourseTreatment(slots, splitCourses, course);
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
                splitCourseTreatment(slots, splitCoursesEvent, course);
                if (course.getRegisterId() != null && !course.isSplitSlot()) {
                    splitCoursesEvent.add(course.clone());
                }
            }
        } catch (ParseException e) {
            LOGGER.error("[Presences@CourseHelper] Failed to parse date [see DateHelper", e);
        }
        return splitCoursesEvent;
    }

    private static void splitCourseTreatment(List<Slot> slots, List<Course> splitCoursesEvent, Course course) throws ParseException {
        Set<String> courseList = new HashSet<>(); // Weird fix to prevent same multiple course
        SimpleDateFormat parser = new SimpleDateFormat(DateHelper.HOUR_MINUTES_SECONDS);
        Date startTime = parser.parse(DateHelper.getTimeString(course.getStartDate(), DateHelper.MONGO_FORMAT));
        Date endTime = parser.parse(DateHelper.getTimeString(course.getEndDate(), DateHelper.MONGO_FORMAT));

        for (int i = 0; i < slots.size(); i++) {
            Slot slot = slots.get(i);
            Date slotStartHour = parser.parse(slot.getStartHour());
            Date slotEndHour = parser.parse(slot.getEndHour());
            if (((slotStartHour.after(startTime) || slotStartHour.equals(startTime)) || (startTime.before(slotEndHour)))
                    && ((slotEndHour.before(endTime) || slotEndHour.equals(endTime)) || (endTime.after(slotStartHour)))
                    && !(course.getRegisterId() != null && !course.isSplitSlot())) {
                Course newCourse = treatingSplitSlot(course, slot, i + 1 < slots.size() ? slots.get(i + 1) : slot, parser);
                if (!courseList.contains(newCourse.mapId())) {
                    courseList.add(newCourse.mapId());
                    splitCoursesEvent.add(newCourse);
                }
            }
        }
    }

    /**
     * Util function that compares the current course element and the current slot time
     * for treating split slot
     *
     * @param course course element
     * @param slot   slot element
     * @param parser Parse format
     * @return new course with new start and end time defined from the slot
     */
    public static Course treatingSplitSlot(Course course, Slot slot, Slot nextSlot, SimpleDateFormat parser) throws ParseException {
        String newStartDate = DateHelper.getDateString(course.getStartDate(), DateHelper.YEAR_MONTH_DAY);
        String newStartTime;
        if (isCourseStartTimeAfterSlotStartTime(course, slot, parser)) {
            newStartTime = DateHelper.getTimeString(course.getStartDate(), DateHelper.MONGO_FORMAT);
        } else {
            newStartTime = DateHelper.getTimeString(slot.getStartHour(), DateHelper.HOUR_MINUTES_SECONDS);
        }
        String newEndDate = DateHelper.getDateString(course.getEndDate(), DateHelper.YEAR_MONTH_DAY);
        String newEndTime;
        if (isCourseEndTimeBeforeSlotEndTime(course, slot, parser) || isCourseEndTimeBeforeNextSlotStartTime(course, nextSlot, parser)) {
            newEndTime = DateHelper.getTimeString(course.getEndDate(), DateHelper.MONGO_FORMAT);
        } else {
            newEndTime = DateHelper.getTimeString(slot.getEndHour(), DateHelper.HOUR_MINUTES_SECONDS);
        }
        Course newCourse = course.clone();
        newCourse.setStartDate(newStartDate + " " + newStartTime);
        newCourse.setEndDate(newEndDate + " " + newEndTime);
        return newCourse;
    }

    private static boolean isCourseEndTimeBeforeNextSlotStartTime(Course course, Slot slot, SimpleDateFormat parser) throws ParseException {
        return !parser.parse(DateHelper.getTimeString(course.getEndDate(), DateHelper.MONGO_FORMAT))
                .after(parser.parse(DateHelper.getTimeString(slot.getStartHour(), DateHelper.HOUR_MINUTES_SECONDS)));
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
