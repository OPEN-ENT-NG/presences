package fr.openent.incidents.service.impl;

import fr.openent.incidents.Incidents;
import fr.openent.incidents.helper.PunishmentTypeHelper;
import fr.openent.incidents.model.PunishmentType;
import fr.openent.incidents.service.PunishmentTypeService;
import fr.openent.presences.common.helper.FutureHelper;
import fr.wseduc.mongodb.MongoDb;
import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.mongodb.MongoDbResult;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DefaultPunishmentTypeService implements PunishmentTypeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultPunishmentTypeService.class);

    @Override
    public void get(String structure_id, Handler<Either<String, JsonArray>> handler) {

        fetchPunishmentsType(structure_id, punishmentTypeAsync -> {
            if (punishmentTypeAsync.isLeft()) {
                String message = "[Incidents@PunishementsTypeService::get] Failed to fetch Punishments type";
                LOGGER.error(message);
                handler.handle(new Either.Left<>(message));
            } else {
                List<PunishmentType> punishmentsType = PunishmentTypeHelper.
                        getPunishmentTypeListFromJsonArray(punishmentTypeAsync.right().getValue());

                List<Integer> punishmentsIds = punishmentsType.stream().map(PunishmentType::getId).collect(Collectors.toList());
                List<Future<JsonObject>> futures = new ArrayList<>();

                for (Integer punishmentId : punishmentsIds) {
                    Promise<JsonObject> promise = Promise.promise();
                    futures.add(promise.future());
                    findPunishmentTypeIdIfExist(structure_id, punishmentId, FutureHelper.handlerEitherPromise(promise));
                }

                Future.all(futures).onComplete(event -> {
                    if (event.failed()) {
                        String message = "[Incidents@PunishementsTypeService::get] Failed to " +
                                "fetch used punishments type in mongodb" + " " + event.cause().toString();
                        LOGGER.error(message);
                        handler.handle(new Either.Left<>(message));
                    } else {
                        for (PunishmentType punishmentType : punishmentsType) {
                            for (int i = 0; i < event.result().size(); i++) {
                                if (!((JsonObject) event.result().resultAt(i)).isEmpty()) {
                                    JsonObject resultCategory = event.result().resultAt(i);
                                    if (punishmentType.getId().equals(resultCategory.getInteger("type_id"))) {
                                        punishmentType.setUsed(true);
                                    }
                                }
                            }
                        }
                        handler.handle(new Either.Right<>(PunishmentTypeHelper.toJsonArray(punishmentsType)));
                    }
                });
            }
        });
    }

    private void findPunishmentTypeIdIfExist(String structure_id, Integer punishmentId, Handler<Either<String, JsonObject>> handler) {
        JsonObject query = new JsonObject().put("type_id", punishmentId).put("structure_id", structure_id);
        MongoDb.getInstance().findOne("presences.punishments", query, message -> handler.handle(MongoDbResult.validResult(message)));
    }

    private void fetchPunishmentsType(String structureId, Handler<Either<String, JsonArray>> handler) {
        JsonArray params = new JsonArray().add(structureId);

        String query = "SELECT * FROM " + Incidents.dbSchema + ".punishment_type where structure_id = ?";

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
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
