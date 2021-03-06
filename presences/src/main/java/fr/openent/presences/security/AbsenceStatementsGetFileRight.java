package fr.openent.presences.security;

import fr.openent.presences.common.helper.WorkflowHelper;
import fr.openent.presences.enums.WorkflowActions;
import fr.wseduc.webutils.http.Binding;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.user.UserInfos;

import java.util.List;

public class AbsenceStatementsGetFileRight implements ResourcesProvider {
    @Override
    public void authorize(HttpServerRequest request, Binding binding, UserInfos user, Handler<Boolean> handler) {
        handler.handle(
                WorkflowHelper.hasRight(user, WorkflowActions.MANAGE_ABSENCE_STATEMENTS.toString())
                || WorkflowHelper.hasRight(user, WorkflowActions.ABSENCE_STATEMENTS_VIEW.toString())
        );
    }
}
