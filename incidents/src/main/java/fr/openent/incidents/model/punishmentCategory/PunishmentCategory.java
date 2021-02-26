package fr.openent.incidents.model.punishmentCategory;

import fr.openent.incidents.Incidents;
import fr.openent.presences.model.Model;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

public abstract class PunishmentCategory extends Model {
    public static final String DUTY = "Modèle 1";
    public static final String EXCLUDE = "Modèle 4";
    public static final String DETENTION = "Modèle 2";
    public static final String BLAME = "Modèle 3";

    public static void getCategoryLabelFromType(Long typeId, String structureId, Handler<AsyncResult<String>> handler) {
        String query = "SELECT c.label AS label " +
                "      FROM " + Incidents.dbSchema + ".punishment_category c " +
                "               INNER JOIN " + Incidents.dbSchema + ".punishment_type t " +
                "                          ON t.punishment_category_id = c.id " +
                "      WHERE t.id =  " + typeId +
                "        AND t.structure_id = '" + structureId + "';";

        Sql.getInstance().raw(query, SqlResult.validUniqueResultHandler(result -> {
            if (result.isLeft()) {
                String message = "[Incidents@PunishmentCategory::getCategoryLabelFromType] Failed to get category";
                handler.handle(Future.failedFuture(message + " " + result.left().getValue()));
                return;
            }

            String label = result.right().getValue().getString("label");
            handler.handle(Future.succeededFuture(label));
        }));
    }


    public static void getSpecifiedCategoryFromType(Long type_id, String structure_id, JsonObject body, Handler<AsyncResult<PunishmentCategory>> handler) {
        getCategoryLabelFromType(type_id, structure_id, result -> {
            if (result.failed()) {
                handler.handle(Future.failedFuture(result.cause().getMessage()));
                return;
            }

            PunishmentCategory category = getSpecifiedCategoryFromLabel(result.result());

            if (category == null) {
                handler.handle(Future.failedFuture("[Incidents@PunishmentCategory::specifyCategoryFromType] category not found"));
                return;
            }
            category.setFromJson(body);
            category.formatDates();
            handler.handle(Future.succeededFuture(category));
        });
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
}
