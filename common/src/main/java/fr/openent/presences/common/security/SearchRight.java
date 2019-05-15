package fr.openent.presences.common.security;

import fr.openent.presences.common.helper.WorkflowHelper;
import fr.wseduc.webutils.http.Binding;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.user.UserInfos;

public class SearchRight implements ResourcesProvider {
    @Override
    public void authorize(HttpServerRequest httpServerRequest, Binding binding, UserInfos user, Handler<Boolean> handler) {
        handler.handle(WorkflowHelper.hasRight(user, "presences.search"));
    }
}
