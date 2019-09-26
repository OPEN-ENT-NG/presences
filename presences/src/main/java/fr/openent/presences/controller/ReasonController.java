package fr.openent.presences.controller;

import fr.openent.presences.security.Manage;
import fr.openent.presences.service.ReasonService;
import fr.openent.presences.service.impl.DefaultReasonService;
import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.http.response.DefaultResponseHandler;

public class ReasonController extends ControllerHelper {

    private ReasonService reasonService;

    public ReasonController() {
        super();
        this.reasonService = new DefaultReasonService();
    }

    @Get("/reasons")
    @ApiDoc("Get reasons")
    @ResourceFilter(Manage.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void get(final HttpServerRequest request) {
        String structureId = request.getParam("structureId");
        if (!request.params().contains("structureId")) {
            badRequest(request);
            return;
        }
        reasonService.get(structureId, DefaultResponseHandler.arrayResponseHandler(request));
    }

    @Post("/reason")
    @ApiDoc("Create reason")
    @ResourceFilter(Manage.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void post(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, reasonBody -> {
            if (isReasonBodyInvalid(reasonBody)) {
                badRequest(request);
                return;
            }
            reasonService.create(reasonBody, either -> {
                if (either.isLeft()) {
                    log.error("[Presences@ReasonController] failed to create reason", either.left().getValue());
                    renderError(request);
                } else {
                    renderJson(request, either.right().getValue());
                }
            });
        });
    }

    private boolean isReasonBodyInvalid(JsonObject reasonBody) {
        return !reasonBody.containsKey("structureId") &&
                !reasonBody.containsKey("label") &&
                !reasonBody.containsKey("absenceCompliance");
    }

    @Put("/reason")
    @ApiDoc("Update reason")
    @ResourceFilter(Manage.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void put(final HttpServerRequest request) {
        if (!request.params().contains("reasonId")) {
            badRequest(request);
            return;
        }
        Integer reasonId = Integer.parseInt(request.getParam("reasonId"));
        RequestUtils.bodyToJson(request, reasonBody -> {
            if (isReasonBodyInvalid(reasonBody) && !reasonBody.containsKey("hidden")) {
                badRequest(request);
                return;
            }
            reasonService.put(reasonId, reasonBody, either -> {
                if (either.isLeft()) {
                    log.error("[Presences@ReasonController] failed to update reason", either.left().getValue());
                    renderError(request);
                } else {
                    renderJson(request, either.right().getValue());
                }
            });
        });
    }

    @Delete("/reason")
    @ApiDoc("Delete reason")
    @ResourceFilter(Manage.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void delete(final HttpServerRequest request) {
        if (!request.params().contains("reasonId")) {
            badRequest(request);
            return;
        }
        Integer reasonId = Integer.parseInt(request.getParam("reasonId"));
        reasonService.delete(reasonId, either -> {
            if (either.isLeft()) {
                log.error("[Presences@ReasonController] failed to delete reason", either.left().getValue());
                renderError(request);
            } else {
                renderJson(request, either.right().getValue());
            }
        });
    }
}
