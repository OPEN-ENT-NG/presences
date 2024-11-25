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

import java.util.*;
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
    public void get(UserInfos user, MultiMap body, List<String> restrictedStudentIds,
                    Handler<AsyncResult<JsonObject>> handler) {
        String structureId = body.get(Field.STRUCTURE_ID);
        String id = body.get(Field.ID);
        String endAt = body.get(Field.END_AT);
        String startAt = body.get(Field.START_AT);
        Boolean isTreated = body.get(Field.IS_TREATED) != null ? Boolean.valueOf(body.get(Field.IS_TREATED)) : null;
        Integer page = body.get(Field.PAGE) != null ? Integer.parseInt(body.get(Field.PAGE)) : null;
        String limit = body.get(Field.LIMIT);
        String offset = body.get(Field.OFFSET);

        Promise<JsonArray> listResultsPromise = Promise.promise();
        Promise<Long> countResultsPromise = Promise.promise();

        List<String> studentIds;

        if (body.getAll(Field.STUDENT_ID) != null && !body.getAll(Field.STUDENT_ID).isEmpty()) {
            studentIds = restrictedStudentIds.isEmpty() ? body.getAll(Field.STUDENT_ID) :
                    body.getAll(Field.STUDENT_ID).stream().filter(restrictedStudentIds::contains)
                    .collect(Collectors.toList());
        } else {
            studentIds = restrictedStudentIds;
        }

        getRequest(page, limit, offset, structureId, id, startAt, endAt, studentIds, isTreated, listResultsPromise);
        countRequest(structureId, id, startAt, endAt, studentIds, isTreated, countResultsPromise);

        Future.all(listResultsPromise.future(), countResultsPromise.future())
                .onFailure(fail -> handler.handle(Future.failedFuture(fail.getMessage())))
                .onSuccess(eventResult -> {

                    boolean restrictedHasNoStudents = !restrictedStudentIds.isEmpty() && studentIds.isEmpty();

                    JsonObject result = new JsonObject()
                            .put(Field.ALL, restrictedHasNoStudents ? new JsonArray() :
                                    listResultsPromise.future().result())
                            .put(Field.PAGE_COUNT, restrictedHasNoStudents ? 0 : countResultsPromise.future().result());

                    if (page != null) {
                        result.put(Field.PAGE, page);
                    } else {
                        if (limit != null) {
                            result.put(Field.LIMIT, limit);
                        }
                        if (offset != null) {
                            result.put(Field.OFFSET, offset);
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
                            List<String> student_ids, Boolean is_treated, Promise<JsonArray> promise) {
        JsonArray params = new JsonArray();
        Sql.getInstance().prepared(queryGetter(false, page, limit, offset, structure_id, id, start_at, end_at, student_ids, is_treated, params),
                params, SqlResult.validResultHandler(eventResult -> {
                    if (eventResult.isLeft()) {
                        String message = "[Presences@DefaultStatementAbsenceService:getRequest] Failed to retrieve absence statements.";
                        log.error(message + " " + eventResult.left().getValue());
                        promise.fail(message);
                        return;
                    }
                    setStudents(eventResult.right().getValue(), result -> {
                        if (result.failed()) {
                            promise.fail(result.cause().getMessage());
                            return;
                        }
                        promise.complete(result.result());
                    });

                }));
    }

    private void countRequest(String structure_id, String id, String start_at, String end_at,
                              List<String> student_ids, Boolean is_treated, Promise<Long> promise) {
        JsonArray params = new JsonArray();
        Sql.getInstance().prepared(queryGetter(true, null, null, null, structure_id, id, start_at, end_at, student_ids, is_treated, params),
                params, SqlResult.validUniqueResultHandler(eventResult -> {
                    if (eventResult.isLeft()) {
                        String message = "[Presences@DefaultStatementAbsenceService:countRequest] Failed to count absence statements.";
                        log.error(message + " " + eventResult.left().getValue());
                        promise.fail(message);
                        return;
                    }

                    Long count = eventResult.right().getValue().getLong("count");
                    Long countPageNumber = count / Presences.PAGE_SIZE;
                    if (count % 20 == 0) countPageNumber--;

                    promise.complete(countPageNumber);
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
