package fr.openent.presences.common.helper;

import fr.openent.presences.model.Person.Student;
import fr.openent.presences.model.Person.User;
import fr.wseduc.webutils.Either;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;

import java.util.ArrayList;
import java.util.List;

public class PersonHelper {

    /**
     * Convert JsonArray into student list
     *
     * @param array JsonArray response
     * @return new list of events
     */
    public List<Student> getStudentListFromJsonArray(JsonArray array) {
        List<Student> studentList = new ArrayList<>();
        for (Object o : array) {
            if (!(o instanceof JsonObject)) continue;
            Student student = new Student((JsonObject) o);
            studentList.add(student);
        }
        return studentList;
    }

    /**
     * Convert JsonArray into student list
     *
     * @param userArray JsonArray User
     * @return new list of users
     */
    public List<User> getUserListFromJsonArray(JsonArray userArray) {
        List<User> userList = new ArrayList<>();
        for (Object o : userArray) {
            if (!(o instanceof JsonObject)) continue;
            User user = new User((JsonObject) o);
            userList.add(user);
        }
        return userList;
    }

    /**
     * get the following students infos :
     * -    displayName
     * -    classeName
     * -    classId
     *
     * @param structureId structure identifier
     * @param studentIds  List of student ids
     * @param handler     handler
     */
    public void getStudentsInfo(String structureId, List<String> studentIds, List<String> restrictedClasses, Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (s:Structure {id: {structureId} })<--()--" +
                "(u:User {profiles:['Student']})-[:IN]->(:ProfileGroup)-[:DEPENDS]->(c:Class) WHERE u.id IN {idStudents} ";

        if (restrictedClasses != null && !restrictedClasses.isEmpty()) {
            query += "AND c.id IN {restrictedClasses} ";
        }

        query += "RETURN distinct (u.lastName + ' ' + u.firstName) as displayName, u.lastName as lastName, " +
                "u.firstName as firstName, u.id as id, c.name as classeName, c.id as classId";
        JsonObject params = new JsonObject()
                .put("structureId", structureId)
                .put("idStudents", studentIds)
                .put("restrictedClasses", restrictedClasses);
        Neo4j.getInstance().execute(query, params, Neo4jResult.validResultHandler(handler));
    }

    public void getStudentsInfo(String structureId, List<String> studentIds, Handler<Either<String, JsonArray>> handler) {
        getStudentsInfo(structureId, studentIds, null, handler);
    }

    public Future<JsonArray> getStudentsInfo(String structureId, List<String> studentIds) {
        Promise<JsonArray> promise = Promise.promise();
        getStudentsInfo(structureId, studentIds, null, FutureHelper.handlerJsonArray(promise));
        return promise.future();
    }

}
