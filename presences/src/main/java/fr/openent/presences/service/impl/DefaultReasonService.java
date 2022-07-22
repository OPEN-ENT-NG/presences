package fr.openent.presences.service.impl;

import fr.openent.presences.Presences;
import fr.openent.presences.common.helper.FutureHelper;
import fr.openent.presences.core.constants.Field;
import fr.openent.presences.enums.ReasonAlertExcludeRulesType;
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

import java.util.*;
import java.util.stream.Collectors;

public class DefaultReasonService implements ReasonService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultReasonService.class);

    @Override
    public Future<JsonArray> get(String structureId, Integer reasonTypeId) {
        Promise<JsonArray> promise = Promise.promise();

        Future<JsonArray> reasonsFuture = fetchReason(structureId, reasonTypeId);
        Future<JsonArray> reasonsUsedFuture = fetchUsedReason(structureId, reasonTypeId);
        Future<JsonArray> reasonAlertRuleFuture = this.fetchReasonAlertRule(reasonTypeId);

        CompositeFuture.all(reasonsFuture, reasonsUsedFuture, reasonAlertRuleFuture)
                .onSuccess(res -> {
                    JsonArray reasons = reasonsFuture.result();
                    JsonArray reasonsUsed = reasonsUsedFuture.result();
                    JsonArray reasonAlertRule = reasonAlertRuleFuture.result();
                    final Map<Long, JsonArray> mapReasonIdAlertRule = reasonAlertRule.stream()
                            .map(JsonObject.class::cast)
                            .collect(Collectors.groupingBy(jsonObject -> jsonObject.getLong(Field.REASON_ID)))
                            .entrySet()
                            .stream().collect(Collectors.toMap(Map.Entry::getKey, stringListEntry -> {
                                List<String> listAlertRule = stringListEntry.getValue().stream().map(jsonObject -> jsonObject.getString(Field.RULE_TYPE)).collect(Collectors.toList());
                                return new JsonArray(listAlertRule);
                            }));
                    for (int i = 0; i < reasons.size(); i++) {
                        JsonObject jsonObjectReason = reasons.getJsonObject(i);
                        jsonObjectReason.put(Field.REASON_ALERT_RULES, mapReasonIdAlertRule.getOrDefault(jsonObjectReason.getLong(Field.ID), new JsonArray()));
                        // set used false by default
                        jsonObjectReason.put(Field.USED, false);
                        for (int j = 0; j < reasonsUsed.size(); j++) {
                            if (jsonObjectReason.getLong(Field.ID)
                                    .equals(reasonsUsed.getJsonObject(j).getLong(Field.ID))) {
                                jsonObjectReason.put(Field.USED, true);
                            }

                        }
                    }
                    promise.complete(reasons);
                })
                .onFailure(err -> {
                    String message = String.format("[Presences@DefaultReasonService] Failed to fetch reason %s", err.getMessage());
                    LOGGER.error(message);
                    promise.fail(message);
                });

        return promise.future();
    }

    private Future<JsonArray> fetchReasonAlertRule(Integer reasonTypeId) {
        Promise<JsonArray> promise = Promise.promise();
        String query = "SELECT r.rule_type, reason_alert.reason_id FROM " + Presences.dbSchema + ".reason_alert" +
                " INNER JOIN " + Presences.dbSchema + ".reason_alert_exclude_rules_type as r ON r.id = reason_alert.reason_alert_exclude_rules_type_id" +
                " INNER JOIN " + Presences.dbSchema + ".reason ON reason.id = reason_alert.reason_id" +
                " WHERE reason_alert.deleted_at IS NULL" +
                " AND reason.reason_type_id = ?";
        JsonArray params = new JsonArray(Collections.singletonList(reasonTypeId));
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(FutureHelper.handlerEitherPromise(promise)));
        return promise.future();
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
    public Future<JsonObject> create(JsonObject reasonBody) {
        Promise<JsonObject> promiseResult = Promise.promise();

        Future<JsonObject> createReasonFuture = createReason(reasonBody);
        createReasonFuture
                .onSuccess(res -> {
                    Integer reasonId = createReasonFuture.result().getInteger(Field.ID);
                    String structureId = reasonBody.getString(Field.STRUCTUREID);
                    List<Future> futureList = new ArrayList<>();
                    Arrays.stream(ReasonAlertExcludeRulesType.values())
                            .filter(reasonAlertExcludeRulesType -> reasonBody.getBoolean(reasonAlertExcludeRulesType.getJsonField(), false))
                            .map(reasonAlertExcludeRulesType -> createOrUpdateReasonAlert(reasonId, structureId, reasonAlertExcludeRulesType, true))
                            .forEach(futureList::add);
                    CompositeFuture.all(futureList)
                            .onSuccess(event -> promiseResult.complete(createReasonFuture.result()))
                            .onFailure(err -> {
                                String message = String.format("[Presences@DefaultReasonService] Failed to create reason %s", err.getMessage());
                                LOGGER.error(message);
                                promiseResult.fail(message);
                            });
                })
                .onFailure(err -> {
                    String message = String.format("[Presences@DefaultReasonService] Failed to create reason %s", err.getMessage());
                    LOGGER.error(message);
                    promiseResult.fail(message);
                });

        return promiseResult.future();
    }

    @Override
    public Future<JsonObject> put(JsonObject reasonBody) {
        Promise<JsonObject> promiseResult = Promise.promise();

        Integer reasonId = reasonBody.getInteger(Field.ID);
        String structureId = reasonBody.getString(Field.STRUCTUREID);
        Future<JsonObject> updateFuture = this.updateReason(reasonBody);
        List<Future> futureList = new ArrayList<>();
        futureList.add(updateFuture);
        Arrays.stream(ReasonAlertExcludeRulesType.values())
                .map(reasonAlertExcludeRulesType -> this.createOrUpdateReasonAlert(reasonId, structureId, reasonAlertExcludeRulesType,
                        reasonBody.getBoolean(reasonAlertExcludeRulesType.getJsonField(), false)))
                .forEach(futureList::add);

        CompositeFuture.all(futureList)
                .onSuccess(event -> promiseResult.complete(updateFuture.result()))
                .onFailure(err -> {
                    String message = String.format("[Presences@DefaultReasonService] Failed to create reason %s", err.getMessage());
                    LOGGER.error(message);
                    promiseResult.fail(message);
                });

        return promiseResult.future();
    }

    private Future<JsonObject> createReason(JsonObject reasonBody) {
        Promise<JsonObject> promise = Promise.promise();
        String query = "INSERT INTO " + Presences.dbSchema + ".reason " +
                "(structure_id, label, proving, comment, hidden, absence_compliance, reason_type_id) " +
                "VALUES (?, ?, ?, '', false, ?, ?) RETURNING id";
        JsonArray params = new JsonArray()
                .add(reasonBody.getString("structureId"))
                .add(reasonBody.getString("label"))
                .add(reasonBody.getBoolean("proving"))
                .add(reasonBody.getBoolean("absenceCompliance"))
                .add(ReasonType.getReasonTypeFromValue(reasonBody.getInteger(Field.REASONTYPEID, ReasonType.ABSENCE.getValue())).getValue());
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(FutureHelper.handlerJsonObject(promise)));

        return promise.future();
    }

    private Future<JsonObject> updateReason(JsonObject reasonBody) {
        Promise<JsonObject> promise = Promise.promise();
        String query = "UPDATE "+ Presences.dbSchema +".reason " +
                "SET label = ?, absence_compliance = ?, hidden = ?, proving = ? WHERE id = ? RETURNING id";
        JsonArray params = new JsonArray()
                .add(reasonBody.getString("label"))
                .add(reasonBody.getBoolean("absenceCompliance"))
                .add(reasonBody.getBoolean("hidden"))
                .add(reasonBody.getBoolean("proving"))
                .add(reasonBody.getInteger("id"));
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(FutureHelper.handlerJsonObject(promise)));
        return promise.future();
    }

    private Future<JsonObject> createOrUpdateReasonAlert(int reasonId, String structureId, ReasonAlertExcludeRulesType reasonAlertExcludeRulesType, boolean exclude) {
        Promise<JsonObject> promise = Promise.promise();
        String query;
        JsonArray params;
        if (exclude){
            query = "INSERT INTO " + Presences.dbSchema + ".reason_alert(structure_id, reason_id, reason_alert_exclude_rules_type_id) VALUES (?, ?, " + reasonAlertExcludeRulesType.getValue() + ") ON CONFLICT ON CONSTRAINT uniq_reason_alert DO UPDATE SET deleted_at = null WHERE reason_alert.structure_id = ? AND reason_alert.reason_id = ? AND reason_alert.reason_alert_exclude_rules_type_id = " + reasonAlertExcludeRulesType.getValue();
            params = new JsonArray()
                    .add(structureId)
                    .add(reasonId)
                    .add(structureId)
                    .add(reasonId);

        } else {
            query = "UPDATE " + Presences.dbSchema + ".reason_alert SET deleted_at = now() WHERE structure_id = ? AND reason_id = ? AND reason_alert_exclude_rules_type_id = " + reasonAlertExcludeRulesType.getValue();
            params = new JsonArray()
                    .add(structureId)
                    .add(reasonId);
        }
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(FutureHelper.handlerJsonObject(promise)));
        return promise.future();
    }

    @Override
    public void delete(Integer reasonId, Handler<Either<String, JsonObject>> handler) {
        String query = "DELETE FROM  " + Presences.dbSchema + ".reason WHERE id = " + reasonId +
                " RETURNING id as id_deleted";
        Sql.getInstance().raw(query, SqlResult.validUniqueResultHandler(handler));
    }
}
