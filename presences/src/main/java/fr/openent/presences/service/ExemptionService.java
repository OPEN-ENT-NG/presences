package fr.openent.presences.service;

import fr.openent.presences.model.Exemption.ExemptionBody;
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
     * @param structure_id structure identifier
     * @param start_date   start date
     * @param end_date     end date
     * @param student_ids  students identifier
     * @param handler      handler data
     */
    void getPageNumber(String structure_id, String start_date, String end_date, List<String> student_ids, String order,
                       boolean reverse, Handler<Either<String, JsonObject>> handler);


    /**
     * Retrieve students exemptions
     *
     * @param studentList  Student list
     * @param structure_id Students structure id
     * @param start_date   Search start date
     * @param end_date     Search end date
     * @param handler      Function handler returning data
     */
    void getRegisterExemptions(List<String> studentList, String structure_id, String start_date,
                               String end_date, Handler<Either<String, JsonArray>> handler);

    /**
     * Create exemptions for a list of students (could be punctual or recursive)
     *
     * @param exemptionBody exemptionBody ExemptionBody requested and expected
     * @param handler       function handler returning data
     */
    void create(ExemptionBody exemptionBody, Handler<Either<String, JsonArray>> handler);

    /**
     * Update an exemption for a list of students (could be punctual or recursive)
     *
     * @param id            exemption or exemption_recursive identifier
     * @param exemptionBody exemptionBody ExemptionBody requested and expected
     * @param handler       function handler returning data
     */
    void update(Integer id, ExemptionBody exemptionBody, Handler<Either<String, JsonObject>> handler);

    /**
     * Delete exemptions from a list
     *
     * @param exemption_ids exemption identifier
     * @param handler       function handler returning data
     */
    void delete(List<String> exemption_ids, Handler<Either<String, JsonArray>> handler);

    /**
     * Delete recursive emptions from a list
     *
     * @param recursive_exemption_ids recursive exemption identifier
     * @param handler                 function handler returning data
     */
    void deleteRecursive(List<String> recursive_exemption_ids, Handler<Either<String, JsonArray>> handler);
}