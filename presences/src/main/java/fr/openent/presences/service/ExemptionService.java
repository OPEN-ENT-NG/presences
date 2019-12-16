package fr.openent.presences.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;

public interface ExemptionService {

    /**
     * Fetch exemptions
     *
     * @param structure_id structure identifier structure identifier
     * @param start_date   start date
     * @param end_date     start date
     * @param student_ids  student identifier (optionnal can be null)
     * @param handler      function handler returning da
     */
    void get(String structure_id, String start_date, String end_date, List<String> student_ids, String page,
             String field, boolean reverse, Handler<Either<String, JsonArray>> handler);

    /**
     * Retrieve user exemptions
     *
     * @param structure_id Structure identifier
     * @param start_date   Start date
     * @param end_date     End date
     * @param userId       user identifier
     * @param page         Page number. Can be null
     * @param handler      Function handler returning data
     */
    void get(String structure_id, String start_date, String end_date, String userId, String page, Handler<Either<String, JsonArray>> handler);

    /**
     * Get exemptions count
     *
     * @param structure_id
     * @param start_date
     * @param end_date
     * @param student_ids
     * @param handler
     */
    void getPageNumber(String structure_id, String start_date, String end_date, List<String> student_ids, String order,
                       boolean reverse, Handler<Either<String, JsonObject>> handler);

    /**
     * Create exemptions for a list of students
     *
     * @param structure_id structure identifier
     * @param student_ids  student identifier
     * @param subject_id   subject identifier
     * @param start_date   start date
     * @param end_date     start date
     * @param attendance   attendance bool
     * @param comment      comment
     * @param handler      function handler returning da
     */
    void create(String structure_id, JsonArray student_ids, String subject_id, String start_date, String end_date, Boolean attendance, String comment, Handler<Either<String, JsonArray>> handler);

    /**
     * Update an exemption
     *
     * @param exemption_id
     * @param student_ids  student identifier
     * @param subject_id   subject identifier
     * @param start_date   start date
     * @param end_date     start date
     * @param attendance   attendance bool
     * @param comment      comment
     * @param handler      function handler returning da
     */
    void update(String exemption_id, String structure_id, String student_ids, String subject_id, String start_date, String end_date, Boolean attendance, String comment, Handler<Either<String, JsonObject>> handler);

    /**
     * Delete exemptions from a list
     *
     * @param exemption_ids exemption identifier
     * @param handler       function handler returning da
     */
    void delete(List<String> exemption_ids, Handler<Either<String, JsonArray>> handler);


    /**
     * Retrieve students exemptions
     *
     * @param studentList  Student list
     * @param structure_id Students structure id
     * @param start_date   Search start date
     * @param end_date     Search end date
     * @param handler      Function handler returning data
     */
    void getRegisterExemptions(List<String> studentList, String structure_id, String start_date, String end_date, Handler<Either<String, JsonArray>> handler);
}