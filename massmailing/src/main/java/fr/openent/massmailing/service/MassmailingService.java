package fr.openent.massmailing.service;

import fr.openent.massmailing.enums.MassmailingType;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
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
    void getStatus(String structure, MassmailingType type, boolean massmailed, List<Integer> reasons, Integer startAt,
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
}
