package fr.openent.presences.helper;

import fr.openent.presences.common.helper.DateHelper;
import fr.openent.presences.model.Course;
import fr.openent.presences.model.Slot;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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
        if (!params.contains("structure")
                || !params.contains("start") || !params.contains("end")
                || !params.get("start").matches("\\d{4}-\\d{2}-\\d{2}")
                || !params.get("end").matches("\\d{4}-\\d{2}-\\d{2}")) {
            return false;
        }

        return true;
    }

    public void getCourses(String structure, List<String> teachers, List<String> groups, String start, String end, Handler<Either<String, JsonArray>> handler) {
        JsonObject action = new JsonObject()
                .put("action", "course.getCoursesOccurences")
                .put("structureId", structure)
                .put("teacherId", new JsonArray(teachers))
                .put("group", new JsonArray(groups))
                .put("begin", start)
                .put("end", end);

        eb.send("viescolaire", action, event -> {
            JsonObject body = (JsonObject) event.result().body();
            if (event.failed() || "error".equals(body.getString("status"))) {
                String err = "[CourseHelper@getCourses] Failed to retrieve courses";
                LOGGER.error(err);
                handler.handle(new Either.Left<>(err));
            } else {
                handler.handle(new Either.Right<>(body.getJsonArray("results")));
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

    public static List<Course> formatCourses(List<Course> courses, boolean multipleSlot) {
        List<Course> formatCourses = new ArrayList<>();

        courses.stream()
                .collect(Collectors.groupingBy(Course::getId))
                .forEach((courseId, listCourses) -> {

                    if (listCourses.stream().anyMatch(listCourse -> listCourse.getRegisterId() != null)) {
                        boolean isSplit = listCourses.stream()
                                .filter(listCourse -> listCourse.getRegisterId() != null)
                                .findAny().get().isSplitSlot();
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
                    if ((startTime.before(slotStartHour) || startTime.equals(slotStartHour))
                            && (endTime.after(slotEndHour) || endTime.equals(slotEndHour))
                            && !(course.getRegisterId() != null && !course.isSplitSlot())) {
                        Course newCourse = treatingSplitSlot(course, slot);
                        splitCoursesEvent.add(newCourse);
                    }
                }

                if (course.getRegisterId() != null && !course.isSplitSlot()) {
                    splitCoursesEvent.add(course.clone());
                }
            }
        } catch (ParseException e) {
            LOGGER.error("[Presences@CourseModel] Failed to parse date", e);
        }
        return splitCoursesEvent;
    }

    /**
     * Check if each course has a split mode
     * If it is true, we then split its courses in x slots
     * (e.g Courses[09:00 - 12:00] would be split in [09:00-10:00], [09:00-11:00], [09:00-12:00])
     * based on slots set as parameter
     * If it is false, we kept its course element just as how it was created
     *
     * @param courses List of courses fetched
     * @param slots   list of slot fetched from default time slots
     * @return new courses (read above)
     */
    public static List<Course> checkSplitableSlot(List<Course> courses, List<Slot> slots) {
        List<Course> checkedCourses = new ArrayList<>();
        SimpleDateFormat parser = new SimpleDateFormat(DateHelper.HOUR_MINUTES_SECONDS);

        for (Course course : courses) {
            if (course.isSplitSlot()) {
                try {
                    Date startTime = parser.parse(DateHelper.getTimeString(course.getStartDate(), DateHelper.MONGO_FORMAT));
                    Date endTime = parser.parse(DateHelper.getTimeString(course.getEndDate(), DateHelper.MONGO_FORMAT));
                    for (Slot slot : slots) {
                        Date slotStartHour = parser.parse(slot.getStartHour());
                        Date slotEndHour = parser.parse(slot.getEndHour());
                        if ((startTime.before(slotStartHour) || startTime.equals(slotStartHour))
                                && (endTime.after(slotEndHour) || endTime.equals(slotEndHour))) {
                            Course newCourse = treatingSplitSlot(course, slot);
                            checkedCourses.add(newCourse);
                        }
                    }
                } catch (ParseException e) {
                    LOGGER.error("[Presences@CourseModel] Failed to parse date for checking splitable slot", e);
                }
            } else {
                checkedCourses.add(course.clone());
            }
        }
        return checkedCourses;
    }

    /**
     * Util function that compares the current course element and the current slot time
     * for treating split slot
     *
     * @param course course element
     * @param slot   slot element
     * @return new course with new start and end time defined from the slot
     */
    private static Course treatingSplitSlot(Course course, Slot slot) throws ParseException {
        String newStartDate = DateHelper.getDateString(course.getStartDate(), DateHelper.YEAR_MONTH_DAY);
        String newStartTime = DateHelper.getTimeString(slot.getStartHour(), DateHelper.HOUR_MINUTES_SECONDS);
        String newEndDate = DateHelper.getDateString(course.getEndDate(), DateHelper.YEAR_MONTH_DAY);
        String newEndTime = DateHelper.getTimeString(slot.getEndHour(), DateHelper.HOUR_MINUTES_SECONDS);
        Course newCourse = course.clone();
        newCourse.setStartDate(newStartDate + " " + newStartTime);
        newCourse.setEndDate(newEndDate + " " + newEndTime);
        return newCourse;
    }
}
