package fr.openent.presences.service.impl;

import fr.openent.presences.common.helper.FutureHelper;
import fr.openent.presences.service.SearchService;
import fr.wseduc.webutils.Either;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DefaultSearchService implements SearchService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultSearchService.class);

    private final EventBus eb;

    public DefaultSearchService(EventBus eb) {
        this.eb = eb;
    }

    @Override
    public void search(String query, String structureId, Handler<Either<String, JsonArray>> handler) {
        Future<JsonArray> userAndGroupFuture = Future.future();
        Future<JsonArray> manualGroup = Future.future();

        CompositeFuture.all(userAndGroupFuture, manualGroup).setHandler(event -> {
            if (event.failed()) {
                String message = "[Presences@DefaultSearchService::search] Failed to retrieve users and groups " + event.cause();
                LOGGER.error(message);
                handler.handle(new Either.Left<>(message));
            } else {
                List items = Stream.concat(userAndGroupFuture.result().stream(), manualGroup.result().stream())
                        .collect(Collectors.toList());
                items.sort((Comparator<JsonObject>) (o1, o2) -> o1.getString("displayName").compareToIgnoreCase(o2.getString("displayName")));
                handler.handle(new Either.Right<>(new JsonArray(items)));
            }
        });
        searchUserAndGroup(query, structureId, FutureHelper.handlerJsonArray(userAndGroupFuture));
        searchManualGroup(query, structureId, FutureHelper.handlerJsonArray(manualGroup));
    }

    @Override
    public void searchGroups(String query, List<String> fields, String structure_id, Handler<Either<String, JsonArray>> handler) {
        Future<JsonArray> groups = Future.future();
        Future<JsonArray> manualGroups = Future.future();

        CompositeFuture.all(groups, manualGroups).setHandler(event -> {
            if (event.failed()) {
                String message = "[Presences@DefaultSearchService::searchGroups] Failed to retrieve groups " + event.cause();
                LOGGER.error(message);
                handler.handle(new Either.Left<>(message));
            } else {
                // correct format like groupsFuture.result() {id, name}
                manualGroups.result().forEach(manualGroup -> {
                    ((JsonObject) manualGroup).put("name", ((JsonObject) manualGroup).getString("displayName"));
                    ((JsonObject) manualGroup).remove("groupId");
                    ((JsonObject) manualGroup).remove("groupName");
                    ((JsonObject) manualGroup).remove("type");
                    ((JsonObject) manualGroup).remove("displayName");
                });
                handler.handle(new Either.Right<>(groups.result().addAll(manualGroups.result())));
            }
        });

        searchGroupsEventBus(query, fields, structure_id, FutureHelper.handlerJsonArray(groups));
        searchManualGroup(query, structure_id, FutureHelper.handlerJsonArray(manualGroups));
    }

    private void searchGroupsEventBus(String query, List<String> fields, String structure_id, Handler<Either<String, JsonArray>> handler) {
        JsonObject action = new JsonObject()
                .put("action", "groupe.search")
                .put("q", query)
                .put("fields", new JsonArray(fields))
                .put("structureId", structure_id);

        eb.send("viescolaire", action, event -> {
            if (event.failed() || event.result() == null || "error".equals(((JsonObject) event.result().body()).getString("status"))) {
                String message = "[Presences@DefaultSearchService::searchGroupsEventBus] Failed to search for groups" + event.cause();
                LOGGER.error(message);
                handler.handle(new Either.Left<>(event.cause().getMessage()));
            } else {
                handler.handle(new Either.Right<>(((JsonObject) event.result().body()).getJsonArray("results")));
            }
        });
    }

    private void searchManualGroup(String query, String structureId, Handler<Either<String, JsonArray>> handler) {
        String searchQuery = "MATCH (User {profiles:['Student']})-[:IN]->(g:ManualGroup)-[:BELONGS|:DEPENDS]->(s:Structure {id: {structureId}}) " +
                "WHERE toLower(g.name) CONTAINS '" + query.toLowerCase() + "' " +
                "RETURN DISTINCT g.id as id, g.name as displayName, 'GROUP' as type, g.id as groupId, g.name as groupName ";

        JsonObject params = new JsonObject().put("structureId", structureId);
        Neo4j.getInstance().execute(searchQuery, params, Neo4jResult.validResultHandler(handler));
    }

    private void searchUserAndGroup(String query, String structureId, Handler<Either<String, JsonArray>> handler) {
        String searchQuery = "MATCH (u:User {profiles: ['Student']})-[:IN]->(:ProfileGroup)-[:DEPENDS]->(c:Class)-[:BELONGS]->(s:Structure {id:{structureId}}) " +
                "WHERE toLower(u.firstName) CONTAINS '" + query.toLowerCase() + "' " +
                "OR toLower(u.lastName) CONTAINS '" + query.toLowerCase() + "' " +
                "RETURN distinct u.id as id, (u.lastName + ' ' + u.firstName) as displayName, 'USER' as type, c.id as groupId, c.name as groupName " +
                "UNION " +
                // MATCHING Functional Group
                "MATCH (g)-[:BELONGS|:DEPENDS]->(s:Structure {id:{structureId}}) " +
                "WHERE toLower(g.name) CONTAINS '" + query.toLowerCase() + "' " +
                "AND (g:Class OR g:FunctionalGroup) " +
                "RETURN g.id as id, g.name as displayName, 'GROUP' as type, g.id as groupId, g.name as groupName ";

        JsonObject params = new JsonObject().put("structureId", structureId);
        Neo4j.getInstance().execute(searchQuery, params, Neo4jResult.validResultHandler(handler));
    }
}
