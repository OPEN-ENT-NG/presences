package fr.openent.presences.security;

import fr.openent.presences.Presences;
import fr.openent.presences.core.constants.Field;
import fr.wseduc.webutils.http.Binding;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;

import java.util.List;

public class DeleteAlertFilter implements ResourcesProvider {
    @Override
    public void authorize(HttpServerRequest request, Binding binding, UserInfos user, Handler<Boolean> handler) {
        List<String> alertIds = request.params().getAll(Field.ID);
        String structureId = request.params().get(Field.STRUCTUREID);

        if (structureId == null && (alertIds == null || alertIds.isEmpty())) {
            handler.handle(false);
            return;
        }

        if (structureId != null) {
            handler.handle(user.getStructures().contains(structureId));
            return;
        }

        authorizeFromAlertIds(alertIds, user, handler);
    }

    private void authorizeFromAlertIds(List<String> alertIds, UserInfos user, Handler<Boolean> handler) {
        String query = "SELECT COUNT(id) as count " +
                "FROM " + Presences.dbSchema + ".alerts " +
                "WHERE id IN " + Sql.listPrepared(alertIds) +
                " AND structure_id IN " + Sql.listPrepared(user.getStructures());

        JsonArray params = new JsonArray().addAll(new JsonArray(alertIds)).addAll(new JsonArray(user.getStructures()));
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(res -> {
            if (res.isLeft()) {
                handler.handle(false);
                return;
            }

            Integer count = res.right().getValue().getInteger(Field.COUNT);
            handler.handle(count == alertIds.size());
        }));
    }

}
