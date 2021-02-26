package fr.openent.incidents.controller;

import fr.openent.incidents.security.presence.PresencesManage;
import fr.openent.presences.common.presences.Presences;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.http.response.DefaultResponseHandler;

public class PresencesController extends ControllerHelper {

    @Get("/structures/:structureId/reasons")
    @ApiDoc("Get reasons")
    @ResourceFilter(PresencesManage.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void get(final HttpServerRequest request) {
        String structureId = request.getParam("structureId");
        Presences.getInstance().getReasons(structureId, DefaultResponseHandler.arrayResponseHandler(request));
    }
}
