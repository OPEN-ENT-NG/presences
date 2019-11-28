package fr.openent.presences.helper;

import fr.openent.presences.common.helper.DateHelper;
import fr.openent.presences.model.Course;
import fr.openent.presences.service.RegisterService;
import fr.openent.presences.service.impl.DefaultRegisterService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
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
    private RegisterService registerService;

    public SquashHelper(EventBus eb) {
        this.registerService = new DefaultRegisterService(eb);
    }
    /**
     * Squash courses with registers. Each course will be squashed with its register.
     *
     * @param structureId           Structure identifier
     * @param start                 Start period date
     * @param end                   End period date
     * @param coursesEvent          Course list
     * @param splitCoursesEvent     Course list split
     * @param handler               Function handler returning data
     */
    public void squash(String structureId, String start, String end,
                       List<Course> coursesEvent, List<Course> splitCoursesEvent,
                       Handler<Either<String, List<Course>>> handler) {
        registerService.list(structureId, start, end, registerEvent -> {
            if (registerEvent.isLeft()) {
                String message = "[Presences@SquashHelper] Failed to retrieve registers";
                LOGGER.error(message);
                handler.handle(new Either.Left<>(message));
                return;
            }

            List<Course> courses = new ArrayList<>();
            for (Course course : coursesEvent) {
                course.setSplitSlot(false);
            }
            for (Course course : splitCoursesEvent) {
                course.setSplitSlot(true);
            }
            courses.addAll(coursesEvent);
            courses.addAll(splitCoursesEvent);
            JsonObject registers = groupRegisters(registerEvent.right().getValue());
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
                        if ((DateHelper.getAbsTimeDiff(course.getStartDate(), register.getString("start_date")) < DateHelper.TOLERANCE
                                && DateHelper.getAbsTimeDiff(course.getEndDate(), register.getString("end_date")) < DateHelper.TOLERANCE)
                                || isMatchRegisterCourse(course, register)) {
                            course.setRegisterId(register.getInteger("id"));
                            course.setRegisterStateId(register.getInteger("state_id"));
                            course.setNotified(register.getBoolean("notified"));
                            found = true;
                        } else {
                            course.setNotified(false);
                        }
                    } catch (ParseException err) {
                        LOGGER.error("[Presences@SquashHelper] Failed to parse date for register " + register.getInteger("id"), err);
                    } finally {
                        j++;
                    }
                }
            }

            handler.handle(new Either.Right<>(courses));
        });
    }

    /**
     * Function that checks all courses who have the same id as the current course
     * and also verify if one of the courses with same id has a split slot set true
     *
     * @param courses list of register
     * @param course  current course
     * @return true if there are courses with same id than the current course having split slot set true
     */
    private boolean checkSplitSlot(List<Course> courses, Course course) {
        return courses.stream().anyMatch(item ->
                item.getId().equals(course.getId()) && item.isSplitSlot()
        );
    }

    /**
     * Function that checks if course date and time matches with register date and time
     * we also check their course id
     *
     * @param course   current course
     * @param register current register
     * @return boolean if this matches
     */
    private boolean isMatchRegisterCourse(Course course, JsonObject register) {
        boolean isMatch = false;
        try {
            Date courseStartDate = DateHelper.parse(course.getStartDate(), DateHelper.MONGO_FORMAT);
            Date courseEndDate = DateHelper.parse(course.getEndDate(), DateHelper.MONGO_FORMAT);
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
    private JsonObject groupRegisters(JsonArray registers) {
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
