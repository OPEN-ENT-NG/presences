package fr.openent.presences.service.impl;

import fr.openent.presences.Presences;
import fr.openent.presences.common.helper.WorkflowHelper;
import fr.openent.presences.common.service.UserService;
import fr.openent.presences.common.service.impl.DefaultUserService;
import fr.openent.presences.core.constants.*;
import fr.openent.presences.enums.WorkflowActions;
import fr.openent.presences.model.StatementAbsence;
import fr.openent.presences.service.StatementAbsenceService;
import io.vertx.core.*;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.storage.Storage;
import org.entcore.common.user.UserInfos;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DefaultStatementAbsenceService implements StatementAbsenceService {

    private static final Logger log = LoggerFactory.getLogger(DefaultStatementAbsenceService.class);

    private Storage storage;
    private UserService userService;

    public DefaultStatementAbsenceService(Storage storage) {
        this.storage = storage;
        this.userService = new DefaultUserService();
    }

    @Override
    public void get(UserInfos user, MultiMap body, Handler<AsyncResult<JsonObject>> handler) {
        String structure_id = body.get("structure_id");
        String id = body.get("id");
        String end_at = body.get("end_at");
        String start_at = body.get("start_at");
        List<String> student_ids = body.getAll("student_id");
        Boolean is_treated = body.get("is_treated") != null ? Boolean.valueOf(body.get("is_treated")) : null;
        Integer page = body.get("page") != null ? Integer.parseInt(body.get("page")) : null;
        String limit = body.get("limit");
        String offset = body.get("offset");

        Future<JsonArray> listResultsFuture = Future.future();
        Future<Long> countResultsFuture = Future.future();

        getRequest(page, limit, offset, structure_id, id, start_at, end_at, student_ids, is_treated, listResultsFuture);
        countRequest(structure_id, id, start_at, end_at, student_ids, is_treated, countResultsFuture);

        CompositeFuture.all(listResultsFuture, countResultsFuture).setHandler(eventResult -> {
            if (eventResult.failed()) {
                handler.handle(Future.failedFuture(eventResult.cause().getMessage()));
                return;
            }

            JsonObject result = new JsonObject()
                    .put("all", listResultsFuture.result())
                    .put("page_count", countResultsFuture.result());

            if(page != null) {
                result.put("page", page);
            } else {
                if (limit != null) {
                    result.put("limit", limit);
                }
                if (offset != null) {
                    result.put("offset", offset);
                }
            }

            handler.handle(Future.succeededFuture(result));
        });
    }

    @Override
    public void create(JsonObject body, HttpServerRequest request, Handler<AsyncResult<JsonObject>> handler) {
        StatementAbsence statementAbsence = new StatementAbsence();
        statementAbsence.setFromJson(body);
        statementAbsence.create(handler);
    }

    @Override
    public void validate(UserInfos user, JsonObject body, Handler<AsyncResult<JsonObject>> handler) {
        StatementAbsence statementAbsence = new StatementAbsence();
        body.put("treated_at", body.getBoolean("is_treated") ? new Date().toString() : null);
        body.put("validator_id", user.getUserId());
        statementAbsence.setFromJson(body);
        statementAbsence.update(handler);
    }

    @Override
    public void getFile(UserInfos user, MultiMap body, Handler<AsyncResult<JsonObject>> handler) {
        List<String> studentIds = null;
        if (!WorkflowHelper.hasRight(user, WorkflowActions.ABSENCE_STATEMENTS_VIEW.toString())) {
            studentIds = body.getAll(Field.STUDENT_ID);
            boolean isValid = !studentIds.isEmpty();
            for (String id : studentIds) {
                if (!(user.getUserId().equals(id) || user.getChildrenIds().contains(id))) isValid = false;
            }
            if (!isValid) {
                String message = String.format("[Presences@%s:getFile] You are not authorized to get this file.",
                        this.getClass().getSimpleName());
                log.error(message);
                handler.handle(Future.failedFuture(message));
                return;
            }
        }

        getRequest(null, null, null,body.get(Field.STRUCTURE_ID), body.get(Field.IDSTATEMENT),
                null, null, studentIds, null, result -> {
            if (result.failed()) {
                String message = String.format("[Presences@%s:getFile] Failed to retrieve absence statements.",
                        this.getClass().getSimpleName());
                log.error(message + " " + result.cause().getMessage());
                handler.handle(Future.failedFuture(message));
                return;
            }

            if (result.result().size() != 1) {
                String message = String.format("[Presences@%s:getFile] You are not authorized to get this file.",
                        this.getClass().getSimpleName());
                log.error(message);
                handler.handle(Future.failedFuture(message));
                return;
            }

            handler.handle(Future.succeededFuture(result.result().getJsonObject(0)));
        });
    }


    private String queryGetter(Boolean isCountQuery, Integer page, String limit, String offset, String structure_id, String id, String start_at, String end_at,
                               List<String> student_ids, Boolean is_treated, JsonArray params) {
        String query = (isCountQuery ? "SELECT COUNT(*) " : "SELECT * ") + " FROM " + Presences.dbSchema + ".statement_absence WHERE structure_id = ? ";

        params.add(structure_id);

        if (id != null) {
            query += "AND id = ? ";
            params.add(id);
        }

        if (end_at != null) {
            query += "AND start_at <= ? ";
            params.add(end_at);
        }

        if (start_at != null) {
            query += "AND end_at >= ? ";
            params.add(start_at);
        }

        if (student_ids != null && student_ids.size() > 0) {
            query += "AND student_id IN " + Sql.listPrepared(student_ids) + " ";
            params.addAll(new JsonArray(student_ids));
        }

        if (is_treated != null) {
            query += "AND treated_at " + (is_treated ? "IS NOT NULL " : "IS NULL ");
        }

        if (!isCountQuery) query += "ORDER BY created_at DESC ";

        if (page != null) {
            query += "LIMIT " + Presences.PAGE_SIZE + " ";
            query += "OFFSET " + page * Presences.PAGE_SIZE + " ";
        } else {
            if (limit != null) {
                query += "LIMIT ? ";
                params.add(limit);
            }

            if (offset != null) {
                query += "OFFSET ? ";
                params.add(offset);
            }
        }

        return query;
    }


    private void getRequest(Integer page, String limit, String offset, String structure_id, String id, String start_at, String end_at,
                            List<String> student_ids, Boolean is_treated, Handler<AsyncResult<JsonArray>> handler) {
        JsonArray params = new JsonArray();
        Sql.getInstance().prepared(queryGetter(false, page, limit, offset, structure_id, id, start_at, end_at, student_ids, is_treated, params),
                params, SqlResult.validResultHandler(eventResult -> {
                    if (eventResult.isLeft()) {
                        String message = "[Presences@DefaultStatementAbsenceService:getRequest] Failed to retrieve absence statements.";
                        log.error(message + " " + eventResult.left().getValue());
                        handler.handle(Future.failedFuture(message));
                        return;
                    }

                    setStudents(eventResult.right().getValue(), result -> {
                        if (result.failed()) {
                            handler.handle(Future.failedFuture(result.cause().getMessage()));
                            return;
                        }
                        handler.handle(Future.succeededFuture(result.result()));
                    });
                }));
    }

    private void getRequest(Integer page, String limit, String offset, String structure_id, String id, String start_at, String end_at,
                            List<String> student_ids, Boolean is_treated, Future<JsonArray> future) {
        JsonArray params = new JsonArray();
        Sql.getInstance().prepared(queryGetter(false, page, limit, offset, structure_id, id, start_at, end_at, student_ids, is_treated, params),
                params, SqlResult.validResultHandler(eventResult -> {
                    if (eventResult.isLeft()) {
                        String message = "[Presences@DefaultStatementAbsenceService:getRequest] Failed to retrieve absence statements.";
                        log.error(message + " " + eventResult.left().getValue());
                        future.fail(message);
                        return;
                    }
                    setStudents(eventResult.right().getValue(), result -> {
                        if (result.failed()) {
                            future.fail(result.cause().getMessage());
                            return;
                        }
                        future.complete(result.result());
                    });

                }));
    }

    private void countRequest(String structure_id, String id, String start_at, String end_at,
                              List<String> student_ids, Boolean is_treated, Future<Long> future) {
        JsonArray params = new JsonArray();
        Sql.getInstance().prepared(queryGetter(true, null, null, null, structure_id, id, start_at, end_at, student_ids, is_treated, params),
                params, SqlResult.validUniqueResultHandler(eventResult -> {
                    if (eventResult.isLeft()) {
                        String message = "[Presences@DefaultStatementAbsenceService:countRequest] Failed to count absence statements.";
                        log.error(message + " " + eventResult.left().getValue());
                        future.fail(message);
                        return;
                    }

                    Long count = eventResult.right().getValue().getLong("count");
                    Long countPageNumber = count / Presences.PAGE_SIZE;
                    if (count % 20 == 0) countPageNumber--;

                    future.complete(countPageNumber);
                }));
    }

    private void setStudents(JsonArray dataList, Handler<AsyncResult<JsonArray>> handler) {
        List<String> studentIds = ((List<JsonObject>) dataList.getList())
                .stream()
                .map(res -> res.getString("student_id"))
                .collect(Collectors.toList());

        userService.getStudents(studentIds, resUsers -> {
            if (resUsers.isLeft()) {
                String message = "[Presences@DefaultStatementAbsenceService::get] Failed to get students";
                log.error(message);
                handler.handle(Future.failedFuture(message));
                return;
            }

            Map<String, JsonObject> studentMap = new HashMap<>();
            resUsers.right().getValue().forEach(oStudent -> {
                JsonObject student = (JsonObject) oStudent;
                studentMap.put(student.getString("id"), student);
            });

            dataList.forEach(oRes -> {
                JsonObject res = (JsonObject) oRes;
                res.put("student", studentMap.get(res.getString("student_id")));
                res.remove("student_id");
            });

            handler.handle(Future.succeededFuture(dataList));
        });
    }

}
