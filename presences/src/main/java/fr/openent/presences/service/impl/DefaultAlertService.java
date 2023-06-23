package fr.openent.presences.service.impl;

import fr.openent.presences.Presences;
import fr.openent.presences.common.helper.FutureHelper;
import fr.openent.presences.common.helper.IModelHelper;
import fr.openent.presences.common.viescolaire.Viescolaire;
import fr.openent.presences.core.constants.Field;
import fr.openent.presences.db.DBService;
import fr.openent.presences.helper.SQLHelper;
import fr.openent.presences.model.Alert;
import fr.openent.presences.model.AlertFilterModel;
import fr.openent.presences.service.AlertService;
import fr.wseduc.webutils.Either;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;


import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DefaultAlertService extends DBService implements AlertService {

    private static final Logger log = LoggerFactory.getLogger(DefaultAlertService.class);

    @Override
    public Future<JsonObject> delete(String structureId, Map<String, List<String>> deletedAlertMap, String startDate, String endDate, String startTime, String endTime) {
        Promise<JsonObject> promise = Promise.promise();
        if (deletedAlertMap != null && deletedAlertMap.isEmpty()) promise.complete(new JsonObject());
        else {
            JsonArray params = new JsonArray();
            String query = String.format("DELETE FROM %s.alerts %s",
                    Presences.dbSchema,
                    getWhereDeleteFilter(params, structureId, deletedAlertMap, startDate, endDate, startTime, endTime)
            );
            sql.prepared(query, params, SqlResult.validUniqueResultHandler(FutureHelper.handlerEitherPromise(promise)));
        }


        return promise.future();
    }

    public static String getWhereDeleteFilter(JsonArray params, String structureId, Map<String, List<String>> deletedAlertMap, String startDate, String endDate, String startTime, String endTime) {
        String query = "AND structure_id = ? ";
        params.add(structureId);

        final StringBuilder studentTypeFilterBuilder = new StringBuilder();
        if (deletedAlertMap != null) {
            deletedAlertMap.forEach((studentId, alertType) -> {
                if (!alertType.isEmpty()) {
                    studentTypeFilterBuilder.append(" OR (student_id = ? AND type IN ").append(Sql.listPrepared(alertType)).append(")");
                    params.add(studentId);
                    params.addAll(new JsonArray(alertType));
                }
            });
        }
        String studentTypeFilter = studentTypeFilterBuilder.toString().replaceFirst("OR ", "AND (");
        if (!studentTypeFilter.isEmpty()) query += studentTypeFilter + ") ";

        if (startDate != null && startTime != null) {
            query += "AND date >= ?::date + ?::time ";
            params.add(startDate);
            params.add(startTime);
        }

        if (endDate != null && endTime != null) {
            query += "AND date <= ?::date + ?::time ";
            params.add(endDate);
            params.add(endTime);
        }

        return query.replaceFirst("AND", "WHERE");
    }

    @Override
    public Future<JsonObject> getSummary(String structureId) {
        Promise<JsonObject> promise = Promise.promise();
        Viescolaire.getInstance().getSchoolYear(structureId)
                .compose(schoolYear -> {
                    String query = "SELECT tc.type, count(*) AS count FROM (SELECT type, count(*) AS count FROM " +
                            Presences.dbSchema + ".alerts WHERE structure_id = ? AND date >= ? GROUP BY student_id, type) as tc" +
                            " WHERE tc.count >= " + Presences.dbSchema + ".get_alert_thresholder(tc.type, ?) GROUP BY tc.type;";
                    JsonArray params = new JsonArray(Arrays.asList(structureId, schoolYear.getString("start_date"), structureId));
                    Promise<JsonArray> promiseSql = Promise.promise();
                    Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(FutureHelper.handlerEitherPromise(promiseSql)));
                    return promiseSql.future();
                })
                .onSuccess(values -> {
                    JsonObject summary = new JsonObject();
                    values.forEach(value -> summary.put(((JsonObject) value).getString(Field.TYPE), ((JsonObject) value).getLong(Field.COUNT)));
                    promise.complete(summary);
                })
                .onFailure(error -> {
                    String message = String.format("[Presences@DefaultAlertService::getSummary] Failed to get alert summary %s", error.getMessage());
                    log.error(message);
                    promise.fail(error.getMessage());
                });

        return promise.future();
    }

    @Override
    public Future<List<Alert>> getAlertsStudents(AlertFilterModel alertFilter) {
        Promise<List<Alert>> promise = Promise.promise();
        JsonArray params = new JsonArray();
        String query = "SELECT student_id, type, count(*) AS count FROM " +
                Presences.dbSchema + ".alerts";

        query += alertsFiltersQuery(alertFilter, params);

        query += " GROUP BY student_id, type HAVING count(*) >= " + Presences.dbSchema + ".get_alert_thresholder(type, ?)";
        params.add(alertFilter.getStructureId());


        if (alertFilter.getPage() != null) {
            query += SQLHelper.addLimitOffset(Presences.ALERT_PAGE_SIZE, Presences.ALERT_PAGE_SIZE * alertFilter.getPage(), params);
        }
        query += ";";

        Promise<JsonArray> alertPromise = Promise.promise();
        alertPromise.future()
                .compose(alerts -> {
                    // Retrieve student's alert present ID with alerts
                    JsonArray studentsAlerts = new JsonArray();
                    for (int i = 0; i < alerts.size(); i++) {
                        studentsAlerts.add(alerts.getJsonObject(i).getString(Field.STUDENT_ID));
                    }

                    // Get names, first names and class name
                    String studentQuery =
                            "MATCH (u:User)-[:IN]->(:ProfileGroup)-[:DEPENDS]->(c:Class) " +
                                    "WHERE u.id IN {studentsId} " +
                                    "RETURN u.firstName as firstName, u.lastName as lastName, c.name as audience, u.id as student_id;";

                    JsonObject studentParam = new JsonObject().put(Field.STUDENTSID, studentsAlerts);
                    Promise<JsonArray> studentPromise = Promise.promise();
                    Neo4j.getInstance().execute(studentQuery, studentParam, Neo4jResult.validResultHandler(FutureHelper.handlerEitherPromise(studentPromise)));
                    return studentPromise.future();
                }).onSuccess(studentList -> {
                    JsonArray alerts = alertPromise.future().result();
                    Map<String, JsonObject> studentMap = studentList.stream()
                            .map(JsonObject.class::cast)
                            .collect(Collectors.groupingBy(student -> student.getString(Field.STUDENT_ID)))
                            .entrySet()
                            .stream().collect(Collectors.toMap(Map.Entry::getKey, student -> student.getValue().get(0))); //can not throw OutOfBound thanks to groupingBy

                    alerts.stream()
                            .map(JsonObject.class::cast)
                            .filter(alert -> studentMap.containsKey(alert.getString(Field.STUDENT_ID)))
                            .forEach(alert -> {
                                String studentId = alert.getString(Field.STUDENT_ID);
                                alert.put(Field.NAME, studentMap.get(studentId).getString(Field.LASTNAME) + " " + studentMap.get(studentId).getString(Field.FIRSTNAME));
                                alert.put(Field.LASTNAME, studentMap.get(studentId).getString(Field.LASTNAME));
                                alert.put(Field.FIRSTNAME, studentMap.get(studentId).getString(Field.FIRSTNAME));
                                alert.put(Field.AUDIENCE, studentMap.get(studentId).getString(Field.AUDIENCE));
                            });

                    promise.complete(IModelHelper.toList(alerts, Alert.class));
                })
                .onFailure(error -> {
                    log.error(String.format("[Presences@DefaultAlertService::getAlertsStudents] Failed to get alert student %s", error.getMessage()));
                    promise.fail(error.getMessage());
                });
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(FutureHelper.handlerEitherPromise(alertPromise)));

        return promise.future();
    }



    @Override
    public void getStudentAlertNumberWithThreshold(String structureId, String studentId, String type, Handler<Either<String, JsonObject>> handler) {
        Future<JsonObject> futureThreshold = Future.future();
        Future<JsonObject> futureCount = Future.future();

        String queryThreshold = "SELECT alert_forgotten_notebook_threshold as threshold" +
                " FROM " + Presences.dbSchema + ".settings " +
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

        String queryCount = "SELECT COUNT(*)" +
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
                result.put("count", futureCount.result().getValue("count"));
                handler.handle(new Either.Right<>(result));
            } else {
                handler.handle(new Either.Left<>(event.cause().getMessage()));
            }
        });
    }

    @Override
    public Future<JsonObject> resetStudentAlertsCount(String structureId, String studentId, String type) {
        Promise<JsonObject> promise = Promise.promise();

        String query = "DELETE FROM " + Presences.dbSchema + ".alerts" +
                " WHERE student_id = ? AND structure_id = ? AND type = ? ";

        JsonArray params = new JsonArray().add(studentId)
                .add(structureId)
                .add(type);

        sql.prepared(query, params, SqlResult.validUniqueResultHandler(res -> {
            if (res.isLeft()) {
                promise.fail(res.left().getValue());
            } else {
                promise.complete(new JsonObject().put("status", "ok"));
            }
        }));

        return promise.future();
    }


    public Future<Integer> getPageCount(AlertFilterModel alertFilter){
        Promise<Integer> promise = Promise.promise();
        JsonArray params = new JsonArray();

        String query = " SELECT count(distinct(student_id, type)) as count from "
                + Presences.dbSchema +  ".alerts ";

        query += alertsFiltersQuery(alertFilter, params);

        Sql.getInstance().prepared(query,params,SqlResult.validUniqueResultHandler(res -> {
            if (res.isLeft()) {
                promise.fail(res.left().getValue());
            } else {
                promise.complete(res.right().getValue().getInteger(Field.COUNT, 0));
            }
        }));
        return promise.future();
    }

    private String alertsFiltersQuery(AlertFilterModel alertFilter, JsonArray params) {
        StringBuilder conditions = new StringBuilder();
        if (alertFilter.getStructureId() != null) {
            conditions.append(" AND");
            conditions.append(SQLHelper.structureId(alertFilter.getStructureId(), params));
        }
        if (!alertFilter.getTypes().isEmpty()) {
            conditions.append(" AND");
            conditions.append(SQLHelper.filterList("type", alertFilter.getTypes(), params));
        }
        if (!alertFilter.getStudents().isEmpty()) {
            conditions.append(" AND");
            conditions.append(SQLHelper.filterList("student_id", alertFilter.getStudents(), params));
        }
        if (alertFilter.getStartDate() != null && alertFilter.getStartTime() != null) {
            conditions.append(" AND");
            conditions.append(SQLHelper.startDateStartTime(alertFilter.getStartDate(), alertFilter.getStartTime(), params));
        }
        if (alertFilter.getEndDate() != null && alertFilter.getEndTime() != null) {
            conditions.append(" AND");
            conditions.append(SQLHelper.endDateEndTime(alertFilter.getEndDate(), alertFilter.getEndTime(), params));
        }

        return conditions.toString().replaceFirst("AND", "WHERE");
    }


}
