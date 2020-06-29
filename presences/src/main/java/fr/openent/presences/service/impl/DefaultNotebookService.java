package fr.openent.presences.service.impl;

import fr.openent.presences.Presences;
import fr.openent.presences.service.NotebookService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

import java.util.List;

public class DefaultNotebookService implements NotebookService {

    @Override
    public void get(String studentId, String startDate, String endDate, Handler<Either<String, JsonArray>> handler) {
        JsonArray params = new JsonArray();
        String query = "SELECT id, student_id, structure_id, to_char(date, 'YYYY-MM-DD') as date" +
                " FROM " + Presences.dbSchema + ".forgotten_notebook WHERE student_id = ?";
        params.add(studentId);
        if (!startDate.contentEquals("null") && !endDate.contentEquals("null")) {
            query += " AND date >= ? AND date <= ? ";
            params.add(startDate);
            params.add(endDate);
        }
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
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
}
