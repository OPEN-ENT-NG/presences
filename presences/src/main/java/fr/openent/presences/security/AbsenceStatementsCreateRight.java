package fr.openent.presences.security;

import fr.openent.presences.common.helper.WorkflowHelper;
import fr.openent.presences.enums.WorkflowActions;
import fr.wseduc.webutils.http.Binding;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.user.UserInfos;

public class AbsenceStatementsCreateRight implements ResourcesProvider {
    @Override
    public void authorize(HttpServerRequest request, Binding binding, UserInfos user, Handler<Boolean> handler) {
        request.setExpectMultipart(true);
        request.endHandler(resultHandler -> {
            handler.handle(
                    WorkflowHelper.hasRight(user, WorkflowActions.ABSENCE_STATEMENTS_CREATE.toString())
                            && user.getChildrenIds().contains(request.getFormAttribute("student_id"))
            );
        });
    }
}
