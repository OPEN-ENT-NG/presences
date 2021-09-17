package fr.openent.presences.helper;

import fr.openent.presences.common.helper.DateHelper;
import fr.openent.presences.core.constants.*;
import fr.openent.presences.model.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.text.ParseException;
import java.util.*;
import java.util.stream.*;

public class SquashHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(SquashHelper.class);

    public SquashHelper() {
    }

    /**
     * Squash courses with registers. Each course will be squashed with its register.
     *
     * @param coursesEvent      Course list
     * @param splitCoursesEvent Course list split
     */
    public static List<Course> squash(List<Course> coursesEvent, List<Course> splitCoursesEvent,
                                      JsonArray registerEvent, boolean multipleSlot) {

        List<Course> courses = new ArrayList<>();
        JsonObject registers = groupRegisters(registerEvent);

        setCoursesRegistersInfos(coursesEvent, registers, false);
        setCoursesRegistersInfos(splitCoursesEvent, registers, true);


        List<Course> createdCourses = coursesEvent.stream()
                .filter(course -> course.getRegisterId() != null).collect(Collectors.toList());

        List<Course> createdSplitCourses = splitCoursesEvent.stream()
                .filter(course -> course.getRegisterId() != null).collect(Collectors.toList());


        courses.addAll(filterDuplicateCourses(createdSplitCourses, createdCourses));
        courses.addAll(filterDuplicateCourses(createdCourses, createdSplitCourses));


        List<Course> futureSplitCourses = splitCoursesEvent.stream()
                .filter(fSplitCourse -> fSplitCourse.getRegisterId() == null
                        && coursesEvent.stream()
                        .noneMatch(course -> course.getRegisterId() != null &&
                                Objects.equals(course.getId(), fSplitCourse.getId())))
                .collect(Collectors.toList());


        List<Course> futureCourses = coursesEvent.stream()
                .filter(fCourse -> fCourse.getRegisterId() == null
                        && splitCoursesEvent.stream()
                        .noneMatch(course -> course.getRegisterId() != null &&
                                Objects.equals(course.getId(), fCourse.getId())))
                .collect(Collectors.toList());

        courses.addAll(Boolean.TRUE.equals(multipleSlot) ? futureSplitCourses : futureCourses);

        return courses;
    }

    private static List<Course> filterDuplicateCourses(List<Course> toFilter, List<Course> toCheck) {
        return toFilter.stream()
                .filter(c1 -> toCheck.stream()
                        .noneMatch(c2 -> Objects.equals(c1.getId(), c2.getId())))
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private static void setCoursesRegistersInfos(List<Course> courses, JsonObject registers, boolean isSplitSlot) {
        for (Course course : courses) {
            course.setSplitSlot(isSplitSlot);

            if (registers.getJsonArray(course.getId()) == null) {
                continue;
            }

            List<JsonObject> courseRegisters = registers.getJsonArray(course.getId()).getList();

            for (JsonObject register : courseRegisters) {
                try {
                    if ((DateHelper.getAbsTimeDiff(course.getStartDate(), register.getString(Field.START_DATE))
                            < DateHelper.TOLERANCE
                            && DateHelper.getAbsTimeDiff(course.getEndDate(), register.getString(Field.END_DATE))
                            < DateHelper.TOLERANCE)
                            || isMatchRegisterCourse(course, register)) {
                        course.setRegisterId(register.getInteger(Field.ID));
                        course.setRegisterStateId(register.getInteger(Field.STATE_ID));
                        course.setNotified(register.getBoolean(Field.NOTIFIED));
                        break;
                    } else {
                        course.setNotified(false);
                    }
                } catch (ParseException err) {
                    LOGGER.error("[Presences@SquashHelper::setCoursesRegistersInfos] Failed to parse date for " +
                            "register " + register.getInteger(Field.ID), err);
                }
            }
        }
    }

    /**
     * Function that checks if course date and time matches with register date and time
     * we also check their course id
     *
     * @param course   current course
     * @param register current register
     * @return boolean if this matches
     */
    private static boolean isMatchRegisterCourse(Course course, JsonObject register) {
        boolean isMatch = false;
        try {
            Date courseStartDate = DateHelper.parse(course.getStartDate());
            Date courseEndDate = DateHelper.parse(course.getEndDate());
            Date registerStartDate = DateHelper.parse(register.getString("start_date"), DateHelper.SQL_FORMAT);
            Date registerEndDate = DateHelper.parse(register.getString("end_date"), DateHelper.SQL_FORMAT);
            if ((courseStartDate.equals(registerStartDate) && courseEndDate.equals(registerEndDate))
                    && course.getId().equals(register.getString("course_id"))) {
                return true;
            }
        } catch (ParseException e) {
            LOGGER.error("[Presences@SquashHelper] Failed to parse date for matching register and course ", e);
        }
        return isMatch;
    }

    /**
     * Format registers by course identifier
     *
     * @param registers Registers list
     * @return Json object containing each registers grouped by course identifier
     */
    private static JsonObject groupRegisters(JsonArray registers) {
        JsonObject values = new JsonObject();
        JsonObject register, o;
        for (int i = 0; i < registers.size(); i++) {
            register = registers.getJsonObject(i);
            if (!values.containsKey(register.getString("course_id"))) {
                values.put(register.getString("course_id"), new JsonArray());
            }

            o = new JsonObject()
                    .put("id", register.getInteger("id"))
                    .put("start_date", register.getString("start_date"))
                    .put("course_id", register.getString("course_id"))
                    .put("end_date", register.getString("end_date"))
                    .put("state_id", register.getInteger("state_id"))
                    .put("notified", register.getBoolean("notified"))
                    .put("split_slot", register.getBoolean("split_slot"));
            values.getJsonArray(register.getString("course_id")).add(o);
        }

        return values;
    }

}
