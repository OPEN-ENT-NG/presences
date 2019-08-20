package fr.openent.presences.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.user.UserInfos;

public interface AbsenceService {

    /**
     * get absences
     *
     * @param handler       Function handler returning data
     */
    void get(Handler<Either<String, JsonArray>> handler);

    /**
     * create absence
     *
     * @param absenceBody   absenceBody fetched
     * @param user          userInfo
     * @param handler       Function handler returning data
     */
    void create(JsonObject absenceBody, UserInfos user, Handler<Either<String, JsonArray>> handler);

    /**
     * CRON task that automatically removes absence that have expired 3 days (72 hours) before date.now()
     *
     * @param handler       Function handler returning data
     */
    void absenceRemovalTask(Handler<Either<String, JsonObject>> handler);
}
