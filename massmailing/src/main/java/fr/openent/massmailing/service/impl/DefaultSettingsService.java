package fr.openent.massmailing.service.impl;

import fr.openent.massmailing.Massmailing;
import fr.openent.massmailing.enums.MailingType;
import fr.openent.massmailing.model.Mailing.*;
import fr.openent.massmailing.service.SettingsService;
import fr.openent.presences.common.helper.*;
import fr.wseduc.webutils.Either;
import io.vertx.core.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

import java.util.List;

public class DefaultSettingsService implements SettingsService {

    private String returningValues = "id, name, content, type, structure_id, category";

    @Override
    public Future<List<Template>> getTemplates(MailingType type, String structure, List<String> listCategory) {
        Promise<List<Template>> promise = Promise.promise();
        final StringBuilder queryBuilder = new StringBuilder("SELECT id, name, content, type, structure_id, category FROM " + Massmailing.dbSchema + ".template WHERE structure_id = ? AND type = ?");
        JsonArray params = new JsonArray()
                .add(structure)
                .add(type.toString());
        if (!listCategory.contains("ALL")) {
            queryBuilder.append("AND ( 0 = 1");
            listCategory.stream().forEach(category -> {
                queryBuilder.append(" OR category = ?");
                params.add(category);
            });
            queryBuilder.append(" OR category = ?)");
            params.add("ALL");
        }
        queryBuilder.append(";");


        Sql.getInstance().prepared(queryBuilder.toString(), params,
                SqlResult.validResultHandler(IModelHelper.sqlResultToIModel(promise, Template.class,
                        String.format("[Massmailing@%s::getTemplates] Failed to get templates", this.getClass().getSimpleName())
                )));

        return promise.future();
    }

    @Override
    public void createTemplate(JsonObject template, String userId, Handler<Either<String, JsonObject>> handler) {
        String type = template.getString("type");
        String content = template.getString("content");
        String category = template.getString("category");

        if (!isRespectedSmsLengthContent(type, content, handler)) {
            return;
        }

        String query = "INSERT INTO " + Massmailing.dbSchema + ".template (structure_id, name, content, type, owner, category) " +
                "VALUES (?, ?, ?, ?, ?, ?) RETURNING " + returningValues;
        JsonArray params = new JsonArray()
                .add(template.getString("structure_id"))
                .add(template.getString("name"))
                .add(content)
                .add(type)
                .add(userId)
                .add(category);

        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void updateTemplate(Integer id, JsonObject template, Handler<Either<String, JsonObject>> handler) {
        String type = template.getString("type");
        String content = template.getString("content");
        String category = template.getString("category");

        if (!isRespectedSmsLengthContent(type, content, handler)) {
            return;
        }

        String query = "UPDATE " + Massmailing.dbSchema + ".template " +
                "SET structure_id = ?, name = ?, content = ?, type = ?, category = ? " +
                "WHERE id = ? " +
                "RETURNING " + returningValues;

        JsonArray params = new JsonArray()
                .add(template.getString("structure_id"))
                .add(template.getString("name"))
                .add(content)
                .add(type)
                .add(category)
                .add(id);

        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));

    }

    @Override
    public void deleteTemplate(Integer id, Handler<Either<String, JsonObject>> handler) {
        String query = "DELETE FROM " + Massmailing.dbSchema + ".template WHERE id = ?;";
        JsonArray params = new JsonArray().add(id);

        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void get(MailingType type, Integer id, String structure, Handler<Either<String, JsonObject>> handler) {
        String query = "SELECT name, content FROM " + Massmailing.dbSchema + ".template WHERE id = ? AND structure_id = ? AND type = ?;";
        JsonArray params = new JsonArray().add(id).add(structure).add(type.name());

        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    private boolean isRespectedSmsLengthContent(String type, String content, Handler<Either<String, JsonObject>> handler) {
        if (MailingType.SMS.toString().equals(type)) {
            if (content.length() > 160) {
                handler.handle(new Either.Left<>("Constraint max length content not respected"));
                return false;
            }
        }
        return true;
    }
}
