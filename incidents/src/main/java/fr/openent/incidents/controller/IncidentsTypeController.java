package fr.openent.incidents.controller;

import fr.openent.incidents.constants.Actions;
import fr.openent.incidents.security.*;
import fr.openent.incidents.service.IncidentsTypeService;
import fr.openent.incidents.service.impl.DefaultIncidentsTypeService;
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

public class IncidentsTypeController extends ControllerHelper {

    private IncidentsTypeService incidentsTypeService;

    public IncidentsTypeController() {
        super();
        this.incidentsTypeService = new DefaultIncidentsTypeService();
    }

    @Get("/types")
    @ApiDoc("Retrieve incidents type")
    @ResourceFilter(ReadIncidentRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void get(final HttpServerRequest request) {
        String structureId = request.getParam("structureId");
        if (!request.params().contains("structureId")) {
            badRequest(request);
            return;
        }
        incidentsTypeService.get(structureId, DefaultResponseHandler.arrayResponseHandler(request));
    }

    @Post("/type")
    @ApiDoc("Create incidents type")
    @ResourceFilter(ManageIncidentRight.class)
    @Trace(Actions.INCIDENT_TYPE_CREATION)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void post(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, incidentTypeBody -> {
            if (isIncidentBodyInvalid(incidentTypeBody)) {
                badRequest(request);
                return;
            }
            incidentsTypeService.create(incidentTypeBody, either -> {
                if (either.isLeft()) {
                    log.error("[Incidents@IncidentsTypeController] failed to create incident type", either.left().getValue());
                    renderError(request);
                } else {
                    renderJson(request, either.right().getValue());
                }
            });
        });
    }

    private boolean isIncidentBodyInvalid(JsonObject incidentTypeBody) {
        return !incidentTypeBody.containsKey("structureId") &&
                !incidentTypeBody.containsKey("label");
    }

    @Put("/type")
    @ApiDoc("Update incidents type")
    @ResourceFilter(ManageIncidentRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @Trace(Actions.INCIDENT_TYPE_UPDATE)
    public void put(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, incidentTypeBody -> {
            if (isIncidentBodyInvalid(incidentTypeBody) && !incidentTypeBody.containsKey("hidden") &&
                    !incidentTypeBody.containsKey("id")) {
                badRequest(request);
                return;
            }
            incidentsTypeService.put(incidentTypeBody, either -> {
                if (either.isLeft()) {
                    log.error("[Incidents@IncidentsTypeController] failed to update incident type", either.left().getValue());
                    renderError(request);
                } else {
                    renderJson(request, either.right().getValue());
                }
            });
        });
    }

    @Delete("/type")
    @ApiDoc("Delete incidents type")
    @ResourceFilter(ManageIncidentRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @Trace(Actions.INCIDENT_TYPE_DELETION)
    public void delete(final HttpServerRequest request) {
        if (!request.params().contains("id")) {
            badRequest(request);
            return;
        }
        Integer incidentTypeId = Integer.parseInt(request.getParam("id"));
        incidentsTypeService.delete(incidentTypeId, either -> {
            if (either.isLeft()) {
                log.error("[Incidents@IncidentsTypeController] failed to delete incident type", either.left().getValue());
                renderError(request);
            } else {
                renderJson(request, either.right().getValue());
            }
        });
    }


}
