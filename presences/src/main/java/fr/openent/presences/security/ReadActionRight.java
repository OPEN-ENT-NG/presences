package fr.openent.presences.security;

import fr.openent.presences.common.helper.*;
import fr.openent.presences.core.constants.*;
import fr.openent.presences.enums.*;
import fr.wseduc.webutils.http.*;
import io.vertx.core.*;
import io.vertx.core.http.*;
import org.entcore.common.http.filter.*;
import org.entcore.common.user.*;

public class ReadActionRight implements ResourcesProvider {
    @Override
    public void authorize(HttpServerRequest request, Binding binding, UserInfos user, Handler<Boolean> handler) {
        String structure = request.getParam(Field.STRUCTUREID);
        handler.handle(user.getStructures().contains(structure) &&
                WorkflowHelper.hasRight(user, WorkflowActions.MANAGE.toString()));
    }
}
