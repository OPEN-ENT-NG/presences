package fr.openent.incidents.controller;

import fr.openent.incidents.security.ManageIncidentRight;
import fr.openent.incidents.service.PartnerService;
import fr.openent.incidents.service.impl.DefaultPartnerService;
import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;

public class PartnerController extends ControllerHelper {

    private PartnerService partnerService;

    public PartnerController() {
        super();
        this.partnerService = new DefaultPartnerService();
    }

    @Get("/incidents/partners")
    @ApiDoc("Retrieve incidents partners")
    @ResourceFilter(ManageIncidentRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void get(final HttpServerRequest request) {
        String structureId = request.getParam("structureId");
        if (!request.params().contains("structureId")) {
            badRequest(request);
            return;
        }

    }

    @Post("/incidents/partner")
    @ApiDoc("Create incidents partner")
    @ResourceFilter(ManageIncidentRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void post(final HttpServerRequest request) {

    }

    @Put("/incidents/partner")
    @ApiDoc("Update incidents partner")
    @ResourceFilter(ManageIncidentRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void put(final HttpServerRequest request) {

    }

    @Delete("/incidents/partner")
    @ApiDoc("Delete incidents partner")
    @ResourceFilter(ManageIncidentRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void delete(final HttpServerRequest request) {

    }


}
