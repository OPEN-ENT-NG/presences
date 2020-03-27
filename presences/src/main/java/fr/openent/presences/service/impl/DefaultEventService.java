package fr.openent.presences.service.impl;

import fr.openent.presences.Presences;
import fr.openent.presences.common.helper.FutureHelper;
import fr.openent.presences.common.helper.WorkflowHelper;
import fr.openent.presences.common.service.GroupService;
import fr.openent.presences.common.service.UserService;
import fr.openent.presences.common.service.impl.DefaultGroupService;
import fr.openent.presences.common.service.impl.DefaultUserService;
import fr.openent.presences.common.viescolaire.Viescolaire;
import fr.openent.presences.enums.EventType;
import fr.openent.presences.enums.Events;
import fr.openent.presences.enums.WorkflowActions;
import fr.openent.presences.helper.AbsenceHelper;
import fr.openent.presences.helper.CalendarHelper;
import fr.openent.presences.helper.EventHelper;
import fr.openent.presences.helper.SlotHelper;
import fr.openent.presences.model.Event.Event;
import fr.openent.presences.service.AbsenceService;
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

import java.util.*;
import java.util.stream.Collectors;

public class DefaultEventService implements EventService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultEventService.class);
    private static String defaultStartTime = "00:00:00";
    private static String defaultEndTime = "23:59:59";
    private SettingsService settingsService = new DefaultSettingsService();
    private AbsenceService absenceService;
    private GroupService groupService;
    private EventHelper eventHelper;
    private SlotHelper slotHelper;
    private UserService userService;

    public DefaultEventService(EventBus eb) {
        this.eventHelper = new EventHelper(eb);
        this.slotHelper = new SlotHelper(eb);
        this.absenceService = new DefaultAbsenceService(eb);
        this.groupService = new DefaultGroupService(eb);
        this.userService = new DefaultUserService();
    }

    @Override
    public void get(String structureId, String startDate, String endDate,
                    List<String> eventType, List<String> listReasonIds, Boolean noReason, List<String> userId,
                    JsonArray userIdFromClasses, List<String> classes, Boolean regularized, Integer page,
                    Handler<Either<String, JsonArray>> handler) {

        Future<JsonObject> slotsFuture = Future.future();
        Future<JsonArray> eventsFuture = Future.future();
        Future<JsonArray> absencesFuture = Future.future();
        Future<JsonArray> exclusionDays = Future.future();
        Future<JsonObject> saturdayCoursesCount = Future.future();
        Future<JsonObject> sundayCoursesCount = Future.future();

        getEvents(structureId, startDate, endDate, eventType, listReasonIds, noReason, userId, userIdFromClasses, regularized, page,
                FutureHelper.handlerJsonArray(eventsFuture));
        slotHelper.getTimeSlots(structureId, FutureHelper.handlerJsonObject(slotsFuture));
        CalendarHelper.getWeekEndCourses(structureId, CalendarHelper.SATURDAY_OF_WEEK, FutureHelper.handlerJsonObject(saturdayCoursesCount));
        CalendarHelper.getWeekEndCourses(structureId, CalendarHelper.SUNDAY_OF_WEEK, FutureHelper.handlerJsonObject(sundayCoursesCount));
        Viescolaire.getInstance().getExclusionDays(structureId, FutureHelper.handlerJsonArray(exclusionDays));
        absenceService.get(structureId, startDate, endDate, new ArrayList<>(),
                FutureHelper.handlerJsonArray(absencesFuture));

        CompositeFuture.all(slotsFuture, eventsFuture, absencesFuture, exclusionDays,
                saturdayCoursesCount, sundayCoursesCount).setHandler(asyncResult -> {
            if (asyncResult.failed()) {
                String message = "[Presences@DefaultEventService] Failed to retrieve slotProfile, " +
                        "absences, exclusions days or events info";
                LOGGER.error(message);
                handler.handle(new Either.Left<>(message));
            } else {
                List<Event> events = EventHelper.getEventListFromJsonArray(eventsFuture.result(), Event.MANDATORY_ATTRIBUTE);
                JsonArray absences = AbsenceHelper.removeDuplicates(events, absencesFuture.result());
                List<Integer> reasonIds = new ArrayList<>();
                List<String> studentIds = new ArrayList<>();
                List<Integer> eventTypeIds = new ArrayList<>();

                for (Event event : events) {
                    reasonIds.add(event.getReason().getId());
                    studentIds.add(event.getStudent().getId());
                    eventTypeIds.add(event.getEventType().getId());
                }

                // remove null value for each list
                reasonIds.removeAll(Collections.singletonList(null));
                studentIds.removeAll(Collections.singletonList(null));
                eventTypeIds.removeAll(Collections.singletonList(null));

                Future<JsonObject> reasonFuture = Future.future();
                Future<JsonObject> eventTypeFuture = Future.future();
                Future<JsonObject> studentFuture = Future.future();

                eventHelper.addReasonsToEvents(events, reasonIds, reasonFuture);
                eventHelper.addEventTypeToEvents(events, eventTypeIds, eventTypeFuture);
                eventHelper.addStudentsToEvents(events, studentIds, startDate, endDate, structureId,
                        absences, slotsFuture.result(), studentFuture);

                CompositeFuture.all(reasonFuture, eventTypeFuture, studentFuture).setHandler(addInfoResult -> {
                    if (addInfoResult.failed()) {
                        String message = "[Presences@DefaultEventService] Failed to retrieve reason, event type or student info";
                        LOGGER.error(message);
                        handler.handle(new Either.Left<>(message));
                    } else {
                        Future<JsonObject> actionFuture = Future.future();
                        Future<JsonObject> excludeFuture = Future.future();
                        Future<JsonObject> ownerFuture = Future.future();

                        eventHelper.addLastActionAbbreviation(events, actionFuture);
                        eventHelper.addOwnerToEvents(events, ownerFuture);
                        interactExclude(exclusionDays, saturdayCoursesCount, sundayCoursesCount, events, excludeFuture);

                        CompositeFuture.all(actionFuture, excludeFuture, ownerFuture).setHandler(eventResult -> {
                            if (eventResult.failed()) {
                                String message = "[Presences@DefaultEventService] Failed to retrieve exclude days or add last action abbreviation";
                                LOGGER.error(message);
                                handler.handle(new Either.Left<>(message));
                            } else {
                                handler.handle(new Either.Right<>(new JsonArray(events)));
                            }
                        });
                    }
                });
            }
        });
    }

    private void interactExclude(Future<JsonArray> exclusionDays, Future<JsonObject> saturdayCoursesCount,
                                 Future<JsonObject> sundayCoursesCount, List<Event> events, Future<JsonObject> future) {
        long saturdayCourses = saturdayCoursesCount.result().getLong("count");
        long sundayCourses = sundayCoursesCount.result().getLong("count");
        List<Event> absenceExcludeDay = events.stream()
                .filter(e -> e.getType().toUpperCase().equals(Events.ABSENCE.toString()))
                .collect(Collectors.toList());
        for (Event absenceDay : absenceExcludeDay) {
            CalendarHelper.setExcludeDay(absenceDay, absenceDay.getStartDate(), exclusionDays.result(),
                    CalendarHelper.SATURDAY_OF_WEEK, saturdayCourses, CalendarHelper.SUNDAY_OF_WEEK, sundayCourses);
        }
        future.complete();
    }

    private void getEvents(String structureId, String startDate, String endDate,
                           List<String> eventType, List<String> listReasonIds, Boolean noReason, List<String> userId, JsonArray userIdFromClasses,
                           Boolean regularized, Integer page, Handler<Either<String, JsonArray>> handler) {
        JsonArray params = new JsonArray();
        Sql.getInstance().prepared(this.getEventsQuery(structureId, startDate, endDate,
                eventType, listReasonIds, noReason, regularized, userId, userIdFromClasses, page, params),
                params, SqlResult.validResultHandler(handler));
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
                                  List<String> listReasonIds, Boolean noReason, Boolean regularized, List<String> userId,
                                  JsonArray userIdFromClasses, Integer page, JsonArray params) {

        String query = "WITH allevents AS (" +
                "  SELECT e.id AS id, e.start_date AS start_date, e.end_date AS end_date, " +
                "  e.created AS created, e.comment AS comment, e.student_id AS student_id," +
                "  e.reason_id AS reason_id, e.owner AS owner, e.register_id AS register_id, " +
                "  e.counsellor_regularisation AS counsellor_regularisation," +
                "  e.type_id AS type_id, 'event'::text AS type" +
                "  FROM " + Presences.dbSchema + ".event e" +
                "  INNER JOIN presences.register AS r " +
                "  ON (r.id = e.register_id AND r.structure_id = ?)";
        params.add(structureId);
        if (eventType != null && !eventType.isEmpty()) {
            query += " INNER JOIN presences.event_type AS event_type ON (event_type.id = e.type_id " +
                    "AND e.type_id IN " + Sql.listPrepared(eventType.toArray()) + " ) ";
            params.addAll(new JsonArray(eventType));
        } else {
            query += "INNER JOIN presences.event_type AS event_type ON event_type.id = e.type_id ";
        }
        query += "WHERE e.start_date > ? AND e.end_date < ? ";
        params.add(startDate + " " + defaultStartTime);
        params.add(endDate + " " + defaultEndTime);
        if (listReasonIds != null && !listReasonIds.isEmpty() && noReason != null) {
            query += " AND (reason_id IN " + Sql.listPrepared(listReasonIds) + (noReason ? " OR reason_id IS NULL" : "") + ") ";
            params.addAll(new JsonArray(listReasonIds));
        } else if (noReason != null) {
            query += (noReason ? " AND reason_id IS NULL" : "");
        }
        query += setParamsForQueryEvents(userId, regularized, noReason, userIdFromClasses, params);
        query += ") SELECT * FROM allevents " +
                "GROUP BY id, start_date, end_date, created, comment, student_id, reason_id, owner," +
                "type_id, register_id, counsellor_regularisation, type, register_id ";
        if (page != null) {
            query += "ORDER BY start_date DESC OFFSET ? LIMIT ? ";
            params.add(Presences.PAGE_SIZE * page);
            params.add(Presences.PAGE_SIZE);
        }
        return query;
    }

    @Override
    public void getPageNumber(String structureId, String startDate, String endDate, List<String> eventType,
                              List<String> listReasonIds, Boolean noReason, List<String> userId, Boolean regularized,
                              JsonArray userIdFromClasses, Handler<Either<String, JsonObject>> handler) {
        JsonArray params = new JsonArray();
        Sql.getInstance().prepared(this.getEventsQueryPagination(structureId, startDate, endDate, eventType,
                userId, listReasonIds, noReason, regularized, userIdFromClasses, params),
                params, SqlResult.validUniqueResultHandler(handler));

    }

    private String getEventsQueryPagination(String structureId, String startDate, String endDate, List<String> eventType,
                                            List<String> userId, List<String> listReasonIds, Boolean noReason, Boolean regularized,
                                            JsonArray userIdFromClasses, JsonArray params) {

        String query = "SELECT (" +
                " SELECT COUNT(DISTINCT (to_char(e.start_date, 'DD/MM/YYYY'), e.student_id))" +
                " FROM " + Presences.dbSchema + ".event e " +
                " INNER JOIN presences.register AS r ON (r.id = e.register_id AND r.structure_id = ?)";
        params.add(structureId);
        if (eventType != null && !eventType.isEmpty()) {
            query += "INNER JOIN presences.event_type AS event_type ON (event_type.id = e.type_id  AND e.type_id IN "
                    + Sql.listPrepared(eventType.toArray()) + ") ";
            params.addAll(new JsonArray(eventType));
        }
        query += "WHERE e.start_date > ? AND e.end_date < ? ";
        params.add(startDate + " " + defaultStartTime);
        params.add(endDate + " " + defaultEndTime);

        if (listReasonIds != null && !listReasonIds.isEmpty() && noReason != null) {
            query += " AND (reason_id IN " + Sql.listPrepared(listReasonIds) + (noReason ? " OR reason_id IS NULL" : "") + ") ";
            params.addAll(new JsonArray(listReasonIds));
        } else if (noReason != null) {
            query += (noReason ? " AND reason_id IS NULL" : "");
        }
        query += setParamsForQueryEvents(userId, regularized, noReason, userIdFromClasses, params);
        query += " ) AS events ";
        return query;
    }

    private String setParamsForQueryEvents(List<String> userId, Boolean regularized, Boolean noReason, JsonArray userIdFromClasses, JsonArray params) {
        String query = "";
        if (userIdFromClasses != null && !userIdFromClasses.isEmpty()) {
            query += " AND student_id IN " + Sql.listPrepared(userIdFromClasses.getList());
            for (int i = 0; i < userIdFromClasses.size(); i++) {
                params.add(userIdFromClasses.getJsonObject(i).getString("studentId"));
            }
        }
        if (userId != null && !userId.isEmpty()) {
            query += " AND student_id IN " + Sql.listPrepared(userId.toArray());
            params.addAll(new JsonArray(userId));
        }

        if (regularized != null && regularized && noReason) {
            query += " AND counsellor_regularisation = " + regularized + " OR reason_id IS NULL";
        } else if (regularized != null) {
            query += " AND counsellor_regularisation = " + regularized + " ";
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
        query += " WHERE id IN " + Sql.listPrepared(eventBody.getJsonArray("ids").getList()) + " AND type_id = 1";
        params.addAll(eventBody.getJsonArray("ids"));
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void changeRegularizedEvents(JsonObject eventBody, Handler<Either<String, JsonObject>> handler) {
        JsonArray params = new JsonArray();
        String query = "UPDATE " + Presences.dbSchema + ".event SET counsellor_regularisation = ? ";
        params.add(eventBody.getBoolean("regularized"));

        query += " WHERE id IN " + Sql.listPrepared(eventBody.getJsonArray("ids").getList()) + " AND type_id = 1";
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
                "event.counsellor_input, " +
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
            JsonObject settings = event.right().getValue();
            String recoveryMethod = settings.getString("event_recovery_method");
            switch (recoveryMethod) {
                case "DAY":
                case "HOUR": {
                    JsonObject eventsQuery = getEventQuery(eventType, students, structure, justified, reasonsId,
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
                        if (slotSetting.containsKey("end_of_half_day") && slotSetting.getString("end_of_half_day") != null) {
                            String halfOfDay = slotSetting.getString("end_of_half_day");
                            JsonObject morningQuery = getEventQuery(eventType, students, structure, justified,
                                    reasonsId, massmailed, startDate, endDate, noReasons, recoveryMethod, defaultStartTime, halfOfDay);
                            JsonObject afternoonQuery = getEventQuery(eventType, students, structure, justified,
                                    reasonsId, massmailed, startDate, endDate, noReasons, recoveryMethod, halfOfDay, defaultEndTime);
                            String query = "WITH count_by_user AS (WITH events as (" + morningQuery.getString("query") + " UNION ALL " + afternoonQuery.getString("query") + ") " +
                                    "SELECT count(*), student_id FROM events GROUP BY student_id) SELECT * FROM count_by_user WHERE count >= " + startAt;
                            JsonArray params = new JsonArray()
                                    .addAll(morningQuery.getJsonArray("params"))
                                    .addAll(afternoonQuery.getJsonArray("params"));
                            Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
                        } else {
                            String message = "[Presences@DefaultEventService] Structure does not initialize end of half day";
                            LOGGER.error(message);
                            handler.handle(new Either.Left<>(message));
                        }
                    });
            }
        });
    }

    private JsonObject getEventQuery(Integer eventTypes, List<String> students, String structure, Boolean justified,
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
                "AND type_id = ?";
        JsonArray params = new JsonArray()
                .add(startDate + " " + defaultStartTime)
                .add(endDate + " " + defaultEndTime)
                .add(structure)
                .add(eventTypes);

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
    public void getEventsByStudent(Integer eventType, List<String> students, String structure, Boolean justified,
                                   List<Integer> reasonsId, Boolean massmailed, String startDate, String endDate,
                                   boolean noReasons, String recoveryMethodUsed, Handler<Either<String, JsonArray>> handler) {
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

        if (eventType != 1) {
            JsonObject eventsQuery = getEventQuery(eventType, students, structure, justified, reasonsId, massmailed, startDate, endDate, noReasons, "HOUR", null, null);
            Sql.getInstance().prepared(eventsQuery.getString("query"), eventsQuery.getJsonArray("params"), SqlResult.validResultHandler(queryHandler));
            return;
        }

        settingsService.retrieve(structure, event -> {
            if (event.isLeft()) {
                handler.handle(new Either.Left<>(event.left().getValue()));
                return;
            }

            JsonObject settings = event.right().getValue();
            String recoveryMethod = recoveryMethodUsed != null ? recoveryMethodUsed : settings.getString("event_recovery_method");
            switch (recoveryMethod) {
                case "DAY":
                case "HOUR": {
                    JsonObject eventsQuery = getEventQuery(eventType, students, structure, justified, reasonsId, massmailed, startDate, endDate, noReasons, recoveryMethod, null, null);
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
                            JsonObject morningQuery = getEventQuery(eventType, students, structure, justified, reasonsId, massmailed, startDate, endDate, noReasons, recoveryMethod, defaultStartTime, halfOfDay);
                            JsonObject afternoonQuery = getEventQuery(eventType, students, structure, justified, reasonsId, massmailed, startDate, endDate, noReasons, recoveryMethod, halfOfDay, defaultEndTime);
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

    @Override
    public void getAbsenceRate(String student, String structure, String start, String end, Handler<Either<String, JsonObject>> handler) {
        groupService.getUserGroups(Collections.singletonList(student), structure, evt -> {
            if (evt.isLeft()) {
                handler.handle(new Either.Left<>(evt.left().getValue()));
                return;
            }

            List<JsonObject> groups = (List<JsonObject>) evt.right().getValue().getList();
            List<String> groupIds = groups.stream().map(group -> group.getString("id")).collect(Collectors.toList());

            String query = "WITH register_count AS (" +
                    "SELECT COUNT(id) FROM " + Presences.dbSchema + ".register " +
                    "INNER JOIN " + Presences.dbSchema + ".rel_group_register ON (register.id = rel_group_register.register_id) " +
                    "WHERE rel_group_register.group_id IN " + Sql.listPrepared(groupIds) +
                    "AND register.structure_id = ?" +
                    "AND register.start_date >= ?" +
                    "AND register.end_date <= ?), " +
                    "event_count AS (SELECT COUNT(id) FROM " + Presences.dbSchema + ".event WHERE student_id = ? AND type_id = 1 AND start_date >= ? AND end_date <= ?) " +
                    "SELECT CASE WHEN register_count.count > 0 AND event_count.count > 0 THEN round(((event_count.count::numeric) / (register_count.count::numeric))*100, 2) ELSE 0 END AS absence_rate " +
                    "FROM register_count, event_count";
            JsonArray params = new JsonArray()
                    .addAll(new JsonArray(groupIds))
                    .add(structure)
                    .add(start)
                    .add(end)
                    .add(student)
                    .add(start)
                    .add(end);

            Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
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

    @Override
    public void getActions(String event_id, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT ea.*, a.label as label FROM " + Presences.dbSchema + ".event_actions as ea" +
                " INNER JOIN " + Presences.dbSchema + ".actions as a" +
                " ON (a.id = ea.action_id)" + " WHERE event_id = ?" +
                " ORDER BY created_date DESC;";
        JsonArray params = new JsonArray().add(event_id);
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(res -> {
            if (res.isLeft()) {
                LOGGER.error("[Presences@DefaultEventService] Failed to retrieve actions");
                handler.handle(new Either.Left(res.left().getValue()));
                return;
            }
            JsonArray actions = res.right().getValue();
            List<String> userIds = new ArrayList<>();
            for (int i = 0; i < actions.size(); i++) {
                userIds.add(actions.getJsonObject(i).getString("owner"));
            }
            userService.getUsers(userIds, resUsers -> {
                if (resUsers.isLeft()) {
                    LOGGER.error("[Presences@DefaultEventService] Failed to match ownerId");
                    handler.handle(new Either.Left(resUsers.left().getValue()));
                    return;
                }

                JsonArray owners = resUsers.right().getValue();
                Map<String, JsonObject> mappedUsers = new HashMap<>();
                for (Object o : owners) {
                    if (!(o instanceof JsonObject)) continue;
                    JsonObject jsonObject = ((JsonObject) o);
                    mappedUsers.put(jsonObject.getString("id"), jsonObject);
                }

                for (int i = 0; i < actions.size(); i++) {
                    JsonObject currentsActions = actions.getJsonObject(i);
                    JsonObject user = mappedUsers.get(currentsActions.getString("owner"));
                    if (user != null) {
                        currentsActions.put("displayName", user.getString("displayName"));
                    }
                }
                handler.handle(new Either.Right<>(actions));
            });
        }));
    }

    @Override
    public void createAction(JsonObject actionBody, Handler<Either<String, JsonObject>> handler) {
        JsonArray statements = new JsonArray();
        for (int i = 0; i < actionBody.getJsonArray("eventId").size(); i++) {
            Integer eventId = actionBody.getJsonArray("eventId").getInteger(i);
            JsonObject event = new JsonObject()
                    .put("owner", actionBody.getString("owner"))
                    .put("eventId", eventId)
                    .put("actionId", actionBody.getInteger("actionId"))
                    .put("comment", actionBody.getString("comment"));
            statements.add(addActionStatement(event));
        }
        Sql.getInstance().transaction(statements, createActionAsync -> {
            Either<String, JsonObject> result = SqlResult.validUniqueResult(0, createActionAsync);
            if (result.isLeft()) {
                String message = "[Presences@DefaultEventeService] Failed to execute action creation statements";
                LOGGER.error(message);
                handler.handle(new Either.Left<>(message));
            }
            handler.handle(new Either.Right<>(result.right().getValue()));
        });
    }

    private JsonObject addActionStatement(JsonObject event) {
        String query = "INSERT INTO " + Presences.dbSchema + ".event_actions " +
                "(owner, event_id, action_id, comment)" + "VALUES (?, ?, ?, ?) RETURNING id";
        JsonArray values = new JsonArray()
                .add(event.getString("owner"))
                .add(event.getInteger("eventId"))
                .add(event.getInteger("actionId"))
                .add(event.getString("comment"));

        return new JsonObject()
                .put("statement", query)
                .put("values", values)
                .put("action", "prepared");
    }
}