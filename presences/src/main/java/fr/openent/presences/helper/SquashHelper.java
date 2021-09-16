package fr.openent.presences.helper;

import fr.openent.presences.common.helper.DateHelper;
import fr.openent.presences.core.constants.*;
import fr.openent.presences.model.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
                                      JsonArray registerEvent, MultipleSlotSettings multipleSlot) {
        for (Course course : coursesEvent) {
            course.setSplitSlot(false);
        }
        for (Course course : splitCoursesEvent) {
            course.setSplitSlot(true);
        }
        List<Course> courses = new ArrayList<>(coursesEvent);
        JsonObject registers = groupRegisters(registerEvent);


        if (Boolean.TRUE.equals(multipleSlot.getUserValue())
                && Boolean.TRUE.equals(multipleSlot.getStructureValue())) {
            courses.addAll(splitCoursesEvent);
        } else if (Boolean.TRUE.equals(multipleSlot.getStructureValue())
                && Boolean.FALSE.equals(multipleSlot.getUserValue())) {
            for (Course course : splitCoursesEvent) {
                JsonArray courseRegisters = registers.getJsonArray(course.getId());
                if ((courseRegisters != null) && courseRegisters.stream()
                        .anyMatch(r -> ((JsonObject) r).getInteger(Field.ID) != null)) {
                    courses.add(course);
                }
            }

        }

        for (Course course : courses) {
            boolean found = false;
            int j = 0;
            JsonArray courseRegisters = registers.getJsonArray(course.getId());
            if (courseRegisters == null) {
                continue;
            }
            while (!found && j < courseRegisters.size()) {
                JsonObject register = courseRegisters.getJsonObject(j);
                try {
                    if ((DateHelper.getAbsTimeDiff(course.getStartDate(), register.getString(Field.START_DATE)) < DateHelper.TOLERANCE
                            && DateHelper.getAbsTimeDiff(course.getEndDate(), register.getString(Field.END_DATE)) < DateHelper.TOLERANCE)
                            || isMatchRegisterCourse(course, register)) {
                        course.setRegisterId(register.getInteger(Field.ID));
                        course.setRegisterStateId(register.getInteger(Field.STATE_ID));
                        course.setNotified(register.getBoolean(Field.NOTIFIED));
                        found = true;
                    } else {
                        course.setNotified(false);
                    }
                } catch (ParseException err) {
                    LOGGER.error("[Presences@SquashHelper::squash] Failed to parse date for register " + register.getInteger("id"), err);
                } finally {
                    j++;
                }
            }
        }
        return courses;
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
