package fr.openent.presences.service;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import org.entcore.common.user.UserInfos;

import java.util.*;

public interface StatementAbsenceService {

    /**
     * get absence statements
     *
     * @param user              current user logged
     * @param body              data to filter
     * @param studentIds        list of student ids
     * @param handler           Function handler returning data
     */
    void get(UserInfos user, MultiMap body, List<String> studentIds, Handler<AsyncResult<JsonObject>> handler);

    /**
     * create absence statement
     *
     * @param body               data to store
     * @param handler            Function handler returning data
     */
    void create(JsonObject body, HttpServerRequest request, Handler<AsyncResult<JsonObject>> handler);

    /**
     * validate absence statement
     *
     * @param user               current user logged
     * @param body               data to update
     * @param handler            Function handler returning data
     */
    void validate(UserInfos user, JsonObject body, Handler<AsyncResult<JsonObject>> handler);


    /**
     * validate absence statement
     *
     * @param user               current user logged
     * @param body               data to update
     * @param handler            Function handler returning data
     */
    void getFile(UserInfos user, MultiMap body, Handler<AsyncResult<JsonObject>> handler);

}
