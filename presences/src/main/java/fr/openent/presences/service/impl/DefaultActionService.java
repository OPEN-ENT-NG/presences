package fr.openent.presences.service.impl;

import fr.openent.presences.Presences;
import fr.openent.presences.common.helper.FutureHelper;
import fr.openent.presences.service.ActionService;
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

import java.util.List;

public class DefaultActionService implements ActionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultActionService.class);

    @Override
    public void get(String structureId, Handler<Either<String, JsonArray>> handler) {
        Future<JsonArray> actionsFuture = Future.future();
        Future<JsonArray> actionsUsedFuture = Future.future();

        fetchActions(structureId, FutureHelper.handlerJsonArray(actionsFuture));
        fetchUsedAction(structureId, FutureHelper.handlerJsonArray(actionsUsedFuture));

        CompositeFuture.all(actionsFuture, actionsUsedFuture).setHandler(event -> {
            if (event.failed()) {
                String message = "[Presences@DefaultActionService] Failed to fetch action";
                LOGGER.error(message);
                handler.handle(new Either.Left<>(message));
            } else {
                JsonArray actions = actionsFuture.result();
                JsonArray actionUsed = actionsUsedFuture.result();
                for (int i = 0; i < actions.size(); i++) {
                    // set used false by default
                    actions.getJsonObject(i).put("used", false);
                    for (int j = 0; j < actionUsed.size(); j++) {
                        if (actions.getJsonObject(i).getLong("id")
                                .equals(actionUsed.getJsonObject(j).getLong("id"))) {
                            actions.getJsonObject(i).put("used", true);
                        }
                    }
                }
                handler.handle(new Either.Right<>(actions));
            }
        });
    }

    private void fetchUsedAction(String structureId, Handler<Either<String, JsonArray>> handler) {
        String query = "WITH ids AS (" +
                "SELECT a.id, a.label, a.abbreviation FROM " + Presences.dbSchema + ".actions a " +
                "WHERE structure_id = '" + structureId +
                "') " +
                "SELECT DISTINCT i.id, i.label, i.abbreviation FROM ids i " +
                "WHERE (i.id IN (SELECT action_id FROM " + Presences.dbSchema + ".event_actions))";
        Sql.getInstance().raw(query, SqlResult.validResultHandler(handler));
    }

    public void fetchActions(String structureId, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT * FROM " + Presences.dbSchema + ".actions WHERE structure_id = '" + structureId + "'";
        Sql.getInstance().raw(query, SqlResult.validResultHandler(handler));
    }

    @Override
    public void create(JsonObject actionBody, Handler<Either<String, JsonObject>> handler) {
        String query = "INSERT INTO " + Presences.dbSchema + ".actions " +
                "(structure_id, label, abbreviation)" + "VALUES (?, ?, ?) RETURNING id";
        JsonArray params = new JsonArray()
                .add(actionBody.getString("structureId"))
                .add(actionBody.getString("label"))
                .add(actionBody.getString("abbreviation"));
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void put(JsonObject actionBody, Handler<Either<String, JsonObject>> handler) {
        String query = "UPDATE presences.actions " +
                "SET label = ?, abbreviation = ?, hidden = ? WHERE id = ? RETURNING id";
        JsonArray params = new JsonArray()
                .add(actionBody.getString("label"))
                .add(actionBody.getString("abbreviation"))
                .add(actionBody.getBoolean("hidden"))
                .add(actionBody.getInteger("id"));
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void delete(Integer actionId, Handler<Either<String, JsonObject>> handler) {
        String query = "DELETE FROM  " + Presences.dbSchema + ".actions WHERE id = " + actionId +
                " RETURNING id as id_deleted";
        Sql.getInstance().raw(query, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void getLastAbbreviations(List<Integer> events, Handler<Either<String, JsonArray>> handler) {
        if (events.isEmpty()) {
            handler.handle(new Either.Right<>(new JsonArray()));
            return;
        }

        String query = "WITH last_actions AS ( " +
                " SELECT MAX(event_actions.id) as id, event_id" +
                " FROM " + Presences.dbSchema + ".event_actions" +
                " INNER JOIN " + Presences.dbSchema + ".actions" +
                " ON (event_actions.action_id = actions.id)" +
                " WHERE event_id IN " + Sql.listPrepared(events)
                + " GROUP BY event_id)" +
                " SELECT actions.abbreviation as abbreviation, last_actions.event_id as event_id" +
                " FROM " + Presences.dbSchema + ".event_actions" +
                " INNER JOIN " + Presences.dbSchema + ".actions ON (actions.id = event_actions.action_id)" +
                " INNER JOIN last_actions ON (last_actions.id = event_actions.id)" +
                " WHERE last_actions.id = event_actions.id";

        JsonArray params = new JsonArray(events);
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }
}
