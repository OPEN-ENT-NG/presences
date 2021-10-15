package fr.openent.presences.service.impl;

import fr.openent.presences.Presences;
import fr.openent.presences.common.helper.DateHelper;
import fr.openent.presences.common.helper.FutureHelper;
import fr.openent.presences.common.helper.WorkflowHelper;
import fr.openent.presences.common.service.GroupService;
import fr.openent.presences.common.service.UserService;
import fr.openent.presences.common.service.impl.DefaultGroupService;
import fr.openent.presences.common.service.impl.DefaultUserService;
import fr.openent.presences.common.viescolaire.Viescolaire;
import fr.openent.presences.db.DBService;
import fr.openent.presences.enums.EventType;
import fr.openent.presences.enums.WorkflowActions;
import fr.openent.presences.helper.CourseHelper;
import fr.openent.presences.helper.EventHelper;
import fr.openent.presences.helper.EventQueryHelper;
import fr.openent.presences.helper.SlotHelper;
import fr.openent.presences.model.Event.Event;
import fr.openent.presences.model.Slot;
import fr.openent.presences.service.AbsenceService;
import fr.openent.presences.service.EventService;
import fr.openent.presences.service.SettingsService;
import fr.wseduc.webutils.Either;
import io.vertx.core.*;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;

import java.util.*;
import java.util.stream.Collectors;

