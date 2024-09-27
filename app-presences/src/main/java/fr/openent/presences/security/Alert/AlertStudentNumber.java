package fr.openent.presences.security.Alert;

import fr.openent.presences.common.helper.WorkflowHelper;
import fr.openent.presences.enums.WorkflowActions;
import fr.wseduc.webutils.http.Binding;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.user.UserInfos;

public class AlertStudentNumber implements ResourcesProvider {
    @Override
    public void authorize(HttpServerRequest httpServerRequest, Binding binding, UserInfos userInfos, Handler<Boolean> handler) {
        String structureId = httpServerRequest.getParam("id");
        handler.handle(userInfos.getStructures().contains(structureId) &&
                WorkflowHelper.hasRight(userInfos, WorkflowActions.ALERT_STUDENT_NUMBER.toString()));
    }
}
