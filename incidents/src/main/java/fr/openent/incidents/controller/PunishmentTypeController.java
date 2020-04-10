package fr.openent.incidents.controller;

import fr.openent.incidents.constants.Actions;
import fr.openent.incidents.enums.PunishmentsType;
import fr.openent.incidents.security.ManageIncidentRight;
import fr.openent.incidents.service.PunishmentTypeService;
import fr.openent.incidents.service.impl.DefaultPunishmentTypeService;
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

public class PunishmentTypeController extends ControllerHelper {

    private PunishmentTypeService punishmentTypeService;

    public PunishmentTypeController() {
        super();
        this.punishmentTypeService = new DefaultPunishmentTypeService();
    }

    @Get("/punishments/type")
    @ApiDoc("Retreive punishments types")
    @ResourceFilter(ManageIncidentRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void get(final HttpServerRequest request) {
        String structure_id = request.getParam("structure_id");
        if (!request.params().contains("structure_id")) {
            badRequest(request);
            return;
        }
        punishmentTypeService.get(structure_id, DefaultResponseHandler.arrayResponseHandler(request));
    }

    @Post("/punishments/type")
    @ApiDoc("Create punishment type")
    @ResourceFilter(ManageIncidentRight.class)
    @Trace(Actions.INCIDENT_PUNISHMENT_TYPE_CREATION)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void post(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, punishmentTypeBody -> {
            if (isPunishmentTypeBodyInvalid(punishmentTypeBody)
                    && !punishmentTypeBody.getString("type").equals(PunishmentsType.PUNITION.toString())
                    && !punishmentTypeBody.getString("type").equals(PunishmentsType.SANCTION.toString())
            ) {
                badRequest(request);
                return;
            }
            punishmentTypeService.create(punishmentTypeBody, either -> {
                if (either.isLeft()) {
                    log.error("[Incidents@PunishmentsTypeController::post] failed to create punishment type",
                            either.left().getValue());
                    renderError(request);
                } else {
                    renderJson(request, either.right().getValue());
                }
            });
        });
    }

    private boolean isPunishmentTypeBodyInvalid(JsonObject punishmentTypeBody) {
        return !punishmentTypeBody.containsKey("structure_id") &&
                !punishmentTypeBody.containsKey("label") &&
                !punishmentTypeBody.containsKey("type") &&
                !punishmentTypeBody.containsKey("punishment_category_id");
    }

    @Put("/punishments/type")
    @ApiDoc("Update punishment type")
    @ResourceFilter(ManageIncidentRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @Trace(Actions.INCIDENT_PUNISHMENT_TYPE_UPDATE)
    public void put(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, punishmentTypeBody -> {
            if (isPunishmentTypeBodyInvalid(punishmentTypeBody)
                    && !punishmentTypeBody.containsKey("hidden")
                    && !punishmentTypeBody.containsKey("id")
                    && !punishmentTypeBody.getString("type").equals(PunishmentsType.PUNITION.toString())
                    && !punishmentTypeBody.getString("type").equals(PunishmentsType.SANCTION.toString())) {
                badRequest(request);
                return;
            }
            punishmentTypeService.put(punishmentTypeBody, either -> {
                if (either.isLeft()) {
                    log.error("[Incidents@PunishmentsTypeController::put] failed to update punishment type",
                            either.left().getValue());
                    renderError(request);
                } else {
                    renderJson(request, either.right().getValue());
                }
            });
        });
    }

    @Delete("/punishments/type")
    @ApiDoc("Delete punishment type")
    @ResourceFilter(ManageIncidentRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @Trace(Actions.INCIDENT_PUNISHMENT_TYPE_DELETE)
    public void delete(final HttpServerRequest request) {
        if (!request.params().contains("id")) {
            badRequest(request);
            return;
        }
        Integer punishmentTypeId = Integer.parseInt(request.getParam("id"));
        punishmentTypeService.delete(punishmentTypeId, either -> {
            if (either.isLeft()) {
                log.error("[Incidents@PunishmentsTypeController::delete] failed to delete punishment type",
                        either.left().getValue());
                renderError(request);
            } else {
                renderJson(request, either.right().getValue());
            }
        });
    }
}
