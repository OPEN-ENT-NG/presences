package fr.openent.presences.service.impl;

import fr.openent.presences.Presences;
import fr.openent.presences.common.helper.FutureHelper;
import fr.openent.presences.helper.DisciplineHelper;
import fr.openent.presences.model.Discipline;
import fr.openent.presences.service.DisciplineService;
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

public class DefaultDisciplineService implements DisciplineService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultDisciplineService.class);

    @Override
    public void get(String structureId, Handler<Either<String, JsonArray>> handler) {
        Future<JsonArray> disciplinesFuture = Future.future();
        Future<JsonArray> disciplinesUsedFuture = Future.future();

        fetchDiscipline(structureId, FutureHelper.handlerJsonArray(disciplinesFuture));
        fetchUsedDiscipline(structureId, FutureHelper.handlerJsonArray(disciplinesUsedFuture));

        CompositeFuture.all(disciplinesFuture, disciplinesUsedFuture).setHandler(event -> {
            if (event.failed()) {
                String message = "[Presences@DefaultDisciplineService] Failed to fetch discipline";
                LOGGER.error(message);
                handler.handle(new Either.Left<>(message));
            } else {
                List<Discipline> disciplines = DisciplineHelper.getDisciplineListFromJsonArray(disciplinesFuture.result());
                List<Discipline> disciplinesUsed = DisciplineHelper.getDisciplineListFromJsonArray(disciplinesUsedFuture.result());

                for (Discipline discipline : disciplines) {
                    discipline.setUsed(false);
                    for (Discipline disciplineUsed : disciplinesUsed) {
                        if (discipline.getId().equals(disciplineUsed.getId())) {
                            discipline.setUsed(true);
                        }
                    }
                }
                handler.handle(new Either.Right<>(DisciplineHelper.toJsonArray(disciplines)));
            }
        });
    }

    private void fetchUsedDiscipline(String structureId, Handler<Either<String, JsonArray>> handler) {
        JsonArray params = new JsonArray().add(structureId);
        String query = "WITH ids AS (" +
                "SELECT d.id, d.label FROM " + Presences.dbSchema + ".discipline d " +
                "WHERE structure_id = ?) " +
                "SELECT DISTINCT i.id, i.label FROM ids i " +
                "WHERE (i.id IN (SELECT discipline_id FROM " + Presences.dbSchema + ".presence))";
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    public void fetchDiscipline(String structureId, Handler<Either<String, JsonArray>> handler) {
        JsonArray params = new JsonArray().add(structureId);
        String query = "SELECT * FROM " + Presences.dbSchema + ".discipline WHERE structure_id = ?";
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void create(JsonObject disciplineBody, Handler<Either<String, JsonObject>> handler) {
        String query = "INSERT INTO " + Presences.dbSchema + ".discipline " +
                "(structure_id, label)" +
                "VALUES (?, ?) RETURNING id";
        JsonArray params = new JsonArray()
                .add(disciplineBody.getString("structureId"))
                .add(disciplineBody.getString("label"));
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void put(JsonObject disciplineBody, Handler<Either<String, JsonObject>> handler) {
        String query = "UPDATE presences.discipline " +
                "SET label = ?, hidden = ? WHERE id = ? RETURNING id";
        JsonArray params = new JsonArray()
                .add(disciplineBody.getString("label"))
                .add(disciplineBody.getBoolean("hidden"))
                .add(disciplineBody.getInteger("id"));
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void delete(Integer disciplineId, Handler<Either<String, JsonObject>> handler) {
        String query = "DELETE FROM  " + Presences.dbSchema + ".discipline WHERE id = " + disciplineId +
                " RETURNING id as id_deleted";
        Sql.getInstance().raw(query, SqlResult.validUniqueResultHandler(handler));
    }
}