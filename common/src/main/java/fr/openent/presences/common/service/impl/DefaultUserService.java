package fr.openent.presences.common.service.impl;

import fr.openent.presences.common.helper.FutureHelper;
import fr.openent.presences.common.service.UserService;
import fr.openent.presences.core.constants.*;
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

import java.util.*;
import java.util.stream.*;

import static fr.openent.presences.model.Model.log;

public class DefaultUserService extends DBService implements UserService {
    public static String HALF_BOARDER = "DEMI-PENSIONNAIRE";
    public static String INTERNAL = "INTERNE";

    @Override
    public void getUsers(List<String> userIds, Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (u:User) WHERE u.id IN {userIds} RETURN u.id as id, u.firstName as firstName, " +
                "u.lastName as lastName, u.lastName + ' ' + u.firstName as displayName ";
        JsonObject params = new JsonObject().put("userIds", userIds);

        Neo4j.getInstance().execute(query, params, Neo4jResult.validResultHandler(handler));
    }

    @Override
    public Future<JsonArray> getUsers(List<String> userIds) {
        Promise<JsonArray> promise = Promise.promise();
        getUsers(userIds, FutureHelper.handlerJsonArray(promise));
        return promise.future();
    }

    @Override
    public void getStudents(List<String> students, Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (u:User {profiles:['Student']})-[:IN]->(:ProfileGroup)-[:DEPENDS]->(c:Class) WHERE u.id IN {userIds} " +
                "OPTIONAL MATCH (s:Structure)<-[:BELONGS|DEPENDS]-(c)" +
                "RETURN u.id as id, u.lastName + ' ' + u.firstName as name, u.lastName as lastName, u.firstName as firstName, " +
                "c.name as className, s.id as structure_id";
        JsonObject params = new JsonObject().put("userIds", students);

        Neo4j.getInstance().execute(query, params, Neo4jResult.validResultHandler(handler));
    }

    @Override
    public Future<JsonArray> getStudents(List<String> studentIds) {
        Promise<JsonArray> promise = Promise.promise();
        getStudents(studentIds, FutureHelper.handlerJsonArray(promise));
        return promise.future();
    }

    @Override
    public Future<JsonArray> getStudents(String structureId, List<String> studentIds, Boolean halfBoarder, Boolean internal) {
        Promise<JsonArray> promise = Promise.promise();
        JsonObject params = new JsonObject();

        String query = "MATCH (u:User {profiles:['Student']})-[:IN]->(:ProfileGroup)-[:DEPENDS]->(c:Class)-[:BELONGS]->(s:Structure) " +
                getWhereFilter(params, structureId, studentIds, halfBoarder, internal) +
                " RETURN u.id as id, u.lastName + ' ' + u.firstName as name, u.lastName as lastName, u.firstName as firstName, " +
                " c.name as className, u.accommodation as accommodation";

        neo4j.execute(query, params, Neo4jResult.validResultHandler(FutureHelper.handlerJsonArray(promise)));

        return promise.future();
    }

    private String getWhereFilter(JsonObject params, String structureId, List<String> studentIds, Boolean halfBoarder,
                                  Boolean internal) {

        String where = " WHERE s.id = {structureId}";
        params.put(Field.STRUCTUREID, structureId);

        if (studentIds != null && !studentIds.isEmpty()) {
            where += " AND u.id IN {studentIds}";
            params.put(Field.STUDENTIDS, studentIds);
        }

        if (halfBoarder != null || internal != null) {
            where += " AND (";
            if (halfBoarder != null) {
                where += String.format(" %s (u.accommodation CONTAINS {halfBoarder})", (halfBoarder ? "" : "NOT"));
                params.put(Field.HALFBOARDER, HALF_BOARDER);
            }
            if (internal != null) {
                where += String.format(" %s %s (u.accommodation CONTAINS {internal})", (halfBoarder != null ? "OR" : ""),
                        (internal ? "" : "NOT"));
                params.put(Field.INTERNAL, INTERNAL);
            }
            where += " ) ";
        }


        return where;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Future<List<String>> getStudentsFromTeacher(String teacherId, String structureId) {
        Promise<List<String>> promise = Promise.promise();
        if (teacherId == null) promise.complete(new ArrayList<>());
        else {
            String query = "MATCH (u:User {profiles:['Student']})-[:IN]->(:ProfileGroup)-[:DEPENDS]->" +
                    "(c:Class)-[:BELONGS]->(s:Structure {id:{structureId}})," +
                    "(t:User {id: {userId}})-[:IN]->(:ProfileGroup)-[:DEPENDS]->(c) " +
                    "RETURN DISTINCT u.id AS id";
            JsonObject params = new JsonObject()
                    .put(Field.STRUCTUREID, structureId)
                    .put(Field.USERID, teacherId);

            Neo4j.getInstance().execute(query, params, Neo4jResult.validResultHandler(res -> {
                if (res.isLeft()) {
                    promise.fail(res.left().getValue());
                } else {
                    promise.complete(((List<JsonObject>) res.right().getValue().getList())
                            .stream().map(group -> group.getString(Field.ID)).collect(Collectors.toList()));
                }
            }));
        }
        return promise.future();
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
