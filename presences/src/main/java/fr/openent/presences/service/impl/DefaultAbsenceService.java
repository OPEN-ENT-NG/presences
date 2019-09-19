package fr.openent.presences.service.impl;

import fr.openent.presences.Presences;
import fr.openent.presences.common.helper.FutureHelper;
import fr.openent.presences.service.AbsenceService;
import fr.openent.presences.service.GroupService;
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
import java.util.List;

public class DefaultAbsenceService implements AbsenceService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultAbsenceService.class);

    private GroupService groupService;

    public DefaultAbsenceService(EventBus eb) {
        this.groupService = new DefaultGroupService(eb);
    }

    @Override
    public void get(String startDate, String endDate, List<String> users, Handler<Either<String, JsonArray>> handler) {
        JsonArray params = new JsonArray();
        String query = "SELECT * FROM " + Presences.dbSchema + ".absence" +
                " WHERE student_id IN " + Sql.listPrepared(users.toArray()) +
                " AND start_date > ?" +
                " AND end_date < ?";

        params.addAll(new JsonArray(users));
        params.add(startDate + " 00:00:00");
        params.add(endDate + " 23:59:59");

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
        params.add(startDate + " 00:00:00");
        params.add(startDate + " 00:00:00");
        params.add(endDate + " 23:59:59");
        params.add(endDate + " 23:59:59");

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void create(JsonObject absenceBody, UserInfos user, Handler<Either<String, JsonObject>> handler) {
        String query = "INSERT INTO " + Presences.dbSchema + ".absence(start_date, end_date, student_id, reason_id) " +
                "VALUES (?, ?, ?, ?) RETURNING id;";
        JsonArray params = new JsonArray()
                .add(absenceBody.getString("start_date"))
                .add(absenceBody.getString("end_date"))
                .add(absenceBody.getString("student_id"));
        if (absenceBody.getInteger("reason_id") != null) {
            params.add(absenceBody.getInteger("reason_id"));
        } else {
            params.addNull();
        }

        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(absenceResult -> {
            if (absenceResult.isLeft()) {
                String message = "[Presences@DefaultAbsenceService] failed to create absence";
                LOGGER.error(message, absenceResult.left().getValue());
                handler.handle(new Either.Left<>(message));
            } else {
                interactingEvents(absenceBody, event -> {
                    if (event.isLeft()) {
                        String message = "[Presences@DefaultAbsenceService] failed to interact with events while creating absence";
                        LOGGER.error(message, absenceResult.left().getValue());
                        handler.handle(new Either.Left<>(message));
                    } else {
                        handler.handle(new Either.Right<>(event.right().getValue()
                                .put("id", absenceResult.right().getValue().getLong("id"))));
                    }
                });
            }
        }));
    }

    @Override
    public void update(Integer absenceId, JsonObject absenceBody, UserInfos user, Handler<Either<String, JsonObject>> handler) {
        String query = "UPDATE " + Presences.dbSchema + ".absence " +
                "SET start_date = ?, end_date = ?, student_id = ?, reason_id = ? WHERE id = ?";

        JsonArray values = new JsonArray()
                .add(absenceBody.getString("start_date"))
                .add(absenceBody.getString("end_date"))
                .add(absenceBody.getString("student_id"));
        if (absenceBody.getInteger("reason_id") != null) {
            values.add(absenceBody.getInteger("reason_id"));
        } else {
            values.addNull();
        }
        values.add(absenceId);

        Sql.getInstance().prepared(query, values,  SqlResult.validUniqueResultHandler(absenceResult -> {
            if (absenceResult.isLeft()) {
                String message = "[Presences@DefaultAbsenceService] failed to update absence";
                LOGGER.error(message, absenceResult.left().getValue());
                handler.handle(new Either.Left<>(message));
            } else {
                interactingEvents(absenceBody, event -> {
                    if (event.isLeft()) {
                        String message = "[Presences@DefaultAbsenceService] failed to interact with events while updating absence";
                        LOGGER.error(message, absenceResult.left().getValue());
                        handler.handle(new Either.Left<>(message));
                    } else {
                        handler.handle(new Either.Right<>(event.right().getValue()
                                .put("id", absenceId)));
                    }
                });
            }
        }));
    }

    private void interactingEvents(JsonObject absenceBody, Handler<Either<String, JsonObject>> handler) {
        List<String> users = new ArrayList<>();
        users.add(absenceBody.getString("student_id"));
        groupService.getUserGroups(users, absenceBody.getString("structure_id"), groupEvent -> {
            if (groupEvent.isLeft()) {
                String message = "[Presences@DefaultAbsenceService] failed to retrieve user info";
                LOGGER.error(message, groupEvent.left().getValue());
                handler.handle(new Either.Left<>(message));
            } else {
                List<String> groupIds = new ArrayList<>();
                for (int i = 0; i < groupEvent.right().getValue().size(); i++) {
                    groupIds.add(groupEvent.right().getValue().getJsonObject(i).getString("id"));
                }
                matchEventsWithAbsents(absenceBody, groupIds, handler);
            }
        });
    }

    private void matchEventsWithAbsents(JsonObject absenceBody, List<String> groupIds, Handler<Either<String, JsonObject>> handler) {
        Future<JsonArray> createEventsFuture = Future.future();
        Future<JsonArray> updateEventsFuture = Future.future();

        createEventsWithAbsents(absenceBody, groupIds, FutureHelper.handlerJsonArray(createEventsFuture));
        updateEventsWithAbsents(absenceBody, groupIds, FutureHelper.handlerJsonArray(updateEventsFuture));

        CompositeFuture.all(createEventsFuture, updateEventsFuture).setHandler(event -> {
            if (event.failed()) {
                String message = "[Presences@DefaultAbsenceService] Failed to create or update events from absent";
                LOGGER.error(message);
                handler.handle(new Either.Left<>(message));
            } else {
                List<Long> updatedRegisterId = new ArrayList<>();
                List<Long> createdRegisterId = new ArrayList<>();
                for (int i = 0; i < updateEventsFuture.result().size(); i++) {
                    updatedRegisterId.add(updateEventsFuture.result().getJsonObject(i).getLong("updated_register_id"));
                }
                for (int j = 0; j < createEventsFuture.result().size(); j++) {
                    createdRegisterId.add(createEventsFuture.result().getJsonObject(j).getLong("created_register_id"));
                }
                handler.handle(new Either.Right<>(new JsonObject()
                        .put("updatedRegisterId", updatedRegisterId)
                        .put("createdRegisterId", createdRegisterId)));
            }
        });
    }

    private void createEventsWithAbsents(JsonObject absenceBody, List<String> groupIds, Handler<Either<String, JsonArray>> handler) {
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
                "INSERT INTO presences.event (start_date, end_date, comment, counsellor_input, student_id, register_id, type_id, reason_id, owner)" +
                "(SELECT register.start_date, register.end_date, '', true, ?," +
                "register.id, 1, ?, ? FROM register) " +
                "returning register_id AS created_register_id";

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
        values.add(absenceBody.getString("owner"));
        Sql.getInstance().prepared(query, values, SqlResult.validResultHandler(handler));
    }

    private void updateEventsWithAbsents(JsonObject absenceBody, List<String> groupIds, Handler<Either<String, JsonArray>> handler) {
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
                "UPDATE presences.event SET reason_id = ? WHERE register_id IN (SELECT id FROM register) " +
                "returning register_id AS updated_register_id";
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
        Sql.getInstance().prepared(query, values, SqlResult.validResultHandler(handler));
    }

    @Override
    public void delete(Integer absenceId, Handler<Either<String, JsonObject>> handler) {
        String query = "DELETE FROM  " + Presences.dbSchema + ".absence WHERE id = " + absenceId;
        Sql.getInstance().raw(query, SqlResult.validUniqueResultHandler(absenceResult -> {
            if (absenceResult.isLeft()) {
                String message = "[Presences@DefaultAbsenceService] failed to delete absence";
                LOGGER.error(message, absenceResult.left().getValue());
                handler.handle(new Either.Left<>(message));
            } else {
                handler.handle(new Either.Right<>(absenceResult.right().getValue()));
            }
        }));
    }

    @Override
    public void absenceRemovalTask(Handler<Either<String, JsonObject>> handler) {
        String query = "DELETE FROM  " + Presences.dbSchema + ".absence WHERE start_date <= NOW() - interval '72 hour'";
        Sql.getInstance().raw(query, SqlResult.validUniqueResultHandler(handler));
    }
}
