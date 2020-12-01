package fr.openent.presences.common.service.impl;

import fr.openent.presences.common.service.StructureService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;

import java.util.List;

public class DefaultStructureService implements StructureService {

    @Override
    public void fetchStructuresInfos(List<String> structuresId, Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (s:Structure) WHERE s.id IN {structuresId} return s.id as id, s.externalId as externalId, s.name as name";
        JsonObject params = new JsonObject().put("structuresId", structuresId);
        Neo4j.getInstance().execute(query, params, Neo4jResult.validResultHandler(handler));
    }
}
