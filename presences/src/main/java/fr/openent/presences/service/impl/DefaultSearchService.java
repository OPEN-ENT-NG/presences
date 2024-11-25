package fr.openent.presences.service.impl;

import fr.openent.presences.common.helper.FutureHelper;
import fr.openent.presences.service.SearchService;
import fr.wseduc.webutils.Either;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
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
    public void search(String query, String structureId, String userId, Handler<Either<String, JsonArray>> handler) {
        Promise<JsonArray> userAndGroupPromise = Promise.promise();
        Promise<JsonArray> manualGroupPromise = Promise.promise();

        Future.all(userAndGroupPromise.future(), manualGroupPromise.future()).onComplete(event -> {
            if (event.failed()) {
                String message = "[Presences@DefaultSearchService::search] Failed to retrieve users and groups " + event.cause();
                LOGGER.error(message);
                handler.handle(new Either.Left<>(message));
            } else {
                List items = Stream.concat(userAndGroupPromise.future().result().stream(), manualGroupPromise.future().result().stream())
                        .collect(Collectors.toList());
                items.sort((Comparator<JsonObject>) (o1, o2) -> o1.getString("displayName").compareToIgnoreCase(o2.getString("displayName")));
                handler.handle(new Either.Right<>(new JsonArray(items)));
            }
        });
        searchUserAndGroup(query, structureId, userId, FutureHelper.handlerEitherPromise(userAndGroupPromise));
        searchManualGroup(query, structureId, userId, FutureHelper.handlerEitherPromise(manualGroupPromise));
    }

    @Override
    public void searchGroups(String query, List<String> fields, String structure_id,
                             String userId, Handler<Either<String, JsonArray>> handler) {
        Promise<JsonArray> groupsPromise = Promise.promise();
        Promise<JsonArray> manualGroupsPromise = Promise.promise();

        Future.all(groupsPromise.future(), manualGroupsPromise.future()).onComplete(event -> {
            if (event.failed()) {
                String message = "[Presences@DefaultSearchService::searchGroups] Failed to retrieve groups " + event.cause();
                LOGGER.error(message);
                handler.handle(new Either.Left<>(message));
            } else {
                // correct format like groupsFuture.result() {id, name}
                manualGroupsPromise.future().result().forEach(manualGroup -> {
                    ((JsonObject) manualGroup).put("name", ((JsonObject) manualGroup).getString("displayName"));
                    ((JsonObject) manualGroup).remove("groupId");
                    ((JsonObject) manualGroup).remove("groupName");
                    ((JsonObject) manualGroup).remove("type");
                    ((JsonObject) manualGroup).remove("displayName");
                });
                handler.handle(new Either.Right<>(groupsPromise.future().result().addAll(manualGroupsPromise.future().result())));
            }
        });

        searchGroupsEventBus(query, fields, structure_id, userId, FutureHelper.handlerEitherPromise(groupsPromise));
        searchManualGroup(query, structure_id, userId, FutureHelper.handlerEitherPromise(manualGroupsPromise));
    }

    private void searchGroupsEventBus(String query, List<String> fields, String structureId,
                                      String userId, Handler<Either<String, JsonArray>> handler) {
        JsonObject action = new JsonObject()
                .put("action", "groupe.search")
                .put("q", query)
                .put("fields", new JsonArray(fields))
                .put("structureId", structureId)
                .put("userId", userId);

        eb.request("viescolaire", action, event -> {
            if (event.failed() || event.result() == null || "error".equals(((JsonObject) event.result().body()).getString("status"))) {
                String message = "[Presences@DefaultSearchService::searchGroupsEventBus] Failed to search for groups" + event.cause();
                LOGGER.error(message);
                handler.handle(new Either.Left<>(event.cause().getMessage()));
            } else {
                handler.handle(new Either.Right<>(((JsonObject) event.result().body()).getJsonArray("results")));
            }
        });
    }

    private void searchManualGroup(String query, String structureId, String userId, Handler<Either<String, JsonArray>> handler) {
        String searchQuery = "MATCH (User {profiles:['Student']})-[:IN]->(g:ManualGroup)-[:DEPENDS]->(s:Structure {id: {structureId}}) " +
                "WHERE toLower(g.name) CONTAINS {query} " +
                "RETURN DISTINCT g.id as id, g.name as displayName, 'GROUP' as type, g.id as groupId, g.name as groupName ";


        String queryFromUserId = "MATCH (u:User {profiles:['Student']})--(:ProfileGroup)--(c:Class)" +
                "--(:ProfileGroup)--(t:User {id: {userId}}) " +
                "WITH u, c MATCH (u)--(g)-[:DEPENDS]->(s:Structure {id: {structureId}}) " +
                "WHERE (g:ManualGroup) AND (toLower(g.name) CONTAINS {query}) " +
                "RETURN DISTINCT g.id as id, g.name as displayName, 'GROUP' as type, g.id AS groupId, g.name AS groupName " +
                "ORDER BY g.name";


        JsonObject params = new JsonObject()
                .put("structureId", structureId)
                .put("userId", userId)
                .put("query", query);
        Neo4j.getInstance().execute((userId != null) ? queryFromUserId : searchQuery, params, Neo4jResult.validResultHandler(handler));
    }

    private void searchUserAndGroup(String query, String structureId, String userId, Handler<Either<String, JsonArray>> handler) {
        String searchQuery = "MATCH (u:User {profiles: ['Student']})-[:IN]->(:ProfileGroup)-[:DEPENDS]->" +
                "(c:Class)-[:BELONGS]->(s:Structure {id:{structureId}}) " +
                "WHERE (toLower(u.firstName) + ' ' + toLower(u.lastName)) CONTAINS {query} " +
                "OR (toLower(u.lastName) + ' ' + toLower(u.firstName)) CONTAINS {query} " +
                "RETURN distinct u.id as id, (u.lastName + ' ' + u.firstName) as displayName, " +
                "'USER' as type, c.id as groupId, c.name as groupName " +
                "UNION " +
                // MATCHING Functional Group
                "MATCH (g)-[:BELONGS|:DEPENDS]->(s:Structure {id:{structureId}}) " +
                "WHERE toLower(g.name) CONTAINS {query} " +
                "AND (g:Class OR g:FunctionalGroup) " +
                "RETURN g.id as id, g.name as displayName, 'GROUP' as type, g.id as groupId, g.name as groupName ";


        String queryFromUserId = "MATCH (u:User {profiles: ['Student']})-[:IN]->" +
                "(:ProfileGroup)-[:DEPENDS]->(c:Class)-[:BELONGS]->(s:Structure {id:{structureId}}), " +
                "(t:User {id: {userId}})-[:IN]->(:ProfileGroup)-[:DEPENDS]->(c) " +
                "WHERE toLower(u.firstName) CONTAINS {query} " +
                "OR toLower(u.lastName) CONTAINS {query} " +
                "RETURN distinct u.id AS id, (u.lastName + ' ' + u.firstName) AS displayName, " +
                "'USER' AS type, c.id AS groupId, c.name AS groupName " +
                "UNION " +
                // MATCHING Class
                "MATCH (u:User {id:{userId}})-[:IN]->" +
                "(:ProfileGroup)-[:DEPENDS]->(g: Class)-[:BELONGS]->(s:Structure {id:{structureId}}) WHERE " +
                "toLower(g.name) CONTAINS {query} " +
                "RETURN g.id AS id, g.name AS displayName, 'GROUP' AS type, g.id AS groupId, g.name AS groupName " +
                "UNION " +
                // MATCHING Functional Group
                "MATCH (u:User {profiles:['Student']})--(:ProfileGroup)--(c:Class)--(:ProfileGroup)--(t:User {id:{userId}}) " +
                "WITH u, c MATCH (u)--(g)-[:DEPENDS]->(s:Structure {id:{structureId}}) WHERE (g:FunctionalGroup) AND " +
                "toLower(g.name) CONTAINS {query} " +
                "RETURN g.id AS id, g.name AS displayName, 'GROUP' AS type, g.id AS groupId, g.name AS groupName";

        JsonObject params = new JsonObject()
                .put("structureId", structureId)
                .put("userId", userId)
                .put("query", query);
        Neo4j.getInstance().execute((userId != null) ? queryFromUserId : searchQuery, params, Neo4jResult.validResultHandler(handler));
    }
}
