package fr.openent.presences.service.impl;

import fr.openent.presences.Presences;
import fr.openent.presences.common.helper.FutureHelper;
import fr.openent.presences.core.constants.Field;
import fr.openent.presences.service.NotebookService;
import fr.wseduc.webutils.Either;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DefaultNotebookService implements NotebookService {

    private static final Logger log = LoggerFactory.getLogger(DefaultNotebookService.class);

    @Override
    public void get(String studentId, String startDate, String endDate, Handler<Either<String, JsonArray>> handler) {
        getStudentNotebooks(studentId, startDate, endDate, null, null, null, false, result -> {
            if (result.isLeft()) {
                handler.handle(new Either.Left<>(result.left().getValue()));
                return;
            }
            handler.handle(new Either.Right<>(result.right().getValue()));
        });
    }

    @Override
    public Future<JsonObject> studentGet(String studentId, String startDate, String endDate, String limit, String offset, String structureId) {
        Promise<JsonObject> promise = Promise.promise();

        getForgottenNotebooksData(structureId, Collections.singletonList(studentId), startDate, endDate, limit, offset)
                .onFailure(promise::fail)
                .onSuccess(result -> {
                    JsonObject response = result.get(studentId)
                            .put(Field.LIMIT, limit)
                            .put(Field.OFFSET, offset);
                    promise.complete(response);
                });

        return promise.future();
    }

    @Override
    public Future<JsonObject> studentsGet(String structureId, List<String> studentIds, String startDate, String endDate, String limit, String offset) {
        Promise<JsonObject> promise = Promise.promise();

        getForgottenNotebooksData(structureId, studentIds, startDate, endDate, limit, offset)
                .onFailure(promise::fail)
                .onSuccess(result -> {
                    JsonObject response = new JsonObject()
                            .put(Field.LIMIT, limit)
                            .put(Field.OFFSET, offset)
                            .put(Field.STUDENTS_EVENTS, result);
                    promise.complete(response);
                });

        return promise.future();
    }

    private Future<Map<String, JsonObject>> getForgottenNotebooksData(String structureId, List<String> studentIds, String startDate,
                                                                      String endDate, String limit, String offset) {
        Promise<Map<String, JsonObject>> promise = Promise.promise();

        Map<String, JsonObject> studentsEvents = studentIds.stream()
                .collect(Collectors.toMap(
                        studentId -> studentId,
                        studentId -> new JsonObject()
                                .put(Field.ALL, new JsonArray())
                                .put(Field.TOTALS, 0)
                ));

        Future<Map<String, JsonObject>> forgottenNotebooksFuture = setForgottenNotebooks(structureId, studentIds, startDate, endDate, limit, offset, studentsEvents);
        Future<Map<String, JsonObject>> countForgottenNotebooksFuture = getCountForgottenNotebooks(structureId, studentIds, startDate, endDate, studentsEvents);
        CompositeFuture.all(forgottenNotebooksFuture, countForgottenNotebooksFuture)
                .onSuccess(result -> {
                    promise.complete(studentsEvents);
                })
                .onFailure(error -> {
                    String message =
                            String.format("[Presences@%s::create] Failed to get protagonist data.",
                                    this.getClass().getSimpleName());
                    log.error(String.format("%s %s", message, error));
                    promise.fail(error);
                });

        return promise.future();
    }

    @Override
    public void get(List<String> studentIds, String startDate, String endDate, Handler<Either<String, JsonArray>> handler) {
        if (studentIds.isEmpty()) {
            handler.handle(new Either.Right<>(new JsonArray()));
            return;
        }
        JsonArray params = new JsonArray();
        String query = "SELECT id, student_id, structure_id, to_char(date, 'YYYY-MM-DD') as date" +
                " FROM " + Presences.dbSchema + ".forgotten_notebook WHERE student_id IN "
                + Sql.listPrepared(studentIds.toArray()) + " ";
        params.addAll(new JsonArray(studentIds));
        if (!startDate.contentEquals("null") && !endDate.contentEquals("null")) {
            query += " AND date >= ? AND date <= ? ";
            params.add(startDate);
            params.add(endDate);
        }
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void create(JsonObject notebookBody, Handler<Either<String, JsonObject>> handler) {
        JsonArray params = new JsonArray();
        String query = "INSERT INTO " + Presences.dbSchema + ".forgotten_notebook(date, student_id, structure_id) " +
                "VALUES (?, ?, ?) RETURNING id;";
        params.add(notebookBody.getString("date"));
        params.add(notebookBody.getString("studentId"));
        params.add(notebookBody.getString("structureId"));
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void update(Integer notebookId, JsonObject notebookBody, Handler<Either<String, JsonObject>> handler) {
        JsonArray params = new JsonArray();
        String query = "UPDATE " + Presences.dbSchema + ".forgotten_notebook SET date = ? WHERE id = ? RETURNING id;";
        params.add(notebookBody.getString("date"));
        params.add(notebookId);
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void delete(Integer notebookId, Handler<Either<String, JsonObject>> handler) {
        JsonArray params = new JsonArray();
        String query = "DELETE FROM " + Presences.dbSchema + ".forgotten_notebook WHERE id = ? RETURNING id;";
        params.add(notebookId);
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    private void getStudentNotebooks(String studentId, String startDate, String endDate, String limit, String offset,
                                     String structureId, Boolean countTotal, Handler<Either<String, JsonArray>> handler) {
        getStudentNotebooks(Collections.singletonList(studentId), startDate, endDate, limit, offset, structureId, countTotal, handler);
    }

    // If countTotal is true, it will handler will return the query count result ({count: Integer})
    private void getStudentNotebooks(List<String> studentIds, String startDate, String endDate, String limit, String offset,
                                     String structureId, Boolean countTotal, Handler<Either<String, JsonArray>> handler) {
        JsonArray params = new JsonArray();
        String query;
        if (countTotal) {
            query = "SELECT COUNT(*)";
        } else {
            query = "SELECT id, student_id, structure_id, to_char(date, 'YYYY-MM-DD') as date";
        }
        query += " FROM " + Presences.dbSchema + ".forgotten_notebook WHERE student_id IN " + Sql.listPrepared(studentIds);
        params.addAll(new JsonArray(studentIds));

        if (structureId != null) {
            query += "AND structure_id = ?";
            params.add(structureId);
        }

        if (!startDate.contentEquals("null") && !endDate.contentEquals("null")) {
            query += " AND date >= ? AND date <= ? ";
            params.add(startDate);
            params.add(endDate);
        }

        if (!countTotal) { // If we count total result, we don't need to order them.
            query += " ORDER BY forgotten_notebook.date DESC ";
        }

        if (limit != null) {
            query += " LIMIT ? ";
            params.add(limit);
        }

        if (offset != null) {
            query += " OFFSET ? ";
            params.add(offset);
        }

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @SuppressWarnings("unchecked")
    private Future<Map<String, JsonObject>> setForgottenNotebooks(String structureId, List<String> studentIds, String startDate,
                                                                  String endDate, String limit, String offset,
                                                                  Map<String, JsonObject> studentsEvents) {
        Promise<Map<String, JsonObject>> promise = Promise.promise();

        JsonArray params = new JsonArray();
        String query = "SELECT id, student_id, structure_id, to_char(date, 'YYYY-MM-DD') as date" +
                getFromWhereQuery(params, structureId, startDate, endDate, studentIds) +
                " ORDER BY forgotten_notebook.date DESC ";

        if (limit != null) {
            query += " LIMIT ? ";
            params.add(limit);
        }

        if (offset != null) {
            query += " OFFSET ? ";
            params.add(offset);
        }

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(FutureHelper.handlerJsonArray(result -> {
            if (result.failed()) {
                String message =
                        String.format("[Presences@%s::setForgottenNotebooks] Failed to get ForgottenNotebooks data.",
                                this.getClass().getSimpleName());
                log.error(String.format("%s %s", message, result.cause().getMessage()));
                promise.fail(message);
            }

            for (JsonObject forgottenNotebook : ((List<JsonObject>) result.result().getList())) {
                JsonObject studentEvents = studentsEvents.get(forgottenNotebook.getString(Field.STUDENT_ID, ""));
                if (studentEvents != null && studentEvents.containsKey(Field.ALL))
                    studentEvents.getJsonArray(Field.ALL, new JsonArray()).add(forgottenNotebook);
            }


            promise.complete(studentsEvents);
        })));

        return promise.future();
    }

    @SuppressWarnings("unchecked")
    private Future<Map<String, JsonObject>> getCountForgottenNotebooks(String structureId, List<String> studentIds, String startDate,
                                                                       String endDate, Map<String, JsonObject> studentsEvents) {
        Promise<Map<String, JsonObject>> promise = Promise.promise();

        JsonArray params = new JsonArray();
        String query = "SELECT student_id, count(*) " +
                getFromWhereQuery(params, structureId, startDate, endDate, studentIds)
                + " GROUP BY student_id ";

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(FutureHelper.handlerJsonArray(result -> {
            if (result.failed()) {
                String message =
                        String.format("[Presences@%s::getCountForgottenNotebooks] Failed to count ForgottenNotebooks.",
                                this.getClass().getSimpleName());
                log.error(String.format("%s %s", message, result.cause().getMessage()));
                promise.fail(message);
            }

            for (JsonObject forgottenNotebook : ((List<JsonObject>) result.result().getList())) {
                JsonObject studentEvents = studentsEvents.get(forgottenNotebook.getString(Field.STUDENT_ID, ""));
                if (studentEvents != null)
                    studentEvents.put(Field.TOTALS, forgottenNotebook.getInteger(Field.COUNT));
            }


            promise.complete(studentsEvents);
        })));

        return promise.future();
    }

    private String getFromWhereQuery(JsonArray params, String structureId, String startDate, String endDate, List<String> studentIds) {
        String query = " FROM " + Presences.dbSchema + ".forgotten_notebook WHERE student_id IN " + Sql.listPrepared(studentIds);
        params.addAll(new JsonArray(studentIds));

        if (structureId != null) {
            query += "AND structure_id = ?";
            params.add(structureId);
        }

        if (!startDate.contentEquals("null") && !endDate.contentEquals("null")) {
            query += " AND date >= ? AND date <= ? ";
            params.add(startDate);
            params.add(endDate);
        }

        return query;
    }


}
