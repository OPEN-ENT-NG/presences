package fr.openent.presences.service.impl;

import fr.openent.presences.Presences;
import fr.openent.presences.service.AlertService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

import java.util.Arrays;

public class DefaultAlertService implements AlertService {

    public void getSummary(String structureId, Handler<Either<String, JsonObject>> handler) {
        String query = "SELECT type, SUM(count) " + "FROM " + Presences.dbSchema +
                ".alerts WHERE structure_id = ? AND exceed_date is NOT NULL GROUP BY type;";
        JsonArray params = new JsonArray(Arrays.asList(structureId));
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(response -> {
            if (response.isLeft()) {
                handler.handle(new Either.Left<>(response.left().getValue()));
            } else {
                JsonArray values = response.right().getValue();
                JsonObject summary = new JsonObject();
                values.forEach(value -> summary.put(((JsonObject) value).getString("type"), ((JsonObject) value).getString("sum")));
                handler.handle(new Either.Right<>(summary));
            }
        }));
    }
}
