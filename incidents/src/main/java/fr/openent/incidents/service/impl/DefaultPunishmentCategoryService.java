package fr.openent.incidents.service.impl;

import fr.openent.incidents.Incidents;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

public class DefaultPunishmentCategoryService {

    public void get(String structureId, Handler<Either<String, JsonObject>> handler) {
        String query = "SELECT * FROM " + Incidents.dbSchema + ".punishment_category where structure_id = '" + structureId + "'";

        Sql.getInstance().raw(query, SqlResult.validUniqueResultHandler(handler));
    }
}
