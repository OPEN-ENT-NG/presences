package fr.openent.presences.service.impl;

import fr.openent.presences.Presences;
import fr.openent.presences.common.helper.DateHelper;
import fr.openent.presences.common.helper.FutureHelper;
import fr.openent.presences.common.message.MessageResponseHandler;
import fr.openent.presences.common.service.GroupService;
import fr.openent.presences.common.service.UserService;
import fr.openent.presences.common.service.impl.DefaultGroupService;
import fr.openent.presences.common.service.impl.DefaultUserService;
import fr.openent.presences.common.viescolaire.Viescolaire;
import fr.openent.presences.constants.Reasons;
import fr.openent.presences.core.constants.Field;
import fr.openent.presences.db.DBService;
import fr.openent.presences.enums.EventRecoveryMethodEnum;
import fr.openent.presences.enums.Markers;
import fr.openent.presences.helper.*;
import fr.openent.presences.model.Event.EventType;
import fr.openent.presences.model.Event.Event;
import fr.openent.presences.enums.EventTypeEnum;
import fr.openent.presences.model.Slot;
import fr.openent.presences.service.*;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.I18n;
import fr.wseduc.webutils.http.Renders;
import io.vertx.core.*;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;
import org.entcore.common.notification.TimelineHelper;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;

import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

