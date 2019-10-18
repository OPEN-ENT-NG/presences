package fr.openent.massmailing.service.impl;

import fr.openent.massmailing.Massmailing;
import fr.openent.massmailing.enums.MailingType;
import fr.openent.massmailing.service.SettingsService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

public class DefaultSettingsService implements SettingsService {

    private String returningValues = "id, name, content, type, structure_id";

    @Override
    public void getTemplates(MailingType type, String structure, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT id, name, content, type, structure_id FROM " + Massmailing.dbSchema + ".template WHERE structure_id = ? AND type = ?;";
        JsonArray params = new JsonArray()
                .add(structure)
                .add(type.toString());

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    @Override
    public void createTemplate(JsonObject template, String userId, Handler<Either<String, JsonObject>> handler) {
        String query = "INSERT INTO " + Massmailing.dbSchema + ".template (structure_id, name, content, type, owner) " +
                "VALUES (?, ?, ?, ?, ?) RETURNING " + returningValues;
        JsonArray params = new JsonArray()
                .add(template.getString("structure_id"))
                .add(template.getString("name"))
                .add(template.getString("content"))
                .add(template.getString("type"))
                .add(userId);

        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void updateTemplate(Integer id, JsonObject template, Handler<Either<String, JsonObject>> handler) {
        String query = "UPDATE " + Massmailing.dbSchema + ".template " +
                "SET structure_id = ?, name = ?, content = ?, type = ? " +
                "WHERE id = ? " +
                "RETURNING " + returningValues;

        JsonArray params = new JsonArray()
                .add(template.getString("structure_id"))
                .add(template.getString("name"))
                .add(template.getString("content"))
                .add(template.getString("type"))
                .add(id);

        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));

    }

    @Override
    public void deleteTemplate(Integer id, Handler<Either<String, JsonObject>> handler) {
        String query = "DELETE FROM " + Massmailing.dbSchema + ".template WHERE id = ?;";
        JsonArray params = new JsonArray().add(id);

        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }
}
