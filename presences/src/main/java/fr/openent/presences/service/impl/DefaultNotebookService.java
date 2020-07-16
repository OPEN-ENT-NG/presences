package fr.openent.presences.service.impl;

import fr.openent.presences.Presences;
import fr.openent.presences.service.NotebookService;
import fr.wseduc.webutils.Either;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

import java.util.Collections;
import java.util.List;

public class DefaultNotebookService implements NotebookService {

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
    public void studentGet(String studentId, String startDate, String endDate, String limit, String offset, String structureId, Handler<Either<String, JsonObject>> handler) {
        getStudentNotebooks(studentId, startDate, endDate, limit, offset, structureId, false, result -> {
            if (result.isLeft()) {
                handler.handle(new Either.Left<>(result.left().getValue()));
                return;
            }


            Future<Integer> future = Future.future();
            if (limit == null && offset == null) { // If we get all result, we just need to get array size to get total results
                future.complete(result.right().getValue().size());
            } else { // Else, we use same query with a count result
                getStudentNotebooks(studentId, startDate, endDate, null, null, structureId, true, resultAllEvents -> {
                    if (resultAllEvents.isLeft()) {
                        future.fail(resultAllEvents.left().getValue());
                        return;
                    }
                    future.complete(resultAllEvents.right().getValue().getJsonObject(0).getInteger("count"));
                });
            }

            CompositeFuture.all(Collections.singletonList(future)).setHandler(resultTotals -> {
                if (resultTotals.failed()) {
                    String message = "[Presences@DefaultNotebookService::studentGet] Failed to get totals from events.";
                    handler.handle(new Either.Left<>(message + " " + resultTotals.cause()));
                    return;
                }

                JsonObject response = new JsonObject()
                        .put("limit", limit)
                        .put("offset", offset)
                        .put("all", result.right().getValue())
                        .put("totals", future.result());
                handler.handle(new Either.Right<>(response));
            });

        });
    }

    @Override
    public void get(List<String> studentIds, String startDate, String endDate, Handler<Either<String, JsonArray>> handler) {
        if(studentIds.isEmpty()) {
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



    // If countTotal is true, it will handler will return the query count result ({count: Integer})
    private void getStudentNotebooks(String studentId, String startDate, String endDate, String limit, String offset, String structureId, Boolean countTotal, Handler<Either<String, JsonArray>> handler) {
        JsonArray params = new JsonArray();
        String query;
        if (countTotal) {
            query = "SELECT COUNT(*)";
        } else {
            query = "SELECT id, student_id, structure_id, to_char(date, 'YYYY-MM-DD') as date";
        }
        query += " FROM " + Presences.dbSchema + ".forgotten_notebook WHERE student_id = ? ";
        params.add(studentId);

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
}
