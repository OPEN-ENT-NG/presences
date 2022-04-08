package fr.openent.presences.service.impl;

import fr.openent.presences.Presences;
import fr.openent.presences.common.helper.FutureHelper;
import fr.openent.presences.core.constants.Field;
import fr.openent.presences.enums.ReasonType;
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
    public void get(String structureId, Integer reasonTypeId, Handler<Either<String, JsonArray>> handler) {
        Future<JsonArray> reasonsFuture = fetchReason(structureId, reasonTypeId);
        Future<JsonArray> reasonsUsedFuture = fetchUsedReason(structureId, reasonTypeId);

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

    private Future<JsonArray> fetchUsedReason(String structureId, Integer reasonTypeId) {
        if (reasonTypeId == null) {
            reasonTypeId = ReasonType.ABSENCE.getValue();
        }
        Promise<JsonArray> promise = Promise.promise();

        JsonArray params = new JsonArray();
        String query = "SELECT DISTINCT r.id, r.label " +
                "FROM " + Presences.dbSchema + ".reason r " +
                "INNER JOIN " + Presences.dbSchema + ".event e on r.id = e.reason_id " +
                "WHERE (r.structure_id = ? OR r.structure_id = '-1') ";
        params.add(structureId);

        if (reasonTypeId != ReasonType.ALL.getValue()) {
            query += "AND r.reason_type_id = ? ";
            params.add(reasonTypeId);
        }

        query += "UNION " +
                "SELECT DISTINCT r.id, r.label " +
                "FROM " + Presences.dbSchema + ".reason r " +
                "INNER JOIN " + Presences.dbSchema + ".absence a on r.id = a.reason_id " +
                "WHERE (r.structure_id = ? OR r.structure_id = '-1') ";
        params.add(structureId);

        if (reasonTypeId != ReasonType.ALL.getValue()) {
            query += "AND r.reason_type_id = ?";
            params.add(reasonTypeId);
        }

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(FutureHelper.handlerJsonArray(promise)));

        return promise.future();
    }

    @Override
    public void fetchAbsenceReason(String structureId, Handler<Either<String, JsonArray>> handler) {
        this.fetchReason(structureId, ReasonType.ABSENCE.getValue())
                .onSuccess(result -> handler.handle(new Either.Right<>(result)))
                .onFailure(error -> handler.handle(new Either.Left<>(error.getMessage())));
    }

    @Override
    public Future<JsonArray> fetchReason(String structureId, Integer reasonTypeId) {
        if (reasonTypeId == null) {
            reasonTypeId = ReasonType.ABSENCE.getValue();
        }
        Promise<JsonArray> promise = Promise.promise();

        JsonArray params = new JsonArray();
        String query = "SELECT * FROM " + Presences.dbSchema + ".reason WHERE (structure_id = ? OR structure_id = '-1')";
        params.add(structureId);

        if (reasonTypeId != ReasonType.ALL.getValue()) {
            params.add(reasonTypeId);
            query += " AND reason_type_id = ?";
        }

        query += " ORDER BY label ASC";

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(FutureHelper.handlerJsonArray(promise)));

        return promise.future();
    }

    @Override
    public Future<JsonArray> fetchAbsenceReason(String structureId) {
        return fetchReason(structureId, ReasonType.ABSENCE.getValue());
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
                "(structure_id, label, proving, comment, hidden, absence_compliance, reason_type_id) " +
                "VALUES (?, ?, ?, '', false, ?, ?) RETURNING id";
        JsonArray params = new JsonArray()
                .add(reasonBody.getString("structureId"))
                .add(reasonBody.getString("label"))
                .add(reasonBody.getBoolean("proving"))
                .add(reasonBody.getBoolean("absenceCompliance"))
                .add(ReasonType.getReasonTypeFromValue(reasonBody.getInteger(Field.REASONTYPEID, ReasonType.ABSENCE.getValue())).getValue());

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
