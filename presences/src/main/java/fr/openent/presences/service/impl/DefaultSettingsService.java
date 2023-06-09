package fr.openent.presences.service.impl;

import fr.openent.presences.Presences;
import fr.openent.presences.common.helper.FutureHelper;
import fr.openent.presences.common.helper.IModelHelper;
import fr.openent.presences.core.constants.Field;
import fr.openent.presences.db.DBService;
import fr.openent.presences.enums.SettingsFieldEnum;
import fr.openent.presences.model.Settings;
import fr.openent.presences.service.SettingsService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class DefaultSettingsService extends DBService implements SettingsService {

    private final Logger log = LoggerFactory.getLogger(DefaultSettingsService.class);

    /**
     * @deprecated Use {@link #retrieveSettings(String)}
     */
    @Deprecated
    @Override
    public void retrieve(String structureId, Handler<Either<String, JsonObject>> handler) {
        retrieveSettings(structureId)
                .onSuccess(settings -> handler.handle(new Either.Right<>(settings.toJson())))
                .onFailure(err -> handler.handle(new Either.Left<>(err.getMessage())));
    }

    /**
     * @deprecated Use {@link #retrieveSettings(String)}
     */
    @Deprecated
    @Override
    public Future<JsonObject> retrieve(String structureId) {
        Promise<JsonObject> promise = Promise.promise();
        retrieveSettings(structureId)
        .onComplete(hand -> promise.complete(hand.result().toJson()))
        .onFailure(promise::fail);
        return promise.future();
    }

    @Override
    public Future<Settings> retrieveSettings(String structureId) {
        Promise<Settings> promise = Promise.promise();

        String query = "SELECT alert_absence_threshold," +
                "alert_lateness_threshold," +
                "alert_incident_threshold," +
                "alert_forgotten_notebook_threshold," +
                "event_recovery_method," +
                "allow_multiple_slots, " +
                "exclude_alert_absence_no_reason, " +
                "exclude_alert_lateness_no_reason, " +
                "exclude_alert_forgotten_notebook " +
                "FROM " + Presences.dbSchema + ".settings WHERE structure_id = ?;";
        JsonArray params = new JsonArray(Collections.singletonList(structureId));

        sql.prepared(query, params, SqlResult.validUniqueResultHandler(IModelHelper.sqlUniqueResultToIModel(promise, Settings.class, "[Presences@%s::retrieveSettings] an error has occurred during retrieving settings")));

        return promise.future();
    }

    @Override
    public Future<JsonObject> retrieveMultipleSlots(String structureId) {
        Promise<JsonObject> promise = Promise.promise();
        String query = "SELECT allow_multiple_slots " +
                "FROM " + Presences.dbSchema + ".settings WHERE structure_id = ?;";
        JsonArray params = new JsonArray(Collections.singletonList(structureId));
        sql.prepared(query, params, SqlResult.validUniqueResultHandler(evt -> {
            if (evt.isLeft()) {
                String message = String.format("[Presences@%s::retrieveMultipleSlots] Failed to retrieve multiple slot setting : %s",
                        this.getClass().getSimpleName(), evt.left().getValue());
                log.error(message, evt.left().getValue());
                promise.fail(evt.left().getValue());
            } else {
                promise.complete(evt.right().getValue());
            }
        }));

        return promise.future();
    }

    @Override
    public Future<JsonObject> put(String structureId, JsonObject settings) {
        Promise<JsonObject> promise = Promise.promise();

        JsonArray params = new JsonArray().add(structureId);
        String query = "SELECT COUNT(structure_id) FROM " + Presences.dbSchema + ".settings WHERE structure_id = ?";
        sql.prepared(query, params, evt -> {
            Long count = SqlResult.countResult(evt);
            if (count == 0) {
                create(structureId, settings, promise.future());
            } else {
                update(structureId, settings, promise);
            }
        });

        return promise.future();
    }

    private void create(String structureId, JsonObject settings, Future<JsonObject> future) {
        List<String> insertValues = new ArrayList<>();
        insertValues.add(Field.STRUCTURE_ID);
        List<String> settingsFields = settings.fieldNames()
                .stream()
                .map(SettingsFieldEnum::getSettingsField)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        insertValues.addAll(settingsFields);

        String query = "INSERT INTO " + Presences.dbSchema + ".settings (";
        query += String.join(",", insertValues);
        JsonArray params = new JsonArray();

        params.add(structureId);
        for (String field : settingsFields) {
            params.add(settings.getValue(field));
        }
        query += ") VALUES " + Sql.listPrepared(insertValues) + " RETURNING *";
        sql.prepared(query, params, SqlResult.validUniqueResultHandler(FutureHelper.handlerJsonObject(future)));
    }

    private void update(String structureId, JsonObject settings, Promise<JsonObject> promise) {
        List<String> columns = new ArrayList<>(settings.fieldNames());
        JsonArray params = new JsonArray();
        StringBuilder query = new StringBuilder("UPDATE " + Presences.dbSchema + ".settings SET ");
        columns = columns.stream()
                .map(SettingsFieldEnum::getSettingsField)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (columns.isEmpty()) {
            promise.fail("No valid column to update");
            return;
        }
        columns.forEach(column -> {
            query.append(SettingsFieldEnum.getSettingsField(column)).append(" = ?,");
            params.add(settings.getValue(column));
        });

        params.add(structureId);
        sql.prepared(query.substring(0, query.length() - 1) + " WHERE structure_id = ? RETURNING *;",
                params, SqlResult.validUniqueResultHandler(FutureHelper.handlerEitherPromise(promise)));
    }
}
