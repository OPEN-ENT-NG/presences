package fr.openent.presences.service;

import fr.openent.presences.model.*;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;

public interface CourseService {

    /**
     * Get given course
     *
     * @param courseId course identifier
     * @param handler  FUnction handler returning data
     */
    void getCourse(String courseId, Handler<Either<String, JsonObject>> handler);

    /**
     * List courses
     *
     * @param structureId     Structure identifier
     * @param teachersList    Teachers list identifiers
     * @param groupsList      Groups list identifiers
     * @param start           Start date
     * @param end             End date
     * @param forgottenFilter forgottenFilter
     * @param multipleSlot    allow split courses
     * @param handler         Function handler returning data
     */
    void listCourses(String structureId, List<String> teachersList, List<String> groupsList,
                     String start, String end, String startTime, String endTime, boolean forgottenFilter,
                     MultipleSlotSettings multipleSlot, Handler<Either<String, JsonArray>> handler);

    void listCourses(String structureId, List<String> teachersList, List<String> groupsList,
                     String start, String end, String startTime, String endTime, boolean forgottenFilter,
                     MultipleSlotSettings multipleSlot, String limit, String offset, String descendingDate,
                     Handler<Either<String, JsonArray>> handler);

    void listCourses(String structureId, List<String> teachersList, List<String> groupsList,
                     String start, String end, String startTime, String endTime, boolean forgottenFilter,
                     MultipleSlotSettings multipleSlot, String limit, String offset, String descendingDate,
                     String isWithTeacherFilter, Handler<Either<String, JsonArray>> handler);
}
