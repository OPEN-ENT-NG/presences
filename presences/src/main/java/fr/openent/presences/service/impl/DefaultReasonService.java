package fr.openent.presences.service.impl;

import fr.openent.presences.Presences;
import fr.openent.presences.service.ReasonService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

public class DefaultReasonService implements ReasonService {

    @Override
    public void get(String structureId, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT * FROM " + Presences.dbSchema + ".reason where structure_id = '" + structureId + "'";
        Sql.getInstance().raw(query, SqlResult.validResultHandler(handler));
    }

    @Override
    public void create(JsonObject reasonBody, Handler<Either<String, JsonObject>> handler) {
        String query = "INSERT INTO " + Presences.dbSchema + ".reason " +
                "(structure_id, label, proving, comment, hidden, absence_compliance, regularisable)" +
                "VALUES (?, ?, true, '', false, ?, ?) RETURNING id";
        JsonArray params = new JsonArray()
                .add(reasonBody.getString("structureId"))
                .add(reasonBody.getString("label"))
                .add(reasonBody.getBoolean("absenceCompliance"))
                .add(reasonBody.getBoolean("regularisable"));
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void put(Integer reasonId, JsonObject reasonBody, Handler<Either<String, JsonObject>> handler) {
        String query = "UPDATE presences.reason " +
                "SET label = ?, absence_compliance = ?, hidden = ?, regularisable = ? WHERE id = ? RETURNING id";
        JsonArray params = new JsonArray()
                .add(reasonBody.getString("label"))
                .add(reasonBody.getBoolean("absenceCompliance"))
                .add(reasonBody.getBoolean("hidden"))
                .add(reasonBody.getBoolean("regularisable"))
                .add(reasonId);
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void delete(Integer reasonId, Handler<Either<String, JsonObject>> handler) {
        String query = "DELETE FROM  " + Presences.dbSchema + ".reason WHERE id = " + reasonId +
                " RETURNING id as id_deleted";
        Sql.getInstance().raw(query, SqlResult.validUniqueResultHandler(handler));
    }
}
