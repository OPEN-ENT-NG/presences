package fr.openent.presences.common.service.impl;

import fr.openent.presences.common.service.UserService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;

import java.util.List;

public class DefaultUserService implements UserService {

    @Override
    public void getUsers(List<String> userIds, Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (u:User) WHERE u.id IN {userIds} RETURN u.id as id, u.lastName + ' ' + u.firstName as displayName";
        JsonObject params = new JsonObject().put("userIds", userIds);

        Neo4j.getInstance().execute(query, params, Neo4jResult.validResultHandler(handler));
    }

    @Override
    public void getStudents(List<String> students, Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (u:User {profiles:['Student']})-[:IN]->(:ProfileGroup)-[:DEPENDS]->(c:Class) WHERE u.id IN {userIds} " +
                "RETURN u.id as id, u.lastName + ' ' + u.firstName as name, c.name as className";
        JsonObject params = new JsonObject().put("userIds", students);

        Neo4j.getInstance().execute(query, params, Neo4jResult.validResultHandler(handler));
    }
}
