package fr.openent.massmailing.security;

import fr.openent.massmailing.Massmailing;
import fr.openent.massmailing.actions.WorkflowActions;
import fr.openent.presences.common.helper.WorkflowHelper;
import fr.wseduc.webutils.http.Binding;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;

public class Manage implements ResourcesProvider {
    @Override
    public void authorize(HttpServerRequest request, Binding binding, UserInfos user, Handler<Boolean> handler) {
        // IN every case, check if user have MANAGE right
        if (!"DELETE".equals(request.method().name())) {
            // In case of POST and PUT request, check if the user creates/updates a template on its structure
            RequestUtils.bodyToJson(request, body -> {
                if (!body.containsKey("structure_id")) {
                    handler.handle(false);
                } else {
                    handler.handle(WorkflowHelper.hasRight(user, WorkflowActions.MANAGE.toString()) && user.getStructures().contains(body.getString("structure_id")));
                }
            });
        } else {
            // In case of DELETE request, check if user deletes a template attached to its structures
            try {
                request.pause();
                Integer templateId = Integer.parseInt(request.getParam("id"));
                String query = "SELECT COUNT(id) as count FROM " + Massmailing.dbSchema + ".template WHERE id = ? AND structure_id IN " + Sql.listPrepared(user.getStructures());
                JsonArray params = new JsonArray()
                        .add(templateId)
                        .addAll(new JsonArray(user.getStructures()));
                Sql.getInstance().prepared(query, params, event -> {
                    request.resume();
                    Long count = SqlResult.countResult(event);
                    handler.handle(WorkflowHelper.hasRight(user, WorkflowActions.MANAGE.toString()) && count != null && count > 0);
                });
            } catch (NumberFormatException e) {
                request.resume();
                handler.handle(false);
                throw e;
            }
        }
    }
}
