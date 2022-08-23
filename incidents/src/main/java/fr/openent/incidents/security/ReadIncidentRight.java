package fr.openent.incidents.security;

import fr.openent.incidents.enums.WorkflowActions;
import fr.openent.presences.common.helper.WorkflowHelper;
import fr.openent.presences.core.constants.*;
import fr.wseduc.webutils.http.Binding;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.user.UserInfos;

public class ReadIncidentRight implements ResourcesProvider {
    @Override
    public void authorize(HttpServerRequest request, Binding binding, UserInfos user, Handler<Boolean> handler) {
        String structure = (request.getParam(Field.STRUCTUREID) != null) ? request.getParam(Field.STRUCTUREID) :
                request.getParam(Field.STRUCTURE_ID);
        handler.handle(user.getStructures().contains(structure) &&
                WorkflowHelper.hasRight(user, WorkflowActions.READ_INCIDENT.toString()));
    }
}