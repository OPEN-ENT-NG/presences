package fr.openent.incidents.service.impl;

import fr.openent.incidents.Incidents;
import fr.openent.incidents.service.ProtagonistTypeService;
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

public class DefaultProtagonistTypeService implements ProtagonistTypeService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultProtagonistTypeService.class);

    @Override
    public void get(String structureId, Handler<Either<String, JsonArray>> handler) {
        Future<JsonArray> protagonistsTypeFuture = Future.future();
        Future<JsonArray> protagonistsTypeUsedFuture = Future.future();

        fetchProtagonistsType(structureId, FutureHelper.handlerJsonArray(protagonistsTypeFuture));
        fetchUsedProtagonistsType(structureId, FutureHelper.handlerJsonArray(protagonistsTypeUsedFuture));

        CompositeFuture.all(protagonistsTypeFuture, protagonistsTypeUsedFuture).setHandler(event -> {
            if (event.failed()) {
                String message = "[Incidents@ProtagonistTypeController] Failed to fetch protagonistsType";
                LOGGER.error(message);
                handler.handle(new Either.Left<>(message));
            } else {
                JsonArray protagonistType = protagonistsTypeFuture.result();
                JsonArray protagonistsTypeUsed = protagonistsTypeUsedFuture.result();
                for (int i = 0; i < protagonistType.size(); i++) {
                    // set used false by default
                    protagonistType.getJsonObject(i).put("used", false);
                    for (int j = 0; j < protagonistsTypeUsed.size(); j++) {
                        if (protagonistType.getJsonObject(i).getLong("id")
                                .equals(protagonistsTypeUsed.getJsonObject(j).getLong("id"))) {
                            protagonistType.getJsonObject(i).put("used", true);
                        }
                    }
                }
                handler.handle(new Either.Right<>(protagonistType));
            }
        });
    }

    private void fetchProtagonistsType(String structureId, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT * FROM " + Incidents.dbSchema + ".protagonist_type where structure_id = '" + structureId + "'";
        Sql.getInstance().raw(query, SqlResult.validResultHandler(handler));
    }

    private void fetchUsedProtagonistsType(String structureId, Handler<Either<String, JsonArray>> handler) {
        String query = "WITH ids AS (" +
                "SELECT p.id, p.label FROM " + Incidents.dbSchema + ".protagonist_type p " +
                "WHERE structure_id = '" + structureId +
                "') " +
                "SELECT DISTINCT i.id, i.label FROM ids i " +
                "WHERE (i.id IN (SELECT type_id FROM " + Incidents.dbSchema + ".protagonist))";
        Sql.getInstance().raw(query, SqlResult.validResultHandler(handler));
    }

    @Override
    public void create(JsonObject protagonistTypeBody, Handler<Either<String, JsonObject>> handler) {
        String query = "INSERT INTO " + Incidents.dbSchema + ".protagonist_type " +
                "(structure_id, label, hidden)" +
                "VALUES (?, ?, false) RETURNING id";
        JsonArray params = new JsonArray()
                .add(protagonistTypeBody.getString("structureId"))
                .add(protagonistTypeBody.getString("label"));
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void put(JsonObject protagonistTypeBody, Handler<Either<String, JsonObject>> handler) {
        String query = "UPDATE " + Incidents.dbSchema + ".protagonist_type " +
                "SET label = ?, hidden = ? WHERE id = ? RETURNING id";
        JsonArray params = new JsonArray()
                .add(protagonistTypeBody.getString("label"))
                .add(protagonistTypeBody.getBoolean("hidden"))
                .add(protagonistTypeBody.getInteger("id"));
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void delete(Integer protagonistTypeId, Handler<Either<String, JsonObject>> handler) {
        String query = "DELETE FROM  " + Incidents.dbSchema + ".protagonist_type WHERE id = " + protagonistTypeId +
                " RETURNING id as id_deleted";
        Sql.getInstance().raw(query, SqlResult.validUniqueResultHandler(handler));
    }
}
