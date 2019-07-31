package fr.openent.presences.service.impl;

import fr.openent.presences.Presences;
import fr.openent.presences.common.helper.RegisterHelper;
import fr.openent.presences.enums.EventType;
import fr.openent.presences.service.EventService;
import fr.wseduc.webutils.Either;
import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;

import java.util.List;

public class DefaultEventService implements EventService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultEventService.class);
    private RegisterHelper registerHelper;
    private EventBus eb;

    public DefaultEventService(EventBus eb) {
        this.eb = eb;
        this.registerHelper = new RegisterHelper(eb, Presences.dbSchema);
    }

    @Override
    public void get(String structureId, String startDate, String endDate,
                    List<String> eventType, List<String> userId, JsonArray userIdFromClasses, List<String> classes,
                    boolean unjustified, boolean regularized, Integer page, Handler<Either<String, JsonArray>> handler) {
        JsonArray params = new JsonArray();

        Sql.getInstance().prepared(this.getEventsQuery(structureId, startDate, endDate,
                eventType, unjustified, regularized, userId, userIdFromClasses, page, params),
                params, SqlResult.validResultHandler((result -> {
                    if (result.isRight()) {
                        JsonArray reasonIds = new JsonArray();
                        JsonArray arrayEvents = result.right().getValue();

                        Future<JsonObject> reasonInfoFuture = Future.future();
                        Future<JsonObject> studentInfoFuture = Future.future();

                        for (int i = 0; i < arrayEvents.size(); i++) {
                            // formatting json since our object event_type is string formatted
                            arrayEvents.getJsonObject(i).put("event_type",
                                    new JsonObject(arrayEvents.getJsonObject(i).getString("event_type")));

                            // Fetching reason ids available
                            if (arrayEvents.getJsonObject(i).getLong("reason_id") != null) {
                                reasonIds.add(arrayEvents.getJsonObject(i).getLong("reason_id"));
                            }
                        }

                        getReasonFromEvent(arrayEvents, reasonIds, reasonInfoFuture);
                        getStudentFromEvent(startDate, endDate, structureId, arrayEvents, studentInfoFuture);

                        CompositeFuture.all(reasonInfoFuture, studentInfoFuture).setHandler(event -> {
                            if (event.failed()) {
                                String message = "[Presences@DefaultEventService] Failed to retrieve reason or student info";
                                LOGGER.error(message);
                                handler.handle(new Either.Left<>(message));
                            } else {
                                handler.handle(new Either.Right<>(arrayEvents));
                            }
                        });
                    } else {
                        handler.handle(new Either.Left<>(result.left().getValue()));
                    }
                })));
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
                                  boolean unjustified, boolean regularized,
                                  List<String> userId, JsonArray userIdFromClasses, Integer page, JsonArray params) {

        String query = "WITH ids AS (SELECT e.id FROM presences.event e ";

        query += "INNER JOIN presences.register AS r ON (r.id = e.register_id AND r.structure_id = ?) ";
        params.add(structureId);

        if (eventType != null && !eventType.isEmpty()) {
            query += "INNER JOIN presences.event_type AS event_type ON (event_type.id = e.type_id AND e.type_id IN " + Sql.listPrepared(eventType.toArray()) + " )";
            params.addAll(new JsonArray(eventType));
        }

        if (startDate != null && endDate != null) {
            query += "WHERE e.start_date > ? ";
            query += "AND e.end_date < ? ";
            params.add(startDate + " 00:00:00");
            params.add(endDate + " 23:59:59");
        }

        query += setParamsForQueryEvents(userId, unjustified, regularized, userIdFromClasses, params);

        if (page != null) {
            query += "ORDER BY e.start_date OFFSET ? LIMIT ? ";
            params.add(Presences.PAGE_SIZE * page);
            params.add(Presences.PAGE_SIZE);
        }

        query += ") SELECT e.id, e.start_date, e.end_date, e.created, e.comment, e.student_id," +
                " e.reason_id, e.register_id, e.counsellor_regularisation, e.type_id, " +
                "to_json(event_type) as event_type, " +
                "to_json(e.reason_id) as reason " +
                "FROM presences.event e " +
                "INNER JOIN ids ON (ids.id = e.id) " +
                "INNER JOIN presences.event_type AS event_type ON event_type.id = e.type_id ";

        query += "GROUP BY e.id, e.start_date, event_type.id";

        return query;
    }

    private void getReasonFromEvent(JsonArray arrayEvents, JsonArray reasonIds, Future future) {
        if (reasonIds.size() == 0 || reasonIds.isEmpty()) {
            future.complete();
            return;
        }
        String query = "SELECT r.id, to_json(r.*) as reason, to_json(type.*) as type FROM " + Presences.dbSchema +
                ".reason r INNER JOIN " + Presences.dbSchema + ".reason_type as type ON type.id = r.type_id " +
                "WHERE r.id IN " + Sql.listPrepared(reasonIds.getList());
        JsonArray params = new JsonArray().addAll(reasonIds);

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler((reasonResult -> {
            if (reasonResult.isRight()) {
                // formatting json object since our "to_json" is string formatted
                for (int i = 0; i < reasonResult.right().getValue().size(); i++) {
                    reasonResult.right().getValue().getJsonObject(i).put("reason",
                            new JsonObject(reasonResult.right().getValue().getJsonObject(i).getString("reason")));
                    reasonResult.right().getValue().getJsonObject(i).put("type",
                            new JsonObject(reasonResult.right().getValue().getJsonObject(i).getString("type")));
                }

                // Adding reason object to event who possesses reason id (ignore if reason_id is null)
                for (int i = 0; i < arrayEvents.size(); i++) {
                    for (int j = 0; j < reasonResult.right().getValue().size(); j++) {
                        JsonObject reason = reasonResult.right().getValue().getJsonObject(j);
                        if (arrayEvents.getJsonObject(i).getLong("reason_id") != null) {
                            if (arrayEvents.getJsonObject(i).getLong("reason_id").equals(reason.getLong("id"))) {
                                arrayEvents.getJsonObject(i).put("reason", reason);
                            }
                        }
                    }
                }
                future.complete();
            } else {
                future.fail("Failed to query reason info");
            }
        })));
    }

    private void getStudentFromEvent(String startDate, String endDate, String structureId, JsonArray arrayEvents, Future future) {
        JsonArray idStudents = new JsonArray();

        // Fetching student id
        for (int i = 0; i < arrayEvents.size(); i++) {
            String studentId = arrayEvents.getJsonObject(i).getString("student_id");
            if (!idStudents.contains(studentId)) {
                idStudents.add(studentId);
            }
        }

        registerHelper.getRegisterEventHistory(startDate, endDate, idStudents, historyEvent -> {
            if (historyEvent.isLeft()) {
                future.fail("[Presences@DefaultEventService] Failed to retrieve register history");
                return;
            }
            for (int i = 0; i < historyEvent.right().getValue().size(); i++) {
                historyEvent.right().getValue().getJsonObject(i).put("events",
                        new JsonArray(historyEvent.right().getValue().getJsonObject(i).getString("events")));
            }
            JsonArray studentEvents = historyEvent.right().getValue();

            String query = "MATCH (s:Structure {id: {structureId} })<-[:ADMINISTRATIVE_ATTACHMENT]-" +
                    "(u:User {profiles:['Student']})-[:IN]->(:ProfileGroup)-[:DEPENDS]->(c:Class) WHERE u.id IN {idStudents} " +
                    "RETURN distinct (u.lastName + ' ' + u.firstName) as displayName, u.id as id, c.name as classeName";
            JsonObject params = new JsonObject().put("structureId", structureId).put("idStudents", idStudents);
            Neo4j.getInstance().execute(query, params, Neo4jResult.validResultHandler(studentResult -> {
                if (studentResult.isRight()) {
                    JsonArray students = studentResult.right().getValue();

                    // Adding student object to event who possesses student_id
                    for (int i = 0; i < arrayEvents.size(); i++) {
                        for (int j = 0; j < students.size(); j++) {
                            JsonObject student = students.getJsonObject(j);
                            //  Add student object if there's no event anyway
                            if (studentEvents.isEmpty()) {
                                if (arrayEvents.getJsonObject(i).getString("student_id").equals(student.getString("id"))) {
                                    student.put("day_history", new JsonArray());
                                    arrayEvents.getJsonObject(i).put("student", student);
                                }
                            } else {
                                for (int k = 0; k < studentEvents.size(); k++) {
                                    if (arrayEvents.getJsonObject(i).getString("student_id").equals(student.getString("id"))) {
                                        if (student.getString("id").equals(studentEvents.getJsonObject(k).getString("student_id"))) {
                                            arrayEvents.getJsonObject(i).put("student", new JsonObject()
                                                    .put("displayName", student.getString("displayName"))
                                                    .put("id", student.getString("id"))
                                                    .put("classeName", student.getString("classeName"))
                                                    .put("day_history",
                                                            filterEvents(studentEvents.getJsonObject(k).getJsonArray("events"),
                                                                    arrayEvents.getJsonObject(i).getJsonObject("event_type").getInteger("id"))));
                                        }
                                    }
                                }
                            }
                        }
                    }
                    matchSlots(arrayEvents, structureId, future);
                } else {
                    future.fail("Failed to query student info");
                }
            }));
        });
    }

    private JsonArray filterEvents(JsonArray events, Integer eventTypeId) {
        JsonArray filteredEvents = new JsonArray();

        for (int i = 0; i < events.size(); ++i) {
            JsonObject obj = events.getJsonObject(i);
            if (obj.getInteger("type_id").equals(eventTypeId)) {
                filteredEvents.add(obj);
            }
        }
        return filteredEvents;
    }

    /**
     * Squash events student event history and structure slot profile.
     *
     * @param events      Events
     * @param structureId Structure identifier
     * @param future      Function handler returning data
     */
    private void matchSlots(JsonArray events, String structureId, Future future) {
        JsonObject action = new JsonObject()
                .put("action", "timeslot.getSlotProfiles")
                .put("structureId", structureId);

        eb.send("viescolaire", action, (Handler<AsyncResult<Message<JsonObject>>>) result -> {
            String status = result.result().body().getString("status");
            JsonObject body = result.result().body();
            JsonArray slots = new JsonArray();
            if ("error".equals(status)) {
                LOGGER.error("[Presences@DefaultEventService] Failed to retrieve slot profile");
            } else if (body.getJsonObject("result").containsKey("slots") && !body.getJsonObject("result").getJsonArray("slots").isEmpty()) {
                slots = body.getJsonObject("result").getJsonArray("slots");
            }

            for (int i = 0; i < events.size(); i++) {
                JsonObject event = events.getJsonObject(i);
                try {
                    JsonArray clone = registerHelper.cloneSlots(slots, event.getString("start_date"));
                    JsonObject student = event.getJsonObject("student");
                    JsonArray history = student.getJsonArray("day_history");
                    JsonArray userSlots = clone.copy();

                    if (history.size() == 0) {
                        student.put("day_history", userSlots);
                    } else {
                        if (student.getJsonArray("day_history").size() != 9) {
                            student.put("day_history", registerHelper.mergeEventsSlots(student.getJsonArray("day_history"), userSlots));
                        }
                    }
                } catch (Exception e) {
                    String message = "[Presences@DefaultEventService] Failed to parse slots";
                    LOGGER.error(message, e);
                    future.fail(message);
                    return;
                }
            }
            future.complete();
        });
    }

    @Override
    public void getPageNumber(String structureId, String startDate, String endDate, List<String> eventType,
                              List<String> userId, boolean unjustified, boolean regularized,
                              JsonArray userIdFromClasses, Handler<Either<String, JsonObject>> handler) {
        JsonArray params = new JsonArray();
        Sql.getInstance().prepared(this.getEventsQueryPagination(structureId, startDate, endDate, eventType,
                userId, unjustified, regularized, userIdFromClasses, params),
                params, SqlResult.validUniqueResultHandler(handler));
    }

    private String getEventsQueryPagination(String structureId, String startDate, String endDate, List<String> eventType,
                                            List<String> userId, boolean unjustified, boolean regularized,
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

        query += setParamsForQueryEvents(userId, unjustified, regularized, userIdFromClasses, params);
        return query;
    }

    private String setParamsForQueryEvents(List<String> userId, boolean unjustified, boolean regularized, JsonArray userIdFromClasses, JsonArray params) {
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

        if (unjustified) {
            query += "AND e.reason_id IS NULL ";
        }

        if (regularized) {
            query += "AND e.counsellor_regularisation = true ";
        }
        return query;
    }

    @Override
    public void getEventsReasonType(String structureId, Handler<Either<String, JsonArray>> handler) {
        String placeTypeQuery = "SELECT * FROM " + Presences.dbSchema + ".reason_type where structure_id = '" + structureId + "'";
        Sql.getInstance().raw(placeTypeQuery, SqlResult.validResultHandler(handler));
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

        // add comma "," if there's more than 1 comma
        if (event.containsKey("counsellor_regularisation")) {
            if (setter.length() - setter.replace(",", "").length() >= 1) {
                setter += ", ";
            } else {
                setter += "";
            }
            setter += "counsellor_regularisation = ?";
            params.add(event.getBoolean("counsellor_regularisation"));
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
    public void delete(Integer id, Handler<Either<String, JsonObject>> handler) {
        String query = "DELETE FROM " + Presences.dbSchema + ".event WHERE id = ?";
        JsonArray params = new JsonArray()
                .add(id);

        Sql.getInstance().prepared(query, params, SqlResult.validRowsResultHandler(handler));
    }

    @Override
    public void list(String structureId, String startDate, String endDate, List<Integer> eventType, List<String> userId, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT event.id, event.start_date, event.end_date, event.type_id, to_char(register.start_date, 'YYYY-MM-DD HH24:MI:SS') as course_start_date, to_char(register.end_date, 'YYYY-MM-DD HH24:MI:SS') as course_end_date, register.course_id " +
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
                "VALUES (?, ?, ?, false, ?, ?, ?, ?) " +
                "RETURNING id, start_date, end_date, comment, counsellor_input, student_id, register_id, type_id, reason_id;";
        JsonArray params = new JsonArray()
                .add(event.getString("start_date"))
                .add(event.getString("end_date"))
                .add(event.containsKey("comment") ? event.getString("comment") : "")
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
