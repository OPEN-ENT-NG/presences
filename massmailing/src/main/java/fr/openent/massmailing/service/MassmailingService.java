package fr.openent.massmailing.service;

import fr.openent.massmailing.enums.MailingType;
import fr.openent.massmailing.enums.MassmailingType;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;

public interface MassmailingService {
    /**
     * Get mass mailing status.
     *
     * @param structure  Structure identifier
     * @param type       Mass mailing type
     * @param massmailed massmailed status.
     * @param reasons    Reason list. Depends of structure parameter.
     * @param startAt    "Start at" number. Define the "start at" number event mass mailing is retrieved.
     * @param startDate  Start date. Define the start date range
     * @param endDate    End date. Define the end date range.
     * @param students   Student list
     * @param handler    Function handler returning data
     */
    void getStatus(String structure, MassmailingType type, Boolean massmailed, List<Integer> reasons, Integer startAt,
                   String startDate, String endDate, List<String> students, Handler<Either<String, JsonObject>> handler);

    /**
     * Get mass mailing status.
     *
     * @param structure  Structure identifier
     * @param type       Mass mailing type
     * @param massmailed massmailed status.
     * @param reasons    Reason list. Depends of structure parameter.
     * @param startAt    "Start at" number. Define the "start at" number event mass mailing is retrieved.
     * @param startDate  Start date. Define the start date range
     * @param endDate    End date. Define the end date range.
     * @param handler    Function handler returning data
     */
    void getStatus(String structure, MassmailingType type, boolean massmailed, List<Integer> reasons, Integer startAt,
                   String startDate, String endDate, Handler<Either<String, JsonObject>> handler);

    /**
     * Retrieve count event by students
     *
     * @param structure  Structure identifier
     * @param type       Mass mailing type
     * @param massmailed Massmailed status
     * @param reasons    Reason list. Depends of structure parameter
     * @param startAt    "Start at" number. Define the "start at" number event mass mailing is retrieved.
     * @param startDate  Start date. Define the start date range
     * @param endDate    End date. Define the end date range.
     * @param students   Student list
     * @param handler    Function handler returning data
     */
    void getCountEventByStudent(String structure, MassmailingType type, Boolean massmailed, List<Integer> reasons, Integer startAt, String startDate, String endDate, List<String> students, Handler<Either<String, JsonArray>> handler);

    /**
     * Retrieve mass mailing anomalies based on given students list
     *
     * @param type     Mailing type
     * @param students Student list
     * @param handler  Function handler returning data
     */
    void getAnomalies(MailingType type, List<String> students, Handler<Either<String, JsonArray>> handler);
}
