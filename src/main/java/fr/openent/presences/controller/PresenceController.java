package fr.openent.presences.controller;

import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.security.SecuredAction;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.controller.ControllerHelper;

public class PresenceController extends ControllerHelper {

    @Get("")
    @ApiDoc("Render view")
    @SecuredAction("view")
    public void view (HttpServerRequest request) {
        renderView(request);
    }
}
