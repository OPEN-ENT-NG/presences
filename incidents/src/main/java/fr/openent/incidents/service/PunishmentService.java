package fr.openent.incidents.service;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.user.UserInfos;

public interface PunishmentService {

    /**
     * get punishment
     *
     * @param user              current user logged
     * @param body              data to filter
     * @param isStudent         if user is a student (and need to be filtered about that).
     * @param handler           Function handler returning data
     */
    void get(UserInfos user, MultiMap body, boolean isStudent, Handler<AsyncResult<JsonObject>> handler);

    /**
     * get punishment
     *
     * @param user              current user logged
     * @param body              data to filter
     * @param isStudent         if user is a student (and need to be filtered about that).
     * @param handler           Function handler returning number of punishments
     */
    void count(UserInfos user, MultiMap body, boolean isStudent, Handler<AsyncResult<Long>> handler);

    /**
     * create punishment
     *
     * @param user               current user logged
     * @param body               data to store
     * @param handler            Function handler returning data
     */
    void create(UserInfos user, JsonObject body, Handler<AsyncResult<JsonArray>> handler);

    /**
     * put punishment
     *
     * @param user               current user logged
     * @param body               data to update
     * @param handler            Function handler returning data
     */
    void update(UserInfos user, JsonObject body, Handler<AsyncResult<JsonObject>> handler);

    /**
     * delete punishment
     *
     * @param body             data containing id to delete
     * @param handler          Function handler returning data
     */
    void delete(UserInfos user, MultiMap body, Handler<AsyncResult<JsonObject>> handler);
}
