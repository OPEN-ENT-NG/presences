package fr.openent.incidents.controller;

import fr.openent.incidents.constants.Actions;
import fr.openent.incidents.security.*;
import fr.openent.incidents.service.PlaceService;
import fr.openent.incidents.service.impl.DefaultPlaceService;
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

public class PlaceController extends ControllerHelper {

    private PlaceService placeService;

    public PlaceController() {
        super();
        this.placeService = new DefaultPlaceService();
    }

    @Get("/places")
    @ApiDoc("Retrieve incidents places")
    @ResourceFilter(ReadIncidentRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void get(final HttpServerRequest request) {
        String structureId = request.getParam("structureId");
        if (!request.params().contains("structureId")) {
            badRequest(request);
            return;
        }
        placeService.get(structureId, DefaultResponseHandler.arrayResponseHandler(request));
    }

    @Post("/place")
    @ApiDoc("Create incidents place")
    @ResourceFilter(ManageIncidentRight.class)
    @Trace(Actions.INCIDENT_PLACE_CREATION)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void post(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, placeBody -> {
            if (isPartnerBodyInvalid(placeBody)) {
                badRequest(request);
                return;
            }
            placeService.create(placeBody, either -> {
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

    @Put("/place")
    @ApiDoc("Update incidents place")
    @ResourceFilter(ManageIncidentRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @Trace(Actions.INCIDENT_PLACE_UPDATE)
    public void put(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, placeBody -> {
            if (isPartnerBodyInvalid(placeBody) && !placeBody.containsKey("hidden") &&
                    !placeBody.containsKey("id")) {
                badRequest(request);
                return;
            }
            placeService.put(placeBody, either -> {
                if (either.isLeft()) {
                    log.error("[Incidents@PlaceController] failed to update place", either.left().getValue());
                    renderError(request);
                } else {
                    renderJson(request, either.right().getValue());
                }
            });
        });
    }

    @Delete("/place")
    @ApiDoc("Delete incidents place")
    @ResourceFilter(ManageIncidentRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @Trace(Actions.INCIDENT_PLACE_DELETION)
    public void delete(final HttpServerRequest request) {
        if (!request.params().contains("id")) {
            badRequest(request);
            return;
        }
        Integer incidentTypeId = Integer.parseInt(request.getParam("id"));
        placeService.delete(incidentTypeId, either -> {
            if (either.isLeft()) {
                log.error("[Incidents@PlaceController] failed to delete place", either.left().getValue());
                renderError(request);
            } else {
                renderJson(request, either.right().getValue());
            }
        });
    }


}

