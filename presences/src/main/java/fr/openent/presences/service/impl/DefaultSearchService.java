package fr.openent.presences.service.impl;

import fr.openent.presences.service.SearchService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;

import java.util.Comparator;
import java.util.List;

public class DefaultSearchService implements SearchService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultSearchService.class);

    @Override
    public void search(String query, String structureId, Handler<Either<String, JsonArray>> handler) {
        String searchQuery = "MATCH (u:User {profiles: ['Student']})-[:IN]->(:ProfileGroup)-[:DEPENDS]->(c:Class)-[:BELONGS]->(s:Structure {id:{structureId}}) " +
                "WHERE toLower(u.firstName) CONTAINS '" + query.toLowerCase() + "' " +
                "OR toLower(u.lastName) CONTAINS '" + query.toLowerCase() + "' " +
                "RETURN distinct u.id as id, (u.lastName + ' ' + u.firstName) as displayName, 'USER' as type, c.id as groupId, c.name as groupName " +
                "UNION " +
                "MATCH (g)-[:BELONGS|:DEPENDS]->(s:Structure {id:{structureId}}) " +
                "WHERE toLower(g.name) CONTAINS '" + query.toLowerCase() + "' " +
                "AND (g:Class OR g:FunctionalGroup) " +
                "RETURN g.id as id, g.name as displayName, 'GROUP' as type, g.id as groupId, g.name as groupName ";

        JsonObject params = new JsonObject()
                .put("structureId", structureId);

        Neo4j.getInstance().execute(searchQuery, params, Neo4jResult.validResultHandler(either -> {
            if (either.isLeft()) {
                LOGGER.error("[Presences@DefaultSearchService] Failed to retrieve users and groups", either.left());
                handler.handle(new Either.Left<>(either.left().getValue()));
                return;
            }

            List items = either.right().getValue().getList();
            items.sort((Comparator<JsonObject>) (o1, o2) -> o1.getString("displayName").compareToIgnoreCase(o2.getString("displayName")));
            handler.handle(new Either.Right<>(new JsonArray(items)));
        }));
    }
}
