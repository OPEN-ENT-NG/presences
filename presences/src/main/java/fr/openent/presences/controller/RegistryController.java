package fr.openent.presences.controller;

import fr.openent.presences.Presences;
import fr.openent.presences.service.RegistryService;
import fr.openent.presences.service.impl.DefaultRegistryService;
import fr.wseduc.rs.Get;
import fr.wseduc.security.SecuredAction;
import io.vertx.core.MultiMap;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.controller.ControllerHelper;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.entcore.common.http.response.DefaultResponseHandler.arrayResponseHandler;

public class RegistryController extends ControllerHelper {

    private RegistryService registryService;

    public RegistryController(EventBus eb) {
        super();
        this.registryService = new DefaultRegistryService(eb);
    }

    @Get("/registry")
    @SecuredAction(Presences.REGISTRY)
    public void getRegistry(HttpServerRequest request) {
        MultiMap params = request.params();
        Pattern p = Pattern.compile("[0-9]{4}-[0-9]{1,2}");
        String monthParams = params.contains("month") ? params.get("month") : "";
        Matcher m = p.matcher(monthParams);
        if (!params.contains("structureId") || !params.contains("month") ||
                !params.contains("group") || !params.contains("type") || !m.matches()) {
            badRequest(request);
            return;
        }

        List<String> groups = params.getAll("group");
        List<String> types = params.getAll("type");
        String month = params.get("month");
        String structureId = params.get("structureId");

        registryService.get(month, groups, types, structureId, arrayResponseHandler(request));
    }
}
