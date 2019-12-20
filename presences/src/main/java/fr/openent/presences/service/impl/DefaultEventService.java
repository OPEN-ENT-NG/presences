package fr.openent.presences.service.impl;

import fr.openent.presences.Presences;
import fr.openent.presences.common.helper.WorkflowHelper;
import fr.openent.presences.common.viescolaire.Viescolaire;
import fr.openent.presences.enums.EventType;
import fr.openent.presences.enums.WorkflowActions;
import fr.openent.presences.helper.EventHelper;
import fr.openent.presences.model.Event.Event;
import fr.openent.presences.service.EventService;
import fr.openent.presences.service.SettingsService;
import fr.wseduc.webutils.Either;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DefaultEventService implements EventService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultEventService.class);
    private SettingsService settingsService = new DefaultSettingsService();
    private EventHelper eventHelper;
    private static String defaultStartTime = "00:00:00";
    private static String defaultEndTime = "23:59:59";

    public DefaultEventService(EventBus eb) {
        this.eventHelper = new EventHelper(eb);
    }

    @Override
    public void get(String structureId, String startDate, String endDate,
                    List<String> eventType, List<String> userId, JsonArray userIdFromClasses, List<String> classes,
                    Boolean regularized, Integer page, Handler<Either<String, JsonArray>> handler) {
        JsonArray params = new JsonArray();

        Sql.getInstance().prepared(this.getEventsQuery(structureId, startDate, endDate,
                eventType, regularized, userId, userIdFromClasses, page, params),
                params, SqlResult.validResultHandler((result -> {
                    if (result.isRight()) {
                        List<Event> events = EventHelper.getEventListFromJsonArray(result.right().getValue(), Event.MANDATORY_ATTRIBUTE);
                        List<Integer> reasonIds = new ArrayList<>();
                        List<String> studentIds = new ArrayList<>();

                        for (Event event : events) {
                            reasonIds.add(event.getReason().getId());
                            studentIds.add(event.getStudent().getId());
                        }
                        reasonIds.removeAll(Collections.singletonList(null));
                        studentIds.removeAll(Collections.singletonList(null));
                        Future<JsonObject> reasonFuture = Future.future();
                        Future<JsonObject> studentFuture = Future.future();

                        eventHelper.addReasonsToEvents(events, reasonIds, reasonFuture);
                        eventHelper.addStudentsToEvents(events, studentIds, startDate, endDate, structureId, studentFuture);

                        CompositeFuture.all(reasonFuture, studentFuture).setHandler(eventResult -> {
                            if (eventResult.failed()) {
                                String message = "[Presences@DefaultEventService] Failed to retrieve reason or student info";
                                LOGGER.error(message);
                                handler.handle(new Either.Left<>(message));
                            } else {
                                handler.handle(new Either.Right<>(new JsonArray(events)));
                            }
                        });
                    } else {
                        handler.handle(new Either.Left<>(result.left().getValue()));
                    }
                })));
    }


    @Override
    public void get(String startDate, String endDate, List<Number> eventType, List<String> users, Handler<Either<String, JsonArray>> handler) {
        JsonArray params = new JsonArray()
                .add(startDate)
                .add(endDate)
                .addAll(new JsonArray(eventType))
                .addAll(new JsonArray(users));

        String query = "SELECT start_date, end_date, student_id, type_id, reason_id, reason.label as reason " +
                "FROM " + Presences.dbSchema + ".event " +
                "LEFT JOIN " + Presences.dbSchema + ".reason ON (event.reason_id = reason.id) " +
                "WHERE start_date >= ? " +
                "AND end_date <= ? " +
                "AND type_id IN " + Sql.listPrepared(eventType) +
                " AND student_id IN " + Sql.listPrepared(users);

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    /**
     * GET query to fetch incidents
     *
     * @param structureId       structure identifier
     * @param startDate         start date
     * @param endDate           end date
     * @param userId            List userId []
     * @param userIdFromClasses userId fetched from classes neo4j
     * @param page              page
     * @param params            Json params
     */
    private String getEventsQuery(String structureId, String startDate, String endDate, List<String> eventType,
                                  Boolean regularized, List<String> userId, JsonArray userIdFromClasses,
                                  Integer page, JsonArray params) {

        String query = "WITH ids AS (SELECT e.id FROM presences.event e ";

        query += "INNER JOIN presences.register AS r ON (r.id = e.register_id AND r.structure_id = ?) ";
        params.add(structureId);

        if (startDate != null && endDate != null) {
            query += "WHERE e.start_date > ? ";
            query += "AND e.end_date < ? ";
            params.add(startDate + " 00:00:00");
            params.add(endDate + " 23:59:59");
        }

        query += setParamsForQueryEvents(userId, regularized, userIdFromClasses, params);

        if (page != null) {
            query += "ORDER BY e.start_date DESC OFFSET ? LIMIT ? ";
            params.add(Presences.PAGE_SIZE * page);
            params.add(Presences.PAGE_SIZE);
        }

        query += ") SELECT e.id, e.start_date, e.end_date, e.created, e.comment, e.student_id," +
                " e.reason_id, e.register_id, e.counsellor_regularisation, e.type_id, " +
                "to_json(event_type) as event_type, " +
                "to_json(e.reason_id) as reason " +
                "FROM presences.event e " +
                "INNER JOIN ids ON (ids.id = e.id) ";
        if (eventType != null && !eventType.isEmpty()) {
            query += "INNER JOIN presences.event_type AS event_type ON (event_type.id =" +
                    " e.type_id AND e.type_id IN " + Sql.listPrepared(eventType.toArray()) + " ) ";
            params.addAll(new JsonArray(eventType));
        } else {
            query += "INNER JOIN presences.event_type AS event_type ON event_type.id = e.type_id ";
        }

        query += "GROUP BY e.id, e.start_date, event_type.id ORDER BY e.start_date DESC";

        return query;
    }

    @Override
    public void getPageNumber(String structureId, String startDate, String endDate, List<String> eventType,
                              List<String> userId, Boolean regularized,
                              JsonArray userIdFromClasses, Handler<Either<String, JsonObject>> handler) {
        JsonArray params = new JsonArray();
        Sql.getInstance().prepared(this.getEventsQueryPagination(structureId, startDate, endDate, eventType,
                userId, regularized, userIdFromClasses, params),
                params, SqlResult.validUniqueResultHandler(handler));
    }

    private String getEventsQueryPagination(String structureId, String startDate, String endDate, List<String> eventType,
                                            List<String> userId, Boolean regularized,
                                            JsonArray userIdFromClasses, JsonArray params) {

        String query = "SELECT count(*) FROM presences.event e " +
                "INNER JOIN presences.register AS register ON (e.register_id = register.id AND register.structure_id = ?) ";
        params.add(structureId);

        if (eventType != null && !eventType.isEmpty()) {
            query += "INNER JOIN presences.event_type AS event_type ON (event_type.id = e.type_id  AND e.type_id IN "
                    + Sql.listPrepared(eventType.toArray()) + ") ";
            params.addAll(new JsonArray(eventType));
        }
        query += "WHERE register.start_date >= to_date(?, 'YYYY-MM-DD') ";
        params.add(startDate);
        query += "AND register.end_date <= to_date(?, 'YYYY-MM-DD') ";
        params.add(endDate);

        query += setParamsForQueryEvents(userId, regularized, userIdFromClasses, params);
        return query;
    }

    private String setParamsForQueryEvents(List<String> userId, Boolean regularized, JsonArray userIdFromClasses, JsonArray params) {
        String query = "";
        if (userIdFromClasses != null && !userIdFromClasses.isEmpty()) {
            query += "AND student_id IN " + Sql.listPrepared(userIdFromClasses.getList());
            for (int i = 0; i < userIdFromClasses.size(); i++) {
                params.add(userIdFromClasses.getJsonObject(i).getString("studentId"));
            }
        }
        if (userId != null && !userId.isEmpty()) {
            query += "AND student_id IN " + Sql.listPrepared(userId.toArray());
            params.addAll(new JsonArray(userId));
        }

        if (regularized != null) {
            query += "AND e.counsellor_regularisation = " + regularized + " ";
        }
        return query;
    }

    @Override
    public void create(JsonObject event, UserInfos user, Handler<Either<String, JsonObject>> handler) {
        JsonArray statements = new JsonArray();
        statements.add(getCreationStatement(event, user));

        if (EventType.ABSENCE.getType().equals(event.getInteger("type_id"))) {
            statements.add(getDeletionEventStatement(event));
        }

        Sql.getInstance().transaction(statements, statementEvent -> {
            Either<String, JsonObject> either = SqlResult.validUniqueResult(0, statementEvent);
            if (either.isLeft()) {
                String err = "[Presences@DefaultEventService] Failed to create event";
                LOGGER.error(err, either.left().getValue());
            }
            handler.handle(either);
        });
    }

    @Override
    public void update(Integer id, JsonObject event, Handler<Either<String, JsonObject>> handler) {
        Integer eventType = event.getInteger("type_id");
        JsonArray params = new JsonArray();

        String setter = "";
        if (EventType.DEPARTURE.getType().equals(eventType)) {
            setter = "start_date = ?";
            params.add(event.getString("start_date"));
        } else if (EventType.LATENESS.getType().equals(eventType)) {
            setter = "end_date = ?";
            params.add(event.getString("end_date"));
        } else if (EventType.REMARK.getType().equals(eventType)) {
            setter += "comment = ?";
            params.add(event.getString("comment"));
        }

        if (!EventType.REMARK.getType().equals(eventType) && event.containsKey("comment")) {
            setter += ", comment = ?";
            params.add(event.getString("comment"));
        }
        String query = "UPDATE " + Presences.dbSchema + ".event SET " + setter + " WHERE id = ?;";
        params.add(id);

        Sql.getInstance().prepared(query, params, message -> {
            Either<String, JsonObject> either = SqlResult.validUniqueResult(message);
            if (either.isLeft()) {
                String err = "[Presences@DefaultEventService] Failed to update event " + id;
                LOGGER.error(err, either.left().getValue());
            }

            handler.handle(either);
        });
    }

    @Override
    public void changeReasonEvents(JsonObject eventBody, Handler<Either<String, JsonObject>> handler) {
        JsonArray params = new JsonArray();
        String query = "UPDATE " + Presences.dbSchema + ".event SET reason_id = ? ";
        if (eventBody.getInteger("reasonId") != null) {
            params.add(eventBody.getInteger("reasonId"));
        } else {
            params.addNull();
        }
        query += " WHERE id IN " + Sql.listPrepared(eventBody.getJsonArray("ids").getList());
        params.addAll(eventBody.getJsonArray("ids"));
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void changeRegularizedEvents(JsonObject eventBody, Handler<Either<String, JsonObject>> handler) {
        JsonArray params = new JsonArray();
        String query = "UPDATE " + Presences.dbSchema + ".event SET counsellor_regularisation = ? ";
        params.add(eventBody.getBoolean("regularized"));

        query += " WHERE id IN " + Sql.listPrepared(eventBody.getJsonArray("ids").getList());
        params.addAll(eventBody.getJsonArray("ids"));
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void delete(Integer id, Handler<Either<String, JsonObject>> handler) {
        String query = "DELETE FROM " + Presences.dbSchema + ".event WHERE id = ?";
        JsonArray params = new JsonArray()
                .add(id);

        Sql.getInstance().prepared(query, params, SqlResult.validRowsResultHandler(handler));
    }

    @Override
    public void list(String structureId, String startDate, String endDate, List<Integer> eventType,
                     List<String> userId, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT event.id, event.start_date, event.end_date, event.type_id, event.reason_id, " +
                "to_char(register.start_date, 'YYYY-MM-DD HH24:MI:SS') as course_start_date, " +
                "to_char(register.end_date, 'YYYY-MM-DD HH24:MI:SS') as course_end_date, register.course_id " +
                "FROM  " + Presences.dbSchema + ".event " +
                "INNER JOIN " + Presences.dbSchema + ".register ON (event.register_id = register.id) " +
                "WHERE student_id IN " + Sql.listPrepared(userId) +
                " AND event.start_date >= ? " +
                "AND event.end_date <= ? " +
                "AND register.structure_id = ? " +
                "AND event.type_id IN " + Sql.listPrepared(eventType);
        JsonArray params = new JsonArray()
                .addAll(new JsonArray(userId))
                .add(startDate)
                .add(endDate)
                .add(structureId)
                .addAll(new JsonArray(eventType));

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void getCountEventByStudent(Integer eventType, List<String> students, String structure, Boolean justified,
                                       Integer startAt, List<Integer> reasonsId, Boolean massmailed, String startDate,
                                       String endDate, boolean noReasons, Handler<Either<String, JsonArray>> handler) {
        settingsService.retrieve(structure, event -> {
            if (event.isLeft()) {
                handler.handle(new Either.Left<>(event.left().getValue()));
                return;
            }
            List<Integer> eventTypes = Arrays.asList(eventType);
            JsonObject settings = event.right().getValue();
            String recoveryMethod = settings.getString("event_recovery_method");
            switch (recoveryMethod) {
                case "DAY":
                case "HOUR": {
                    JsonObject eventsQuery = getEventQuery(eventTypes, students, structure, justified, reasonsId,
                            massmailed, startDate, endDate, noReasons, recoveryMethod, null, null);
                    String query = "WITH count_by_user AS (WITH events as (" + eventsQuery.getString("query") + ") " +
                            "SELECT count(*), student_id FROM events GROUP BY student_id) SELECT * FROM count_by_user WHERE count >= " + startAt;
                    Sql.getInstance().prepared(query, eventsQuery.getJsonArray("params"), SqlResult.validResultHandler(handler));
                    break;
                }
                case "HALF_DAY":
                default:
                    Viescolaire.getInstance().getSlotProfileSetting(structure, evt -> {
                        if (evt.isLeft()) {
                            LOGGER.error("[Presences@DefaultEventService] Failed to retrieve default slot profile setting");
                            handler.handle(new Either.Left(evt.left().getValue()));
                            return;
                        }

                        JsonObject slotSetting = evt.right().getValue();
                        if (slotSetting.containsKey("end_of_half_day")) {
                            String halfOfDay = slotSetting.getString("end_of_half_day");
                            JsonObject morningQuery = getEventQuery(eventTypes, students, structure, justified,
                                    reasonsId, massmailed, startDate, endDate, noReasons, recoveryMethod, defaultStartTime, halfOfDay);
                            JsonObject afternoonQuery = getEventQuery(eventTypes, students, structure, justified,
                                    reasonsId, massmailed, startDate, endDate, noReasons, recoveryMethod, halfOfDay, defaultEndTime);
                            String query = "WITH count_by_user AS (WITH events as (" + morningQuery.getString("query") + " UNION ALL " + afternoonQuery.getString("query") + ") " +
                                    "SELECT count(*), student_id FROM events GROUP BY student_id) SELECT * FROM count_by_user WHERE count >= " + startAt;
                            JsonArray params = new JsonArray()
                                    .addAll(morningQuery.getJsonArray("params"))
                                    .addAll(afternoonQuery.getJsonArray("params"));
                            Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
                        } else {
                            handler.handle(new Either.Left<>("Structure does not initialize end of half day"));
                        }
                    });
            }
        });
    }

    private JsonObject getEventQuery(List<Integer> eventTypes, List<String> students, String structure, Boolean justified,
                                     List<Integer> reasonsId, Boolean massmailed, String startDate, String endDate,
                                     boolean noReasons, String recoveryMethod, String startTime, String endTime) {
        String dateCast = !"HOUR".equals(recoveryMethod) ? "::date" : "";
        String periodRange;
        periodRange = "HALF_DAY".equals(recoveryMethod) && endTime.equals(defaultEndTime) ? ",'AFTERNOON' as period " : "";
        periodRange = "HALF_DAY".equals(recoveryMethod) && startTime.equals(defaultStartTime) ? ",'MORNING' as period " : periodRange;
        String query = "SELECT event.student_id, event.start_date" + dateCast + ", event.end_date" +
                dateCast + ", event.type_id, '" + recoveryMethod +
                "' as recovery_method, json_agg(row_to_json(event)) as events " + periodRange +
                "FROM " + Presences.dbSchema + ".event INNER JOIN presences.register ON (register.id = event.register_id) " +
                "WHERE event.start_date" + dateCast + " >= ? " +
                "AND event.end_date" + dateCast + "<= ? " +
                "AND register.structure_id = ? " +
                "AND type_id IN " + Sql.listPrepared(eventTypes);
        JsonArray params = new JsonArray()
                .add(startDate)
                .add(endDate)
                .add(structure)
                .addAll(new JsonArray(eventTypes));

        if ("HALF_DAY".equals(recoveryMethod)) {
            query += " AND event.start_date::time >= '" + startTime + "' AND event.start_date::time <= '" + endTime + "'";
        }

        if (!students.isEmpty()) {
            query += " AND student_id IN " + Sql.listPrepared(students);
            params.addAll(new JsonArray(students));
        }
        if (justified != null) {
//            query += " AND (counsellor_regularisation = " + (justified ? "true) " : "false OR reason_id IS NULL) ");
            query += " AND reason_id IS " + (justified ? "NOT" : "") + " NULL ";
        }
        if (!reasonsId.isEmpty()) {
            query += " AND (reason_id IN " + Sql.listPrepared(reasonsId) + (noReasons ? " OR reason_id IS NULL" : "") + ") ";
            params.addAll(new JsonArray(reasonsId));
        }
        if (massmailed != null) {
            query += " AND massmailed = ? ";
            params.add(massmailed);
        }

        query += " GROUP BY event.start_date" + dateCast + ", event.student_id, event.end_date" + dateCast + ", event.type_id ";
        if (!"HALF_DAY".equals(recoveryMethod)) {
            query += "ORDER BY event.start_date" + dateCast;
        }
        return new JsonObject()
                .put("query", query)
                .put("params", params);
    }

    @Override
    public void getEventsByStudent(List<Integer> eventTypes, List<String> students, String structure, Boolean justified,
                                   List<Integer> reasonsId, Boolean massmailed, String startDate, String endDate,
                                   boolean noReasons, Handler<Either<String, JsonArray>> handler) {
        settingsService.retrieve(structure, event -> {
            if (event.isLeft()) {
                handler.handle(new Either.Left<>(event.left().getValue()));
                return;
            }

            Handler<Either<String, JsonArray>> queryHandler = eventsEvt -> {
                if (eventsEvt.isLeft()) {
                    handler.handle(eventsEvt);
                } else {
                    JsonArray events = eventsEvt.right().getValue();
                    for (int i = 0; i < events.size(); i++) {
                        events.getJsonObject(i).put("events", new JsonArray(events.getJsonObject(i).getString("events")));
                    }
                    handler.handle(new Either.Right<>(events));
                }
            };

            JsonObject settings = event.right().getValue();
            String recoveryMethod = settings.getString("event_recovery_method");
            switch (recoveryMethod) {
                case "DAY":
                case "HOUR": {
                    JsonObject eventsQuery = getEventQuery(eventTypes, students, structure, justified, reasonsId, massmailed, startDate, endDate, noReasons, recoveryMethod, null, null);
                    Sql.getInstance().prepared(eventsQuery.getString("query"), eventsQuery.getJsonArray("params"), SqlResult.validResultHandler(queryHandler));
                    break;
                }
                case "HALF_DAY":
                default:
                    Viescolaire.getInstance().getSlotProfileSetting(structure, evt -> {
                        if (evt.isLeft()) {
                            LOGGER.error("[Presences@DefaultEventService] Failed to retrieve default slot profile setting");
                            handler.handle(new Either.Left(evt.left().getValue()));
                            return;
                        }

                        JsonObject slotSetting = evt.right().getValue();
                        if (slotSetting.containsKey("end_of_half_day")) {
                            String halfOfDay = slotSetting.getString("end_of_half_day");
                            JsonObject morningQuery = getEventQuery(eventTypes, students, structure, justified, reasonsId, massmailed, startDate, endDate, noReasons, recoveryMethod, defaultStartTime, halfOfDay);
                            JsonObject afternoonQuery = getEventQuery(eventTypes, students, structure, justified, reasonsId, massmailed, startDate, endDate, noReasons, recoveryMethod, halfOfDay, defaultEndTime);
                            String query = "WITH events as (" + morningQuery.getString("query") + " UNION ALL " + afternoonQuery.getString("query") + ") SELECT * FROM events ORDER BY start_date";
                            JsonArray params = new JsonArray()
                                    .addAll(morningQuery.getJsonArray("params"))
                                    .addAll(afternoonQuery.getJsonArray("params"));
                            Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(queryHandler));
                        } else {
                            handler.handle(new Either.Left<>("Structure does not initialize end of half day"));
                        }
                    });
            }
        });
    }

    private JsonObject getDeletionEventStatement(JsonObject event) {
        String query = "DELETE FROM " + Presences.dbSchema + ".event WHERE type_id IN (2, 3) AND register_id = ? AND student_id = ?";
        JsonArray params = new JsonArray()
                .add(event.getInteger("register_id"))
                .add(event.getString("student_id"));

        return new JsonObject()
                .put("action", "prepared")
                .put("statement", query)
                .put("values", params);
    }

    private JsonObject getCreationStatement(JsonObject event, UserInfos user) {
        String query = "INSERT INTO " + Presences.dbSchema + ".event (start_date, end_date, comment, counsellor_input, student_id, register_id, type_id, owner) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
                "RETURNING id, start_date, end_date, comment, counsellor_input, student_id, register_id, type_id, reason_id;";
        JsonArray params = new JsonArray()
                .add(event.getString("start_date"))
                .add(event.getString("end_date"))
                .add(event.containsKey("comment") ? event.getString("comment") : "")
                .add(WorkflowHelper.hasRight(user, WorkflowActions.MANAGE.toString()))
                .add(event.getString("student_id"))
                .add(event.getInteger("register_id"))
                .add(event.getInteger("type_id"))
                .add(user.getUserId());

        return new JsonObject()
                .put("action", "prepared")
                .put("statement", query)
                .put("values", params);
    }
}