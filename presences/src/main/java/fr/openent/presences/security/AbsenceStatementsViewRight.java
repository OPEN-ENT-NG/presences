package fr.openent.presences.security;

import fr.openent.presences.common.helper.WorkflowHelper;
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
        boolean result = WorkflowHelper.hasRight(user, WorkflowActions.MANAGE_ABSENCE_STATEMENTS.toString());
        if (WorkflowHelper.hasRight(user, WorkflowActions.ABSENCE_STATEMENTS_VIEW.toString())) {
            result = request.params().size() > 0;
            List<String> student_ids = request.params().getAll("student_id");
            result = student_ids.size() > 0;
            for (String id : student_ids) {
                if (!(user.getUserId().equals(id) || user.getChildrenIds().contains(id))) result = false;
            }
        }

        handler.handle(result);
    }
}
