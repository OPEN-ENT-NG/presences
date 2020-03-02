package fr.openent.massmailing.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;

public interface MailingService {

    /**
     * Get mailings
     *
     * @param structureId  Structure identifier
     * @param startDate    start date
     * @param endDate      end date
     * @param mailingTypes mailing type (can have several)
     * @param eventTypes   event type (can have several)
     * @param studentsIds  student identifier (can have several)
     * @param page         page number
     * @param handler      Function handler returning data
     */
    void getMailings(String structureId, String startDate, String endDate, List<String> mailingTypes, List<String> eventTypes,
                     List<String> studentsIds, Integer page, Handler<Either<String, JsonArray>> handler);

    /**
     * Get mailings page number
     *
     * @param structureId   Structure identifier
     * @param startDate     start date
     * @param endDate       end date
     * @param mailingTypes  mailing type (can have several)
     * @param eventTypes    event type (can have several)
     * @param studentsIds   student identifier (can have several)
     * @param handler       Function handler returning data
     */
    void getMailingsPageNumber(String structureId, String startDate, String endDate, List<String> mailingTypes,
                               List<String> eventTypes, List<String> studentsIds, Handler<Either<String, JsonObject>> handler);
}
