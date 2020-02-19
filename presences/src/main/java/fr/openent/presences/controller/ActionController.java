package fr.openent.presences.controller;

import fr.openent.presences.constants.Actions;
import fr.openent.presences.security.Manage;
import fr.openent.presences.service.ActionService;
import fr.openent.presences.service.impl.DefaultActionService;
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

public class ActionController extends ControllerHelper {
    private ActionService actionService = new DefaultActionService();

    @Get("/actions")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(Manage.class)
    @ApiDoc("Get given structure")
    public void get(final HttpServerRequest request) {
        String structureId = request.getParam("structureId");
        if (!request.params().contains("structureId")) {
            badRequest(request);
            return;
        }
        actionService.get(structureId, DefaultResponseHandler.arrayResponseHandler(request));
    }

    @Post("/action")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(Manage.class)
    @ApiDoc("Create action")
    @Trace(Actions.SETTING_ACTION_TYPE_CREATION)
    public void post(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, actionBody -> {
            if (isActionBodyInvalid(actionBody)) {
                badRequest(request);
                return;
            }
            actionService.create(actionBody, either -> {
                if (either.isLeft()) {
                    log.error("[Presences@ActionController] failed to create action", either.left().getValue());
                    renderError(request);
                } else {
                    renderJson(request, either.right().getValue());
                }
            });
        });
    }

    @Put("/action")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(Manage.class)
    @ApiDoc("Update action")
    @Trace(Actions.SETTING_ACTION_TYPE_UPDATE)
    public void put(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, actionBody -> {
            if (isActionBodyInvalid(actionBody) && !actionBody.containsKey("hidden") &&
                    !actionBody.containsKey("id")) {
                badRequest(request);
                return;
            }
            actionService.put(actionBody, either -> {
                if (either.isLeft()) {
                    log.error("[Presences@ActionController] failed to update action", either.left().getValue());
                    renderError(request);
                } else {
                    renderJson(request, either.right().getValue());
                }
            });
        });
    }

    @Delete("/action")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(Manage.class)
    @ApiDoc("Delete action")
    @Trace(Actions.SETTING_ACTION_TYPE_DELETE)
    public void delete(final HttpServerRequest request) {
        if (!request.params().contains("id")) {
            badRequest(request);
            return;
        }
        Integer actionId = Integer.parseInt(request.getParam("id"));
        actionService.delete(actionId, either -> {
            if (either.isLeft()) {
                log.error("[Presences@ActionController] failed to delete action", either.left().getValue());
                renderError(request);
            } else {
                renderJson(request, either.right().getValue());
            }
        });
    }

    private boolean isActionBodyInvalid(JsonObject actionBody) {
        return !actionBody.containsKey("structureId") &&
                !actionBody.containsKey("label") &&
                !actionBody.containsKey("abbreviation") &&
                !(actionBody.getString("abbreviation").length() <= 10);
    }
}
