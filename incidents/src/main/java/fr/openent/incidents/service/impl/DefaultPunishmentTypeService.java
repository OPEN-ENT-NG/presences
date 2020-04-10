package fr.openent.incidents.service.impl;

import fr.openent.incidents.Incidents;
import fr.openent.incidents.helper.PunishmentTypeHelper;
import fr.openent.incidents.model.PunishmentType;
import fr.openent.incidents.service.PunishmentTypeService;
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

import java.util.List;

public class DefaultPunishmentTypeService implements PunishmentTypeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultPunishmentTypeService.class);

    @Override
    public void get(String structure_id, Handler<Either<String, JsonArray>> handler) {
        Future<JsonArray> punishmentsTypeFuture = Future.future();
        Future<JsonArray> punishmentsTypeUsedFuture = Future.future();

        fetchPunishmentsType(structure_id, FutureHelper.handlerJsonArray(punishmentsTypeFuture));
        fetchUsedPunishmentsType(structure_id, FutureHelper.handlerJsonArray(punishmentsTypeUsedFuture));

        CompositeFuture.all(punishmentsTypeFuture, punishmentsTypeUsedFuture).setHandler(event -> {
            if (event.failed()) {
                String message = "[Incidents@PunishementsTypeService::get] Failed to fetch Punishments type";
                LOGGER.error(message);
                handler.handle(new Either.Left<>(message));
            } else {
                List<PunishmentType> punishmentsType = PunishmentTypeHelper.getPunishmentTypeListFromJsonArray(punishmentsTypeFuture.result());
                List<PunishmentType> punishmentsTypeUsed = PunishmentTypeHelper.getPunishmentTypeListFromJsonArray(punishmentsTypeUsedFuture.result());

                for (PunishmentType punishmentType : punishmentsType) {
                    punishmentType.setUsed(false);
                    for (PunishmentType punishmentTypeUsed : punishmentsTypeUsed) {
                        if (punishmentType.getId().equals(punishmentTypeUsed.getId())) {
                            punishmentType.setUsed(true);
                        }
                    }
                }
                handler.handle(new Either.Right<>(PunishmentTypeHelper.toJsonArray(punishmentsType)));
            }
        });
    }

    private void fetchPunishmentsType(String structureId, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT * FROM " + Incidents.dbSchema + ".punishment_type where structure_id = '" + structureId + "'";
        Sql.getInstance().raw(query, SqlResult.validResultHandler(handler));
    }

    private void fetchUsedPunishmentsType(String structureId, Handler<Either<String, JsonArray>> handler) {
        String query = "WITH ids AS (" +
                "SELECT i.id, i.label, i.type, i.punishment_category_id FROM " + Incidents.dbSchema + ".punishment_type i " +
                "WHERE structure_id = '" + structureId +
                "') " +
                "SELECT DISTINCT i.id, i.label, i.type, i.punishment_category_id FROM ids i " +
                "WHERE (i.id IN (SELECT type_id FROM " + Incidents.dbSchema + ".incident))";
        Sql.getInstance().raw(query, SqlResult.validResultHandler(handler));
    }

    @Override
    public void create(JsonObject punishmentTypeBody, Handler<Either<String, JsonObject>> handler) {
        String query = "INSERT INTO " + Incidents.dbSchema + ".punishment_type " +
                "(structure_id, label, type, punishment_category_id, hidden)" + "VALUES (?, ?, ?, ?, false) RETURNING id";
        JsonArray params = new JsonArray()
                .add(punishmentTypeBody.getString("structure_id"))
                .add(punishmentTypeBody.getString("label"))
                .add(punishmentTypeBody.getString("type"))
                .add(punishmentTypeBody.getInteger("punishment_category_id"));
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void put(JsonObject punishmentTypeBody, Handler<Either<String, JsonObject>> handler) {
        String query = "UPDATE " + Incidents.dbSchema + ".punishment_type " +
                "SET label = ?, type = ?, punishment_category_id = ?, hidden = ? WHERE id = ? RETURNING id";
        JsonArray params = new JsonArray()
                .add(punishmentTypeBody.getString("label"))
                .add(punishmentTypeBody.getString("type"))
                .add(punishmentTypeBody.getInteger("punishment_category_id"))
                .add(punishmentTypeBody.getBoolean("hidden"))
                .add(punishmentTypeBody.getInteger("id"));
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void delete(Integer punishmentTypeId, Handler<Either<String, JsonObject>> handler) {
        String query = "DELETE FROM  " + Incidents.dbSchema + ".punishment_type WHERE id = " + punishmentTypeId +
                " RETURNING id as id_deleted";
        Sql.getInstance().raw(query, SqlResult.validUniqueResultHandler(handler));
    }
}
