package fr.openent.presences.service;

import fr.openent.presences.model.*;
import fr.wseduc.webutils.Either;
import io.vertx.core.*;
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
                     boolean multipleSlot, Handler<Either<String, JsonArray>> handler);

    void listCourses(String structureId, List<String> teachersList, List<String> groupsList,
                     String start, String end, String startTime, String endTime, boolean forgottenFilter,
                     boolean multipleSlot, String limit, String offset, String descendingDate,
                     Handler<Either<String, JsonArray>> handler);

    void listCourses(String structureId, List<String> teachersList, List<String> groupsList,
                     String start, String end, String startTime, String endTime, boolean forgottenFilter,
                     boolean multipleSlot, String limit, String offset, String descendingDate,
                     String isWithTeacherFilter, Handler<Either<String, JsonArray>> handler);

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
                     String start, String end, String startTime, String endTime,
                     boolean forgottenFilter, boolean multipleSlot,
                     String limit, String offset, String descendingDate,
                     String searchTeacher, String crossDateFilter, Handler<Either<String, JsonArray>> handler);

    /**
     * List registers after fetching courses
     * @param structureId       structure identifier
     * @param teacherIds        {@link List} of teacher identifiers
     * @param groupsList        {@link List} of group names
     * @param start             start date filter (format YYY-MM-DD)
     * @param end               end date filter (format YYY-MM-DD)
     * @param startTime         start time filter (optional, format HH:mm)
     * @param endTime           end time filter (optional, format HH:mm)
     * @param multipleSlot      multiple slot filter
     * @param limit             limit of courses
     * @param offset            offset to get courses
     * @param descendingDate    true -> order courses from most recent to the oldest;
     *                          false -> order courses from oldest to most recent
     * @param searchTeacher     true -> fetch courses with teachers;
     *                          false -> fetch courses without teachers
     * @param crossDateFilter   cross date filter (true : get registers beginning < start date and finishing end date)
     * @return                  {@link Future} of {@link List}
     */
    Future<JsonArray> listRegistersWithCourses(String structureId, List<String> teacherIds, List<String> groupsList, String start,
                                               String end, String startTime, String endTime, boolean multipleSlot, String limit,
                                               String offset, String descendingDate, String crossDateFilter, String searchTeacher);

    /**
     * List forgotten registers squashed with corresponding courses
     * @param structureId           structure identifier
     * @param teacherIds            {@link List} of teacher identifiers
     * @param groupsList            {@link List} of group names
     * @param startDate             start date filter (format YYY-MM-DD)
     * @param endDate               end date filter (format YYY-MM-DD)
     * @param multipleSlot          multiple slot filter
     * @param limit                 limit of courses
     * @param offset                offset to get courses
     * @param isWithTeacherFilter   true -> fetch courses with teachers;
     *                              false -> fetch courses without teachers
     * @return                      {@link Future} of {@link List}
     */
    Future<JsonArray> listCoursesWithForgottenRegisters(String structureId, List<String> teacherIds, List<String> groupsList, String startDate,
                                                        String endDate, boolean multipleSlot,
                                                        String limit, String offset, String isWithTeacherFilter);

    /**
     * Fetch list of courses ids with no teachers
     * @param structureId       structure identifier
     * @param startDate         start date filter
     * @param endDate           end date filter
     * @return {@link Future} of {@link List}
     */
    Future<List<String>> getCourseIdsWithoutTeacher(String structureId, String startDate, String endDate);
}
