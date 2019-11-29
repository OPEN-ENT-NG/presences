package fr.openent.presences.service.impl;

import fr.openent.presences.Presences;
import fr.openent.presences.service.SettingsService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DefaultSettingsService implements SettingsService {
    @Override
    public void retrieve(String structureId, Handler<Either<String, JsonObject>> handler) {
        String query = "SELECT alert_absence_threshold, alert_lateness_threshold, alert_incident_threshold, alert_forgotten_notebook_threshold,event_recovery_method " +
                "FROM " + Presences.dbSchema + ".settings WHERE structure_id = ?;";
        JsonArray params = new JsonArray(Arrays.asList(structureId));
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void put(String structureId, JsonObject settings, Handler<Either<String, JsonObject>> handler) {
        String query = "SELECT COUNT(structure_id) FROM " + Presences.dbSchema + ".settings WHERE structure_id = '" + structureId + "'";
        Sql.getInstance().raw(query, evt -> {
            Long count = SqlResult.countResult(evt);
            if (count == 0) create(structureId, settings, handler);
            else update(structureId, settings, handler);
        });
    }

    private void create(String structureId, JsonObject settings, Handler<Either<String, JsonObject>> handler) {
        List<String> insertValues = new ArrayList<>();
        insertValues.add("structure_id");
        insertValues.addAll(settings.fieldNames());
        JsonArray params = new JsonArray()
                .add(structureId);

        String query = "INSERT INTO " + Presences.dbSchema + ".settings (";
        for (String field : insertValues) {
            query += field + ",";
        }

        for (String field : settings.fieldNames()) {
            params.add(settings.getValue(field));
        }
        query = query.substring(0, query.length() - 1);
        query += ") VALUES " + Sql.listPrepared(insertValues) + " RETURNING *";
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    private void update(String structureId, JsonObject settings, Handler<Either<String, JsonObject>> handler) {
        List<String> columns = new ArrayList<>(settings.fieldNames());
        JsonArray params = new JsonArray();
        StringBuilder query = new StringBuilder("UPDATE " + Presences.dbSchema + ".settings SET ");
        for (String column : columns) {
            query.append(column + "= ?,");
            params.add(settings.getValue(column));
        }

        params.add(structureId);
        Sql.getInstance().prepared(query.substring(0, query.length() - 1) + " WHERE structure_id = ? RETURNING *;", params, SqlResult.validUniqueResultHandler(handler));
    }
}
