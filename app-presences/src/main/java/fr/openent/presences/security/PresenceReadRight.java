package fr.openent.presences.security;

import fr.openent.presences.enums.WorkflowActionsCouple;
import fr.wseduc.webutils.http.Binding;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.user.UserInfos;

public class PresenceReadRight implements ResourcesProvider {
    @Override
    public void authorize(HttpServerRequest httpServerRequest, Binding binding, UserInfos userInfos, Handler<Boolean> handler) {
        handler.handle(WorkflowActionsCouple.READ_PRESENCE.hasRight(userInfos));
    }
}
