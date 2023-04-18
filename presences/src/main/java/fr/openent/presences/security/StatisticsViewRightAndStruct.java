package fr.openent.presences.security;

import fr.openent.presences.core.constants.Field;
import fr.openent.presences.enums.WorkflowActionsCouple;
import fr.wseduc.webutils.http.Binding;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.user.UserInfos;

public class StatisticsViewRightAndStruct implements ResourcesProvider {
    @Override
    public void authorize(HttpServerRequest request, Binding binding, UserInfos user, Handler<Boolean> handler) {
        String structure = request.getParam(Field.STRUCTUREID);
        structure = structure != null ? structure : request.getParam(Field.STRUCTURE_ID);
        structure = structure != null ? structure : request.getParam(Field.STRUCTURE);

        handler.handle(
                user.getStructures().contains(structure) &&
                        WorkflowActionsCouple.VIEW_STATISTICS.hasRight(user)
        );
    }
}
