package fr.openent.statistics_presences.indicator.worker;

import fr.openent.presences.common.presences.Presences;
import fr.openent.statistics_presences.StatisticsPresences;
import fr.openent.statistics_presences.bean.global.GlobalStat;
import fr.openent.statistics_presences.indicator.IndicatorWorker;
import fr.openent.statistics_presences.utils.EventType;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Global extends IndicatorWorker {
    private final List<EventType> eventTypes = Arrays.asList(EventType.DEPARTURE, EventType.INCIDENT, EventType.JUSTIFIED_UNREGULARIZED_ABSENCE, EventType.LATENESS, EventType.REGULARIZED_ABSENCE, EventType.UNJUSTIFIED_ABSENCE);
    static final String START_DATE = "1969-01-01"; //Fix that ? Do we really need school year dates ?
    static final String END_DATE = "2099-12-31";

    private Handler<Message<JsonObject>> countHandler(Future<List<GlobalStat>> future) {
        return SqlResult.validResultHandler(either -> {
            if (either.isLeft()) {
                future.fail(either.left().getValue());
            } else {
                JsonArray result = either.right().getValue();
                List<GlobalStat> stats = new ArrayList<>();
                for (int i = 0; i < result.size(); i++) {
                    JsonObject incident = result.getJsonObject(i);
                    GlobalStat stat = new GlobalStat()
                            .setSlots(0)
                            .setStartDate(incident.getString("start_date"))
                            .setEndDate(incident.getString("end_date"));
                    stats.add(stat);
                }

                future.complete(stats);
            }
        });
    }

    /*
        Global indicator. For each student retrieve (hour + slot):
        - absence count (unjustified + justified)
        - unjustified absence count
        - justified - unregularized absence count
        - regularized absence count
        - lateness count
        - departure count
        - sanction/punishment count <== Currently unprocessed
        - incident count
     */
    @Override
    @SuppressWarnings("unchecked")
    protected Future<List<JsonObject>> processStudent(String structureId, String studentId) {
        Future<List<JsonObject>> future = Future.future();
        List<Future> futures = new ArrayList<>();
        for (EventType eventType : eventTypes) {
            futures.add(fetchEvent(eventType, structureId, studentId));
        }

        Future<JsonArray> audienceFuture = retrieveAudiences(structureId, studentId);
        Future<JsonObject> studentFuture = retrieveUser(structureId, studentId);
        futures.add(audienceFuture);
        futures.add(studentFuture);
        CompositeFuture.all(futures).setHandler(ar -> {
            if (ar.failed()) {
                log.error(String.format("Failed to process student %s in structure %s for indicator %s", studentId, structureId, indicatorName()), ar.cause());
                future.fail(ar.cause());
            } else {
                log.debug(String.format("Student %s proceed", studentId));
                List<JsonObject> userStats = new ArrayList<>();
                JsonObject student = studentFuture.result();
                if (!student.containsKey("name")) {
                    future.complete(userStats);
                    return;
                }

                for (int i = 0; i < eventTypes.size(); i++) {
                    List<GlobalStat> stats = (List<GlobalStat>) futures.get(i).result();
                    final EventType type = eventTypes.get(i);
                    stats.forEach(stat -> {
                        stat.setUser(studentId)
                                .setName(student.getString("name"))
                                .setClassName(String.join(",", student.getJsonArray("className").getList()))
                                .setType(type)
                                .setStructure(structureId)
                                .setAudiences(audienceFuture.result());

                        userStats.add(stat.toJSON());
                    });
                }

                future.complete(userStats);
            }
        });

        return future;
    }

    private Future<List<GlobalStat>> fetchIncidentValue(String structureId, String studentId) {
        Future<List<GlobalStat>> future = Future.future();
        String query = "SELECT date as start_date, date as end_date " +
                "FROM %s.incident " +
                "INNER JOIN %s.protagonist ON (incident.id = protagonist.incident_id) " +
                "WHERE incident.structure_id = ? " +
                "AND protagonist.user_id = ?;";

        JsonArray params = new JsonArray()
                .add(structureId)
                .add(studentId);

        Sql.getInstance().prepared(String.format(query, StatisticsPresences.INCIDENTS_SCHEMA, StatisticsPresences.INCIDENTS_SCHEMA), params, countHandler(future));

        return future;
    }

    private Future<List<GlobalStat>> fetchEvent(EventType type, String structureId, String studentId) {
        Future<List<GlobalStat>> future;
        switch (type) {
            case INCIDENT:
                future = fetchIncidentValue(structureId, studentId);
                break;
            case DEPARTURE:
                future = retrieveEventCount(structureId, studentId, 3);
                break;
            case LATENESS:
                future = retrieveEventCount(structureId, studentId, 2);
                break;
            case UNJUSTIFIED_ABSENCE:
                future = fetchEventCountFromPresences(structureId, studentId, null, true, false);
                break;
            case JUSTIFIED_UNREGULARIZED_ABSENCE:
                future = fetchEventCountFromPresences(structureId, studentId, null, false, false);
                break;
            case REGULARIZED_ABSENCE:
                future = fetchEventCountFromPresences(structureId, studentId, null, false, true);
                break;
            default:
                future = Future.failedFuture(new RuntimeException("Unrecognized event type"));
        }

        return future;
    }

    private Future<List<GlobalStat>> fetchEventCountFromPresences(String structureId, String studentId, Boolean justified, Boolean noReasons, Boolean regularized) {
        Future<List<GlobalStat>> future = Future.future();
        Presences.getInstance().getEventsByStudent(1, Arrays.asList(studentId), structureId, justified, new ArrayList<>(), null, START_DATE, END_DATE, noReasons, recoveryMethod(structureId), regularized, either -> {
            if (either.isLeft()) {
                future.fail(either.left().getValue());
            } else {
                JsonArray result = either.right().getValue();
                List<GlobalStat> stats = new ArrayList<>();
                ((List<JsonObject>) result.getList()).forEach(event -> {
                    GlobalStat stat = new GlobalStat();
                    JsonArray events = event.getJsonArray("events", new JsonArray());
                    List<Integer> ids = ((List<JsonObject>) events.getList())
                            .stream()
                            .map(evt -> evt.getInteger("reason_id"))
                            .collect(Collectors.toList());
                    ids.removeAll(Collections.singleton(null));
                    stat.setSlots(events.size())
                            .setReasons(ids)
                            .setStartDate(event.getString("start_date"))
                            .setEndDate(event.getString("end_date"));
                    stats.add(stat);
                });

                future.complete(stats);
            }
        });

        return future;
    }

    private Future<List<GlobalStat>> retrieveEventCount(String structureId, String studentId, Integer eventType) {
        Future<List<GlobalStat>> future = Future.future();
        String query = "SELECT event.start_date, event.end_date " +
                "FROM %s.event " +
                "INNER JOIN %s.register ON (event.register_id = register.id) " +
                "WHERE event.student_id = ? " +
                "AND register.structure_id = ? " +
                "AND event.type_id = ?";
        JsonArray params = new JsonArray()
                .add(studentId)
                .add(structureId)
                .add(eventType);

        Sql.getInstance().prepared(String.format(query, StatisticsPresences.PRESENCES_SCHEMA, StatisticsPresences.PRESENCES_SCHEMA), params, countHandler(future));

        return future;
    }

    private Future<JsonArray> retrieveAudiences(String structureId, String studentId) {
        Future<JsonArray> future = Future.future();
        String query = "MATCH (u:User {id:{studentId}})-[:IN]->(:ProfileGroup)-[:DEPENDS]->(g:Class)-[:BELONGS]->(s:Structure {id:{structureId}}) return g.id as id " +
                "UNION " +
                "MATCH (u:User {id:{studentId}})-[:IN]->(g:FunctionalGroup)-[:DEPENDS]->(s:Structure {id:{structureId}}) return g.id as id";
        JsonObject params = new JsonObject()
                .put("studentId", studentId)
                .put("structureId", structureId);

        Neo4j.getInstance().execute(query, params, Neo4jResult.validResultHandler(either -> {
            if (either.isLeft()) {
                future.fail(either.left().getValue());
            } else {
                List<String> audiences = ((List<JsonObject>) either.right().getValue().getList()).stream().map(g -> g.getString("id")).collect(Collectors.toList());
                future.complete(new JsonArray(audiences));
            }
        }));

        return future;
    }

    private Future<JsonObject> retrieveUser(String structureId, String studentId) {
        Future<JsonObject> future = Future.future();
        String query = "MATCH (s:Structure {id:{structureId}})<-[:BELONGS]-(c:Class)<-[:DEPENDS]-(:ProfileGroup)<-[:IN]-" +
                "(u:User {id: {studentId}}) RETURN (u.lastName + ' ' + u.firstName) as name, collect(c.name) as className";
        JsonObject params = new JsonObject()
                .put("structureId", structureId)
                .put("studentId", studentId);

        Neo4j.getInstance().execute(query, params, Neo4jResult.validUniqueResultHandler(either -> {
            if (either.isLeft()) {
                future.fail(either.left().getValue());
            } else {
                future.complete(either.right().getValue());
            }
        }));

        return future;
    }
}
