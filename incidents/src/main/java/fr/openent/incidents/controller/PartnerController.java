package fr.openent.incidents.controller;

import fr.openent.incidents.constants.Actions;
import fr.openent.incidents.security.ManageIncidentRight;
import fr.openent.incidents.service.PartnerService;
import fr.openent.incidents.service.impl.DefaultPartnerService;
import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.http.filter.Trace;
import org.entcore.common.http.response.DefaultResponseHandler;

public class PartnerController extends ControllerHelper {

    private PartnerService partnerService;

    public PartnerController() {
        super();
        this.partnerService = new DefaultPartnerService();
    }

    @Get("/partners")
    @ApiDoc("Retrieve incidents partners")
    @ResourceFilter(ManageIncidentRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void get(final HttpServerRequest request) {
        String structureId = request.getParam("structureId");
        if (!request.params().contains("structureId")) {
            badRequest(request);
            return;
        }
        partnerService.get(structureId, DefaultResponseHandler.arrayResponseHandler(request));
    }

    @Post("/partner")
    @ApiDoc("Create incidents partner")
    @ResourceFilter(ManageIncidentRight.class)
    @Trace(Actions.INCIDENT_PARTNER_CREATION)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void post(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, partnerBody -> {
            if (isPartnerBodyInvalid(partnerBody)) {
                badRequest(request);
                return;
            }
            partnerService.create(partnerBody, either -> {
                if (either.isLeft()) {
                    log.error("[Incidents@PartnerController] failed to create partner", either.left().getValue());
                    renderError(request);
                } else {
                    renderJson(request, either.right().getValue());
                }
            });
        });
    }

    private boolean isPartnerBodyInvalid(JsonObject partnerBody) {
        return !partnerBody.containsKey("structureId") &&
                !partnerBody.containsKey("label");
    }

    @Put("/partner")
    @ApiDoc("Update incidents partner")
    @ResourceFilter(ManageIncidentRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @Trace(Actions.INCIDENT_PARTNER_UPDATE)
    public void put(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, partnerBody -> {
            if (isPartnerBodyInvalid(partnerBody) && !partnerBody.containsKey("hidden") &&
                    !partnerBody.containsKey("id")) {
                badRequest(request);
                return;
            }
            partnerService.put(partnerBody, either -> {
                if (either.isLeft()) {
                    log.error("[Incidents@PartnerController] failed to update partner", either.left().getValue());
                    renderError(request);
                } else {
                    renderJson(request, either.right().getValue());
                }
            });
        });
    }

    @Delete("/partner")
    @ApiDoc("Delete incidents partner")
    @ResourceFilter(ManageIncidentRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @Trace(Actions.INCIDENT_PARTNER_DELETION)
    public void delete(final HttpServerRequest request) {
        if (!request.params().contains("id")) {
            badRequest(request);
            return;
        }
        Integer incidentTypeId = Integer.parseInt(request.getParam("id"));
        partnerService.delete(incidentTypeId, either -> {
            if (either.isLeft()) {
                log.error("[Incidents@PartnerController] failed to delete partner", either.left().getValue());
                renderError(request);
            } else {
                renderJson(request, either.right().getValue());
            }
        });
    }


}
