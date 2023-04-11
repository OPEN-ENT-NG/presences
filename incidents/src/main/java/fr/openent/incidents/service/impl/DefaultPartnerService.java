package fr.openent.incidents.service.impl;

import fr.openent.incidents.Incidents;
import fr.openent.incidents.service.PartnerService;
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

public class DefaultPartnerService implements PartnerService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultPartnerService.class);

    @Override
    public void get(String structureId, Handler<Either<String, JsonArray>> handler) {
        Future<JsonArray> partnersFuture = Future.future();
        Future<JsonArray> partnersUsedFuture = Future.future();

        fetchPartners(structureId, FutureHelper.handlerJsonArray(partnersFuture));
        fetchUsedPartners(structureId, FutureHelper.handlerJsonArray(partnersUsedFuture));

        CompositeFuture.all(partnersFuture, partnersUsedFuture).setHandler(event -> {
            if (event.failed()) {
                String message = "[Incidents@PartnerController] Failed to fetch partners";
                LOGGER.error(message);
                handler.handle(new Either.Left<>(message));
            } else {
                JsonArray partner = partnersFuture.result();
                JsonArray partnersUsed = partnersUsedFuture.result();
                for (int i = 0; i < partner.size(); i++) {
                    // set used false by default
                    partner.getJsonObject(i).put("used", false);
                    for (int j = 0; j < partnersUsed.size(); j++) {
                        if (partner.getJsonObject(i).getLong("id")
                                .equals(partnersUsed.getJsonObject(j).getLong("id"))) {
                            partner.getJsonObject(i).put("used", true);
                        }
                    }
                }
                handler.handle(new Either.Right<>(partner));
            }
        });
    }

    private void fetchPartners(String structureId, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT * FROM " + Incidents.dbSchema + ".partner where structure_id = ?";
        JsonArray params = new JsonArray().add(structureId);
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    private void fetchUsedPartners(String structureId, Handler<Either<String, JsonArray>> handler) {
        String query = "WITH ids AS (" +
                "SELECT p.id, p.label FROM " + Incidents.dbSchema + ".partner p " +
                "WHERE structure_id = ?) " +
                "SELECT DISTINCT i.id, i.label FROM ids i " +
                "WHERE (i.id IN (SELECT partner_id FROM " + Incidents.dbSchema + ".incident))";
        JsonArray params = new JsonArray().add(structureId);
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void create(JsonObject partnerBody, Handler<Either<String, JsonObject>> handler) {
        String query = "INSERT INTO " + Incidents.dbSchema + ".partner " +
                "(structure_id, label, hidden)" +
                "VALUES (?, ?, false) RETURNING id";
        JsonArray params = new JsonArray()
                .add(partnerBody.getString("structureId"))
                .add(partnerBody.getString("label"));
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void put(JsonObject partnerBody, Handler<Either<String, JsonObject>> handler) {
        String query = "UPDATE " + Incidents.dbSchema + ".partner " +
                "SET label = ?, hidden = ? WHERE id = ? RETURNING id";
        JsonArray params = new JsonArray()
                .add(partnerBody.getString("label"))
                .add(partnerBody.getBoolean("hidden"))
                .add(partnerBody.getInteger("id"));
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void delete(Integer partnerId, Handler<Either<String, JsonObject>> handler) {
        String query = "DELETE FROM  " + Incidents.dbSchema + ".partner WHERE id = " + partnerId +
                " RETURNING id as id_deleted";
        Sql.getInstance().raw(query, SqlResult.validUniqueResultHandler(handler));
    }
}
