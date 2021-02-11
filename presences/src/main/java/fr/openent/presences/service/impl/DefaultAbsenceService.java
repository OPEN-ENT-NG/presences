package fr.openent.presences.service.impl;

import fr.openent.presences.Presences;
import fr.openent.presences.common.helper.DateHelper;
import fr.openent.presences.common.helper.FutureHelper;
import fr.openent.presences.common.service.GroupService;
import fr.openent.presences.common.service.UserService;
import fr.openent.presences.common.service.impl.DefaultGroupService;
import fr.openent.presences.common.service.impl.DefaultUserService;
import fr.openent.presences.db.DBService;
import fr.openent.presences.enums.EventType;
import fr.openent.presences.model.Event.Event;
import fr.openent.presences.service.AbsenceService;
import fr.wseduc.webutils.Either;
import io.vertx.core.AsyncResult;
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

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DefaultAbsenceService extends DBService implements AbsenceService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultAbsenceService.class);
    private static String defaultStartTime = "00:00:00";
    private static String defaultEndTime = "23:59:59";

    private GroupService groupService;
    private UserService userService;

    public DefaultAbsenceService(EventBus eb) {
        this.groupService = new DefaultGroupService(eb);
        this.userService = new DefaultUserService();
    }

    @Override
    public void get(String structureId, String startDate, String endDate,
                    List<String> users, Handler<Either<String, JsonArray>> handler) {
        JsonArray params = new JsonArray();
        String query = "SELECT * FROM " + Presences.dbSchema + ".absence";

        query += " WHERE structure_id = ? AND start_date > ? AND end_date < ? OR ? > start_date ";
        params.add(structureId)
                .add(startDate + " " + defaultStartTime)
                .add(endDate + " " + defaultEndTime)
                .add(endDate + " " + defaultEndTime);
        if (!users.isEmpty()) {
            query += " AND student_id IN " + Sql.listPrepared(users.toArray());
            params.addAll(new JsonArray(users));
        }
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void getAbsenceInEvents(String structureId, String startDate, String endDate,
                                   List<String> users, Handler<Either<String, JsonArray>> handler) {
        JsonArray params = new JsonArray();
        String query = "SELECT * FROM " + Presences.dbSchema + ".absence";

        query += " WHERE absence.structure_id = ? " +
                " AND (start_date > ? AND end_date < ? OR ? > start_date)";
        params.add(structureId)
                .add(startDate + " " + defaultStartTime)
                .add(endDate + " " + defaultEndTime)
                .add(endDate + " " + defaultEndTime);
        if (!users.isEmpty()) {
            query += " AND student_id IN " + Sql.listPrepared(users.toArray());
            params.addAll(new JsonArray(users));
        }
        query += " AND absence.student_id IN (" +
                " SELECT distinct event.student_id FROM " + Presences.dbSchema + ".event" +
                " WHERE absence.start_date::date = event.start_date::date" +
                " AND absence.end_date::date = event.end_date::date" +
                " ) ";
        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void getAbsenceId(Integer absenceId, Handler<Either<String, JsonObject>> handler) {
        String query = "SELECT * FROM " + Presences.dbSchema + ".absence WHERE id = " + absenceId;
        Sql.getInstance().raw(query, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void getAbsencesBetween(String startDate, String endDate, List<String> users, Handler<Either<String, JsonArray>> handler) {
        JsonArray params = new JsonArray();
        String query = "SELECT * FROM " + Presences.dbSchema + ".absence" +
                " WHERE student_id IN " + Sql.listPrepared(users.toArray()) +
                " AND (? >= start_date OR ? < end_date)" +
                " AND (end_date <= ? OR ? > start_date)";

        params.addAll(new JsonArray(users));
        params.add(startDate + " " + defaultStartTime);
        params.add(startDate + " " + defaultStartTime);
        params.add(endDate + " " + defaultEndTime);
        params.add(endDate + " " + defaultEndTime);

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void getAbsencesBetweenDates(String startDate, String endDate, List<String> users, Handler<Either<String, JsonArray>> handler) {
        JsonArray params = new JsonArray();
        String query = "SELECT * FROM " + Presences.dbSchema + ".absence" +
                " WHERE student_id IN " + Sql.listPrepared(users.toArray()) +
                " AND ? < end_date" +
                " AND start_date < ? ";

        params.addAll(new JsonArray(users));
        params.add(startDate);
        params.add(endDate);

        sql.prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void getAbsencesFromCollective(String structureId, Long collectiveId, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT * FROM " + Presences.dbSchema + ".absence " +
                " WHERE structure_id = ? AND collective_id = ? ";

        JsonArray params = new JsonArray().add(structureId);
        params.add(collectiveId);

        sql.prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void create(JsonObject absenceBody, List<String> studentIds, UserInfos user, Long collectiveId, Handler<AsyncResult<JsonArray>> handler) {
        if (studentIds.isEmpty()) {
            handler.handle(Future.succeededFuture(new JsonArray()));
            return;
        }

        JsonArray statements = new JsonArray();
        for (String studentId : studentIds) {
            statements.add(insertStatement(absenceBody, studentId, user, collectiveId));
        }
        sql.transaction(statements, SqlResult.validResultHandler(result -> {
            if (result.isLeft()) {
                String message = "[Presences@DefaultAbsenceService::create] failed to create absences from collective";
                LOGGER.error(message, result.left().getValue());
                handler.handle(Future.failedFuture(message));
                return;
            }

            if (Boolean.TRUE.equals(absenceBody.getBoolean("counsellor_regularisation", false)))
                regularizeAfterCollectiveCreate(collectiveId, handler);
            else handler.handle(Future.succeededFuture(result.right().getValue()));
        }));
    }

    private JsonObject insertStatement(JsonObject absenceBody, String studentId, UserInfos user, Long collectiveId) {
        String query = "INSERT INTO " + Presences.dbSchema + ".absence(structure_id, student_id, start_date, end_date, owner, reason_id, " +
                " collective_id) " +
                " SELECT ?, ?, ?, ?, ?, ?, ? ";

        JsonArray params = new JsonArray()
                .add(absenceBody.getString("structure_id"))
                .add(studentId)
                .add(absenceBody.getString("start_date"))
                .add(absenceBody.getString("end_date"))
                .add(user.getUserId());

        if (absenceBody.getLong("reason_id") != null) params.add(absenceBody.getLong("reason_id"));
        else params.addNull();

        if (collectiveId != null) {
            params.add(collectiveId);
            query += " WHERE NOT EXISTS " +
                    "        ( "  +
                    "            SELECT id "  +
                    "            FROM presences.absence "  +
                    "                WHERE collective_id = ? "  +
                    "                AND student_id = ? "  +
                    "        ) ";
            params.add(collectiveId)
                    .add(studentId);
        } else params.addNull();

        return new JsonObject()
                .put("action", "prepared")
                .put("statement", query)
                .put("values", params);
    }

    private void regularizeAfterCollectiveCreate(Long collectiveId, Handler<AsyncResult<JsonArray>> handler) {
        String query = "UPDATE " + Presences.dbSchema + ".absence " +
                "SET counsellor_regularisation = ? WHERE collective_id = ?";
        JsonArray params = new JsonArray()
                .add(true)
                .add(collectiveId);
        sql.prepared(query, params, SqlResult.validResultHandler(FutureHelper.handlerJsonArray(handler)));
    }

    @Override
    public void create(JsonObject absenceBody, UserInfos user, boolean editEvents, Handler<Either<String, JsonObject>> handler) {
        String query = "INSERT INTO " + Presences.dbSchema + ".absence(structure_id, start_date, end_date, student_id, owner, reason_id) " +
                "VALUES (?, ?, ?, ?, ?, ?) RETURNING id;";
        JsonArray params = new JsonArray()
                .add(absenceBody.getString("structure_id"))
                .add(absenceBody.getString("start_date"))
                .add(absenceBody.getString("end_date"))
                .add(absenceBody.getString("student_id"))
                .add(user.getUserId());
        if (absenceBody.getInteger("reason_id") != null) {
            params.add(absenceBody.getInteger("reason_id"));
        } else {
            params.addNull();
        }

        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(absenceResult -> {
            afterPersistAbsence(
                    absenceResult.isRight() ? absenceResult.right().getValue().getLong("id") : null,
                    absenceBody,
                    null,
                    editEvents,
                    user.getUserId(),
                    handler,
                    absenceResult
            );
        }));
    }

    @Override
    public void update(Long absenceId, JsonObject absenceBody, UserInfos user, boolean editEvents, Handler<Either<String, JsonObject>> handler) {
        String beforeUpdateAbsenceQuery = "SELECT * FROM " + Presences.dbSchema + ".absence WHERE id = ?";
        Sql.getInstance().prepared(beforeUpdateAbsenceQuery, new JsonArray().add(absenceId), SqlResult.validUniqueResultHandler(oldAbsenceResult -> {
            if (oldAbsenceResult.isLeft()) {
                String message = "[Presences@DefaultAbsenceService::update] failed to retrieve absence";
                LOGGER.error(message, oldAbsenceResult.left().getValue());
                handler.handle(new Either.Left<>(message));
                return;
            }
            JsonObject oldAbsence = oldAbsenceResult.right().getValue();

            String query = "UPDATE " + Presences.dbSchema + ".absence " +
                    "SET structure_id = ?, start_date = ?, end_date = ?, student_id = ?, reason_id = ? WHERE id = ?";

            JsonArray values = new JsonArray()
                    .add(absenceBody.getString("structure_id"))
                    .add(absenceBody.getString("start_date"))
                    .add(absenceBody.getString("end_date"))
                    .add(absenceBody.getString("student_id"));
            if (absenceBody.getInteger("reason_id") != null) {
                values.add(absenceBody.getInteger("reason_id"));
            } else {
                values.addNull();
            }
            values.add(absenceId);

            Sql.getInstance().prepared(query, values, SqlResult.validUniqueResultHandler(absenceResult -> {
                afterPersistAbsence(absenceId, absenceBody, oldAbsence, editEvents, user.getUserId(), handler, absenceResult);
            }));
        }));
    }

    @Override
    @SuppressWarnings("unchecked")
    public void updateFromCollective(JsonObject absenceBody, UserInfos user, Long collectiveId, boolean editEvents, Handler<AsyncResult<JsonObject>> handler) {
        String beforeUpdateAbsenceQuery = "SELECT * FROM " + Presences.dbSchema + ".absence WHERE collective_id = ?";
        sql.prepared(beforeUpdateAbsenceQuery, new JsonArray().add(collectiveId), SqlResult.validResultHandler(oldAbsenceResult -> {
            if (oldAbsenceResult.isLeft()) {
                String message = "[Presences@DefaultAbsenceService::update] failed to retrieve absences";
                LOGGER.error(message, oldAbsenceResult.left().getValue());
                handler.handle(Future.failedFuture(message));
                return;
            }
            List<JsonObject> oldAbsences = oldAbsenceResult.right().getValue().getList();

            String query = "UPDATE " + Presences.dbSchema + ".absence " +
                    "SET start_date = ?, end_date = ?, reason_id = ? WHERE collective_id = ? AND structure_id = ?";

            JsonArray values = new JsonArray()
                    .add(absenceBody.getString("start_date"))
                    .add(absenceBody.getString("end_date"));
            if (absenceBody.getInteger("reason_id") != null) {
                values.add(absenceBody.getInteger("reason_id"));
            } else {
                values.addNull();
            }

            values.add(collectiveId)
                    .add(absenceBody.getString("structure_id"));

            sql.prepared(query, values, SqlResult.validUniqueResultHandler(absenceResult -> {
                List<Future<JsonObject>> futures = new ArrayList<>();

                for (JsonObject oldAbsence : oldAbsences) {
                    Future<JsonObject> future = Future.future();
                    futures.add(future);

                    absenceBody.put("student_id", oldAbsence.getString("student_id"));

                    afterPersistAbsence(oldAbsence.getLong("id"), absenceBody, oldAbsence, editEvents, user.getUserId(),
                            FutureHelper.handlerJsonObject(future), absenceResult);
                }

                FutureHelper.all(futures).setHandler(result -> {
                    if (result.failed()) {
                        handler.handle(Future.failedFuture(result.cause().getMessage()));
                        return;
                    }
                    handler.handle(Future.succeededFuture(new JsonObject().put("success", "ok")));
                });
            }));
        }));
    }

    private void afterPersistAbsence(Long absenceId, JsonObject absenceBody, JsonObject oldAbsence, boolean editEvents, String userInfoId, Handler<Either<String, JsonObject>> handler, Either<String, JsonObject> absenceResult) {
        if (absenceResult.isLeft()) {
            String message = "[Presences@DefaultAbsenceService] failed to update absence";
            LOGGER.error(message, absenceResult.left().getValue());
            handler.handle(new Either.Left<>(message));
        } else if (editEvents) {
            interactingEvents(absenceBody, oldAbsence, userInfoId, event -> {
                if (event.isLeft()) {
                    String message = "[Presences@DefaultAbsenceService] failed to interact with events while updating absence";
                    LOGGER.error(message, event.left().getValue());
                    handler.handle(new Either.Left<>(message));
                } else {
                    handler.handle(new Either.Right<>(event.right().getValue()
                            .put("id", absenceId)));
                }
            });
        } else {
            handler.handle(new Either.Right<>(absenceResult.right().getValue()));
        }
    }

    private void afterPersistAbsences(List<Long> absenceIds, String userInfoId, boolean editEvents, Handler<Either<String, JsonObject>> handler,
                                      Either<String, JsonObject> absencesResult) {
        if (absencesResult.isLeft()) {
            String message = "[Presences@DefaultAbsenceService] failed to update absence";
            LOGGER.error(message, absencesResult.left().getValue());
            handler.handle(new Either.Left<>(message));
        } else {
            String query = " SELECT * " +
                    " FROM " + Presences.dbSchema + ".absence " +
                    " WHERE id IN " + Sql.listPrepared(absenceIds);

            JsonArray params = new JsonArray();
            params.addAll(new JsonArray(absenceIds));

            afterPersistAbsence(query, params, userInfoId, editEvents, handler);
        }
    }

    @Override
    public void afterPersistCollective(Long collectiveId, String structureId, String userInfoId, boolean editEvents, Handler<AsyncResult<JsonObject>> handler) {
        String query = " SELECT * " +
                " FROM " + Presences.dbSchema + ".absence " +
                " WHERE collective_id = ? AND structure_id = ?";

        JsonArray params = new JsonArray().add(collectiveId).add(structureId);
        afterPersistAbsence(query, params, userInfoId, editEvents, FutureHelper.handlerJsonObject(handler));
    }

    private void afterPersistAbsence(String getAbsencesQuery, JsonArray params, String userInfoId, boolean editEvents, Handler<Either<String, JsonObject>> handler) {
        Sql.getInstance().prepared(getAbsencesQuery, params, SqlResult.validResultHandler(result -> {
            if (result.isLeft()) {
                String message = "[Presences@DefaultAbsenceService::afterPersistAbsences] Failed to retrieve absences";
                LOGGER.error(message);
                handler.handle(new Either.Left<>(message));
            } else if (editEvents) {
                JsonArray absences = result.right().getValue();

                List<Future> futures = new ArrayList<>();

                absences.forEach(oAbsence -> {
                    Future<JsonObject> future = Future.future();
                    futures.add(future);
                    JsonObject absence = (JsonObject) oAbsence;
                    interactingEvents(absence, userInfoId, future);
                });

                CompositeFuture.all(futures).setHandler(event -> {
                    if (event.failed()) {
                        LOGGER.info("[Presences@DefaultAbsenceService::afterPersistAbsences::CompositeFuture]: " +
                                "An error has occured)");
                        handler.handle(new Either.Left<>(event.cause().getMessage()));
                    } else {
                        handler.handle(new Either.Right<>(new JsonObject().put("status", "ok")));
                    }
                });
            } else {
                handler.handle(new Either.Right<>(new JsonObject().put("status", "ok")));
            }
        }));
    }

    private void interactingEvents(JsonObject absenceBody, JsonObject oldAbsence, String userInfoId, Handler<Either<String, JsonObject>> handler) {
        List<String> users = new ArrayList<>();
        users.add(absenceBody.getString("student_id"));
        groupService.getUserGroups(users, absenceBody.getString("structure_id"), groupEvent -> {
            if (groupEvent.isLeft()) {
                String message = "[Presences@DefaultAbsenceService::interactingEvents] failed to retrieve user info";
                LOGGER.error(message, groupEvent.left().getValue());
                handler.handle(new Either.Left<>(message));
            } else {
                List<String> groupIds = new ArrayList<>();
                for (int i = 0; i < groupEvent.right().getValue().size(); i++) {
                    groupIds.add(groupEvent.right().getValue().getJsonObject(i).getString("id"));
                }
                matchEventsWithAbsents(absenceBody, oldAbsence, groupIds, userInfoId, handler);
            }
        });
    }

    private void interactingEvents(JsonObject absenceBody, String userInfoId, Future<JsonObject> future) {
        interactingEvents(absenceBody, null, userInfoId, FutureHelper.handlerJsonObject(future));
    }

    private void matchEventsWithAbsents(JsonObject absenceBody, JsonObject oldAbsence, List<String> groupIds, String userInfoId, Handler<Either<String, JsonObject>> handler) {
        Future<JsonArray> createEventsFuture = Future.future();
        Future<JsonArray> updateEventsFuture = Future.future();
        Future<JsonObject> deleteEventsFuture = Future.future();

        Boolean isRegularized = absenceBody.getBoolean("counsellor_regularisation");
        createEventsWithAbsents(absenceBody, isRegularized, groupIds, userInfoId, FutureHelper.handlerJsonArray(createEventsFuture));
        updateEventsWithAbsents(absenceBody, isRegularized, groupIds, FutureHelper.handlerJsonArray(updateEventsFuture));
        deleteEventsFromAbsence(absenceBody, oldAbsence, FutureHelper.handlerJsonObject(deleteEventsFuture));


        CompositeFuture.all(createEventsFuture, updateEventsFuture, deleteEventsFuture).setHandler(event -> {
            if (event.failed()) {
                String message = "[Presences@DefaultAbsenceService] Failed to create or update events from absent";
                LOGGER.error(message);
                handler.handle(new Either.Left<>(message));
            } else {
                List<Long> updatedRegisterId = new ArrayList<>();
                List<Long> createdRegisterId = new ArrayList<>();
                for (int i = 0; i < updateEventsFuture.result().size(); i++) {
                    updatedRegisterId.add(updateEventsFuture.result().getJsonObject(i).getLong("register_id"));
                }
                for (int j = 0; j < createEventsFuture.result().size(); j++) {
                    createdRegisterId.add(createEventsFuture.result().getJsonObject(j).getLong("register_id"));
                }
                handler.handle(new Either.Right<>(new JsonObject()
                        .put("updatedRegisterId", updatedRegisterId)
                        .put("createdRegisterId", createdRegisterId)));
            }
        });
    }

    @Override
    public void changeReasonAbsences(JsonObject absenceBody, UserInfos user, Handler<Either<String, JsonObject>> handler) {
        JsonArray params = new JsonArray();
        String query = "UPDATE " + Presences.dbSchema + ".absence SET reason_id = ? ";
        if (absenceBody.getInteger("reasonId") != null) {
            params.add(absenceBody.getInteger("reasonId"));
        } else {
            params.addNull();
        }
        query += " WHERE id IN " + Sql.listPrepared(absenceBody.getJsonArray("ids").getList());
        params.addAll(absenceBody.getJsonArray("ids"));
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(result -> {
            List<Long> ids = new ArrayList<>();
            for (int i = 0; i < absenceBody.getJsonArray("ids").size(); i++) {
                ids.add(Long.parseLong(absenceBody.getJsonArray("ids").getInteger(i).toString()));
            }
            afterPersistAbsences(ids, user.getUserId(), true, handler, result);
        }));
    }

    @Override
    public void changeRegularizedAbsences(JsonObject absenceBody, UserInfos user, boolean editEvents, Handler<Either<String, JsonObject>> handler) {
        Boolean isRegularized = absenceBody.getBoolean("regularized");
        JsonArray ids = absenceBody.getJsonArray("ids");
        JsonArray params = new JsonArray();
        if (isRegularized == null || ids == null || ids.isEmpty()) {
            String message = "[Presences@DefaultAbsenceService::changeRegularizedAbsences] some fields are null or empty.";
            LOGGER.error(message);
            handler.handle(new Either.Left<>(message));
            return;
        }
        String query = "UPDATE " + Presences.dbSchema + ".absence SET counsellor_regularisation = ? ";
        params.add(isRegularized);

        query += " WHERE id IN " + Sql.listPrepared(absenceBody.getJsonArray("ids").getList());
        params.addAll(ids);
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(result -> {
            List<Long> resultIds = absenceBody.getJsonArray("ids").stream().map(id -> Long.parseLong(id.toString())).collect(Collectors.toList());
            afterPersistAbsences(resultIds, user.getUserId(), editEvents, handler, result);
        }));
    }

    @Override
    public void changeRegularizedAbsences(JsonObject absenceBody, UserInfos user, Handler<Either<String, JsonObject>> handler) {
        changeRegularizedAbsences(absenceBody, user, true, handler);
    }

    private void createEventsWithAbsents(JsonObject absenceBody, Boolean isRegularized, List<String> groupIds, String ownerId, Handler<Either<String, JsonArray>> handler) {
        String query = "WITH register as " +
                "(" +
                "SELECT register.id, register.start_date, register.end_date FROM " + Presences.dbSchema + ".register " +
                "INNER JOIN presences.rel_group_register as rgr ON (rgr.register_id = register.id) " +
                "WHERE rgr.group_id IN " + Sql.listPrepared(groupIds.toArray()) + " " +
                "AND register.start_date >= ? " +
                "AND register.end_date <= ? " +
                "AND register.id NOT IN (" +
                "  SELECT event.register_id FROM " + Presences.dbSchema + ".event " +
                "  WHERE event.type_id = 1 and event.register_id = register.id and event.student_id = ?" +
                ")" +
                ") " +
                "INSERT INTO " + Presences.dbSchema + ".event (start_date, end_date, comment, counsellor_input, student_id, register_id, type_id, reason_id, owner)" +
                "(SELECT register.start_date, register.end_date, '', true, ?," +
                "register.id, 1, ?, ? FROM register) " +
                "RETURNING *";

        JsonArray values = new JsonArray()
                .addAll(new JsonArray(groupIds))
                .add(absenceBody.getString("start_date"))
                .add(absenceBody.getString("end_date"))
                .add(absenceBody.getString("student_id"));
        values.add(absenceBody.getString("student_id"));

        if (absenceBody.getInteger("reason_id") != null) {
            values.add(absenceBody.getInteger("reason_id"));
        } else {
            values.addNull();
        }
        values.add((absenceBody.getString("owner") != null) ? absenceBody.getString("owner") : ownerId);
        Sql.getInstance().prepared(query, values, SqlResult.validResultHandler(result -> {
            if (result.isLeft()) {
                String message = "[Presences@DefaultAbsenceService::createEventsWithAbsents] Failed to create events from absent.";
                LOGGER.error(message + " " + result.left().getValue());
                handler.handle(new Either.Left<>(message));
                return;
            }

            JsonArray events = result.right().getValue();
            regularizeEventsFromAbsence(events, isRegularized, handler);
        }));
    }

    private void updateEventsWithAbsents(JsonObject absenceBody, Boolean isRegularized, List<String> groupIds, Handler<Either<String, JsonArray>> handler) {
        String query = "WITH register as " +
                "(" +
                "SELECT register.id, register.start_date, register.end_date FROM " + Presences.dbSchema + ".register " +
                "INNER JOIN presences.rel_group_register as rgr ON (rgr.register_id = register.id) " +
                "WHERE rgr.group_id IN " + Sql.listPrepared(groupIds.toArray()) + " " +
                "AND register.start_date >= ? " +
                "AND register.end_date <= ? " +
                "AND register.id IN (" +
                "  SELECT event.register_id FROM " + Presences.dbSchema + ".event" +
                "  WHERE event.type_id = 1 and event.register_id = register.id and event.student_id = ?" +
                ") " +
                ") " +
                "UPDATE " + Presences.dbSchema + ".event SET reason_id = ? WHERE register_id IN (SELECT id FROM register) AND student_id = ? " +
                "RETURNING *";
        JsonArray values = new JsonArray()
                .addAll(new JsonArray(groupIds))
                .add(absenceBody.getString("start_date"))
                .add(absenceBody.getString("end_date"))
                .add(absenceBody.getString("student_id"));

        if (absenceBody.getInteger("reason_id") != null) {
            values.add(absenceBody.getInteger("reason_id"));
        } else {
            values.addNull();
        }

        values.add(absenceBody.getString("student_id"));
        Sql.getInstance().prepared(query, values, SqlResult.validResultHandler(result -> {
            if (result.isLeft()) {
                String message = "[Presences@DefaultAbsenceService::updateEventsWithAbsents] Failed to update events from absent.";
                LOGGER.error(message + " " + result.left().getValue());
                handler.handle(new Either.Left<>(message));
                return;
            }

            JsonArray events = result.right().getValue();
            regularizeEventsFromAbsence(events, isRegularized, handler);
        }));
    }

    private void regularizeEventsFromAbsence(JsonArray events, Boolean isRegularized, Handler<Either<String, JsonArray>> handler) {
        if (isRegularized != null) {
            changeRegularizedEvents(events, isRegularized, result -> {
                if (result.isLeft()) {
                    String message = "[Presences@DefaultAbsenceService::regularizeEventsFromAbsence] Failed to regularize absence linked events.";
                    LOGGER.error(message + " " + result.left().getValue());
                    handler.handle(new Either.Left<>(message));
                    return;
                }
                handler.handle(new Either.Right<>(events));
            });
        } else {
            handler.handle(new Either.Right<>(events));
        }
    }

    private void deleteEventsFromAbsence(JsonObject newAbsence, JsonObject oldAbsence, Handler<Either<String, JsonObject>> handler) {
        if (oldAbsence == null || oldAbsence.isEmpty()) {
            handler.handle(new Either.Right<>(new JsonObject().put("status", "ok")));
            return;
        }

        Future<JsonObject> beforeEventsRemovalFuture = Future.future();
        Future<JsonObject> afterEventsRemovalFuture = Future.future();

        // Here if the new period start later than the oldest one, we remove events corresponding to the period it no longer covers
        // so we check if oldAbsence.start_date < (plus tôt que..) newAbsence.start_date
        removeEventsIfDateBefore(
                oldAbsence.getString("start_date"),
                newAbsence.getString("start_date"),
                oldAbsence.getString("student_id"),
                FutureHelper.handlerJsonObject(beforeEventsRemovalFuture)
        );

        // Here if the new period start later than the oldest one, we remove events corresponding to the period it no longer covers
        // so we check if newAbsence.end_date < (plus tôt que..) oldAbsence.end_date
        removeEventsIfDateBefore(
                newAbsence.getString("end_date"),
                oldAbsence.getString("end_date"),
                oldAbsence.getString("student_id"),
                FutureHelper.handlerJsonObject(afterEventsRemovalFuture)
        );

        CompositeFuture.all(beforeEventsRemovalFuture, afterEventsRemovalFuture).setHandler(event -> {
            if (event.failed()) {
                String message = "[Presences@DefaultAbsenceService::deleteEventsFromAbsence] Failed to delete events";
                LOGGER.error(message);
                handler.handle(new Either.Left<>(message));
            } else {
                handler.handle(new Either.Right<>(new JsonObject().put("status", "ok")));
            }
        });
    }

    private void removeEventsIfDateBefore(String date1, String date2, String student_id, Handler<Either<String, JsonObject>> handler) {
        try {
            if (DateHelper.isBefore(date1, date2)) {
                JsonObject deleteData = new JsonObject()
                        .put("start_date", date1)
                        .put("end_date", date2)
                        .put("student_id", student_id);
                deleteEventsOnDelete(deleteData, handler);
            } else {
                handler.handle(new Either.Right<>(new JsonObject().put("status", "ok")));
            }
        } catch (ParseException e) {
            String message = "[Presences@DefaultAbsenceService::removeEventsIfDateBefore] An error occurred while parsing dates";
            LOGGER.error(message, e.getCause().getMessage());
            handler.handle(new Either.Left<>(message));
        }
    }

    // same function that in DefaultEventService. we so it there, because we cannot cross service instances
    private void changeRegularizedEvents(JsonArray events, Boolean regularized, Handler<Either<String, JsonObject>> handler) {
        List<Integer> ids = new ArrayList<>();
        for (int i = 0; i < events.size(); i++) {
            Event event = new Event(events.getJsonObject(i), new ArrayList<>());
            ids.add(event.getId());
        }

        if (ids.size() > 0) {
            JsonArray params = new JsonArray();
            String query = "UPDATE " + Presences.dbSchema + ".event SET counsellor_regularisation = ? ";
            params.add(regularized);

            query += " WHERE id IN " + Sql.listPrepared(ids) + " AND type_id = 1";
            params.addAll(new JsonArray(ids));
            Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
            return;
        }
        handler.handle(new Either.Right<>(new JsonObject().put("status", "ok")));
    }

    @Override
    public void delete(Integer absenceId, Handler<Either<String, JsonObject>> handler) {
        String query = "SELECT * FROM " + Presences.dbSchema + ".absence WHERE id = " + absenceId;
        Sql.getInstance().raw(query, SqlResult.validUniqueResultHandler(absenceResult -> {
            if (absenceResult.isLeft()) {
                String message = "[Presences@DefaultAbsenceService] failed to fetch absence id before deletion";
                LOGGER.error(message, absenceResult.left().getValue());
                handler.handle(new Either.Left<>(message));
            } else {
                Future<JsonObject> deleteEventsFuture = Future.future();
                Future<JsonObject> resetEventsFuture = Future.future();
                Future<JsonObject> deleteAbsenceFuture = Future.future();

                deleteEventsOnDelete(absenceResult.right().getValue(), FutureHelper.handlerJsonObject(deleteEventsFuture));
                resetEventsOnDelete(absenceResult.right().getValue(), FutureHelper.handlerJsonObject(resetEventsFuture));
                deleteAbsence(absenceId, FutureHelper.handlerJsonObject(deleteAbsenceFuture));

                CompositeFuture.all(deleteEventsFuture, resetEventsFuture, deleteAbsenceFuture).setHandler(event -> {
                    if (event.failed()) {
                        String message = "[Presences@DefaultAbsenceService] Failed to delete events or absence";
                        LOGGER.error(message);
                        handler.handle(new Either.Left<>(message));
                    } else {
                        handler.handle(new Either.Right<>(absenceResult.right().getValue()));
                    }
                });
            }
        }));
    }

    private void deleteEventsOnDelete(JsonObject absenceResult, Handler<Either<String, JsonObject>> handler) {
        String query = "DELETE FROM " + Presences.dbSchema + ".event" +
                " WHERE student_id = ? AND start_date >= ? AND end_date <= ? AND counsellor_input = true AND type_id = "
                + EventType.ABSENCE.getType();

        JsonArray params = new JsonArray()
                .add(absenceResult.getString("student_id"))
                .add(absenceResult.getString("start_date"))
                .add(absenceResult.getString("end_date"));
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    private void resetEventsOnDelete(JsonObject absenceResult, Handler<Either<String, JsonObject>> handler) {
        String query = "UPDATE " + Presences.dbSchema + ".event SET reason_id = null " +
                "WHERE student_id = ? AND start_date >= ? AND end_date <= ? AND counsellor_input = false AND type_id = "
                + EventType.ABSENCE.getType();

        JsonArray params = new JsonArray()
                .add(absenceResult.getString("student_id"))
                .add(absenceResult.getString("start_date"))
                .add(absenceResult.getString("end_date"));
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    private void deleteAbsence(Integer absenceId, Handler<Either<String, JsonObject>> handler) {
        String query = "DELETE FROM " + Presences.dbSchema + ".absence WHERE id = " + absenceId;
        Sql.getInstance().raw(query, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void absenceRemovalTask(Handler<Either<String, JsonObject>> handler) {
        String query = "DELETE FROM  " + Presences.dbSchema + ".absence WHERE start_date <= NOW() - interval '72 hour'";
        Sql.getInstance().raw(query, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void retrieve(String structure, List<String> students, String start, String end, Boolean justified, Boolean regularized, List<Integer> reasons, Handler<Either<String, JsonArray>> handler) {
        JsonArray params = new JsonArray().add(structure);
        String query = "SELECT id, start_date, end_date, student_id, reason_id, counsellor_regularisation " +
                "FROM " + Presences.dbSchema + ".absence " +
                "WHERE structure_id = ? ";
        if (!students.isEmpty()) {
            query += "AND student_id IN " + Sql.listPrepared(students);
            params.addAll(new JsonArray(students));
        }

        if (regularized != null || justified != null) {
            query += " AND counsellor_regularisation = ? ";
            params.add(regularized != null ? regularized : justified);
        }

        if (start != null) {
            query += " AND end_date > ? ";
            params.add(start + " " + defaultStartTime);
        }

        if (end != null) {
            query += " AND start_date < ? ";
            params.add(end + " " + defaultEndTime);
        }

        query += " ORDER BY start_date DESC";

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(res -> {
            if (res.isLeft()) {
                LOGGER.error("[Presences@DefaultAbsenceService] Failed to retrieve absences", res.left().getValue());
                handler.handle(res.left());
                return;
            }

            JsonArray absences = res.right().getValue();
            if (absences.isEmpty()) {
                handler.handle(res.right());
                return;
            }

            List<String> studentIds = ((List<JsonObject>) absences.getList()).stream().map(absence -> absence.getString("student_id")).collect(Collectors.toList());
            userService.getStudents(studentIds, users -> {
                if (users.isLeft()) {
                    LOGGER.error("[Presences@DefaultAbsenceService] Failed to retrieve absences users", users.left().getValue());
                    handler.handle(users.left());
                    return;
                }

                JsonArray result = users.right().getValue();
                Map<String, JsonObject> studentMap = ((List<JsonObject>) result.getList())
                        .stream()
                        .collect(Collectors.toMap(user -> user.getString("id"), Function.identity(), (student1, student2) -> student1));

                ((List<JsonObject>) absences.getList())
                        .forEach(absence -> absence.put("student", studentMap.get(absence.getString("student_id"))));
                handler.handle(new Either.Right<>(absences));
            });
        }));
    }

    @Override
    public void getAbsentStudentIds(String structureId, String currentDate, Handler<Either<String, JsonArray>> handler) {
        JsonArray params = new JsonArray()
                .add(currentDate)
                .add(currentDate)
                .add(structureId)
                .add(structureId)
                .add(currentDate)
                .add(currentDate);

        String query = "SELECT ab.student_id FROM " + Presences.dbSchema + ".absence ab" +
                " WHERE (ab.start_date < ?" +
                " AND ab.end_date > ?" +
                " AND ab.structure_id = ?)" +
                " UNION" +
                " SELECT ev.student_id FROM " + Presences.dbSchema + ".event ev" +
                " INNER JOIN " + Presences.dbSchema + ".register AS r" +
                " ON (r.id = ev.register_id AND r.structure_id = ?)" +
                " WHERE (ev.start_date < ? AND ev.end_date > ?)";

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }
}
