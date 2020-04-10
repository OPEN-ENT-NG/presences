package fr.openent.incidents.controller;

import fr.openent.incidents.security.ManageIncidentRight;
import fr.openent.incidents.service.PunishmentCategoryService;
import fr.openent.incidents.service.impl.DefaultPunishmentCategoryService;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.http.response.DefaultResponseHandler;

public class PunishmentCategoryController extends ControllerHelper {

    private PunishmentCategoryService punishmentCategoryService;

    public PunishmentCategoryController() {
        super();
        this.punishmentCategoryService = new DefaultPunishmentCategoryService();
    }

    @Get("/punishments/category")
    @ApiDoc("Retreive punishments categories")
    @ResourceFilter(ManageIncidentRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void get(final HttpServerRequest request) {
        punishmentCategoryService.get(DefaultResponseHandler.arrayResponseHandler(request));
    }
}
