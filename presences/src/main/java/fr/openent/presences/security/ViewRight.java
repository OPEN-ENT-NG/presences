package fr.openent.presences.security;

import fr.openent.presences.*;
import fr.openent.presences.common.helper.*;
import fr.wseduc.webutils.http.*;
import io.vertx.core.*;
import io.vertx.core.http.*;
import org.entcore.common.http.filter.*;
import org.entcore.common.user.*;

public class ViewRight implements ResourcesProvider {
    @Override
    public void authorize(HttpServerRequest request, Binding binding, UserInfos user,
                          Handler<Boolean> handler) {
        handler.handle(WorkflowHelper.hasRight(user, Presences.VIEW));
    }
}