package fr.openent.presences.service.impl;

import fr.openent.presences.Presences;
import fr.openent.presences.service.AlertService;
import fr.wseduc.webutils.Either;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultAlertService implements AlertService {

    @Override
    public void delete(List<String> alerts, Handler<Either<String, JsonObject>> handler) {
        String query = "DELETE FROM " + Presences.dbSchema +
                ".alerts WHERE id IN " + Sql.listPrepared(alerts);

        JsonArray params = new JsonArray(alerts);
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    public void getSummary(String structureId, Handler<Either<String, JsonObject>> handler) {
        String query = "SELECT type, COUNT(student_id) AS count FROM " + Presences.dbSchema +
                ".alerts WHERE structure_id = ? AND exceed_date is NOT NULL GROUP BY type;";
        JsonArray params = new JsonArray(Arrays.asList(structureId));
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(response -> {
            if (response.isLeft()) {
                handler.handle(new Either.Left<>(response.left().getValue()));
            } else {
                JsonArray values = response.right().getValue();
                JsonObject summary = new JsonObject();
                values.forEach(value -> summary.put(((JsonObject) value).getString("type"), ((JsonObject) value).getLong("count")));
                handler.handle(new Either.Right<>(summary));
            }
        }));
    }

    @Override
    public void getAlertsStudents(String structureId, List<String> types, List<String> students, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT id, student_id, type, count, exceed_date " +
                "FROM " + Presences.dbSchema + ".alerts " +
                "WHERE structure_id = ? " +
                "AND exceed_date is NOT NULL " +
                "AND type IN " + Sql.listPrepared(types);
        if (!students.isEmpty()) {
            query += " AND student_id IN " + Sql.listPrepared(students);
        }

        query += " ORDER BY exceed_date DESC;";
        JsonArray params = new JsonArray()
                .add(structureId)
                .addAll(new JsonArray(types));
        if (!students.isEmpty()) params.addAll(new JsonArray(students));

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(response -> {
            if (response.isLeft()) {
                handler.handle(new Either.Left<>(response.left().getValue()));
                return;
            }

            JsonArray alerts = response.right().getValue();
            // Récupérer les identifiants des élèves présents dans les alertes
            JsonArray studentsAlerts = new JsonArray();
            for (int i = 0; i < alerts.size(); i++) {
                studentsAlerts.add(alerts.getJsonObject(i).getString("student_id"));
            }

            // Récupérer leurs noms, prénoms et nom de la classe :
            String studentQuery =
                    "MATCH (u:User)-[:IN]->(:ProfileGroup)-[:DEPENDS]->(c:Class) " +
                            "WHERE u.id IN {studentsId} " +
                            "RETURN u.firstName as firstName, u.lastName as lastName, c.name as audience, u.id as student_id;";

            JsonObject studentParam = new JsonObject().put("studentsId", studentsAlerts);

            Neo4j.getInstance().execute(studentQuery, studentParam, Neo4jResult.validResultHandler(studentResult -> {
                if (studentResult.isLeft()) {
                    handler.handle(new Either.Left<>(response.left().getValue()));
                    return;
                }

                JsonArray studentList = studentResult.right().getValue();
                Map<String, JsonObject> studentMap = new HashMap<>();
                for (int i = 0; i < studentList.size(); i++) {
                    JsonObject student = studentList.getJsonObject(i);
                    studentMap.put(student.getString("student_id"), student);
                }

                for (int i = 0; i < alerts.size(); i++) {
                    JsonObject alert = alerts.getJsonObject(i);
                    String studentId = alert.getString("student_id");
                    if (!studentMap.containsKey(studentId)) continue;
                    alert.put("name", studentMap.get(studentId).getString("lastName") + " " + studentMap.get(studentId).getString("firstName"));
                    alert.put("audience", studentMap.get(studentId).getString("audience"));
                }

                handler.handle(new Either.Right<>(alerts));
            }));
        }));
    }

    @Override
    public void getStudentAlertNumberWithThreshold(String structureId, String studentId, String type, Handler<Either<String, JsonObject>> handler) {
        Future<JsonObject> futureThreshold = Future.future();
        Future<JsonObject> futureCount = Future.future();

        String queryThreshold = "SELECT alert_forgotten_notebook_threshold as threshold" +
                " FROM "  + Presences.dbSchema + ".settings " +
                " WHERE structure_id = ?";
        JsonArray paramsThreshold = new JsonArray();
        paramsThreshold.add(structureId);

        Sql.getInstance().prepared(queryThreshold, paramsThreshold, SqlResult.validUniqueResultHandler(result -> {
            if (result.isRight()) {
                futureThreshold.complete(result.right().getValue());
            } else {
                futureThreshold.fail((result.left().getValue()));
            }
        }));

        String queryCount = "SELECT count " +
                " FROM " + Presences.dbSchema + ".alerts " +
                " WHERE student_id = ? " +
                " AND structure_id = ? " +
                " AND type = ? ";
        JsonArray paramsCount = new JsonArray();
        paramsCount.add(studentId);
        paramsCount.add(structureId);
        paramsCount.add(type);


        Sql.getInstance().prepared(queryCount, paramsCount, SqlResult.validUniqueResultHandler(result -> {
            if (result.isRight()) {
                futureCount.complete(result.right().getValue());
            } else {
                futureCount.fail((result.left().getValue()));
            }
        }));


        CompositeFuture.all(futureThreshold, futureCount).setHandler(event -> {
            if (event.succeeded()) {
                JsonObject result = new JsonObject();
                result.put("threshold", futureThreshold.result().getValue("threshold"));
                result.put("count" ,futureCount.result().getValue("count"));
                handler.handle(new Either.Right<>(result));
            } else {
                handler.handle(new Either.Left<>(event.cause().getMessage()));
            }
        });
    }
}