public class DefaultEventService extends DBService implements EventService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultEventService.class);
    private static final String defaultStartTime = "00:00:00";
    private static final String defaultEndTime = "23:59:59";
    private final EventBus eb;
    private final SettingsService settingsService = new DefaultSettingsService();
    private final AbsenceService absenceService;
    private final GroupService groupService;
    private final EventHelper eventHelper;
    private final CourseHelper courseHelper;
    private final SlotHelper slotHelper;
    private final UserService userService;
    private final RegisterService registerService;
    private final TimelineHelper timelineHelper;
    private final CommonPresencesServiceFactory commonPresencesServiceFactory;

    public DefaultEventService(EventBus eb) {
        this.eb = eb;
        this.eventHelper = new EventHelper(eb);
        this.courseHelper = new CourseHelper(eb);
        this.slotHelper = new SlotHelper(eb);
        this.absenceService = new DefaultAbsenceService(eb);
        this.groupService = new DefaultGroupService(eb);
        this.userService = new DefaultUserService();

        // todo spread new class CommonPresencesServiceFactory toward other classes that use EventService
        this.registerService = null;
        this.commonPresencesServiceFactory = null;
        this.timelineHelper = null;
    }

    public DefaultEventService(CommonPresencesServiceFactory commonPresencesServiceFactory) {
        this.eb = commonPresencesServiceFactory.eventBus();
        this.eventHelper = commonPresencesServiceFactory.eventHelper();
        this.courseHelper = commonPresencesServiceFactory.courseHelper();
        this.slotHelper = commonPresencesServiceFactory.slotHelper();
        this.absenceService = commonPresencesServiceFactory.absenceService();
        this.groupService = commonPresencesServiceFactory.groupService();
        this.userService = commonPresencesServiceFactory.userService();
        this.registerService = commonPresencesServiceFactory.registerService();
        this.timelineHelper = commonPresencesServiceFactory.timelineHelper();
        this.commonPresencesServiceFactory = commonPresencesServiceFactory;
    }

    @Override
    public void get(String structureId, String startDate, String endDate, String startTime, String endTime,
                    List<String> eventType, List<String> listReasonIds, Boolean noAbsenceReason, Boolean noLatenessReason, List<String> userId,
                    List<String> restrictedClasses, Boolean regularized, Boolean followed, Integer page, Handler<AsyncResult<JsonArray>> handler) {

        Future<JsonArray> eventsFuture = Future.future();
        Future<JsonObject> slotsFuture = Future.future();

        getDayMainEvents(structureId, startDate, endDate, startTime, endTime, userId, eventType, listReasonIds, noAbsenceReason,
                noLatenessReason, regularized, followed, page, eventsFuture);
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

                        eventHelper.addStudentsToEvents(structureId, events, studentIds, restrictedClasses, startDate, endDate, startTime,
                                endTime, eventType, listReasonIds, noAbsenceReason, noLatenessReason, regularized, followed, absences,
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
    public Future<JsonArray> getEventsBetweenDates(String startDate, String endDate, List<String> users,
                                                   List<Integer> eventType, String structureId) {
        Promise<JsonArray> promise = Promise.promise();

        JsonArray params = new JsonArray();
        String query = "SELECT e.* FROM " + Presences.dbSchema + ".event AS e" +
                " INNER JOIN " + Presences.dbSchema + ".register AS r ON (r.id = e.register_id AND r.structure_id = ?)" +
                " WHERE ? < e.end_date AND e.start_date < ? ";
        params.add(structureId);
        params.add(startDate);
        params.add(endDate);

        if (users != null && !users.isEmpty()) {
            query += " AND e.student_id IN " + Sql.listPrepared(users.toArray()) + " ";
            params.addAll(new JsonArray(users));
        }

        if (eventType != null && !eventType.isEmpty()) {
            query += " AND type_id IN " + Sql.listPrepared(eventType) + " ";
            params.addAll(new JsonArray(eventType));
        }

        sql.prepared(query, params, SqlResult.validResultHandler(event -> {
            if (event.isLeft()) {
                String message = String.format("[Presences@%s::getEventsBetweenDates] Failed to get events between date: %s",
                        this.getClass().getSimpleName(), event.left().getValue());
                LOGGER.error(message, event.left());
                promise.fail(event.left().getValue());
            } else {
                promise.complete(event.right().getValue());
            }
        }));
        return promise.future();
    }

    @Override
    public void getDayMainEvents(String structureId, String startDate, String endDate, String startTime, String endTime,
                                 List<String> studentIds, List<String> typeIds,
                                 List<String> reasonIds, Boolean noAbsenceReason, Boolean noLatenessReason, Boolean regularized, Boolean followed,
                                 Integer page, Handler<AsyncResult<JsonArray>> handler) {
        JsonArray params = new JsonArray();
        String query = getDayMainEventsQuery(structureId, startDate, endDate, startTime,
                endTime, studentIds, typeIds, reasonIds, noAbsenceReason, noLatenessReason, regularized, followed, params) +
                " ORDER BY date DESC, created DESC, student_id " +
                " OFFSET ? LIMIT ? ";

        params.add(Presences.PAGE_SIZE * page);
        params.add(Presences.PAGE_SIZE);
        sql.prepared(query, params, SqlResult.validResultHandler(FutureHelper.handlerJsonArray(handler)));
    }

    private String getDayMainEventsQuery(String structureId, String startDate, String endDate, String startTime, String endTime,
                                         List<String> studentIds, List<String> typeIds, List<String> reasonIds, Boolean noAbsenceReason,
                                         Boolean noLatenessReason, Boolean regularized, Boolean followed, JsonArray params) {
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
                EventQueryHelper.filterReasons(reasonIds, noAbsenceReason, noLatenessReason, regularized, followed, typeIds, params) +
                " GROUP BY student_id, date ";
    }

    @Override
    public void getEvents(String structureId, String startDate, String endDate,
                          List<String> eventType, List<String> listReasonIds, Boolean noReason, Boolean noReasonLateness, List<String> userId, JsonArray userIdFromClasses,
                          Boolean regularized, Boolean followed, Integer page, Handler<Either<String, JsonArray>> handler) {
        JsonArray params = new JsonArray();
        sql.prepared(this.getEventsQuery(structureId, startDate, endDate,
                        eventType, listReasonIds, noReason, noReasonLateness, regularized, followed, userId, userIdFromClasses, page, params),
                params, SqlResult.validResultHandler(handler));
    }

    @Override
    public Future<JsonObject> getEventsCount(String structureId, String startDate, String endDate,
                                             List<String> eventType, List<String> listReasonIds, Boolean noReason, Boolean noReasonLateness, List<String> userId, JsonArray userIdFromClasses,
                                             Boolean regularized, Boolean followed) {
        Promise<JsonObject> promise = Promise.promise();
        JsonArray params = new JsonArray();
        sql.prepared(this.getEventsQuery(structureId, startDate, endDate,
                        eventType, listReasonIds, noReason, noReasonLateness, regularized, followed, userId, userIdFromClasses, null, params, true),
                params, SqlResult.validUniqueResultHandler(FutureHelper.handlerJsonObject(promise)));

        return promise.future();
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

    private String getEventsQuery(String structureId, String startDate, String endDate, List<String> eventType,
                                  List<String> listReasonIds, Boolean noReason, Boolean noReasonLateness, Boolean regularized, Boolean followed,
                                  List<String> userId, JsonArray userIdFromClasses, Integer page, JsonArray params) {

        return this.getEventsQuery(structureId, startDate, endDate, eventType, listReasonIds, noReason, noReasonLateness, regularized,
                followed, userId, userIdFromClasses, page, params, false);
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
                                  List<String> listReasonIds, Boolean noReason, Boolean noReasonLateness, Boolean regularized, Boolean followed,
                                  List<String> userId, JsonArray userIdFromClasses, Integer page, JsonArray params, Boolean isCount) {

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

        query += setParamsForQueryEvents(listReasonIds, userId, regularized, followed, noReason, noReasonLateness,
                userIdFromClasses, eventType, params);
        if (Boolean.TRUE.equals(isCount)) {
            query += ") SELECT COUNT(id) FROM allevents";
        } else {
            query += ") SELECT * FROM allevents " +
                    "GROUP BY id, start_date, end_date, created, comment, student_id, reason_id, owner," +
                    "type_id, register_id, counsellor_regularisation, followed, type, register_id " +
                    " ORDER BY start_date DESC, id DESC";
            if (page != null) {
                query += " OFFSET ? LIMIT ? ";
                params.add(Presences.PAGE_SIZE * page);
                params.add(Presences.PAGE_SIZE);
            }
        }

        return query;
    }

    @Override
    public void getPageNumber(String structureId, String startDate, String endDate, String startTime, String endTime,
                              List<String> eventType, List<String> listReasonIds, Boolean noAbsenceReason, Boolean noLatenessReason, List<String> userId,
                              Boolean regularized, Boolean followed, Handler<Either<String, JsonObject>> handler) {
        JsonArray params = new JsonArray();
        Sql.getInstance().prepared(this.getEventsQueryPagination(structureId, startDate, endDate, startTime, endTime, eventType,
                userId, listReasonIds, noAbsenceReason, noLatenessReason, regularized, followed, params), params, SqlResult.validUniqueResultHandler(handler));
    }

    private String getEventsQueryPagination(String structureId, String startDate, String endDate, String startTime, String endTime,
                                            List<String> eventType, List<String> userId, List<String> listReasonIds, Boolean noAbsenceReason, Boolean noLatenessReason,
                                            Boolean regularized, Boolean followed, JsonArray params) {
        return " SELECT count(DISTINCT (e.student_id, e.start_date::date)) FROM " + Presences.dbSchema + ".event e " +
                EventQueryHelper.joinRegister(structureId, params) +
                EventQueryHelper.joinEventType(eventType, params) +
                EventQueryHelper.filterDates(startDate, endDate, params) +
                EventQueryHelper.filterTimes(startTime, endTime, params) +
                EventQueryHelper.filterReasons(listReasonIds, noAbsenceReason, noLatenessReason, regularized, followed, eventType, params) +
                EventQueryHelper.filterStudentIds(userId, params) +
                EventQueryHelper.filterFollowed(followed, params);
    }

    private String setParamsForQueryEvents(List<String> listReasonIds, List<String> userId, Boolean regularized,
                                           Boolean followed, Boolean noReason, Boolean noReasonLateness, JsonArray userIdFromClasses,
                                           List<String> typeIds, JsonArray params) {
        List<String> studentId = new ArrayList<>(userId);
        userIdFromClasses.stream().map(o -> (JsonObject) o).forEach(jsonObject -> studentId.add(jsonObject.getString("studentId")));
        String query = EventQueryHelper.filterStudentIds(studentId, params);
        query = query.replaceFirst("AND ", "");

        String reasonFilter = EventQueryHelper.filterReasons(listReasonIds, noReason, noReasonLateness, regularized, followed, typeIds, params);

        return query.isEmpty() ? reasonFilter : " AND (" + query + reasonFilter + ")";
    }

    @Override
    public Future<JsonObject> getAbsencesCountSummary(String structureId, String startAt, String endAt) {
        return getAbsencesCountSummary(structureId, startAt, endAt, Arrays.stream(Markers.values())
                .map(Enum::name)
                .collect(Collectors.toList()));
    }

    @Override
    @SuppressWarnings("unchecked")
    public Future<JsonObject> getAbsencesCountSummary(String structureId, String startAt, String endAt, List<String> requiredMarkers) {
        Promise<JsonObject> promise = Promise.promise();
        Promise<JsonArray> countCurrentStudentsFuture = Promise.promise();
        Future<JsonArray> halfBoardersFuture = requiredMarkers.contains(Markers.NB_HALFBOARDERS.name()) ?
                userService.getStudents(structureId, null, true, null) :
                Future.succeededFuture(new JsonArray());

        Future<JsonArray> internalsFuture = requiredMarkers.contains(Markers.NB_INTERNALS.name()) ?
                userService.getStudents(structureId, null, null, true) :
                Future.succeededFuture(new JsonArray());

        Map<String, Future<JsonObject>> countsFutures = new HashMap<>();

        String startDay = DateHelper.getDateString(startAt, DateHelper.MONGO_FORMAT, DateHelper.YEAR_MONTH_DAY);
        String startDayTime = DateHelper.fetchTimeString(startAt, DateHelper.MONGO_FORMAT);
        String endDay = DateHelper.getDateString(endAt, DateHelper.MONGO_FORMAT, DateHelper.YEAR_MONTH_DAY);
        String endDayTime = DateHelper.fetchTimeString(endAt, DateHelper.MONGO_FORMAT);

        CompositeFuture.all(halfBoardersFuture, internalsFuture, countCurrentStudentsFuture.future())
                .compose(studentsFutures -> {
                    List<String> halfBoarderIds = ((List<JsonObject>) halfBoardersFuture.result().getList()).stream()
                            .map(student -> student.getString(Field.ID))
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());

                    List<String> internalIds = ((List<JsonObject>) internalsFuture.result().getList()).stream()
                            .map(student -> student.getString(Field.ID))
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());

                    countsFutures.put(Field.HALFBOARDER,
                            halfBoarderIds.isEmpty() ? Future.succeededFuture(new JsonObject().put(Field.COUNT, 0)) :
                                    absenceService.countAbsentStudents(structureId, halfBoarderIds, startAt, endAt)
                    );
                    countsFutures.put(Field.INTERNAL,
                            internalIds.isEmpty() ? Future.succeededFuture(new JsonObject().put(Field.COUNT, 0)) :
                                    absenceService.countAbsentStudents(structureId, internalIds, startAt, endAt));

                    countsFutures.put(Field.ABSENCE, requiredMarkers.contains(Markers.NB_ABSENTS.name()) || requiredMarkers.contains(Markers.NB_PRESENTS.name()) ?
                            absenceService.countAbsentStudents(structureId, null, startAt, endAt) :
                            Future.succeededFuture(new JsonObject().put(Field.COUNT, 0)));


                    return CompositeFuture.all(new ArrayList<>(countsFutures.values()));
                })
                .onFailure(err -> {
                    String message = String.format("[Presences@%s::getAbsencesCountSummary] " +
                            "Failed to retrieve absent data numbers", this.getClass().getName());
                    LOGGER.error(String.format("%s %s", message, err.getMessage()));
                    promise.fail(message);
                })
                .onSuccess(futuresRes -> {
                    int nbCurrentStudents = countCurrentStudentsFuture.future().result().size();
                    int countAbsents = Math.max(countsFutures.get(Field.ABSENCE).result().getInteger(Field.COUNT, 0), 0);

                    JsonObject res = new JsonObject();
                    if (requiredMarkers.contains(Markers.NB_ABSENTS.name())) res.put(Field.NB_ABSENTS, countAbsents);
                    if (requiredMarkers.contains(Markers.NB_HALFBOARDERS.name()))
                        res.put(Field.NB_DAY_STUDENTS, Math.max(countsFutures.get(Field.HALFBOARDER).result().getInteger(Field.COUNT, 0), 0));
                    if (requiredMarkers.contains(Markers.NB_PRESENTS.name()))
                        res.put(Field.NB_PRESENTS, Math.max((nbCurrentStudents - countAbsents), 0));
                    if (requiredMarkers.contains(Markers.NB_INTERNALS.name()))
                        res.put(Field.NB_INTERNALS, Math.max(countsFutures.get(Field.INTERNAL).result().getInteger(Field.COUNT, 0), 0));

                    promise.complete(res);
                });

        if (requiredMarkers.contains(Markers.NB_PRESENTS.name()))
            getCoursesStudentIds(structureId, startDay, startDayTime, endDay, endDayTime,
                    FutureHelper.handlerJsonArray(countCurrentStudentsFuture));
        else countCurrentStudentsFuture.complete(new JsonArray());

        return promise.future();
    }


    /**
     * Get number of students in all occurring courses during the specified date.
     *
     * @param structureId structure identifier
     * @param startAt     start date (format yyyy-MM-dd)
     * @param startTime   start hour (format HH:mm:ss)
     * @param endAt       end date (format yyyy-MM-dd)
     * @param endTime     end hour (format HH:mm:ss)
     * @param handler     Function handler returning data
     */
    @SuppressWarnings("unchecked")
    private void getCoursesStudentIds(String structureId, String startAt, String startTime,
                                      String endAt, String endTime,
                                      Handler<Either<String, JsonArray>> handler) {
        courseHelper.getCourses(structureId, startAt, endAt, startTime, endTime, Boolean.TRUE.toString(), event -> {
            if (event.isLeft()) {
                handler.handle(new Either.Left<>(event.left().getValue()));
                return;
            }

            JsonArray courses = event.right().getValue();

            List<String> classesName = ((List<JsonObject>) courses.getList()).stream()
                    .flatMap(course -> ((List<String>) course.getJsonArray(Field.CLASSES, new JsonArray()).getList()).stream())
                    .distinct()
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            List<String> groupsName = ((List<JsonObject>) courses.getList()).stream()
                    .flatMap(course -> ((List<String>) course.getJsonArray(Field.GROUPS, new JsonArray()).getList()).stream())
                    .distinct()
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            String query = "MATCH (u:User {profiles: ['Student']})-[:IN]->(pg:ProfileGroup)-[:DEPENDS]->(s:Structure {id:{structureId}})" +
                    " OPTIONAL MATCH (c:Class)<-[:DEPENDS]-(:ProfileGroup)<-[:IN]-(u)" +
                    " OPTIONAL MATCH (fg:FunctionalGroup)<-[:IN]-(u)" +
                    " OPTIONAL MATCH (mg:ManualGroup)<-[:IN]-(u)" +
                    " WITH u, c, fg, mg" +
                    " WHERE c.name IN {classesName} OR fg.name IN {groupsName} OR mg.name IN {groupsName}" +
                    " RETURN distinct(u.id) AS id";


            JsonObject params = new JsonObject()
                    .put("classesName", new JsonArray(classesName))
                    .put("groupsName", new JsonArray(groupsName))
                    .put("structureId", structureId);
            Neo4j.getInstance().execute(query, params, Neo4jResult.validResultHandler(handler));
        });
    }

    @Override
    public void create(JsonObject event, UserInfos user, Handler<Either<String, JsonObject>> handler) {
        checkPresenceEvent(event)
                .onSuccess(aVoid -> {
                    JsonArray statements = new JsonArray();
                    statements.add(eventHelper.getCreationStatement(event, user));

                    if (EventTypeEnum.ABSENCE.getType().equals(event.getInteger("type_id"))) {
                        statements.add(eventHelper.getDeletionEventStatement(event));
                    }

                    Sql.getInstance().transaction(statements, statementEvent -> {
                        Either<String, JsonObject> either = SqlResult.validUniqueResult(0, statementEvent);
                        if (either.isLeft()) {
                            String err = "[Presences@DefaultEventService] Failed to create event";
                            LOGGER.error(err, either.left().getValue());
                        }
                        handler.handle(either);
                    });
                })
                .onFailure(err -> handler.handle(new Either.Left<>(err.getMessage())));
    }

    /**
     * Analyse event body and add a present reason if event's body date are matching with findable presences
     * will search its structure by retrieving a register if found then fetch list of presences and match
     * dates within event body
     * ONLY applies to ABSENCE event type
     *
     * @param event body to create an event
     * @return {@link Future<Void>}
     */
    @SuppressWarnings("unchecked")
    private Future<Void> checkPresenceEvent(JsonObject event) {
        Promise<Void> promise = Promise.promise();
        if (!event.getInteger(Field.TYPEID, 0).equals(EventTypeEnum.ABSENCE.getType())) {
            promise.complete();
            return promise.future();
        }
        registerService.fetchRegister(event.getInteger(Field.REGISTER_ID))
                .compose(register -> {
                    // complete this sequential with new ArrayList if no register object is found or structure is not found
                    if (register.isEmpty() || !register.containsKey(Field.STRUCTURE_ID)) {
                        Promise<JsonArray> registerPromise = Promise.promise();
                        registerPromise.complete(new JsonArray());
                        return registerPromise.future();
                    }
                    String startDate = DateHelper.getDateString(register.getString(Field.START_DATE), DateHelper.YEAR_MONTH_DAY);
                    String endDate = DateHelper.getDateString(register.getString(Field.END_DATE), DateHelper.YEAR_MONTH_DAY);
                    String studentId = event.getString(Field.STUDENT_ID);
                    String structureId = register.getString(Field.STRUCTURE_ID);
                    return commonPresencesServiceFactory.presenceService().fetchPresence(structureId, startDate, endDate,
                            new ArrayList<>(Collections.singletonList(studentId)), new ArrayList<>(), new ArrayList<>());
                })
                .compose(presences -> {
                    Promise<Void> presencePromise = Promise.promise();
                    boolean containPresenceInEvent = ((List<JsonObject>) presences.getList())
                            .stream()
                            .anyMatch(presence -> {
                                try {
                                    return DateHelper.isBetween(
                                            presence.getString(Field.START_DATE),
                                            presence.getString(Field.END_DATE),
                                            event.getString(Field.START_DATE),
                                            event.getString(Field.END_DATE),
                                            DateHelper.SQL_FORMAT,
                                            DateHelper.SQL_FORMAT
                                    );
                                } catch (ParseException err) {
                                    String message = String.format("[Presences@%s::checkPresenceEvent::isBetween] Failed to parse: %s",
                                            this.getClass().getSimpleName(), err.getMessage());
                                    LOGGER.error(message, err);
                                    return false;
                                }
                            });
                    if (containPresenceInEvent) {
                        event.put(Field.REASON_ID, Reasons.PRESENT_IN_STRUCTURE);
                    }
                    presencePromise.complete();
                    return presencePromise.future();
                })
                .onSuccess(promise::complete)
                .onFailure(err -> {
                    String message = String.format("[Presences@%s::checkPresenceEvent] Failed to check potential " +
                            "presences in event creation : %s", this.getClass().getSimpleName(), err.getMessage());
                    LOGGER.error(message, err);
                    promise.fail(err.getMessage());
                });

        return promise.future();
    }

    @Override
    public Future<JsonObject> update(Integer id, JsonObject event){
        Promise<JsonObject> promise = Promise.promise();
        update(id, event, FutureHelper.handlerEitherPromise(promise));
        return promise.future();
    }

    @Override
    public void update(Integer id, JsonObject event, Handler<Either<String, JsonObject>> handler) {
        Integer eventType = event.getInteger("type_id");
        JsonArray params = new JsonArray();

        String setter = "";
        if (EventTypeEnum.DEPARTURE.getType().equals(eventType)) {
            setter = "start_date = ?";
            params.add(event.getString("start_date"));
        } else if (EventTypeEnum.LATENESS.getType().equals(eventType)) {
            setter = "end_date = ?";
            params.add(event.getString("end_date"));
            if (event.containsKey(Field.REASON_ID)) {
                setter += ", reason_id = ?";
                params.add(event.getInteger(Field.REASON_ID));
            }
        } else if (EventTypeEnum.REMARK.getType().equals(eventType)) {
            setter += "comment = ?";
            params.add(event.getString("comment"));
        }

        if (!EventTypeEnum.REMARK.getType().equals(eventType) && event.containsKey("comment")) {
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
        query += " WHERE id IN " + Sql.listPrepared(ids) + " AND (type_id = 1 OR type_id = 2) RETURNING *";
        params.addAll(new JsonArray(ids));
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(result -> {
            if (result.isLeft()) {
                String message = "[Presences@DefaultEventService] Failed to edit reason on events";
                LOGGER.error(message);
                handler.handle(new Either.Left<>(message));
                return;
            }
            List<Event> absenceEvent = events.stream().filter(event -> event.getEventType() != null && EventTypeEnum.ABSENCE.getType().equals(event.getEventType().getId()))
                    .collect(Collectors.toList());
            if (absenceEvent.isEmpty()) {
                handler.handle(new Either.Right<>(new JsonObject().put(Field.STATUS, Field.OK)));
            } else {
                Boolean regularized = result.right().getValue().getList().get(0) instanceof JsonObject ?
                        result.right().getValue().getJsonObject(0).getBoolean(Field.COUNSELLOR_REGULARISATION, null) : null;
                editCorrespondingAbsences(absenceEvent, user, eventBody.getString(Field.STUDENT_ID), eventBody.getString(Field.STRUCTURE_ID),
                        regularized, eventBody.getInteger(Field.REASONID), handler);
            }
        }));
    }

    @Override
    public Future<JsonObject> changeReasonEvents(List<Long> eventsIds, Integer reasonId) {
        Promise<JsonObject> promise = Promise.promise();
        JsonArray params = new JsonArray();
        String query = "UPDATE " + Presences.dbSchema + ".event SET reason_id = ? ";
        if (reasonId != null) {
            params.add(reasonId);
        } else {
            params.addNull();
        }
        query += " WHERE id IN " + Sql.listPrepared(eventsIds);
        params.addAll(new JsonArray(eventsIds));
        sql.prepared(query, params, SqlResult.validUniqueResultHandler(event -> {
            if (event.isLeft()) {
                String message = String.format("[Presences@%s::changeReasonEvents] Failed change identifier reason %s " +
                                "to these identifiers events [%s]: %s",
                        this.getClass().getSimpleName(), reasonId, eventsIds, event.left().getValue());
                LOGGER.error(message, event.left());
                promise.fail(event.left().getValue());
            } else {
                promise.complete(event.right().getValue());
            }
        }));
        return promise.future();
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
                "                    ON (a.student_id = e.student_id) " +
                "                        AND e.student_id = ?" +
                "                        AND ((a.start_date < e.end_date AND e.start_date < a.end_date) OR " +
                "                             (e.start_date < a.end_date AND a.start_date < e.end_date)) " +
                "      WHERE e.type_id = 1 AND " +
                "       ((e.start_date < ? AND ? < e.end_date) " +
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
                    .put(Field.STUDENT_ID, studentId)
                    .put(Field.STRUCTURE_ID, structureId)
                    .put(Field.REASON_ID, reasonId != null ? reasonId : getAbsenceReasonId(events))
                    .put(Field.START_DATE, DateHelper.getDateString(startDate, DateHelper.SQL_FORMAT))
                    .put(Field.END_DATE, DateHelper.getDateString(endDate, DateHelper.SQL_FORMAT));

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
                "to_char(course_dates.course_start_date, 'YYYY-MM-DD HH24:MI:SS') as course_start_date, " +
                "to_char(course_dates.course_end_date, 'YYYY-MM-DD HH24:MI:SS') as course_end_date, " +
                "register.course_id " +
                "FROM  " + Presences.dbSchema + ".event " +
                "INNER JOIN " + Presences.dbSchema + ".register ON (event.register_id = register.id) " +
                "INNER JOIN (" +
                "SELECT r.course_id, MIN(r.start_date) as course_start_date, MAX(r.end_date) as course_end_date " +
                "FROM " + Presences.dbSchema + ".register r " +
                "GROUP BY r.course_id" +
                ") as course_dates ON (course_dates.course_id = register.course_id) " +
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
            switch (EventRecoveryMethodEnum.getInstanceFromString(recoveryMethod)) {
                case DAY:
                case HOUR: {
                    JsonObject eventsQuery = getEventQuery(eventType, students, structure, reasonsId,
                            massmailed, null, startDate, endDate, noReasons, recoveryMethod, null, null, null, null, true, regularized);
                    String query = "WITH count_by_user AS (WITH events as (" + eventsQuery.getString("query") + ") " +
                            "SELECT count(*), student_id FROM events GROUP BY student_id) SELECT * FROM count_by_user WHERE count >= " + startAt;
                    Sql.getInstance().prepared(query, eventsQuery.getJsonArray("params"), SqlResult.validResultHandler(handler));
                    break;
                }
                case HALF_DAY:
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
                            JsonObject morningQuery = getEventQuery(eventType, students, structure,
                                    reasonsId, massmailed, null, startDate, endDate, noReasons, recoveryMethod, defaultStartTime, halfOfDay, null, null, true, regularized);
                            JsonObject afternoonQuery = getEventQuery(eventType, students, structure,
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

    private JsonObject getEventQuery(Integer eventTypes, List<String> students, String structure,
                                     List<Integer> reasonsId, Boolean massmailed, Boolean compliance, String startDate, String endDate,
                                     boolean noReasons, String recoveryMethod, String startTime, String endTime,
                                     String limit, String offset, boolean isCount, Boolean regularized) {
        return getEventQuery(eventTypes, students, structure, reasonsId, massmailed, compliance, startDate, endDate,
                noReasons, null, recoveryMethod, startTime, endTime, limit, offset, isCount, regularized, null);
    }

    private JsonObject getEventQuery(Integer eventTypes, List<String> students, String structure,
                                     List<Integer> reasonsId, Boolean massmailed, Boolean compliance, String startDate, String endDate,
                                     boolean noReasons, Boolean noReasonLateness, String recoveryMethod, String startTime, String endTime,
                                     String limit, String offset, boolean isCount, Boolean regularized, Boolean followed) {
        recoveryMethod = EventRecoveryMethodEnum.getInstanceFromString(recoveryMethod) == null ? EventRecoveryMethodEnum.HOUR.getValue() : recoveryMethod;
        String dateCast = !EventRecoveryMethodEnum.HOUR.getValue().equals(recoveryMethod) ? "::date" : "";
        String periodRangeName = EventRecoveryMethodEnum.HALF_DAY.getValue().equals(recoveryMethod) && endTime.equals(defaultEndTime) ? "AFTERNOON" : "";
        periodRangeName = EventRecoveryMethodEnum.HALF_DAY.getValue().equals(recoveryMethod) && startTime.equals(defaultStartTime) ? "MORNING" : periodRangeName;
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
            query += " AND event.start_date::time >" + dateEquality + " ? AND event.start_date::time <" + dateEquality + " ?";
            params.add(startTime).add(endTime);
        }

        if (students != null && !students.isEmpty()) {
            query += " AND student_id IN " + Sql.listPrepared(students);
            params.addAll(new JsonArray(students));
        }

        List<String> listReasonId = reasonsId.stream().map(Object::toString).collect(Collectors.toList());
        query += EventQueryHelper.filterReasons(listReasonId, noReasons, noReasonLateness, regularized, followed, Collections.singletonList(eventTypes.toString()), params);

        query += EventQueryHelper.filterFollowed(followed, params);

        if (massmailed != null) {
            query += " AND massmailed = ? ";
            params.add(massmailed);
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
    public Future<JsonArray> getEventsByStudent(Boolean canSeeAllStudent, Integer eventType, List<String> students, String structure,
                                                List<Integer> reasonsId, Boolean massmailed, Boolean compliance, String startDate, String endDate,
                                                boolean noReasons, boolean noReasonsLateness, String recoveryMethodUsed, String limit, String offset,
                                                Boolean regularized, Boolean followed) {
        Promise<JsonArray> promise = Promise.promise();
        this.getEventsByStudent(canSeeAllStudent, eventType, students, structure, reasonsId, massmailed, compliance, startDate, endDate, noReasons,
                noReasonsLateness, recoveryMethodUsed, limit, offset, regularized, followed, event -> {
                    if (event.isLeft()) {
                        String message = String.format("[Presences@%s::getEventsByStudent] an error has occurred while fetching " +
                                "events by students: %s", this.getClass().getSimpleName(), event.left().getValue());
                        LOGGER.error(message);
                        promise.fail(event.left().getValue());
                    } else {
                        promise.complete(event.right().getValue());
                    }
                });
        return promise.future();
    }

    @Override
    public void getEventsByStudent(Boolean canSeeAllStudent, Integer eventType, List<String> students, String structure,
                                   List<Integer> reasonsId, Boolean massmailed, Boolean compliance, String startDate, String endDate,
                                   boolean noReasons, Boolean noReasonsLateness, String recoveryMethodUsed, String limit, String offset,
                                   Boolean regularized, Boolean followed, Handler<Either<String, JsonArray>> handler) {
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
            JsonObject eventsQuery = getEventQuery(eventType, students, structure, reasonsId, massmailed, compliance,
                    startDate, endDate, noReasons, noReasonsLateness, "HOUR", null, null, limit, offset, false, regularized, followed);
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
            switch (EventRecoveryMethodEnum.getInstanceFromString(recoveryMethod)) {
                case DAY:
                case HOUR: {
                    JsonObject eventsQuery = getEventQuery(eventType, students, structure, reasonsId,
                            massmailed, compliance, startDate, endDate, noReasons, noReasonsLateness, recoveryMethod,
                            null, null, limit, offset, false, regularized, followed);
                    Sql.getInstance().prepared(eventsQuery.getString("query"), eventsQuery.getJsonArray("params"), SqlResult.validResultHandler(queryHandler));
                    break;
                }
                case HALF_DAY:
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
                            JsonObject afternoonQuery = getEventQuery(eventType, students, structure, reasonsId,
                                    massmailed, compliance, startDate, endDate, noReasons, noReasonsLateness, recoveryMethod, halfOfDay, defaultEndTime, null, null, false, regularized, followed);
                            JsonObject morningQuery = getEventQuery(eventType, students, structure, reasonsId,
                                    massmailed, compliance, startDate, endDate, noReasons, noReasonsLateness, recoveryMethod, defaultStartTime, halfOfDay, null, null, false, regularized, followed);
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

                            Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(res -> {
                                if (res.isLeft()) {
                                    queryHandler.handle(new Either.Left<>(res.left().getValue()));
                                } else {
                                    //If we do not have permission to see the information of all the students
                                    // and the list of our students is empty
                                    // then we do not return any data
                                    queryHandler.handle(new Either.Right<>(!Boolean.TRUE.equals(canSeeAllStudent) && (students == null || students.isEmpty()) ?
                                            new JsonArray() : res.right().getValue()));
                                }
                            }));
                        } else {
                            handler.handle(new Either.Left<>("Structure does not initialize end of half day"));
                        }
                    });
            }
        });
    }

    @Override
    public void getEventsByStudent(Integer eventType, List<String> students, String structure,
                                   List<Integer> reasonsId, Boolean massmailed, String startDate, String endDate,
                                   boolean noReasons, String recoveryMethodUsed, Boolean regularized,
                                   Handler<Either<String, JsonArray>> handler) {
        this.getEventsByStudent(eventType, students, structure, reasonsId, massmailed, startDate, endDate, noReasons,
                recoveryMethodUsed, null, null, regularized, handler);
    }

    @Override
    public void getEventsByStudent(Integer eventType, List<String> students, String structure, List<Integer> reasonsId,
                                   Boolean massmailed, Boolean compliance, String startDate, String endDate, boolean noReasons,
                                   String recoveryMethodUsed, Boolean regularized, Handler<Either<String, JsonArray>> handler) {
        this.getEventsByStudent(null, eventType, students, structure, reasonsId, massmailed, compliance, startDate,
                endDate, noReasons, null, recoveryMethodUsed, null, null, regularized, null, handler);
    }

    @Override
    public void getEventsByStudent(Integer eventType, List<String> students, String structure,
                                   List<Integer> reasonsId, Boolean massmailed, String startDate, String endDate,
                                   boolean noReasons, String recoveryMethodUsed, String limit, String offset,
                                   Boolean regularized, Handler<Either<String, JsonArray>> handler) {
        this.getEventsByStudent(null, eventType, students, structure, reasonsId, massmailed, null, startDate,
                endDate, noReasons, null, recoveryMethodUsed, limit, offset, regularized, null, handler);
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

    public Future<JsonArray> getStudentsInfos(List<String> studentIds, String structureId) {
        Promise<JsonArray> promise = Promise.promise();

        JsonObject action = new JsonObject()
                .put("action", "eleve.getInfoEleve")
                .put("idEleves", studentIds)
                .put("idEtablissement", structureId);

        eb.request("viescolaire", action, MessageResponseHandler.messageJsonArrayHandler(FutureHelper.handlerEitherPromise(promise)));

        return promise.future();
    }

    public Future<Void> sendEventNotification(JsonObject event, UserInfos user, HttpServerRequest request) {
        Promise<Void> promise = Promise.promise();

        JsonObject params = new JsonObject().put(Field.RESOURCEURI, "/presences#/dashboard");
        String notificationTitle, notificationName;

        if(event.getString(Field.OLDEVENTTYPE) != null){ //If we have the oldEventType, it means we are in edition mode, else in creation
            notificationTitle = "presences.push.event.edited";
            notificationName = "presences.event-update";
            params.put(Field.OLDEVENTTYPE, event.getString(Field.OLDEVENTTYPE));
        }
        else {
            notificationTitle = "presences.push.event.created";
            notificationName = "presences.event-creation";
        }

        params.put(Field.PUSHNOTIF, new JsonObject()
                .put(Field.TITLE, notificationTitle)
                .put(Field.BODY, ""));

        userService.getStudents(Collections.singletonList(event.getString(Field.STUDENT_ID)))
                .compose(students -> {
                    params.put(Field.STUDENTNAME, students.getJsonObject(0).getString(Field.NAME));
                    return EventTypeHelper.getEventType(event.getInteger(Field.TYPE_ID));
                })
                .compose(eventType -> {
                    params.put(Field.EVENTTYPE, I18n.getInstance()
                            .translate(eventType.getLabel() + Field.DOTNOTIFICATION, Renders.getHost(request), I18n.acceptLanguage(request)));
                    params.put(Field.EVENTTYPEDECLARATION, I18n.getInstance()
                            .translate(eventType.getLabel() + Field.DOTNOTIFICATION + Field.DOTDECLARATION, Renders.getHost(request), I18n.acceptLanguage(request)));
                    return Viescolaire.getInstance().getResponsables(event.getString(Field.STUDENT_ID));
                })
                .onSuccess(responsablesIds -> {
                    List<String> ids = responsablesIds.stream()
                            .map(responsable -> ((JsonObject) responsable).getString(Field.IDRESPONSABLE))
                            .filter(Objects::nonNull)
                            .distinct()
                            .collect(Collectors.toList());
                    timelineHelper.notifyTimeline(null, notificationName, user,
                            ids, "", params);
                    promise.complete();
                })
                .onFailure(err -> {
                    String message = "An error has occurred during event notification";
                    String logMessage = String.format("[Presences@%s::sendEventNotificationCreate] %s: %s",
                            this.getClass().getSimpleName(), message, err.getMessage());
                    promise.fail(logMessage);
                });

        return promise.future();
    }

    public Future<JsonObject> getEvent (Integer eventId) {
        Promise<JsonObject> promise = Promise.promise();

        String query = "SELECT * FROM " + Presences.dbSchema + ".event" +
                " WHERE id = ?";
        JsonArray params = new JsonArray().add(eventId);

        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(FutureHelper.handlerEitherPromise(promise)));

        return promise.future();
    }
}