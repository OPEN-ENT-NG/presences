package fr.openent.presences.controller;

import fr.openent.presences.constants.Actions;
import fr.openent.presences.core.constants.*;
import fr.openent.presences.security.*;
import fr.openent.presences.service.SettingsService;
import fr.openent.presences.service.impl.DefaultSettingsService;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Put;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.*;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.*;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.http.filter.Trace;

import static org.entcore.common.http.response.DefaultResponseHandler.defaultResponseHandler;

public class SettingsController extends ControllerHelper {
    private SettingsService settingsService = new DefaultSettingsService();

    @Get("/structures/:id/settings")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(Manage.class)
    @ApiDoc("Retrieve given structure settings")
    public void retrieve(HttpServerRequest request) {
        settingsService.retrieve(request.getParam("id"), defaultResponseHandler(request));
    }

    @Get("/structures/:id/settings/multiple-slots")
    @ApiDoc("Retrieve given structure multiple slot setting")
    @ResourceFilter(ViewRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void retrieveMultipleSlots(HttpServerRequest request) {
        String id = request.getParam(Field.ID);
        settingsService.retrieveMultipleSlots(id)
                .onSuccess(res -> renderJson(request, res))
                .onFailure(err -> badRequest(request));
    }

    @Put("/structures/:id/settings")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(SettingFilter.class)
    @Trace(Actions.SETTING_UPDATE)
    @ApiDoc("Update settings for given structure identifier")
    public void put(HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + "settings", settings ->
                settingsService.put(request.getParam(Field.ID), settings)
                        .onSuccess(res -> renderJson(request, res, 200))
                        .onFailure(error -> renderError(request))
        );
    }
}
