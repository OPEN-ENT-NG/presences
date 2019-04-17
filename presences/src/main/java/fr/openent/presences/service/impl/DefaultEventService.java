package fr.openent.presences.service.impl;

import fr.openent.presences.Presences;
import fr.openent.presences.enums.EventType;
import fr.openent.presences.service.EventService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;

public class DefaultEventService implements EventService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultEventService.class);


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
    public void delete(Integer id, Handler<Either<String, JsonObject>> handler) {
        String query = "DELETE FROM " + Presences.dbSchema + ".event WHERE id = ?";
        JsonArray params = new JsonArray()
                .add(id);

        Sql.getInstance().prepared(query, params, SqlResult.validRowsResultHandler(handler));
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
