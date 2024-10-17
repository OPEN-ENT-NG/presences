package fr.openent.presences.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;

public interface NotebookService {

    /**
     * Retrieve forgotten notebook based on student_id and date since filter is possible
     *
     * @param studentId student identifier
     * @param startDate beginning date to filter. Format is : YYYY-DD-MM
     * @param endDate   end date to filter. Format is : YYYY-DD-MM
     * @param handler   Function handler returning data. Returns a JsonArray
     */
    void get(String studentId, String startDate, String endDate, Handler<Either<String, JsonArray>> handler);

    /**
     * Retrieve forgotten notebook from student based on student_id and date since filter is possible
     *
     * @param studentId   student identifier
     * @param startDate   beginning date to filter. Format is : YYYY-DD-MM
     * @param endDate     end date to filter. Format is : YYYY-DD-MM
     * @param limit       limit of occurrences.
     * @param offset      offset to get occurrences.
     * @param structureId structure identifier
     * @return future returning data. Returns a JsonArray
     */
    Future<JsonObject> studentGet(String studentId, String startDate, String endDate, String limit, String offset, String structureId);

    Future<JsonObject> studentsGet(String structureId, List<String> studentIds, String startDate, String endDate, String limit, String offset);

    /**
     * Retrieve forgotten notebook based on a list of student_id and date
     *
     * @param studentIds student identifier
     * @param startDate  beginning date to filter. Format is : YYYY-DD-MM
     * @param endDate    end date to filter. Format is : YYYY-DD-MM
     * @param handler    Function handler returning data. Returns a JsonArray
     */
    void get(List<String> studentIds, String startDate, String endDate, Handler<Either<String, JsonArray>> handler);

    /**
     * create a forgotten notebook based on the body containing student_id, structure_id and
     * date Format is : YYYY-DD-MM
     *
     * @param notebookBody notebookBody fetched
     * @param handler      Function handler returning data
     */
    void create(JsonObject notebookBody, Handler<Either<String, JsonObject>> handler);

    /**
     * update a forgotten notebook based on the notebookId param and its body containing date. Format is : YYYY-DD-MM
     *
     * @param notebookId   notebook identifier used to update its notebook
     * @param notebookBody notebookBody fetched
     * @param handler      Function handler returning data
     */
    void update(Integer notebookId, JsonObject notebookBody, Handler<Either<String, JsonObject>> handler);

    /**
     * delete notebook
     *
     * @param notebookId notebook identifier used to delete notebook
     * @param handler    Function handler returning data
     */
    void delete(Integer notebookId, Handler<Either<String, JsonObject>> handler);
}
