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


        courses.addAll(createdSplitCourses);
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

        // Add split courses if part of the course has an opened register
        futureCourses.addAll(futureSplitCourses.stream()
                .filter(fSplitCourse -> courses.stream()
                        .anyMatch(course -> Objects.equals(course.getId(), fSplitCourse.getId())
                                && course.getRegisterId() != null))
                .collect(Collectors.toList()));

        courses.addAll(Boolean.TRUE.equals(multipleSlot) ? futureSplitCourses : futureCourses);

        return courses;
    }

    private static List<Course> filterDuplicateCourses(List<Course> toFilter, List<Course> toCheck) {
        return toFilter.stream()
                .filter(c1 -> toCheck.stream()
                        .noneMatch(c2 -> Objects.equals(c1.getId(), c2.getId())
                                && (Objects.equals(c1.getStartDate(), c2.getStartDate())
                                    && Objects.equals(c1.getEndDate(), c2.getEndDate()))))
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
                    course.setIsOpenedByPersonnel(register.getBoolean(Field.ISOPENEDBYPERSONNEL, false));
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
    @SuppressWarnings("unchecked")
    private static JsonObject groupRegisters(JsonArray registers) {
        JsonObject values = new JsonObject();

        ((List<JsonObject>) registers.getList()).forEach(register -> {
            if (!values.containsKey(register.getString(Field.COURSE_ID))) {
                values.put(register.getString(Field.COURSE_ID), new JsonArray());
            }

            JsonObject r = new JsonObject()
                    .put(Field.ID, register.getInteger(Field.ID))
                    .put(Field.START_DATE, register.getString(Field.START_DATE))
                    .put(Field.COURSE_ID, register.getString(Field.COURSE_ID))
                    .put(Field.END_DATE, register.getString(Field.END_DATE))
                    .put(Field.STATE_ID, register.getInteger(Field.STATE_ID))
                    .put(Field.NOTIFIED, register.getBoolean(Field.NOTIFIED))
                    .put(Field.SPLIT_SLOT, register.getBoolean(Field.SPLIT_SLOT))
                    .put(Field.ISOPENEDBYPERSONNEL, register.getBoolean(Field.IS_OPENED_BY_PERSONNEL));
            values.getJsonArray(register.getString(Field.COURSE_ID)).add(r);
        });

        return values;
    }

}
