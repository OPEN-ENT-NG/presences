package fr.openent.presences.common.service.impl;

import fr.openent.presences.common.helper.FutureHelper;
import fr.openent.presences.common.service.UserService;
import fr.openent.presences.db.DBService;
import fr.wseduc.webutils.Either;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static fr.openent.presences.model.Model.log;

public class DefaultUserService extends DBService implements UserService {

    @Override
    public void getUsers(List<String> userIds, Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (u:User) WHERE u.id IN {userIds} RETURN u.id as id, u.firstName as firstName, " +
                "u.lastName as lastName, u.lastName + ' ' + u.firstName as displayName ";
        JsonObject params = new JsonObject().put("userIds", userIds);

        Neo4j.getInstance().execute(query, params, Neo4jResult.validResultHandler(handler));
    }

    @Override
    public void getStudents(List<String> students, Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (u:User {profiles:['Student']})-[:IN]->(:ProfileGroup)-[:DEPENDS]->(c:Class) WHERE u.id IN {userIds} " +
                "RETURN u.id as id, u.lastName + ' ' + u.firstName as name, u.lastName as lastName, u.firstName as firstName, c.name as className";
        JsonObject params = new JsonObject().put("userIds", students);

        Neo4j.getInstance().execute(query, params, Neo4jResult.validResultHandler(handler));
    }

    @Override
    public void getStudentsWithAudiences(String structureId, List<String> studentIds, Handler<AsyncResult<JsonArray>> handler) {
        String query = "MATCH (u:User {profiles:['Student']}) " +
                "WHERE u.id IN {studentIds} " +
                "OPTIONAL MATCH (s:Structure {id: {structureId}})<-[:BELONGS|DEPENDS]-(g:Group)<-[:IN]-(u) " +
                "OPTIONAL MATCH (s:Structure {id: {structureId}})<-[:BELONGS|DEPENDS]-(c:Class)<-[:DEPENDS]-(:ProfileGroup)<-[:IN]-(u) " +
                "OPTIONAL MATCH (s:Structure {id: {structureId}})<-[:BELONGS|DEPENDS]-(mg:ManualGroup)<-[:IN]-(u) " +
                "RETURN u.id as id, u.lastName + ' ' + u.firstName as displayName, u.lastName as lastName, u.firstName as firstName," +
                "collect(DISTINCT({id: g.id, name: g.name, type: 'GROUP'})) + " +
                "collect(DISTINCT({id: c.id, name: c.name, type: 'CLASS'})) + " +
                "collect(DISTINCT({id: mg.id, name: mg.name, type: 'MANUAL_GROUP'})) as audiences";

        JsonObject params = new JsonObject().put("structureId", structureId).put("studentIds", studentIds);

        neo4j.execute(query, params, Neo4jResult.validResultHandler(FutureHelper.handlerJsonArray(handler)));
    }

    @Override
    public void getAllStudentsIdsWithAccommodation(String structureId, Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (u:User)-[:ADMINISTRATIVE_ATTACHMENT]->(s:Structure)" +
                " WHERE s.id = {structureId}" +
                " AND u.accommodation IS NOT NULL " +
                " RETURN u.id AS id, u.accommodation AS accommodation";
        JsonObject params = new JsonObject()
                .put("structureId", structureId);

        Neo4j.getInstance().execute(query, params, Neo4jResult.validResultHandler(handler));
    }

    @Override
    public Future<JsonArray> fetchAllStudentsFromStructure(List<String> structures) {
        Promise<JsonArray> promise = Promise.promise();

        String query = "MATCH (s:Structure)<-[:BELONGS]-(c:Class)<-[:DEPENDS]-(:ProfileGroup)<-[:IN]-" +
                "(u:User {profiles:['Student']}) WHERE s.id IN {structures} RETURN s.id as structure, collect(u.id) as users";
        JsonObject params = new JsonObject().put("structures", structures);

        Neo4j.getInstance().execute(query, params, Neo4jResult.validResultHandler(event -> {
            if (event.isLeft()) {
                log.error("[Presences@DefaultUserService::fetchAllStudentsFromStructure] An error has occured during" +
                        " the process " + event.left().getValue());
                promise.fail(event.left().getValue());
            } else {
                promise.complete(event.right().getValue());
            }
        }));

        return promise.future();
    }


