package fr.openent.incidents.controller;

import fr.openent.incidents.security.ManageIncidentRight;
import fr.openent.incidents.service.ProtagonistTypeService;
import fr.openent.incidents.service.impl.DefaultProtagonistTypeService;
import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.http.response.DefaultResponseHandler;

public class ProtagonistTypeController extends ControllerHelper {

    private ProtagonistTypeService protagonistTypeService;

    public ProtagonistTypeController() {
        super();
        this.protagonistTypeService = new DefaultProtagonistTypeService();
    }

    @Get("/protagonists/type")
    @ApiDoc("Retrieve incidents places")
    @ResourceFilter(ManageIncidentRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void get(final HttpServerRequest request) {
        String structureId = request.getParam("structureId");
        if (!request.params().contains("structureId")) {
            badRequest(request);
            return;
        }
        protagonistTypeService.get(structureId, DefaultResponseHandler.arrayResponseHandler(request));
    }

    @Post("/protagonist/type")
    @ApiDoc("Create incidents place")
    @ResourceFilter(ManageIncidentRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void post(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, placeBody -> {
            if (isPartnerBodyInvalid(placeBody)) {
                badRequest(request);
                return;
            }
            protagonistTypeService.create(placeBody, either -> {
                if (either.isLeft()) {
                    log.error("[Incidents@PlaceController] failed to create place", either.left().getValue());
                    renderError(request);
                } else {
                    renderJson(request, either.right().getValue());
                }
            });
        });
    }

    private boolean isPartnerBodyInvalid(JsonObject placeBody) {
        return !placeBody.containsKey("structureId") &&
                !placeBody.containsKey("label");
    }

    @Put("/protagonist/type")
    @ApiDoc("Update incidents place")
    @ResourceFilter(ManageIncidentRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void put(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, placeBody -> {
            if (isPartnerBodyInvalid(placeBody) && !placeBody.containsKey("hidden") &&
                    !placeBody.containsKey("id")) {
                badRequest(request);
                return;
            }
            protagonistTypeService.put(placeBody, either -> {
                if (either.isLeft()) {
                    log.error("[Incidents@PlaceController] failed to update place", either.left().getValue());
                    renderError(request);
                } else {
                    renderJson(request, either.right().getValue());
                }
            });
        });
    }

    @Delete("/protagonist/type")
    @ApiDoc("Delete incidents place")
    @ResourceFilter(ManageIncidentRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void delete(final HttpServerRequest request) {
        if (!request.params().contains("id")) {
            badRequest(request);
            return;
        }
        Integer incidentTypeId = Integer.parseInt(request.getParam("id"));
        protagonistTypeService.delete(incidentTypeId, either -> {
            if (either.isLeft()) {
                log.error("[Incidents@PlaceController] failed to delete place", either.left().getValue());
                renderError(request);
            } else {
                renderJson(request, either.right().getValue());
            }
        });
    }
}
