package fr.openent.presences.security;

import fr.openent.presences.Presences;
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
        List<String> alerts = request.params().getAll("id");
        String query = "SELECT COUNT(id) as count " +
                "FROM " + Presences.dbSchema + ".alerts " +
                "WHERE id IN " + Sql.listPrepared(alerts) +
                " AND structure_id IN " + Sql.listPrepared(user.getStructures());

        JsonArray params = new JsonArray().addAll(new JsonArray(alerts)).addAll(new JsonArray(user.getStructures()));
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(res -> {
            if (res.isLeft()) {
                handler.handle(false);
                return;
            }

            Integer count = res.right().getValue().getInteger("count");
            handler.handle(count == alerts.size());
        }));
    }
}
