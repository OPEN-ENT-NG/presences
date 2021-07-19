package fr.openent.presences.service.impl;

import fr.openent.presences.Presences;
import fr.openent.presences.common.helper.FutureHelper;
import fr.openent.presences.service.ReasonService;
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

import java.util.List;

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
        String query = " SELECT DISTINCT r.id, r.label " +
                " FROM " + Presences.dbSchema + ".reason r " +
                " INNER JOIN " + Presences.dbSchema + ".event e on r.id = e.reason_id " +
                " WHERE r.structure_id = '" + structureId + "' OR r.structure_id = '-1' " +
                " UNION " +
                " SELECT DISTINCT r.id, r.label " +
                " FROM " + Presences.dbSchema + ".reason r " +
                " INNER JOIN " + Presences.dbSchema + ".absence a on r.id = a.reason_id " +
                " WHERE r.structure_id = '" + structureId + "' OR r.structure_id = '-1'; ";

        Sql.getInstance().raw(query, SqlResult.validResultHandler(handler));
    }

    public void fetchReason(String structureId, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT * FROM " + Presences.dbSchema +
                ".reason where structure_id = '" + structureId + "' OR structure_id = '-1' ORDER BY label ASC";
        Sql.getInstance().raw(query, SqlResult.validResultHandler(handler));
    }

    public Future<JsonArray> fetchReason(String structureId) {
        Promise<JsonArray> promise = Promise.promise();

        fetchReason(structureId, event -> {
           if (event.isLeft()) {
               promise.fail(event.left().getValue());
           } else {
               promise.complete(event.right().getValue());
           }
        });

        return promise.future();
    }

    public void getReasons(List<Integer> reasonIds, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT r.id, to_json(r.*) as reason FROM " + Presences.dbSchema +
                ".reason r WHERE r.id IN " + Sql.listPrepared(reasonIds);
        JsonArray params = new JsonArray(reasonIds);
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void create(JsonObject reasonBody, Handler<Either<String, JsonObject>> handler) {
        String query = "INSERT INTO " + Presences.dbSchema + ".reason " +
                "(structure_id, label, proving, comment, hidden, absence_compliance)" +
                "VALUES (?, ?, ?, '', false, ?) RETURNING id";
        JsonArray params = new JsonArray()
                .add(reasonBody.getString("structureId"))
                .add(reasonBody.getString("label"))
                .add(reasonBody.getBoolean("proving"))
                .add(reasonBody.getBoolean("absenceCompliance"));
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void put(JsonObject reasonBody, Handler<Either<String, JsonObject>> handler) {
        String query = "UPDATE presences.reason " +
                "SET label = ?, absence_compliance = ?, hidden = ?, proving = ? WHERE id = ? RETURNING id";
        JsonArray params = new JsonArray()
                .add(reasonBody.getString("label"))
                .add(reasonBody.getBoolean("absenceCompliance"))
                .add(reasonBody.getBoolean("hidden"))
                .add(reasonBody.getBoolean("proving"))
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