    /**
     * Might change neo4j to adapt personal/teacher info (can current fetch relative and children data)
     */
    public String getUserQueryNeo4j(Boolean isRelative, Boolean student) {
        String relativeQuery = "(:User {id: {id} })<-[RELATED]-(u:User)-";
        String userQuery = "(u:User {id: {id} })-";

        return "MATCH " + (isRelative != null && isRelative ? relativeQuery : userQuery) + "[:IN]->(g:ProfileGroup)-[:DEPENDS]->(s:Structure) " +
                "MATCH (u)--(m:Group " + (student != null && student ? "{filter:'Student'}" : "") + ")--(c:Class)-[:BELONGS]->(ss:Structure) " +
                "WITH u, c, s, ss, collect(DISTINCT({id: c.id, name: c.name})) as classes " +
                "RETURN u.id as id, u.firstName as firstName, u.lastName as lastName, u.displayName as displayName, u.birthDate as birth, " +
                "collect(DISTINCT({id: c.id, name: c.name, structure: ss.id})) as classesList, " +
                "collect(DISTINCT({id: ss.id, name: ss.name})) as structures;";
    }

    @Override
    @SuppressWarnings("unchecked")
    public void getChildren(String relativeId, final Handler<Either<String, JsonArray>> handler) {
        Neo4j.getInstance().execute(getUserQueryNeo4j(true, true), new JsonObject().put("id", relativeId),
                Neo4jResult.validResultHandler(event -> {
                    if (event.isLeft()) {
                        log.error("[Presences@DefaultUserService::getChildren] Failed to retrieve own info " + event.left().getValue());
                        handler.handle(new Either.Left<>(event.left().getValue()));
                    } else {
                        JsonArray childrenData = event.right().getValue();
                        // We handler childrenData as empty
                        if (childrenData.isEmpty()) {
                            handler.handle(new Either.Right<>(childrenData));
                        } else {
                            ((List<JsonObject>) childrenData.getList()).forEach(this::setClassesToStructures);
                            handler.handle(new Either.Right<>(childrenData));
                        }
                    }
                }));
    }

    @Override
    public void getChildInfo(String id, Handler<Either<String, JsonObject>> handler) {
        Neo4j.getInstance().execute(getUserQueryNeo4j(null, true), new JsonObject().put("id", id),
                Neo4jResult.validUniqueResultHandler(event -> {
                    if (event.isLeft()) {
                        log.error("[Presences@DefaultUserService::getChildrenInfo] Failed to retrieve own info " + event.left().getValue());
                        handler.handle(new Either.Left<>(event.left().getValue()));
                    } else {
                        JsonObject childData = event.right().getValue();
                        if (childData.isEmpty()) {
                            handler.handle(new Either.Right<>(childData));
                        } else {
                            setClassesToStructures(childData);
                            handler.handle(new Either.Right<>(childData));
                        }
                    }
                }));
    }

    private void setClassesToStructures(JsonObject child) {
        Map<String, JsonArray> structureClassesMap = formatMapClasses(child.getJsonArray("classesList"));
        for (int i = 0; i < child.getJsonArray("structures").size(); i++) {
            JsonObject structure = child.getJsonArray("structures").getJsonObject(i);
            structure.put("classes", new JsonArray());
            structureClassesMap.forEach((s, entries) -> {
                if (s.contains(structure.getString("id"))) {
                    structure.getJsonArray("classes").addAll(entries);
                }
            });
        }
        child.remove("classesList");
    }

    private HashMap<String, JsonArray> formatMapClasses(JsonArray classesList) {
        HashMap<String, JsonArray> map = new HashMap<>();
        for (int i = 0; i < classesList.size(); i++) {
            JsonObject classe = classesList.getJsonObject(i);
            if (!map.containsKey(classe.getString("structure"))) {
                map.put(classe.getString("structure"), new JsonArray().add(classe));
            } else {
                map.get(classe.getString("structure")).add(classe);
            }
        }
        return map;
    }
}
