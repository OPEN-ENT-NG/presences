package fr.openent.incidents.service.impl;

import fr.openent.incidents.Incidents;
import fr.openent.incidents.service.SeriousnessService;
import fr.openent.presences.common.helper.FutureHelper;
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

public class DefaultSeriousnessService implements SeriousnessService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultSeriousnessService.class);

    @Override
    public void get(String structureId, Handler<Either<String, JsonArray>> handler) {
        Future<JsonArray> seriousnessesFuture = Future.future();
        Future<JsonArray> seriousnessesUsedFuture = Future.future();

        fetchSeriousnesses(structureId, FutureHelper.handlerJsonArray(seriousnessesFuture));
        fetchUsedSeriousnesses(structureId, FutureHelper.handlerJsonArray(seriousnessesUsedFuture));

        CompositeFuture.all(seriousnessesFuture, seriousnessesUsedFuture).setHandler(event -> {
            if (event.failed()) {
                String message = "[Incidents@SeriousnessController] Failed to fetch seriousnesses";
                LOGGER.error(message);
                handler.handle(new Either.Left<>(message));
            } else {
                JsonArray seriousnesses = seriousnessesFuture.result();
                JsonArray seriousnessesUsed = seriousnessesUsedFuture.result();
                for (int i = 0; i < seriousnesses.size(); i++) {
                    // set used false by default
                    seriousnesses.getJsonObject(i).put("used", false);
                    for (int j = 0; j < seriousnessesUsed.size(); j++) {
                        if (seriousnesses.getJsonObject(i).getLong("id")
                                .equals(seriousnessesUsed.getJsonObject(j).getLong("id"))) {
                            seriousnesses.getJsonObject(i).put("used", true);
                        }
                    }
                }
                handler.handle(new Either.Right<>(seriousnesses));
            }
        });
    }

    private void fetchSeriousnesses(String structureId, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT * FROM " + Incidents.dbSchema + ".seriousness where structure_id = '" + structureId + "'";
        Sql.getInstance().raw(query, SqlResult.validResultHandler(handler));
    }

    private void fetchUsedSeriousnesses(String structureId, Handler<Either<String, JsonArray>> handler) {
        String query = "WITH ids AS (" +
                "SELECT p.id, p.label FROM " + Incidents.dbSchema + ".seriousness p " +
                "WHERE structure_id = '" + structureId +
                "') " +
                "SELECT DISTINCT i.id, i.label FROM ids i " +
                "INNER JOIN " + Incidents.dbSchema + ".incident AS incident ON (incident.seriousness_id = i.id) ";
        Sql.getInstance().raw(query, SqlResult.validResultHandler(handler));
    }

    @Override
    public void create(JsonObject seriousnessBody, Handler<Either<String, JsonObject>> handler) {
        String query = "INSERT INTO " + Incidents.dbSchema + ".seriousness " +
                "(structure_id, label, level, hidden)" +
                "VALUES (?, ?, ?, false) RETURNING id";
        JsonArray params = new JsonArray()
                .add(seriousnessBody.getString("structureId"))
                .add(seriousnessBody.getString("label"))
                .add(seriousnessBody.getInteger("level"));
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void put(JsonObject seriousnessBody, Handler<Either<String, JsonObject>> handler) {
        String query = "UPDATE " + Incidents.dbSchema + ".seriousness " +
                "SET label = ?, level = ?, hidden = ? WHERE id = ? RETURNING id";
        JsonArray params = new JsonArray()
                .add(seriousnessBody.getString("label"))
                .add(seriousnessBody.getInteger("level"))
                .add(seriousnessBody.getBoolean("hidden"))
                .add(seriousnessBody.getInteger("id"));
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void delete(Integer seriousnessId, Handler<Either<String, JsonObject>> handler) {
        String query = "DELETE FROM  " + Incidents.dbSchema + ".seriousness WHERE id = " + seriousnessId +
                " RETURNING id as id_deleted";
        Sql.getInstance().raw(query, SqlResult.validUniqueResultHandler(handler));
    }
}

