package fr.openent.presences.security;

import fr.openent.presences.enums.WorkflowActionsCouple;
import fr.wseduc.webutils.http.Binding;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.user.UserInfos;

public class SearchRight implements ResourcesProvider {
    @Override
    public void authorize(HttpServerRequest request, Binding binding, UserInfos user, Handler<Boolean> handler) {
        handler.handle(WorkflowActionsCouple.SEARCH.hasRight(user) || WorkflowActionsCouple.SEARCH_VIESCO.hasRight(user));
    }
}