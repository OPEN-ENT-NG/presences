package fr.openent.incidents.security;

import fr.openent.incidents.enums.WorkflowActions;
import fr.openent.presences.common.helper.WorkflowHelper;
import fr.openent.presences.core.constants.Field;
import fr.wseduc.webutils.http.Binding;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.user.UserInfos;

import java.util.List;

public class StudentsEventsViewRight implements ResourcesProvider {
    @Override
    @SuppressWarnings("unchecked")
    public void authorize(HttpServerRequest request, Binding binding, UserInfos user, Handler<Boolean> handler) {
        request.pause();
        RequestUtils.bodyToJson(request, body -> {
            request.resume();
            List<String> studentIds = body.getJsonArray(Field.STUDENT_IDS, new JsonArray()).getList();
            handler.handle(
                    WorkflowHelper.hasRight(user, WorkflowActions.STUDENT_EVENTS_VIEW.toString()) &&
                            !studentIds.isEmpty() && user.getChildrenIds().containsAll(studentIds)
            );
        });
    }
}
