package fr.openent.incidents.model.punishmentCategory;

import fr.openent.incidents.Incidents;
import fr.openent.presences.core.constants.*;
import fr.openent.presences.model.Model;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.*;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

public abstract class PunishmentCategory extends Model {
    public static final String DUTY = "Modèle 1";
    public static final String EXCLUDE = "Modèle 4";
    public static final String DETENTION = "Modèle 2";
    public static final String BLAME = "Modèle 3";

    public static void getCategoryLabelFromType(Long typeId, String structureId, Handler<AsyncResult<String>> handler) {
        JsonArray params = new JsonArray();

        String query = "SELECT c.label AS label " +
                "   FROM " + Incidents.dbSchema + ".punishment_category c " +
                "  INNER JOIN " + Incidents.dbSchema + ".punishment_type t " +
                "  ON t.punishment_category_id = c.id " +
                "  WHERE t.id =  ?" +
                "  AND t.structure_id = ?";

        params.add(typeId)
                .add(structureId);

        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(result -> {
            if (result.isLeft()) {
                String message = "[Incidents@PunishmentCategory::getCategoryLabelFromType] Failed to get category";
                handler.handle(Future.failedFuture(message + " " + result.left().getValue()));
                return;
            }

            String label = result.right().getValue().getString(Field.LABEL);
            handler.handle(Future.succeededFuture(label));
        }));
    }


    public static void getSpecifiedCategoryFromType(Long type_id, String structure_id, JsonObject body, Handler<AsyncResult<PunishmentCategory>> handler) {
        getCategoryLabelFromType(type_id, structure_id, result -> {
            if (result.failed()) {
                handler.handle(Future.failedFuture(result.cause().getMessage()));
                return;
            }

            PunishmentCategory category = initCategoryFromLabel(result.result(), body);

            if (category == null) {
                String message = String.format("[Incidents@%s::specifyCategoryFromType] category not found"
                        , "PunishmentCategory");
                log.error(String.format("%s %s", message, result.cause().getMessage()));
                handler.handle(Future.failedFuture(message));
                return;
            }

            handler.handle(Future.succeededFuture(category));
        });
    }

    public static Future<PunishmentCategory> getSpecifiedCategoryFromType(String structure_id, Long type_id) {
        Promise<PunishmentCategory> promise = Promise.promise();
        getSpecifiedCategoryFromType(type_id, structure_id, null, promise);
        return promise.future();
    }

    public static PunishmentCategory initCategoryFromLabel(String label, JsonObject body) {
        PunishmentCategory category = getSpecifiedCategoryFromLabel(label);

        if (category == null) return null;

        if (body != null) formatFromBody(category, body);

        return category;
    }

    public static void formatFromBody(PunishmentCategory category, JsonObject body) {
        category.setFromJson(body);
        category.formatDates();
    }


    public static PunishmentCategory getSpecifiedCategoryFromLabel(String label) {
        if (label == null) {
            return null;
        }

        switch (label) {
            case DUTY: {
                return new DutyCategory();
            }
            case EXCLUDE: {
                return new ExcludeCategory();
            }
            case DETENTION: {
                return new DetentionCategory();
            }
            case BLAME: {
                return new BlameCategory();
            }
            default: {
                return null;
            }
        }
    }

    public abstract void formatDates();

    public abstract String getLabel();
}
