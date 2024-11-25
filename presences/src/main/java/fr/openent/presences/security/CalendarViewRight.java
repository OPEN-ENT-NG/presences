package fr.openent.presences.security;

import fr.openent.presences.common.helper.*;
import fr.openent.presences.enums.*;
import fr.wseduc.webutils.http.*;
import io.vertx.core.*;
import io.vertx.core.http.*;
import org.entcore.common.http.filter.*;
import org.entcore.common.user.*;

public class CalendarViewRight implements ResourcesProvider {
    @Override
    public void authorize(HttpServerRequest request, Binding binding, UserInfos user, Handler<Boolean> handler) {
        handler.handle(WorkflowHelper.hasRight(user, WorkflowActions.CALENDAR_VIEW.toString()));
    }
}
