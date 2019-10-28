package fr.openent.massmailing.security;

import fr.openent.massmailing.actions.WorkflowActions;
import fr.openent.presences.common.helper.WorkflowHelper;
import fr.wseduc.webutils.http.Binding;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.user.UserInfos;

import java.util.List;

public class CanAccessMassMailing implements ResourcesProvider {
    @Override
    public void authorize(HttpServerRequest request, Binding binding, UserInfos userInfos, Handler<Boolean> handler) {
        String structureId = request.getParam("structure");
        List<String> structures = userInfos.getStructures();
        handler.handle(structures.contains(structureId) && WorkflowHelper.hasRight(userInfos, WorkflowActions.VIEW.toString()));
    }
}
