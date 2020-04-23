package fr.openent.incidents.service.impl;

import fr.openent.incidents.Incidents;
import fr.openent.incidents.service.PunishmentCategoryService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

public class DefaultPunishmentCategoryService implements PunishmentCategoryService {

    @Override
    public void get(Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT * FROM " + Incidents.dbSchema + ".punishment_category";

        Sql.getInstance().raw(query, SqlResult.validResultHandler(handler));
    }
}