public class DefaultEventService extends DBService implements EventService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultEventService.class);
    private final EventBus eb;
    private static final String defaultStartTime = "00:00:00";
    private static final String defaultEndTime = "23:59:59";
    private final SettingsService settingsService = new DefaultSettingsService();
    private final AbsenceService absenceService;
    private final GroupService groupService;
    private final EventHelper eventHelper;
    private final CourseHelper courseHelper;
    private final SlotHelper slotHelper;
    private final UserService userService;

    public DefaultEventService(EventBus eb) {
        this.eb = eb;
        this.eventHelper = new EventHelper(eb);
        this.courseHelper = new CourseHelper(eb);
        this.slotHelper = new SlotHelper(eb);
        this.absenceService = new DefaultAbsenceService(eb);
        this.groupService = new DefaultGroupService(eb);
        this.userService = new DefaultUserService();
    }

    @Override
    public void get(String structureId, String startDate, String endDate, String startTime, String endTime,
                    List<String> eventType, List<String> listReasonIds, Boolean noReason, List<String> userId,
                    Boolean regularized, Boolean followed, Integer page, Handler<AsyncResult<JsonArray>> handler) {

        Future<JsonArray> eventsFuture = Future.future();
        Future<JsonObject> slotsFuture = Future.future();

        getDayMainEvents(structureId, startDate, endDate, startTime, endTime, userId, eventType, listReasonIds, noReason, regularized,
                followed, page, eventsFuture);
        slotHelper.getTimeSlots(structureId, FutureHelper.handlerJsonObject(slotsFuture));


        CompositeFuture.all(eventsFuture, slotsFuture).setHandler(eventAsyncResult -> {
            if (eventAsyncResult.failed()) {
                String message = "[Presences@DefaultEventService] Failed to retrieve events info and slotProfile";
                LOGGER.error(message + eventAsyncResult.cause().getMessage());
                handler.handle(Future.failedFuture(message));
            } else {
                List<Event> events = EventHelper.getEventListFromJsonArray(eventsFuture.result(), Event.MANDATORY_ATTRIBUTE);

                if (events.isEmpty()) { //no need to proceed treatment if no events
                    handler.handle(Future.succeededFuture(new JsonArray(events)));
                    return;
                }

                List<Integer> reasonIds = events.stream().map(event -> event.getReason().getId()).collect(Collectors.toList());
                List<String> studentIds = events.stream().map(event -> event.getStudent().getId()).collect(Collectors.toList());

                // remove null value for each list
                reasonIds.removeAll(Collections.singletonList(null));
                studentIds.removeAll(Collections.singletonList(null));

                Future<JsonArray> absencesFuture = Future.future();
                Future<JsonObject> reasonFuture = Future.future();

                absenceService.get(structureId, startDate, endDate, studentIds, FutureHelper.handlerJsonArray(absencesFuture));
                eventHelper.addReasonsToEvents(events, reasonIds, reasonFuture);

                CompositeFuture.all(absencesFuture, reasonFuture).setHandler(asyncResult -> {
                    if (asyncResult.failed()) {
                        String message = "[Presences@DefaultEventService] Failed to retrieve absences, " +
                                "reason or event type";
                        LOGGER.error(message);
                        handler.handle(Future.failedFuture(message));
                    } else {
                        JsonArray absences = absencesFuture.result();

                        Future<JsonObject> studentFuture = Future.future();

                        eventHelper.addStudentsToEvents(structureId, events, studentIds, startDate, endDate, startTime,
                                endTime, eventType, listReasonIds, noReason, regularized, followed, absences,
                                slotsFuture.result(), studentFuture);

                        studentFuture.setHandler(addInfoResult -> {
                            if (addInfoResult.failed()) {
                                String message = "[Presences@DefaultEventService] Failed to retrieve student info";
                                LOGGER.error(message);
                                handler.handle(Future.failedFuture(message));
                            } else {
                                Future<JsonObject> actionFuture = Future.future();
                                Future<JsonObject> ownerFuture = Future.future();

                                eventHelper.addLastActionAbbreviation(events, actionFuture);
                                eventHelper.addOwnerToEvents(events, ownerFuture);

                                CompositeFuture.all(actionFuture, ownerFuture).setHandler(eventResult -> {
                                    if (eventResult.failed()) {
                                        String message = "[Presences@DefaultEventService::get] Failed to retrieve owners " +
                                                "or add last action abbreviation to existing event";
                                        LOGGER.error(message);
                                        handler.handle(Future.failedFuture(message));
                                    } else {
                                        handler.handle(Future.succeededFuture(EventHelper.getMainEventsJsonArrayFromEventList(events)));
                                    }
                                });
                            }
                        });
                    }
                });
            }
        });
    }

    @Override
    public void getDayMainEvents(String structureId, String startDate, String endDate, String startTime, String endTime,
                                 List<String> studentIds, List<String> typeIds, List<String> reasonIds, Boolean noReason,
                                 Boolean regularized, Boolean followed, Integer page, Handler<AsyncResult<JsonArray>> handler) {
        JsonArray params = new JsonArray();
        String query = getDayMainEventsQuery(structureId, startDate, endDate, startTime,
                endTime, studentIds, typeIds, reasonIds, noReason, regularized, followed, params) +
                " ORDER BY date DESC, created DESC, student_id " +
                " OFFSET ? LIMIT ? ";

        params.add(Presences.PAGE_SIZE * page);
        params.add(Presences.PAGE_SIZE);
        sql.prepared(query, params, SqlResult.validResultHandler(FutureHelper.handlerJsonArray(handler)));
    }

    private String getDayMainEventsQuery(String structureId, String startDate, String endDate, String startTime, String endTime,
                                         List<String> studentIds, List<String> typeIds, List<String> reasonIds, Boolean noReason,
                                         Boolean regularized, Boolean followed, JsonArray params) {
        return "SELECT e.student_id AS student_id, " +
                " e.start_date::date AS date, " +
                " 'event'::text AS TYPE, " +
                " MAX(e.created) AS created, " +
                EventQueryHelper.addMainReasonEvent() +
                EventQueryHelper.MAIN_COUNSELLOR_REGULARIZED_QUERY +
                EventQueryHelper.MAIN_MASSMAILED_QUERY +
                " FROM presences.event e " +
                EventQueryHelper.joinRegister(structureId, params) +
                EventQueryHelper.joinEventType(typeIds, params) +
                EventQueryHelper.filterDates(startDate, endDate, params) +
                EventQueryHelper.filterTimes(startTime, endTime, params) +
                EventQueryHelper.filterStudentIds(studentIds, params) +
                EventQueryHelper.filterFollowed(followed, params) +
                EventQueryHelper.filterReasons(reasonIds, noReason, regularized, typeIds, params) +
                " GROUP BY student_id, date ";
    }

    // *Swa = students_with_actions
    private String getDayMainEventQueryFromSwaCte() {
        return " SELECT DISTINCT swa.* " +
                " FROM presences.event e " +
                " INNER JOIN students_with_actions swa ON swa.student_id = e.student_id AND swa.date = e.start_date::date ";
    }

    private void getEvents(String structureId, String startDate, String endDate,
                           List<String> eventType, List<String> listReasonIds, Boolean noReason, List<String> userId, JsonArray userIdFromClasses,
                           Boolean regularized, Boolean followed, Integer page, Handler<Either<String, JsonArray>> handler) {
        JsonArray params = new JsonArray();
        sql.prepared(this.getEventsQuery(structureId, startDate, endDate,
                eventType, listReasonIds, noReason, regularized, followed, userId, userIdFromClasses, page, params),
                params, SqlResult.validResultHandler(handler));
    }


    @Override
    public void get(String startDate, String endDate, List<Number> eventType, List<String> users, Handler<Either<String, JsonArray>> handler) {
        JsonArray params = new JsonArray()
                .add(startDate)
                .add(endDate)
                .addAll(new JsonArray(eventType))
                .addAll(new JsonArray(users));

        String query = "SELECT start_date, end_date, student_id, type_id, " +
                "counsellor_regularisation, followed, reason_id, reason.label as reason " +
                "FROM " + Presences.dbSchema + ".event " +
                "LEFT JOIN " + Presences.dbSchema + ".reason ON (event.reason_id = reason.id) " +
                "WHERE start_date >= ? " +
                "AND end_date <= ? " +
                "AND type_id IN " + Sql.listPrepared(eventType) +
                " AND student_id IN " + Sql.listPrepared(users);

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void getCsvData(String structureId, String startDate, String endDate, List<String> eventType,
                           List<String> listReasonIds, Boolean noReason, List<String> userId, JsonArray userIdFromClasses,
                           List<String> classes, Boolean regularized, Boolean followed, Handler<AsyncResult<List<Event>>> handler) {
        getEvents(structureId, startDate, endDate, eventType, listReasonIds, noReason, userId, userIdFromClasses,
                regularized, followed, null, eventHandler -> {
                    if (eventHandler.isLeft()) {
                        String err = "[Presences@DefaultEventService::getCsvData] Failed to fetch events: " + eventHandler.left().getValue() ;
                        LOGGER.error(err, eventHandler.left().getValue());
                        handler.handle(Future.failedFuture(eventHandler.left().getValue()));
                    } else {
                        List<Event> events = EventHelper.getEventListFromJsonArray(eventHandler.right().getValue(), Event.MANDATORY_ATTRIBUTE);

                        List<Integer> reasonIds = new ArrayList<>();
                        List<String> studentIds = new ArrayList<>();
                        List<String> ownerIds = new ArrayList<>();
                        List<Integer> eventTypeIds = new ArrayList<>();

                        for (Event event : events) {
                            reasonIds.add(event.getReason().getId());
                            if (!studentIds.contains(event.getStudent().getId())) {
                                studentIds.add(event.getStudent().getId());
                            }
                            if (!ownerIds.contains(event.getOwner().getId())) {
                                ownerIds.add(event.getOwner().getId());
                            }
                            eventTypeIds.add(event.getEventType().getId());
                        }

                        // remove potential null value for each list
                        reasonIds.removeAll(Collections.singletonList(null));
                        studentIds.removeAll(Collections.singletonList(null));
                        ownerIds.removeAll(Collections.singletonList(null));
                        eventTypeIds.removeAll(Collections.singletonList(null));

                        Future<JsonObject> reasonFuture = Future.future();
                        Future<JsonObject> studentFuture = Future.future();
                        Future<JsonObject> ownerFuture = Future.future();
                        Future<JsonObject> eventTypeFuture = Future.future();

                        eventHelper.addReasonsToEvents(events, reasonIds, reasonFuture);
                        eventHelper.addStudentsToEvents(events, studentIds, structureId, studentFuture);
                        eventHelper.addOwnerToEvents(events, ownerIds, ownerFuture);
                        eventHelper.addEventTypeToEvents(events, eventTypeIds, eventTypeFuture);

                        CompositeFuture.all(reasonFuture, eventTypeFuture, studentFuture, ownerFuture)
                                .setHandler(eventResult -> {
                                    if (eventResult.failed()) {
                                        String message = "[Presences@DefaultEventService::getCsvData] Failed to add " +
                                                "reasons, eventType, students or owner to corresponding event ";
                                        LOGGER.error(message + eventResult.cause().getMessage());
                                        handler.handle(Future.failedFuture(message));
                                    } else {
                                        handler.handle(Future.succeededFuture(events));
                                    }
                                });
                    }
                });
    }

    @Override
    public Future<List<Event>> getCsvData(String structureId, String startDate, String endDate, List<String> eventType,
                           List<String> listReasonIds, Boolean noReason, List<String> userId, JsonArray userIdFromClasses,
                           List<String> classes, Boolean regularized, Boolean followed) {
        Promise<List<Event>> promise = Promise.promise();

        getCsvData(structureId, startDate, endDate, eventType, listReasonIds, noReason, userId, userIdFromClasses,
                classes, regularized, followed, event -> {
            if (event.failed()) {
                promise.fail(event.cause());
            } else {
                promise.complete(event.result());
            }
        });

        return promise.future();
    }

    /**
     * GET query to fetch events
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
                                  List<String> listReasonIds, Boolean noReason, Boolean regularized, Boolean followed,
                                  List<String> userId, JsonArray userIdFromClasses, Integer page, JsonArray params) {

        String query = "WITH allevents AS (" +
                "  SELECT e.id AS id, e.start_date AS start_date, e.end_date AS end_date, " +
                "  e.created AS created, e.comment AS comment, e.student_id AS student_id," +
                "  e.reason_id AS reason_id, e.owner AS owner, e.register_id AS register_id, " +
                "  e.counsellor_regularisation AS counsellor_regularisation, e.followed AS followed, " +
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

        query += setParamsForQueryEvents(listReasonIds, userId, regularized, followed, noReason,
                userIdFromClasses, eventType, params);
        query += ") SELECT * FROM allevents " +
                "GROUP BY id, start_date, end_date, created, comment, student_id, reason_id, owner," +
                "type_id, register_id, counsellor_regularisation, followed, type, register_id " +
                " ORDER BY start_date DESC, id DESC";
        if (page != null) {
            query += " OFFSET ? LIMIT ? ";
            params.add(Presences.PAGE_SIZE * page);
            params.add(Presences.PAGE_SIZE);
        }

        return query;
    }

    @Override
    public void getPageNumber(String structureId, String startDate, String endDate, String startTime, String endTime,
                              List<String> eventType, List<String> listReasonIds, Boolean noReason, List<String> userId,
                              Boolean regularized, Boolean followed, Handler<Either<String, JsonObject>> handler) {
        JsonArray params = new JsonArray();
        Sql.getInstance().prepared(this.getEventsQueryPagination(structureId, startDate, endDate, startTime, endTime, eventType,
                userId, listReasonIds, noReason, regularized, followed, params), params, SqlResult.validUniqueResultHandler(handler));
    }

    private String getEventsQueryPagination(String structureId, String startDate, String endDate, String startTime, String endTime,
                                            List<String> eventType, List<String> userId, List<String> listReasonIds, Boolean noReason,
                                            Boolean regularized, Boolean followed, JsonArray params) {
        return " SELECT count(DISTINCT (e.student_id, e.start_date::date)) FROM " + Presences.dbSchema + ".event e " +
                EventQueryHelper.joinRegister(structureId, params) +
                EventQueryHelper.joinEventType(eventType, params) +
                EventQueryHelper.filterDates(startDate, endDate, params) +
                EventQueryHelper.filterTimes(startTime, endTime, params) +
                EventQueryHelper.filterReasons(listReasonIds, noReason, regularized, eventType, params) +
                EventQueryHelper.filterStudentIds(userId, params) +
                EventQueryHelper.filterFollowed(followed, params);
    }

    private String setParamsForQueryEvents(List<String> listReasonIds, List<String> userId, Boolean regularized,
                                           Boolean followed, Boolean noReason, JsonArray userIdFromClasses,
                                           List<String> typeIds, JsonArray params) {
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

        // this condition occurs when we want to filter no reason and regularized event at the same time
        if ((noReason != null && noReason) && (regularized != null && regularized)) {
            query += "AND (reason_id IS NULL OR (reason_id IS NOT NULL AND counsellor_regularisation = true)";

            if (listReasonIds != null && !listReasonIds.isEmpty()) {
                query += " AND reason_id IN " + Sql.listPrepared(listReasonIds) + ")";
                params.addAll(new JsonArray(listReasonIds));
            } else {
                query += ")";
            }

            // else is default condition
        } else {
            // If we want to fetch events WITH reasonId, array reasonIds fetched is not empty
            // (optional if we wish noReason fetched at same time then noReason is TRUE)
            if (listReasonIds != null && !listReasonIds.isEmpty()) {
                query += " AND ((reason_id IN " + Sql.listPrepared(listReasonIds) + ")";

                if (noReason != null && noReason) {
                    query += " OR reason_id IS NULL";
                } else {
                    query += typeIds.contains(EventType.LATENESS.getType().toString())
                            ? (" OR type_id = " + EventType.LATENESS.getType()) : "";
                }
                query += ")";
                params.addAll(new JsonArray(listReasonIds));
            }

            // If we want to fetch events with NO reasonId, array reasonIds fetched is empty
            // AND noReason is TRUE
            if ((listReasonIds == null || listReasonIds.isEmpty()) && (noReason != null && noReason)) {
                query += " AND (reason_id IS NULL " + (regularized != null ? " OR counsellor_regularisation = " + regularized + "" : "") + ") ";
            }

            if (regularized != null) {
                query += " AND counsellor_regularisation = " + regularized + " ";
            }
        }

        if (followed != null) {
            query += " AND followed = " + followed + " ";
        }

        return query;
    }

    @Override
    public void getAbsencesCountSummary(String structureId, String currentDate, Handler<Either<String, JsonObject>> handler) {
        Future<JsonArray> absentStudentIdsFuture = Future.future();
        Future<JsonArray> studentsWithAccommodationFuture = Future.future();
        Future<JsonArray> countCurrentStudentsFuture = Future.future();

        String currentDateDay = DateHelper.getDateString(currentDate, DateHelper.MONGO_FORMAT, DateHelper.YEAR_MONTH_DAY);
        String currentDateTime = DateHelper.fetchTimeString(currentDate, DateHelper.MONGO_FORMAT);

        CompositeFuture.all(absentStudentIdsFuture, studentsWithAccommodationFuture, countCurrentStudentsFuture).setHandler(resultFuture -> {
            if (resultFuture.failed()) {
                String message = "[Presences@DefaultEventService::getAbsencesCountSummary] Failed to retrieve " +
                        "absent students, student ids and accommodations and current student counts";
                LOGGER.error(message);
                handler.handle(new Either.Left<>(resultFuture.cause().getMessage()));
            } else {
                JsonArray absentStudentIds = absentStudentIdsFuture.result();
                JsonArray studentsAccommodations = studentsWithAccommodationFuture.result();
                JsonArray countStudents = countCurrentStudentsFuture.result();

                int nbCurrentStudents = countStudents.getJsonObject(0).getInteger("nbStudents") +
                        countStudents.getJsonObject(1).getInteger("nbStudents") +
                        countStudents.getJsonObject(2).getInteger("nbStudents");

                int nbDayStudents = 0;

                @SuppressWarnings("unchecked")
                long countAbsentIds = ((List<JsonObject>) absentStudentIds.getList())
                        .stream()
                        .filter(student -> student.getString("student_id") != null).count();

                for (int i = 0; i < studentsAccommodations.size(); i++) {
                    JsonObject student = studentsAccommodations.getJsonObject(i);

                    if (student != null && student.getString("accommodation") != null &&
                            student.getString("accommodation").contains("DEMI-PENSIONNAIRE")) {

                        // Counting the number of absent day students
                        for (int j = 0; j < absentStudentIds.size(); j++) {
                            JsonObject absentStudent = absentStudentIds.getJsonObject(j);
                            if (absentStudent != null &&
                                    absentStudent.getString("student_id") != null &&
                                    student.getString("id") != null &&
                                    absentStudent.getString("student_id").equals(student.getString("id"))) {
                                nbDayStudents++;
                            }
                        }
                    }
                }

                JsonObject res = new JsonObject()
                        .put("nb_absents", countAbsentIds > 0 ? countAbsentIds : 0)
                        .put("nb_day_students", nbDayStudents > 0 ? nbDayStudents : 0)
                        .put("nb_presents", (nbCurrentStudents - countAbsentIds) > 0 ?
                                (nbCurrentStudents - countAbsentIds) : 0);

                handler.handle(new Either.Right<>(res));
            }
        });
        absenceService.getAbsentStudentIds(structureId, currentDate, FutureHelper.handlerJsonArray(absentStudentIdsFuture));
        userService.getAllStudentsIdsWithAccommodation(structureId, FutureHelper.handlerJsonArray(studentsWithAccommodationFuture));
        this.getCoursesStudentCount(structureId, currentDateDay, currentDateTime, FutureHelper.handlerJsonArray(countCurrentStudentsFuture));
    }


    /**
     * Get number of students in all occurring courses during the specified date.
     *
     * @param structureId structure identifier
     * @param date        a date (format yyyy-MM-dd)
     * @param time        an hour (format HH:mm:ss)
     * @param handler     Function handler returning data
     */
    private void getCoursesStudentCount(String structureId, String date, String time,
                                        Handler<Either<String, JsonArray>> handler) {
        courseHelper.getCourses(structureId, date, date, time, time, "true", event -> {
            if (event.isLeft()) {
                handler.handle(new Either.Left<>(event.left().getValue()));
                return;
            }

            ArrayList<String> classesName = new ArrayList<>();
            ArrayList<String> groupsName = new ArrayList<>();

            JsonArray courses = event.right().getValue();

            for (int i = 0; i < courses.size(); i++) {
                JsonObject course = courses.getJsonObject(i);
                JsonArray classNames = course.getJsonArray("classes");
                JsonArray groupsNames = course.getJsonArray("groups");

                for (int classIndex = 0; classIndex < classNames.size(); classIndex++) {
                    classesName.add(classNames.getString(classIndex));
                }
                for (int groupIndex = 0; groupIndex < groupsNames.size(); groupIndex++) {
                    groupsName.add(groupsNames.getString(groupIndex));
                }
            }

            String query = "MATCH" +
                    " (c:Class)<-[:DEPENDS]-(:ProfileGroup)<-[:IN]-(u:User {profiles: ['Student']})-[:ADMINISTRATIVE_ATTACHMENT]->(s:Structure {id:{structureId}})" +
                    " WHERE c.name IN {classesName}" +
                    " RETURN COUNT(DISTINCT(u)) AS nbStudents" +
                    " UNION ALL" +
                    " MATCH (fg:FunctionalGroup)<-[:IN]-(u:User {profiles:['Student']})-[:ADMINISTRATIVE_ATTACHMENT]->(s:Structure {id:{structureId}})" +
                    " WHERE fg.name IN {groupsName}" +
                    " RETURN COUNT(DISTINCT(u)) AS nbStudents" +
                    " UNION ALL" +
                    " MATCH (mg:ManualGroup)<-[:IN]-(u:User {profiles:['Student']})-[:ADMINISTRATIVE_ATTACHMENT]->(s:Structure {id:{structureId}})" +
                    " WHERE mg.name IN {groupsName}" +
                    " RETURN COUNT(DISTINCT(u)) AS nbStudents";


            JsonObject params = new JsonObject()
                    .put("classesName", new JsonArray(classesName))
                    .put("groupsName", new JsonArray(groupsName))
                    .put("structureId", structureId);
            Neo4j.getInstance().execute(query, params, Neo4jResult.validResultHandler(handler));
        });
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
    public void updateEvent(Integer id, JsonObject event, Handler<Either<String, JsonObject>> handler) {
        JsonArray params = new JsonArray();
        String query = "UPDATE " + Presences.dbSchema + ".event SET start_date = ?, end_date = ?, comment = ?, counsellor_input = ?, " +
                " student_id = ?, reason_id = ?, counsellor_regularisation = ?" +
                " WHERE id = ?";

        params.add(event.getString("start_date"));
        params.add(event.getString("end_date"));
        params.add(event.getString("comment"));
        params.add((event.getBoolean("counsellor_input") != null) ?
                event.getBoolean("counsellor_input", false) : false);
        params.add(event.getString("student_id"));
        if ((event.getInteger("reason_id") != null && event.getInteger("reason_id") != -1)) {
            params.add(event.getInteger("reason_id"));
        } else {
            params.addNull();
        }
        params.add(event.getBoolean("counsellor_regularisation") != null ?
                event.getBoolean("counsellor_regularisation", false) : false);
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
    public void changeReasonEvents(JsonObject eventBody, UserInfos user, Handler<Either<String, JsonObject>> handler) {
        List<Event> events = new ArrayList<>();
        List<Integer> ids = new ArrayList<>();
        JsonArray arrayEvents = eventBody.getJsonArray("events");
        for (int i = 0; i < arrayEvents.size(); i++) {
            Event event = new Event(arrayEvents.getJsonObject(i), new ArrayList<>());
            ids.add(event.getId());
            events.add(event);
        }

        JsonArray params = new JsonArray();
        String query = "UPDATE " + Presences.dbSchema + ".event SET reason_id = ? ";
        if (eventBody.getInteger("reasonId") != null) {
            params.add(eventBody.getInteger("reasonId"));
        } else {
            params.addNull();
        }
        query += " WHERE id IN " + Sql.listPrepared(ids) + " AND type_id = 1 RETURNING *";
        params.addAll(new JsonArray(ids));
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(result -> {
            if (result.isLeft()) {
                String message = "[Presences@DefaultEventService] Failed to edit reason on events";
                LOGGER.error(message);
                handler.handle(new Either.Left<>(message));
                return;
            }
            editCorrespondingAbsences(events, user, eventBody.getString("student_id"), eventBody.getString("structure_id"),
                    result.right().getValue().getJsonObject(0).getBoolean("counsellor_regularisation"),
                    eventBody.getInteger("reasonId"), handler);
        }));
    }


    @Override
    public void changeRegularizedEvents(JsonObject eventBody, UserInfos user, Handler<Either<String, JsonObject>> handler) {
        List<Event> events = new ArrayList<>();
        List<Integer> ids = new ArrayList<>();
        JsonArray arrayEvents = eventBody.getJsonArray("events");
        for (int i = 0; i < arrayEvents.size(); i++) {
            Event event = new Event(arrayEvents.getJsonObject(i), new ArrayList<>());
            ids.add(event.getId());
            events.add(event);
        }

        JsonArray params = new JsonArray();
        String query = "UPDATE " + Presences.dbSchema + ".event SET counsellor_regularisation = ? ";
        params.add(eventBody.getBoolean("regularized"));

        query += " WHERE id IN " + Sql.listPrepared(ids) + " AND type_id = 1";
        params.addAll(new JsonArray(ids));
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(eventEither -> {
            if (eventEither.isLeft()) {
                String message = "[Presences@DefaultEventService] Failed to edit reason on events";
                LOGGER.error(message);
                handler.handle(new Either.Left<>(message));
                return;
            }

            editCorrespondingAbsences(events, user, eventBody.getString("student_id"), eventBody.getString("structure_id"),
                    eventBody.getBoolean("regularized"), handler);
        }));
    }

    @SuppressWarnings("unchecked")
    private void editCorrespondingAbsences(List<Event> editedEvents, UserInfos user, String studentId, String structureId,
                                           Boolean regularized, Integer reasonId, Handler<Either<String, JsonObject>> handler) {
        Future<JsonArray> slotsFuture = Future.future();
        Future<JsonArray> absencesFuture = Future.future();

        getAbsenceWithEvents(editedEvents, studentId, absencesFuture);
        getTimeSlots(structureId, slotsFuture);

        FutureHelper.all(Arrays.asList(slotsFuture, absencesFuture)).setHandler(result -> {
            if (result.failed()) {
                String message = "[Presences@DefaultEventService::editCorrespondingAbsences] Failed to retrieve data";
                LOGGER.error(message, result.cause().getMessage());
                handler.handle(new Either.Left<>(message));
                return;
            }

            JsonArray absences = absencesFuture.result();
            List<Slot> slots = SlotHelper.getSlotListFromJsonArray(slotsFuture.result());

            JsonObject nullAbsenceEvents = ((List<JsonObject>) absences.getList()).stream()
                    .filter(absence -> absence.getInteger("id") == null)
                    .findFirst()
                    .orElse(null);

            List<JsonObject> absencesWithEvents = ((List<JsonObject>) absences.getList()).stream()
                    .filter(absence -> absence.getInteger("id") != null)
                    .collect(Collectors.toList());

            List<Future<JsonObject>> futures = new ArrayList<>();

            if (nullAbsenceEvents != null) {
                Future<JsonObject> future = Future.future();
                futures.add(future);
                createAbsencesFromEvents(slots, nullAbsenceEvents, regularized, reasonId, user, studentId, structureId, future);
            }

            for (JsonObject absence : absencesWithEvents) {
                Future<JsonObject> future = Future.future();
                updateAbsenceFromEvents(absence, regularized, reasonId, user, future);
            }

            FutureHelper.all(futures).setHandler(event -> {
                if (event.failed()) {
                    LOGGER.info("[Presences@DefaultEventService::editCorrespondingAbsences::CompositeFuture]: " +
                            "An error has occured)");
                    handler.handle(new Either.Left<>(event.cause().getMessage()));
                } else {
                    handler.handle(new Either.Right<>(new JsonObject().put("status", "ok")));
                }
            });

        });
    }

    private void getAbsenceWithEvents(List<Event> editedEvents, String studentId, Handler<AsyncResult<JsonArray>> handler) {
        Date startDate = editedEvents.stream()
                .map(event -> DateHelper.parseDate(event.getStartDate()))
                .min(Date::compareTo)
                .orElse(null);

        Date endDate = editedEvents.stream()
                .map(event -> DateHelper.parseDate(event.getEndDate()))
                .max(Date::compareTo)
                .orElse(null);

        if (startDate == null || endDate == null) {
            String message = "[Presences@DefaultEventService::getAbsenceWithEvents] Failed to get date range";
            LOGGER.error(message);
            handler.handle(Future.failedFuture(message));
            return;
        }

        String query = "SELECT a.id, " +
                "       a.student_id, " +
                "       a.counsellor_regularisation, " +
                "       a.start_date, " +
                "       a.end_date, " +
                "       a.structure_id, " +
                "       a.reason_id, " +
                "       array_to_json(array_agg(e)) as events " +
                "      FROM presences.absence a " +
                "         RIGHT JOIN presences.event e " +
                "                    ON e.type_id = 1 " +
                "                        AND (a.student_id = e.student_id) " +
                "                        AND e.student_id = ?" +
                "                        AND ((a.start_date < e.end_date AND e.start_date < a.end_date) OR " +
                "                             (e.start_date < a.end_date AND a.start_date < e.end_date)) " +
                "      WHERE ((e.start_date < ? AND ? < e.end_date) " +
                "       OR (? < e.end_date AND e.start_date < ?) " +
                "       OR (a.start_date < ? AND ? < a.end_date) " +
                "       OR (? < a.end_date AND a.start_date < ?)) " +
                "      AND e.student_id = ? " +
                "      GROUP BY a.id;";

        JsonArray params = new JsonArray();
        params.add(studentId);
        params.add(endDate.toString());
        params.add(startDate.toString());
        params.add(startDate.toString());
        params.add(endDate.toString());
        params.add(endDate.toString());
        params.add(startDate.toString());
        params.add(startDate.toString());
        params.add(endDate.toString());
        params.add(studentId);

        sql.prepared(query, params, SqlResult.validResultHandler(FutureHelper.handlerJsonArray(handler)));
    }

    private void getTimeSlots(String structureId, Handler<AsyncResult<JsonArray>> handler) {
        slotHelper.getTimeSlots(structureId, result -> {
            if (result.isLeft()) {
                handler.handle(Future.failedFuture(result.left().getValue()));
                return;
            }
            handler.handle(Future.succeededFuture(result.right().getValue().getJsonArray("slots", new JsonArray())));
        });
    }

    private void editCorrespondingAbsences(List<Event> editedEvents, UserInfos user, String studentId, String structureId,
                                           Boolean regularized, Handler<Either<String, JsonObject>> handler) {
        editCorrespondingAbsences(editedEvents, user, studentId, structureId, regularized, null, handler);
    }

    @SuppressWarnings("unchecked")
    private void createAbsencesFromEvents(List<Slot> slots, JsonObject nullAbsenceEvents, Boolean regularized, Integer reasonId,
                                          UserInfos user, String studentId, String structureId, Future<JsonObject> future) {

        ArrayList<Event> independentEvents = (ArrayList<Event>) new JsonArray(nullAbsenceEvents.getString("events"))
                .stream()
                .map(oEvent -> new Event((JsonObject) oEvent, new ArrayList<>()))
                .collect(Collectors.toList());

        //This list will contained Maps of eventId -> event. Each Map represent an absence to create.
        List<Map<Integer, Event>> listChainedEvents = new ArrayList<>();

        for (Event event : independentEvents) {
            Event nextEvent = getNextEvent(event, independentEvents);
            if (nextEvent == null) {
                addEventsToNewChainedMap(listChainedEvents, Collections.singletonList(event));
                continue;
            }

            Slot currentSlot = SlotHelper.getCurrentSlot(event.getEndDate(), slots);
            if (currentSlot == null) {
                addEventsToNewChainedMap(listChainedEvents, Collections.singletonList(event));
                continue;
            }

            Slot nextSlot = SlotHelper.getNextTimeSlot(currentSlot, slots);
            if (nextSlot == null) {
                addEventsToNewChainedMap(listChainedEvents, Collections.singletonList(event));
                continue;
            }

            String nextEventStartTime = DateHelper.fetchTimeString(nextEvent.getStartDate(), DateHelper.SQL_FORMAT);

            //Check if next event time (from the current event) correspond to the next slot (from the current slot,
            // himself based on the current event).
            if (DateHelper.isHourAfterOrEqual(nextEventStartTime, nextSlot.getStartHour(), DateHelper.HOUR_MINUTES)
                    && DateHelper.isDateFormatBefore(nextEventStartTime, nextSlot.getEndHour(), DateHelper.HOUR_MINUTES))
                addEventsToNewChainedMap(listChainedEvents, Arrays.asList(event, nextEvent));
            else
                addEventsToNewChainedMap(listChainedEvents, Collections.singletonList(event));
        }

        createAbsencesFromChainedEvents(listChainedEvents, regularized, reasonId,
                user, studentId, structureId, future);
    }

    private Event getNextEvent(Event event, List<Event> independentEvents) {
        long currentEventEndDate = DateHelper.parseDate(event.getEndDate()).getTime();
        return independentEvents
                .stream()
                .filter(afterEvent -> DateHelper.parseDate(afterEvent.getStartDate()).getTime() - currentEventEndDate > 0)
                .min((eventA, eventB) -> {
                    long dateA = DateHelper.parseDate(eventA.getStartDate()).getTime() - currentEventEndDate;
                    long dateB = DateHelper.parseDate(eventB.getStartDate()).getTime() - currentEventEndDate;
                    return Long.compare(dateA, dateB);
                }).orElse(null);
    }

    private void addEventsToNewChainedMap(List<Map<Integer, Event>> listChainedEvents, List<Event> events) {
        Map<Integer, Event> map = listChainedEvents.stream()
                .filter(toto -> events.stream().anyMatch(event -> toto.containsKey(event.getId())))
                .findFirst()
                .orElse(new HashMap<>());

        if (map.isEmpty()) listChainedEvents.add(map);
        for (Event event : events) map.put(event.getId(), event);
    }

    private void createAbsencesFromChainedEvents(List<Map<Integer, Event>> listChainedEvents, Boolean regularized,
                                                 Integer reasonId, UserInfos user, String studentId, String structureId,
                                                 Future<JsonObject> future) {
        List<Future<JsonObject>> absencesFutures = new ArrayList<>();
        for (Map<Integer, Event> eventsMap : listChainedEvents) {
            List<Event> events = new ArrayList<>(eventsMap.values());
            Future<JsonObject> absenceFuture = Future.future();
            absencesFutures.add(absenceFuture);
            Date startDate = events.stream()
                    .map(event -> DateHelper.parseDate(event.getStartDate()))
                    .min(Date::compareTo)
                    .orElse(null);

            Date endDate = events.stream()
                    .map(event -> DateHelper.parseDate(event.getEndDate()))
                    .max(Date::compareTo)
                    .orElse(null);

            if (startDate == null || endDate == null) {
                String message = "[Presences@DefaultEventService::editCorrespondingAbsences] Failed to get absence date range";
                LOGGER.error(message);
                absenceFuture.fail(message);
                continue;
            }

            JsonObject createAbsence = new JsonObject()
                    .put("student_id", studentId)
                    .put("structure_id", structureId)
                    .put("reason_id", reasonId != null ? reasonId : getAbsenceReasonId(events))
                    .put("start_date", startDate.toString())
                    .put("end_date", endDate.toString());

            absenceService.create(createAbsence, user, false, resultCreate -> {
                if (resultCreate.isLeft()) {
                    String message = "[Presences@DefaultEventService::editCorrespondingAbsences] Failed to create absence";
                    absenceFuture.fail(message);
                    LOGGER.error(message);
                    return;
                }
                createAbsence.put("id", resultCreate.right().getValue().getInteger("id"));
                // Here we update twice because of 013-MA-403-sync-absence-with-events.sql trigger.
                updateRegularizationAbsence(createAbsence, regularized, user, absenceFuture);
            });
        }

        FutureHelper.all(absencesFutures).setHandler(result -> {
            if (result.failed()) {
                String message = "[Presences@DefaultEventService::editCorrespondingAbsences] Failed to create absences";
                LOGGER.error(message + " " + result.cause().getMessage());
                future.fail(message);
                return;
            }
            future.complete(new JsonObject().put("success", "ok"));
        });
    }

    private void updateAbsenceFromEvents(JsonObject absence, Boolean regularized, Integer reason_id, UserInfos user, Future<JsonObject> future) {
        ArrayList<Event> events = (ArrayList<Event>) new JsonArray(absence.getString("events"))
                .stream()
                .map((oEvent) -> new Event((JsonObject) oEvent, new ArrayList<>()))
                .collect(Collectors.toList());

        Integer newReasonId = getAbsenceReasonId(events);

        absence.put("reason_id", reason_id != null ? reason_id : newReasonId);
        absenceService.update(absence.getLong("id"), absence, user, false, resultUpdate -> {
            if (resultUpdate.isLeft()) {
                String message = "[Presences@DefaultEventService::editCorrespondingAbsences] Failed to update absence";
                LOGGER.error(message);
            } else {
                // Here we update twice because of 013-MA-403-sync-absence-with-events.sql trigger.
                updateRegularizationAbsence(absence, regularized, user, future);
            }
        });
    }

    private Integer getAbsenceReasonId(List<Event> events) {
        List<Integer> distinctReasons = events.stream().map(event -> event.getReason().getId()).distinct().collect(Collectors.toList());

        if (distinctReasons.size() > 1) {
            return -1;
        } else if (distinctReasons.size() == 1) {
            return distinctReasons.get(0);
        }
        return null;
    }

    private void updateRegularizationAbsence(JsonObject absence, Boolean regularized, UserInfos user, Future<JsonObject> future) {
        if (regularized != null) {
            absence.put("regularized", regularized);
            absence.put("ids", (new JsonArray()).add(absence.getInteger("id")));
            absenceService.changeRegularizedAbsences(absence, user, false, resultUpdate -> {
                if (resultUpdate.isLeft()) {
                    String message = "[Presences@DefaultEventService::editCorrespondingAbsences] Failed to update absence";
                    LOGGER.error(message);
                    future.fail(resultUpdate.left().getValue());
                } else {
                    future.complete();
                }
            });
        } else {
            future.complete();
        }
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
                "event.counsellor_input, event.counsellor_regularisation, event.followed, event.comment, " +
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
                                       String endDate, boolean noReasons, String recoveryMethodUsed, Boolean regularized,
                                       Handler<Either<String, JsonArray>> handler) {
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
                    JsonObject eventsQuery = getEventQuery(eventType, students, structure, justified, reasonsId,
                            massmailed, null, startDate, endDate, noReasons, recoveryMethod, null, null, null, null, true, regularized);
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
                                    reasonsId, massmailed, null, startDate, endDate, noReasons, recoveryMethod, defaultStartTime, halfOfDay, null, null, true, regularized);
                            JsonObject afternoonQuery = getEventQuery(eventType, students, structure, justified,
                                    reasonsId, massmailed, null, startDate, endDate, noReasons, recoveryMethod, halfOfDay, defaultEndTime, null, null, true, regularized);
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
                                     List<Integer> reasonsId, Boolean massmailed, Boolean compliance, String startDate, String endDate,
                                     boolean noReasons, String recoveryMethod, String startTime, String endTime,
                                     String limit, String offset, boolean isCount, Boolean regularized) {
        String dateCast = !"HOUR".equals(recoveryMethod) ? "::date" : "";
        String periodRangeName = "HALF_DAY".equals(recoveryMethod) && endTime.equals(defaultEndTime) ? "AFTERNOON" : "";
        periodRangeName = "HALF_DAY".equals(recoveryMethod) && startTime.equals(defaultStartTime) ? "MORNING" : periodRangeName;
        String periodRange = periodRangeName.length() != 0 ? ",'" + periodRangeName + "' as period " : "";
        String query = "SELECT event.student_id, event.start_date" + dateCast + ", event.end_date" +
                dateCast + ", event.type_id, '" + recoveryMethod + "' as recovery_method, " +
                "json_agg(" +
                "json_build_object('id', event.id, " +
                "'start_date', event.start_date, " +
                "'end_date', event.end_date, " +
                "'comment', event.comment, " +
                "'counsellor_input', event.counsellor_input, " +
                "'student_id', event.student_id, " +
                "'register_id', event.register_id, " +
                "'type_id', event.type_id, " +
                "'reason_id', event.reason_id, " +
                "'owner', event.owner, " +
                "'created', event.created, " +
                "'counsellor_regularisation', event.counsellor_regularisation, " +
                "'followed', event.followed, " +
                "'massmailed', event.massmailed, " +
                "'reason', json_build_object('id', reason.id, 'absence_compliance', reason.absence_compliance)" +
                ")) as events " +
                periodRange +
                "FROM " + Presences.dbSchema + ".event " +
                "LEFT JOIN presences.reason ON (reason.id = event.reason_id) " +
                "INNER JOIN presences.register ON (register.id = event.register_id) " +
                "WHERE event.start_date" + dateCast + " >= ? " +
                "AND event.end_date" + dateCast + "<= ? " +
                "AND register.structure_id = ? " +
                "AND type_id = ?";
        JsonArray params = new JsonArray()
                .add(startDate + " " + defaultStartTime)
                .add(endDate + " " + defaultEndTime)
                .add(structure)
                .add(eventTypes);

        if (compliance != null) {
            query += " AND (reason.absence_compliance IS " + compliance + (compliance ? " OR reason.absence_compliance IS NULL) " : ")");
        }

        if ("HALF_DAY".equals(recoveryMethod)) {
            String dateEquality = "AFTERNOON".equals(periodRangeName) ? "=" : "";
            query += " AND event.start_date::time >" + dateEquality + " '" + startTime + "' AND event.start_date::time <" + dateEquality + " '" + endTime + "'";
        }

        if (students != null && !students.isEmpty()) {
            query += " AND student_id IN " + Sql.listPrepared(students);
            params.addAll(new JsonArray(students));
        }

        // If we want to fetch events WITH reasonId, array reasonIds fetched is not empty 
        // (optional if we wish noReason fetched at same time then noReason is TRUE)
        if (reasonsId != null && !reasonsId.isEmpty()) {
            query += " AND (reason_id IN " + Sql.listPrepared(reasonsId) + (noReasons ? " OR reason_id IS NULL" : "") + ") ";
            params.addAll(new JsonArray(reasonsId));
        }

        // If we want to fetch events with NO reasonId, array reasonIds fetched is empty 
        // AND noReason is TRUE
        if (reasonsId != null && reasonsId.isEmpty() && noReasons) {
            query += " AND reason_id IS NULL ";
        }

        if (massmailed != null) {
            query += " AND massmailed = ? ";
            params.add(massmailed);
        }

        if (regularized != null || justified != null) {
            query += " AND counsellor_regularisation = ?";
            params.add(regularized != null ? regularized : justified);
        }

        query += " GROUP BY event.start_date" + dateCast + ", event.student_id, event.end_date" + dateCast + ", event.type_id ";
        if (!isCount) {
            query += "ORDER BY event.end_date" + dateCast + " DESC, event.start_date" + dateCast + " DESC";
        }

        if (limit != null) {
            query += " LIMIT ? ";
            params.add(limit);
        }

        if (offset != null) {
            query += " OFFSET ? ";
            params.add(offset);
        }


        return new JsonObject()
                .put("query", query)
                .put("params", params);
    }

    @Override
    public void getEventsByStudent(Integer eventType, List<String> students, String structure, Boolean justified,
                                   List<Integer> reasonsId, Boolean massmailed, Boolean compliance, String startDate, String endDate,
                                   boolean noReasons, String recoveryMethodUsed, String limit, String offset,
                                   Boolean regularized, Handler<Either<String, JsonArray>> handler) {
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
            JsonObject eventsQuery = getEventQuery(eventType, students, structure, justified, reasonsId, massmailed, compliance,
                    startDate, endDate, noReasons, "HOUR", null, null, limit, offset, false, null);
            Sql.getInstance().prepared(eventsQuery.getString("query"), eventsQuery.getJsonArray("params"), SqlResult.validResultHandler(queryHandler));
            return;
        }

        settingsService.retrieve(structure, event -> {
            if (event.isLeft()) {
                handler.handle(new Either.Left<>(event.left().getValue()));
                return;
            }

            JsonObject settings = event.right().getValue();
            String recoveryMethod = recoveryMethodUsed != null ? recoveryMethodUsed : settings.getString("event_recovery_method", "");
            switch (recoveryMethod) {
                case "DAY":
                case "HOUR": {
                    JsonObject eventsQuery = getEventQuery(eventType, students, structure, justified, reasonsId,
                            massmailed, compliance, startDate, endDate, noReasons, recoveryMethod, null, null, limit, offset, false, regularized);
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
                        if (slotSetting.containsKey("end_of_half_day") && slotSetting.getString("end_of_half_day") != null) {
                            String halfOfDay = slotSetting.getString("end_of_half_day");
                            JsonObject afternoonQuery = getEventQuery(eventType, students, structure, justified, reasonsId,
                                    massmailed, compliance, startDate, endDate, noReasons, recoveryMethod, halfOfDay, defaultEndTime, null, null, false, regularized);
                            JsonObject morningQuery = getEventQuery(eventType, students, structure, justified, reasonsId,
                                    massmailed, compliance, startDate, endDate, noReasons, recoveryMethod, defaultStartTime, halfOfDay, null, null, false, regularized);
                            String query = "WITH events as ((" + afternoonQuery.getString("query") + ") UNION ALL (" + morningQuery.getString("query") + ")" +
                                    ") SELECT * FROM events ORDER BY end_date DESC, start_date DESC, period";

                            JsonArray params = new JsonArray()
                                    .addAll(afternoonQuery.getJsonArray("params"))
                                    .addAll(morningQuery.getJsonArray("params"));

                            if (limit != null) {
                                query += " LIMIT ? ";
                                params.add(limit);
                            }

                            if (offset != null) {
                                query += " OFFSET ? ";
                                params.add(offset);
                            }

                            Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(queryHandler));
                        } else {
                            handler.handle(new Either.Left<>("Structure does not initialize end of half day"));
                        }
                    });
            }
        });
    }

    @Override
    public void getEventsByStudent(Integer eventType, List<String> students, String structure, Boolean justified,
                                   List<Integer> reasonsId, Boolean massmailed, String startDate, String endDate,
                                   boolean noReasons, String recoveryMethodUsed, Boolean regularized,
                                   Handler<Either<String, JsonArray>> handler) {
        this.getEventsByStudent(eventType, students, structure, justified, reasonsId, massmailed, startDate, endDate, noReasons,
                recoveryMethodUsed, null, null, regularized, handler);
    }

    @Override
    public void getEventsByStudent(Integer eventType, List<String> students, String structure, Boolean justified, List<Integer> reasonsId,
                                   Boolean massmailed, Boolean compliance, String startDate, String endDate, boolean noReasons,
                                   String recoveryMethodUsed, Boolean regularized, Handler<Either<String, JsonArray>> handler) {
        this.getEventsByStudent(eventType, students, structure, justified, reasonsId, massmailed, compliance, startDate,
                endDate, noReasons, recoveryMethodUsed, null, null, regularized, handler);
    }

    @Override
    public void getEventsByStudent(Integer eventType, List<String> students, String structure, Boolean justified,
                                   List<Integer> reasonsId, Boolean massmailed, String startDate, String endDate,
                                   boolean noReasons, String recoveryMethodUsed, String limit, String offset,
                                   Boolean regularized, Handler<Either<String, JsonArray>> handler) {
        this.getEventsByStudent(eventType, students, structure, justified, reasonsId, massmailed, null, startDate,
                endDate, noReasons, recoveryMethodUsed, null, null, regularized, handler);
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