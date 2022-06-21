package fr.openent.incidents.controller;

import fr.openent.incidents.constants.Actions;
import fr.openent.incidents.security.ManageIncidentRight;
import fr.openent.incidents.service.SeriousnessService;
import fr.openent.incidents.service.impl.DefaultSeriousnessService;
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

public class SeriousnessController extends ControllerHelper {

    private SeriousnessService seriousnessService;

    public SeriousnessController() {
        super();
        this.seriousnessService = new DefaultSeriousnessService();
    }

    @Get("/seriousnesses")
    @ApiDoc("Retrieve incidents seriousnesses")
    @ResourceFilter(ManageIncidentRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void get(final HttpServerRequest request) {
        String structureId = request.getParam("structureId");
        if (!request.params().contains("structureId")) {
            badRequest(request);
            return;
        }
        seriousnessService.get(structureId, DefaultResponseHandler.arrayResponseHandler(request));
    }

    @Post("/seriousness")
    @ApiDoc("Create incidents seriousness")
    @ResourceFilter(ManageIncidentRight.class)
    @Trace(Actions.INCIDENT_SERIOUSNESS_CREATION)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void post(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + "seriousnessCreate", seriousnessBody -> {
            if (isPartnerBodyInvalid(seriousnessBody) || seriousnessBody.getInteger("level") > 7) {
                badRequest(request);
                return;
            }
            seriousnessService.create(seriousnessBody)
                    .onSuccess(res -> renderJson(request, res))
                    .onFailure(error -> {
                        log.error("[Incidents@SeriousnessController] failed to create seriousness", error.getMessage());
                        renderError(request);
                    });
        });
    }

    private boolean isPartnerBodyInvalid(JsonObject seriousnessBody) {
        return !seriousnessBody.containsKey("structureId") &&
                !seriousnessBody.containsKey("label") &&
                !seriousnessBody.containsKey("level");
    }

    @Put("/seriousness")
    @ApiDoc("Update incidents seriousness")
    @ResourceFilter(ManageIncidentRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @Trace(Actions.INCIDENT_SERIOUSNESS_UPDATE)
    public void put(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + "seriousnessUpdate", seriousnessBody -> {
            if (isPartnerBodyInvalid(seriousnessBody) && !seriousnessBody.containsKey("hidden") &&
                    !seriousnessBody.containsKey("id") || seriousnessBody.getInteger("level") > 7) {
                badRequest(request);
                return;
            }
            seriousnessService.put(seriousnessBody)
                    .onSuccess(res -> renderJson(request, res))
                    .onFailure(error -> {
                        log.error("[Incidents@SeriousnessController] failed to update seriousness", error.getMessage());
                        renderError(request);
                    });
        });
    }

    @Delete("/seriousness")
    @ApiDoc("Delete incidents seriousness")
    @ResourceFilter(ManageIncidentRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @Trace(Actions.INCIDENT_SERIOUSNESS_DELETION)
    public void delete(final HttpServerRequest request) {
        if (!request.params().contains("id")) {
            badRequest(request);
            return;
        }
        Integer incidentTypeId = Integer.parseInt(request.getParam("id"));
        seriousnessService.delete(incidentTypeId, either -> {
            if (either.isLeft()) {
                log.error("[Incidents@SeriousnessController] failed to delete seriousness", either.left().getValue());
                renderError(request);
            } else {
                renderJson(request, either.right().getValue());
            }
        });
    }
}
