package fr.openent.incidents.security;

import fr.openent.incidents.enums.WorkflowActions;
import fr.openent.presences.common.helper.WorkflowHelper;
import fr.wseduc.webutils.http.Binding;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.user.UserInfos;

public class StudentEventsViewRight implements ResourcesProvider {
    @Override
    public void authorize(HttpServerRequest request, Binding binding, UserInfos user, Handler<Boolean> handler) {
        handler.handle(
                WorkflowHelper.hasRight(user, WorkflowActions.STUDENT_EVENTS_VIEW.toString())
                && (user.getUserId().equals(request.params().get("id"))
                || user.getChildrenIds().contains(request.params().get("id")))
        );
    }
}
