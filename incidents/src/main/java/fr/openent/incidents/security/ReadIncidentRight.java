package fr.openent.incidents.security;

import fr.openent.incidents.enums.WorkflowActions;
import fr.openent.presences.common.helper.WorkflowHelper;
import fr.wseduc.webutils.http.Binding;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.user.UserInfos;

public class ReadIncidentRight implements ResourcesProvider {
    @Override
    public void authorize(HttpServerRequest request, Binding binding, UserInfos user, Handler<Boolean> handler) {
        handler.handle(WorkflowHelper.hasRight(user, WorkflowActions.READ_INCIDENT.toString()));
    }
}