package fr.openent.presences.common.service.impl;

import fr.openent.presences.common.service.StructureService;
import fr.openent.presences.db.DBService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;
import org.entcore.common.sql.SqlResult;

import java.util.List;

public class DefaultStructureService extends DBService implements StructureService {

    private final Logger log = LoggerFactory.getLogger(DefaultStructureService.class);

    @Override
    public void fetchStructuresInfos(List<String> structuresId, Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (s:Structure) WHERE s.id IN {structuresId} return s.id as id, s.externalId as externalId, s.name as name";
        JsonObject params = new JsonObject().put("structuresId", structuresId);
        Neo4j.getInstance().execute(query, params, Neo4jResult.validResultHandler(handler));
    }

    @Override
    public Future<JsonArray> fetchStructuresInfos(List<String> structuresId) {
        Promise<JsonArray> promise = Promise.promise();
        fetchStructuresInfos(structuresId, event -> {
            if (event.isLeft()) {
                promise.fail(event.left().getValue());
            } else {
                promise.complete(event.right().getValue());
            }
        });

        return promise.future();
    }



    /**
     * fetch all structures using presence's module
     * @return {@link Future<JsonArray>}
     */
    @Override
    public Future<JsonArray> fetchActiveStructure() {
        Promise<JsonArray> promise = Promise.promise();

        String queryStructures = "SELECT id_etablissement as id FROM presences.etablissements_actifs";
        sql.raw(queryStructures, SqlResult.validResultHandler(resultStructures -> {
            if (resultStructures.isLeft()) {
                log.error("[PresencesCommon@DefaultStructureService] Failed to retrieve actives structures: ",
                        resultStructures.left().getValue());
                promise.fail(resultStructures.left().getValue());
            } else {
                promise.complete(resultStructures.right().getValue());
            }
        }));

        return promise.future();
    }

    @Override
    public Future<JsonArray> activateStructures(List<String> structureIds) {
        Promise<JsonArray> promise = Promise.promise();

        if (structureIds.isEmpty()) {
            promise.complete(new JsonArray());
        } else {
            String query = "INSERT INTO presences.etablissements_actifs (id_etablissement, actif) VALUES ";
            JsonArray params = new JsonArray();

            for (String structureId : structureIds) {
                query += "(?, true),";
                params.add(structureId);
            }

            query = query.substring(0, query.length() - 1);

            query += " RETURNING id_etablissement";

            sql.prepared(query, params, SqlResult.validResultHandler(event -> {
                if (event.isLeft()) {
                    String message = String.format("[PresencesCommon@%s] Failed to activate structures: %s",
                            this.getClass().getSimpleName(), event.left().getValue());
                    log.error(message, event.left().getValue());
                    promise.fail(event.left().getValue());
                } else {
                    promise.complete(event.right().getValue());
                }
            }));
        }

        return promise.future();
    }
}
