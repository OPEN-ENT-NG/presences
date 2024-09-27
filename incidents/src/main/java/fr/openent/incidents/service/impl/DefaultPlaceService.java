package fr.openent.incidents.service.impl;

import fr.openent.incidents.Incidents;
import fr.openent.incidents.service.PlaceService;
import fr.openent.presences.common.helper.FutureHelper;
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

public class DefaultPlaceService implements PlaceService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultPlaceService.class);

    @Override
    public void get(String structureId, Handler<Either<String, JsonArray>> handler) {
        Promise<JsonArray> placesPromise = Promise.promise();
        Promise<JsonArray> placesUsedPromise = Promise.promise();

        fetchPlaces(structureId, FutureHelper.handlerEitherPromise(placesPromise));
        fetchUsedPlaces(structureId, FutureHelper.handlerEitherPromise(placesPromise));

        Future.all(placesPromise.future(), placesUsedPromise.future()).onComplete(event -> {
            if (event.failed()) {
                String message = "[Incidents@PlaceController] Failed to fetch places";
                LOGGER.error(message);
                handler.handle(new Either.Left<>(message));
            } else {
                JsonArray place = placesPromise.future().result();
                JsonArray placesUsed = placesUsedPromise.future().result();
                for (int i = 0; i < place.size(); i++) {
                    // set used false by default
                    place.getJsonObject(i).put("used", false);
                    for (int j = 0; j < placesUsed.size(); j++) {
                        if (place.getJsonObject(i).getLong("id")
                                .equals(placesUsed.getJsonObject(j).getLong("id"))) {
                            place.getJsonObject(i).put("used", true);
                        }
                    }
                }
                handler.handle(new Either.Right<>(place));
            }
        });
    }

    private void fetchPlaces(String structureId, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT * FROM " + Incidents.dbSchema + ".place where structure_id = ?";
        JsonArray params = new JsonArray()
                .add(structureId);

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    private void fetchUsedPlaces(String structureId, Handler<Either<String, JsonArray>> handler) {
        String query = "WITH ids AS (" +
                "SELECT p.id, p.label FROM " + Incidents.dbSchema + ".place p " +
                "WHERE structure_id = ?) " +
                "SELECT DISTINCT i.id, i.label FROM ids i " +
                "WHERE (i.id IN (SELECT place_id FROM " + Incidents.dbSchema + ".incident))";
        JsonArray params = new JsonArray()
                .add(structureId);
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void create(JsonObject placeBody, Handler<Either<String, JsonObject>> handler) {
        String query = "INSERT INTO " + Incidents.dbSchema + ".place " +
                "(structure_id, label, hidden)" +
                "VALUES (?, ?, false) RETURNING id";
        JsonArray params = new JsonArray()
                .add(placeBody.getString("structureId"))
                .add(placeBody.getString("label"));
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void put(JsonObject placeBody, Handler<Either<String, JsonObject>> handler) {
        String query = "UPDATE " + Incidents.dbSchema + ".place " +
                "SET label = ?, hidden = ? WHERE id = ? RETURNING id";
        JsonArray params = new JsonArray()
                .add(placeBody.getString("label"))
                .add(placeBody.getBoolean("hidden"))
                .add(placeBody.getInteger("id"));
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void delete(Integer placeId, Handler<Either<String, JsonObject>> handler) {
        String query = "DELETE FROM  " + Incidents.dbSchema + ".place WHERE id = " + placeId +
                " RETURNING id as id_deleted";
        Sql.getInstance().raw(query, SqlResult.validUniqueResultHandler(handler));
    }
}
