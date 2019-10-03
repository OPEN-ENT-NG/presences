package fr.openent.incidents.service.impl;

import fr.openent.incidents.Incidents;
import fr.openent.incidents.service.IncidentsTypeService;
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

public class DefaultIncidentsTypeService implements IncidentsTypeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultIncidentsTypeService.class);

    @Override
    public void get(String structureId, Handler<Either<String, JsonArray>> handler) {
        Future<JsonArray> incidentsTypeFuture = Future.future();
        Future<JsonArray> incidentsTypeUsedFuture = Future.future();

        fetchIncidentsType(structureId, FutureHelper.handlerJsonArray(incidentsTypeFuture));
        fetchUsedIncidentsType(structureId, FutureHelper.handlerJsonArray(incidentsTypeUsedFuture));

        CompositeFuture.all(incidentsTypeFuture, incidentsTypeUsedFuture).setHandler(event -> {
            if (event.failed()) {
                String message = "[Incidents@IncidentsTypeService] Failed to fetch Incidents type";
                LOGGER.error(message);
                handler.handle(new Either.Left<>(message));
            } else {
                JsonArray incidentsType = incidentsTypeFuture.result();
                JsonArray incidentsTypeUsed = incidentsTypeUsedFuture.result();
                for (int i = 0; i < incidentsType.size(); i++) {
                    // set used false by default
                    incidentsType.getJsonObject(i).put("used", false);
                    for (int j = 0; j < incidentsTypeUsed.size(); j++) {
                        if (incidentsType.getJsonObject(i).getLong("id")
                                .equals(incidentsTypeUsed.getJsonObject(j).getLong("id"))) {
                            incidentsType.getJsonObject(i).put("used", true);
                        }
                    }
                }
                handler.handle(new Either.Right<>(incidentsType));
            }
        });
    }

    private void fetchIncidentsType(String structureId, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT * FROM " + Incidents.dbSchema + ".incident_type where structure_id = '" + structureId + "'";
        Sql.getInstance().raw(query, SqlResult.validResultHandler(handler));
    }

    private void fetchUsedIncidentsType(String structureId, Handler<Either<String, JsonArray>> handler) {
        String query = "WITH ids AS (" +
                "SELECT i.id, i.label FROM " + Incidents.dbSchema + ".incident_type i " +
                "WHERE structure_id = '" + structureId +
                "') " +
                "SELECT DISTINCT i.id, i.label FROM ids i " +
                "INNER JOIN " + Incidents.dbSchema + ".incident AS incident ON (incident.type_id = i.id) ";
        Sql.getInstance().raw(query, SqlResult.validResultHandler(handler));
    }

    @Override
    public void create(JsonObject incidentTypeBody, Handler<Either<String, JsonObject>> handler) {
        String query = "INSERT INTO " + Incidents.dbSchema + ".incident_type " +
                "(structure_id, label, hidden)" +
                "VALUES (?, ?, false) RETURNING id";
        JsonArray params = new JsonArray()
                .add(incidentTypeBody.getString("structureId"))
                .add(incidentTypeBody.getString("label"));
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void put(JsonObject incidentTypeBody, Handler<Either<String, JsonObject>> handler) {
        String query = "UPDATE " + Incidents.dbSchema + ".incident_type " +
                "SET label = ?, hidden = ? WHERE id = ? RETURNING id";
        JsonArray params = new JsonArray()
                .add(incidentTypeBody.getString("label"))
                .add(incidentTypeBody.getBoolean("hidden"))
                .add(incidentTypeBody.getInteger("id"));
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void delete(Integer incidentTypeId, Handler<Either<String, JsonObject>> handler) {
        String query = "DELETE FROM  " + Incidents.dbSchema + ".incident_type WHERE id = " + incidentTypeId +
                " RETURNING id as id_deleted";
        Sql.getInstance().raw(query, SqlResult.validUniqueResultHandler(handler));
    }

}
