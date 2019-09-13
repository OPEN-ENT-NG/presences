package fr.openent.presences.service.impl;

import fr.openent.presences.Presences;
import fr.openent.presences.common.helper.WorkflowHelper;
import fr.openent.presences.enums.WorkflowActions;
import fr.openent.presences.service.AbsenceService;
import fr.openent.presences.service.GroupService;
import fr.wseduc.webutils.Either;
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

    private EventBus eb;
    private GroupService groupService;

    public DefaultAbsenceService(EventBus eb) {
        this.eb = eb;
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
    public void create(JsonObject absenceBody, UserInfos user, Handler<Either<String, JsonArray>> handler) {
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
                List<String> users = new ArrayList<>();
                users.add(absenceBody.getString("student_id"));
                groupService.getUserGroups(users, absenceBody.getString("structure_id"), groupEvent -> {
                    if (groupEvent.isLeft()) {
                        String message = "[Presences@DefaultAbsenceService] failed to retrieve user info";
                        LOGGER.error(message, groupEvent.left().getValue());
                        handler.handle(new Either.Left<>(message));
                    } else {
                        matchEventsWithAbsents(absenceBody, user, groupEvent.right().getValue().getJsonObject(0).getString("id"), event -> {
                            if (event.isLeft()) {
                                String message = "[Presences@DefaultAbsenceService] failed to retrieve user info";
                                LOGGER.error(message, event.left().getValue());
                                handler.handle(new Either.Left<>(message));
                            } else {
                                handler.handle(new Either.Right<>(event.right().getValue()));
                            }
                        });
                    }
                });
            }
        }));
    }

    @Override
    public void absenceRemovalTask(Handler<Either<String, JsonObject>> handler) {
        String query = "DELETE FROM  " + Presences.dbSchema + ".absence where start_date <= NOW() - interval '72 hour'";
        Sql.getInstance().raw(query, SqlResult.validUniqueResultHandler(handler));
    }

    private void matchEventsWithAbsents(JsonObject absenceBody, UserInfos user, String id, Handler<Either<String, JsonArray>> handler) {
        String query = "WITH register as ( " +
                " SELECT register.id, register.start_date, register.end_date FROM " + Presences.dbSchema + ".register " +
                " INNER JOIN " + Presences.dbSchema + ".rel_group_register as rgr ON (rgr.register_id = register.id) " +
                " WHERE rgr.group_id = ? " +
                " AND register.start_date >= ? " +
                " AND register.end_date <= ? " +
                "AND register.id NOT IN (SELECT event.register_id FROM " + Presences.dbSchema + ".event " +
                "WHERE event.type_id = 1 and event.register_id = register.id and event.student_id = ? ))" +
                "INSERT INTO " + Presences.dbSchema + ".event (start_date, end_date, comment, counsellor_input, student_id, register_id, type_id, reason_id, owner)" +
               "(SELECT  register.start_date, register.end_date, '', ?, ?, register.id, 1, ?, ? FROM register) returning register_id";

        JsonArray params = new JsonArray()
                .add(id)
                .add(absenceBody.getString("start_date"))
                .add(absenceBody.getString("end_date"))
                .add(absenceBody.getString("student_id"))
                .add(true)
                .add(absenceBody.getString("student_id"));

        if (absenceBody.getInteger("reason_id") != null) {
            params.add(absenceBody.getInteger("reason_id"));
        } else {
            params.addNull();
        }
        params.add(absenceBody.getString("owner"));

       Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }
}
