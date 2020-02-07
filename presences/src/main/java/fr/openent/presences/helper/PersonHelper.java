package fr.openent.presences.helper;

import fr.openent.presences.model.Person.Student;
import fr.openent.presences.model.Person.User;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
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
     * @param array               JsonArray response
     * @param mandatoryAttributes List of mandatory attributes
     * @return new list of events
     */
    public List<Student> getStudentListFromJsonArray(JsonArray array, List<String> mandatoryAttributes) {
        List<Student> studentList = new ArrayList<>();
        for (Object o : array) {
            if (!(o instanceof JsonObject)) continue;
            Student student = new Student((JsonObject) o, mandatoryAttributes);
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
    public void getStudentsInfo(String structureId, List<String> studentIds, Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH (s:Structure {id: {structureId} })<-[:ADMINISTRATIVE_ATTACHMENT]-" +
                "(u:User {profiles:['Student']})-[:IN]->(:ProfileGroup)-[:DEPENDS]->(c:Class) WHERE u.id IN {idStudents} " +
                "RETURN distinct (u.lastName + ' ' + u.firstName) as displayName, u.id as id, c.name as classeName, c.id as classId";
        JsonObject params = new JsonObject().put("structureId", structureId).put("idStudents", studentIds);
        Neo4j.getInstance().execute(query, params, Neo4jResult.validResultHandler(handler));
    }
}
