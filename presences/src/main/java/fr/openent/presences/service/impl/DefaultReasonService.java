package fr.openent.presences.service.impl;

import fr.openent.presences.Presences;
import fr.openent.presences.common.helper.FutureHelper;
import fr.openent.presences.service.ReasonService;
import fr.wseduc.webutils.Either;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

public class DefaultReasonService implements ReasonService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultReasonService.class);

    @Override
    public void get(String structureId, Handler<Either<String, JsonArray>> handler) {
        Future<JsonArray> reasonsFuture = Future.future();
        Future<JsonArray> reasonsUsedFuture = Future.future();

        fetchReason(structureId, FutureHelper.handlerJsonArray(reasonsFuture));
        fetchUsedReason(structureId, FutureHelper.handlerJsonArray(reasonsUsedFuture));

        CompositeFuture.all(reasonsFuture, reasonsUsedFuture).setHandler(event -> {
            if (event.failed()) {
                String message = "[Presences@DefaultReasonService] Failed to fetch reason";
                LOGGER.error(message);
                handler.handle(new Either.Left<>(message));
            } else {
                JsonArray reasons = reasonsFuture.result();
                JsonArray reasonsUsed = reasonsUsedFuture.result();
                for (int i = 0; i < reasons.size(); i++) {
                    // set used false by default
                    reasons.getJsonObject(i).put("used", false);
                    for (int j = 0; j < reasonsUsed.size(); j++) {
                        if (reasons.getJsonObject(i).getLong("id")
                                .equals(reasonsUsed.getJsonObject(j).getLong("id"))) {
                            reasons.getJsonObject(i).put("used", true);
                        }
                    }
                }
                handler.handle(new Either.Right<>(reasons));
            }
        });
    }

    private void fetchUsedReason(String structureId, Handler<Either<String, JsonArray>> handler) {
        String query = "WITH ids AS (" +
                "SELECT r.id, r.label FROM " + Presences.dbSchema + ".reason r " +
                "WHERE structure_id = '" + structureId +
                "') " +
                "SELECT DISTINCT i.id, i.label FROM ids i " +
                "INNER JOIN presences.event AS event ON (event.reason_id = i.id) " +
                "INNER JOIN presences.absence AS absence ON (absence.reason_id = i.id)";
        Sql.getInstance().raw(query, SqlResult.validResultHandler(handler));
    }

    private void fetchReason(String structureId, Handler<Either<String, JsonArray>> handler) {
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
    public void put(JsonObject reasonBody, Handler<Either<String, JsonObject>> handler) {
        String query = "UPDATE presences.reason " +
                "SET label = ?, absence_compliance = ?, hidden = ?, regularisable = ? WHERE id = ? RETURNING id";
        JsonArray params = new JsonArray()
                .add(reasonBody.getString("label"))
                .add(reasonBody.getBoolean("absenceCompliance"))
                .add(reasonBody.getBoolean("hidden"))
                .add(reasonBody.getBoolean("regularisable"))
                .add(reasonBody.getInteger("id"));
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void delete(Integer reasonId, Handler<Either<String, JsonObject>> handler) {
        String query = "DELETE FROM  " + Presences.dbSchema + ".reason WHERE id = " + reasonId +
                " RETURNING id as id_deleted";
        Sql.getInstance().raw(query, SqlResult.validUniqueResultHandler(handler));
    }
}
