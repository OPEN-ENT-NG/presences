package fr.openent.statistics_presences.indicator;


import fr.openent.presences.common.helper.DateHelper;
import fr.openent.presences.common.helper.FutureHelper;
import fr.openent.presences.common.helper.IModelHelper;
import fr.openent.presences.common.incidents.Incidents;
import fr.openent.presences.common.presences.Presences;
import fr.openent.presences.common.viescolaire.Viescolaire;
import fr.openent.presences.core.constants.Field;
import fr.openent.presences.enums.ReasonType;
import fr.openent.presences.model.StructureStatisticsUser;
import fr.openent.statistics_presences.StatisticsPresences;
import fr.openent.statistics_presences.bean.Report;
import fr.openent.statistics_presences.indicator.worker.StatisticsWorker;
import io.vertx.core.*;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class IndicatorGeneric {
    static Logger log = LoggerFactory.getLogger(IndicatorGeneric.class);
    static final String START_DATE = "1969-01-01"; //Fix that ? Do we really need school year dates ?
    static final String END_DATE = "2099-12-31";

    private IndicatorGeneric() {
    }

    public static IndicatorGeneric getInstance() {
        return IndicatorGenericHolder.instance;
    }

    @SuppressWarnings("unchecked")
    public static Future<JsonArray> retrieveAudiences(String structureId, String studentId) {
        Promise<JsonArray> promise = Promise.promise();
        String query = "MATCH (u:User {id:{studentId}})-[:IN]->(:ProfileGroup)-[:DEPENDS]->(g:Class)-[:BELONGS]->(s:Structure {id:{structureId}}) return g.id as id " +
                "UNION " +
                "MATCH (u:User {id:{studentId}})-[:IN]->(g:FunctionalGroup)-[:DEPENDS]->(s:Structure {id:{structureId}}) return g.id as id";
        JsonObject params = new JsonObject()
                .put("studentId", studentId)
                .put("structureId", structureId);

        Neo4j.getInstance().execute(query, params, Neo4jResult.validResultHandler(either -> {
            if (either.isLeft()) {
                log.error(String.format("[Statistics@%s::retrieveAudiences] Fail to retrieve audiences %s",
                        IndicatorGeneric.class.getSimpleName(), either.left().getValue()));
                promise.fail(either.left().getValue());
            } else {
                List<String> audiences = ((List<JsonObject>) either.right().getValue().getList()).stream().map(g -> g.getString("id")).collect(Collectors.toList());
                promise.complete(new JsonArray(audiences));
            }
        }));

        return promise.future();
    }

    public static Future<JsonObject> retrieveUser(String structureId, String studentId) {
        Promise<JsonObject> promise = Promise.promise();
        String query = "MATCH (s:Structure {id:{structureId}})<-[:BELONGS]-(c:Class)<-[:DEPENDS]-(:ProfileGroup)<-[:IN]-" +
                "(u:User {id: {studentId}}) RETURN (u.lastName + ' ' + u.firstName) as name, collect(c.name) as className, " +
                "collect(c.id) as classIds";

        JsonObject params = new JsonObject()
                .put("structureId", structureId)
                .put("studentId", studentId);

        Neo4j.getInstance().execute(query, params, Neo4jResult.validUniqueResultHandler(either -> {
            if (either.isLeft()) {
                log.error(String.format("[Statistics@%s::retrieveUser] Fail to retrieve user %s",
                        IndicatorGeneric.class.getSimpleName(), either.left().getValue()));
                promise.fail(either.left().getValue());
            } else {
                promise.complete(either.right().getValue());
            }
        }));

        return promise.future();
    }

    public static Future<JsonObject> retrieveSettings(String structureId) {
        Promise<JsonObject> promise = Promise.promise();
        Presences.getInstance().getSettings(structureId, FutureHelper.handlerJsonObject(promise));
        return promise.future();
    }

    public static Future<JsonObject> retrieveSlotsSettings(String structureId) {
        Promise<JsonObject> promise = Promise.promise();
        Viescolaire.getInstance().getSlotProfileSetting(structureId, FutureHelper.handlerJsonObject(promise));
        return promise.future();
    }

    public static Future<JsonArray> retrieveReasons(String structureId) {
        Promise<JsonArray> promise = Promise.promise();
        Presences.getInstance().getReasons(structureId, ReasonType.ALL, FutureHelper.handlerJsonArray(promise));
        return promise.future();
    }

    /**
     * No filter date
     * 
     * @deprecated Replaced by {@link #fetchIncidentValue(String, String, String, String, String, String)}
     */
    @Deprecated
    public static Future<JsonArray> fetchIncidentValue(String structureId, String studentId, String select, String group) {
        return fetchIncidentValue(structureId, studentId, select, group, null, null);
    }

    public static Future<JsonArray> fetchIncidentValue(String structureId, String studentId, String select, String group,
                                                       String startDate, String endDate) {
        Promise<JsonArray> promise = Promise.promise();
        String query = "SELECT " + select + " " +
                "FROM %s.incident " +
                "INNER JOIN %s.protagonist ON (incident.id = protagonist.incident_id) " +
                "WHERE incident.structure_id = ? " +
                "AND protagonist.user_id = ? ";

        JsonArray params = new JsonArray()
                .add(structureId)
                .add(studentId);

        if (startDate != null && endDate != null) {
            query += "AND incident.date >= ? ";
            query += "AND incident.date <= ? ";
            params.add(startDate);
            params.add(endDate);
        }

        if (group != null) {
            query += "GROUP BY " + group;
        }

        Sql.getInstance().prepared(String.format(query, StatisticsPresences.INCIDENTS_SCHEMA, StatisticsPresences.INCIDENTS_SCHEMA), params,
                SqlResult.validResultHandler(FutureHelper.handlerJsonArray(promise)));

        return promise.future();
    }

    /**
     * No filter date
     *
     * @deprecated Replaced by {@link #retrieveEventCount(String, String, Integer, String, String, List, String, String)}
     */
    @Deprecated
    public static Future<JsonArray> retrieveEventCount(String structureId, String studentId, Integer eventType, String select, String group, List<Integer> reasonIds) {
        return retrieveEventCount(structureId, studentId, eventType, select, group, reasonIds, null, null);
    }

    public static Future<JsonArray> retrieveEventCount(String structureId, String studentId, Integer eventType, String select, String group,
                                                       List<Integer> reasonIds, String startDate, String endDate) {
        Promise<JsonArray> promise = Promise.promise();
        String query = "SELECT " + select + " " +
                "FROM %s.event " +
                "INNER JOIN %s.register ON (event.register_id = register.id) " +
                "WHERE event.student_id = ? " +
                "AND register.structure_id = ? " +
                "AND event.type_id = ? " +
                "AND (event.reason_id IS NULL";
        JsonArray params = new JsonArray()
                .add(studentId)
                .add(structureId)
                .add(eventType);


        if (reasonIds != null && !reasonIds.isEmpty()){
            query += " OR event.reason_id IN " + Sql.listPrepared(reasonIds);
            params.addAll(new JsonArray(reasonIds));
        }
        query += ") ";

        if (startDate != null && endDate != null) {
            query += "AND event.start_date >= ? ";
            query += "AND event.end_date <= ? ";
            params.add(startDate);
            params.add(endDate);
        }

        if (group != null) {
            query += "GROUP BY ? ";
            params.add(group);
        }


        Sql.getInstance().prepared(String.format(query, StatisticsPresences.PRESENCES_SCHEMA, StatisticsPresences.PRESENCES_SCHEMA), params,
                SqlResult.validResultHandler(FutureHelper.handlerJsonArray(promise)));

        return promise.future();
    }

    /**
     * No filter date
     *
     * @deprecated Replaced by {@link #fetchEventsFromPresences(String, String, List, Boolean, Boolean, String, String)}
     */
    @Deprecated
    public static Future<JsonArray> fetchEventsFromPresences(String structureId, String studentId, List<Integer> reasonIds, Boolean noReasons, Boolean regularized) {
        return fetchEventsFromPresences(structureId, studentId, reasonIds, noReasons, regularized, START_DATE, END_DATE);
    }

    public static Future<JsonArray> fetchEventsFromPresences(String structureId, String studentId, List<Integer> reasonIds,
                                                             Boolean noReasons, Boolean regularized, String startDate, String endDate) {
        Promise<JsonArray> promise = Promise.promise();
        String startTime = startDate == null ? START_DATE : DateHelper.getDateString(startDate, DateHelper.YEAR_MONTH_DAY);
        String endTime = endDate == null ? END_DATE : DateHelper.getDateString(endDate, DateHelper.YEAR_MONTH_DAY);
        Presences.getInstance().getEventsByStudent(1, Collections.singletonList(studentId), structureId, null,
                reasonIds, null, startTime, endTime, noReasons, "HOUR", regularized,
                FutureHelper.handlerJsonArray(promise));
        return promise.future();
    }

    /**
     * No filter date
     *
     * @deprecated Replaced by {@link #retrievePunishments(String, String, String, String, String)}
     */
    @Deprecated
    public static Future<JsonArray> retrievePunishments(String structureId, String studentId, String eventType) {
        return retrievePunishments(structureId, studentId, eventType, null, null);
    }

    public static Future<JsonArray> retrievePunishments(String structureId, String studentId, String eventType, String startDate, String endDate) {
        Promise<JsonArray> promise = Promise.promise();
        Incidents.getInstance().getPunishmentsByStudent(structureId, startDate, endDate, Collections.singletonList(studentId),
                null, eventType, null, null, FutureHelper.handlerJsonArray(promise));
        return promise.future();
    }


    private static class IndicatorGenericHolder {
        private static final IndicatorGeneric instance = new IndicatorGeneric();

        private IndicatorGenericHolder() {
        }
    }

    /**
     * Process CRON indicator calculation
     *
     * @param structures student stats data group by structure
     * @return Future ending process
     * @deprecated  Replaced by {@link #process(Vertx, List)}
     */
    @Deprecated
    public static Future<Report> process(Vertx vertx, JsonObject structures) {
        log.info(String.format("[StatisticsPresences@%s::process] processing all indicator", IndicatorGeneric.class.getName()));
        Promise<Report> promise = Promise.promise();

        MessageConsumer<Report> consumer = vertx.eventBus().consumer("fr.openent.statistics_presences.indicator.impl.StatisticsWorker");
        Handler<Message<Report>> handler = message -> {
            log.info(String.format("[StatisticsPresences@%s::process] SIGTERM sent", IndicatorGeneric.class.getName()));
            consumer.unregister();
            promise.handle(Future.succeededFuture(message.body()));
        };

        consumer.handler(handler);
        deployWorker(vertx, structures);
        return promise.future();
    }

    /**
     * Process CRON indicator calculation
     *
     * @param structureStatisticsUserList student stats data group by structure
     * @return Future ending process
     */
    public static Future<Report> process(Vertx vertx, List<StructureStatisticsUser> structureStatisticsUserList) {
        log.info(String.format("[StatisticsPresences@%s::process] processing all indicator", IndicatorGeneric.class.getName()));
        Promise<Report> promise = Promise.promise();

        MessageConsumer<Report> consumer = vertx.eventBus().consumer("fr.openent.statistics_presences.indicator.impl.StatisticsWorker");
        Handler<Message<Report>> handler = message -> {
            log.info(String.format("[StatisticsPresences@%s::process] SIGTERM sent", IndicatorGeneric.class.getName()));
            consumer.unregister();
            promise.handle(Future.succeededFuture(message.body()));
        };

        consumer.handler(handler);
        deployWorker(vertx, structureStatisticsUserList);
        return promise.future();
    }

    /**
     * Process one indicator to run computing statistics
     * (JsonObject is a map with structure id as key and array of student id)
     *
     * @param structureStatisticsUserList student stats data group by structure
     * @return Future ending process
     */
    public static Future<JsonObject> manualProcess(List<StructureStatisticsUser> structureStatisticsUserList) {
        Promise<JsonObject> promise = Promise.promise();
        StatisticsWorker indicatorWorker = new StatisticsWorker();
        // todo to change indicatorWorker, should not be named "worker" as it is not the case
        indicatorWorker.manualStart(structureStatisticsUserList)
                .onSuccess(promise::complete)
                .onFailure(promise::fail);
        return promise.future();
    }

    /**
     * @deprecated  Replaced by {@link #deployWorker(Vertx, List)}
     */
    @Deprecated
    private static void deployWorker(Vertx vertx, JsonObject structures) {
        JsonObject config = new JsonObject()
                .put(Field.STRUCTURES, structures)
                .put(Field.ENDPOINT, "fr.openent.statistics_presences.indicator.impl.StatisticsWorker");

        String workerName = String.format("%s.worker.StatisticsWorker", Indicator.class.getPackage().getName());
        vertx.deployVerticle(workerName, new DeploymentOptions().setConfig(config).setWorker(true));
    }

    private static void deployWorker(Vertx vertx, List<StructureStatisticsUser> structureStatisticsUserList) {
        JsonObject config = new JsonObject()
                .put(Field.STRUCTURE_STATISTICS_USERS, IModelHelper.toJsonArray(structureStatisticsUserList))
                .put(Field.ENDPOINT, "fr.openent.statistics_presences.indicator.impl.StatisticsWorker");

        String workerName = String.format("%s.worker.StatisticsWorker", Indicator.class.getPackage().getName());
        vertx.deployVerticle(workerName, new DeploymentOptions().setConfig(config).setWorker(true));
    }

}

