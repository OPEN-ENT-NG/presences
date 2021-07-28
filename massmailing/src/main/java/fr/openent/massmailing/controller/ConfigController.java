package fr.openent.massmailing.controller;

import fr.openent.presences.common.helper.StringHelper;
import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.AdminFilter;
import org.entcore.common.http.filter.ResourceFilter;

public class ConfigController extends ControllerHelper {

    @Get("/config")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdminFilter.class)
    public void getConfig(final HttpServerRequest request) {
        JsonObject pdfGenerator = config.getJsonObject("pdf-generator", new JsonObject());
        pdfGenerator.put("auth", StringHelper.repeat("*", pdfGenerator.getString("auth", "").length()));
        renderJson(request, config);
    }
}
