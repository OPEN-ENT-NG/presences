package fr.openent.presences.security;

import fr.openent.presences.common.helper.WorkflowHelper;
import fr.openent.presences.core.constants.*;
import fr.openent.presences.enums.WorkflowActions;
import fr.wseduc.webutils.http.Binding;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.user.UserInfos;

import java.util.List;

public class AbsenceStatementsViewRight implements ResourcesProvider {
    @Override
    public void authorize(HttpServerRequest request, Binding binding, UserInfos user, Handler<Boolean> handler) {
        List<String> studentIds = request.params().getAll(Field.STUDENT_ID);
        boolean result = WorkflowHelper.hasRight(user, WorkflowActions.MANAGE_ABSENCE_STATEMENTS.toString()) ||
                WorkflowHelper.hasRight(user, WorkflowActions.MANAGE_ABSENCE_STATEMENTS_RESTRICTED.toString()) ||
                (WorkflowHelper.hasRight(user, WorkflowActions.ABSENCE_STATEMENTS_VIEW.toString())
                        && request.params().size() > 0 && (!studentIds.isEmpty())
                        && studentIds.stream()
                        .anyMatch(id -> user.getUserId().equals(id) || user.getChildrenIds().contains(id)));
        handler.handle(result);
    }
}
