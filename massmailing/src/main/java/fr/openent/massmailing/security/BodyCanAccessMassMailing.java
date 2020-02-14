package fr.openent.massmailing.security;

import fr.openent.massmailing.actions.WorkflowActions;
import fr.openent.presences.common.helper.WorkflowHelper;
import fr.wseduc.webutils.http.Binding;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.user.UserInfos;

import java.util.List;

public class BodyCanAccessMassMailing implements ResourcesProvider {
    @Override
    public void authorize(HttpServerRequest request, Binding binding, UserInfos userInfos, Handler<Boolean> handler) {
        request.pause();
        RequestUtils.bodyToJson(request, body -> {
            request.resume();
            String structureId = body.getString("structure");
            List<String> structures = userInfos.getStructures();
            handler.handle(structures.contains(structureId) && WorkflowHelper.hasRight(userInfos, WorkflowActions.VIEW.toString()));
        });
    }
}
