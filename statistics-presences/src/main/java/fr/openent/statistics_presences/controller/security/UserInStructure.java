package fr.openent.statistics_presences.controller.security;

import fr.openent.presences.common.helper.WorkflowHelper;
import fr.openent.presences.core.constants.*;
import fr.openent.statistics_presences.enums.WorkflowActions;
import fr.openent.statistics_presences.enums.WorkflowActionsCouple;
import fr.wseduc.webutils.http.Binding;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.user.UserInfos;

public class UserInStructure implements ResourcesProvider {
    @Override
    public void authorize(HttpServerRequest request, Binding binding, UserInfos user, Handler<Boolean> handler) {
        String structure = request.getParam(Field.STRUCTURE);
        handler.handle(user.getStructures().contains(structure)
                && WorkflowActionsCouple.STATISTICS_PRESENCES_MANAGE.hasRight(user));
    }
}
