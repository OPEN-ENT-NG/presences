package fr.openent.presences.controller;

import fr.openent.presences.Presences;
import fr.openent.presences.export.RegistryCSVExport;
import fr.openent.presences.security.*;
import fr.openent.presences.service.RegistryService;
import fr.openent.presences.service.impl.DefaultRegistryService;
import fr.wseduc.rs.Get;
import fr.wseduc.security.*;
import io.vertx.core.MultiMap;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.*;

import java.util.ArrayList;
import java.util.Arrays;
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
                !params.contains("group") || (!params.contains("type") && !params.contains("forgottenNotebook")) || !m.matches()) {
            badRequest(request);
            return;
        }

        List<String> groups = params.getAll("group");
        List<String> types = params.getAll("type");
        String month = params.get("month");
        String structureId = params.get("structureId");
        boolean forgottenNotebook = Boolean.parseBoolean(params.get("forgottenNotebook"));

        registryService.get(month, groups, types, structureId, forgottenNotebook, arrayResponseHandler(request));
    }

    @Get("/registry/export")
    @ResourceFilter(RegistryRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void exportRegistry(HttpServerRequest request) {
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
        boolean forgottenNotebook = Boolean.parseBoolean(params.get("forgottenNotebook"));
        registryService.getCSV(month, groups, types, structureId, forgottenNotebook, result -> {

            List<String> csvHeaders = new ArrayList<>(Arrays.asList(
                    "presences.registry.csv.header.student.lastName",
                    "presences.registry.csv.header.student.firstName",
                    "presences.registry.csv.header.classname",
                    "presences.registry.csv.header.type",
                    "presences.registry.csv.header.motive",
                    "presences.registry.csv.header.start.date",
                    "presences.registry.csv.header.start.time",
                    "presences.registry.csv.header.end.date",
                    "presences.registry.csv.header.end.time",
                    "presences.registry.csv.lateness.date",
                    "presences.registry.csv.lateness.time",
                    "presences.registry.csv.header.incident.place",
                    "presences.registry.csv.header.incident.protagonist.type"
                    ));


            RegistryCSVExport rce = new RegistryCSVExport(result.right().getValue());
            rce.setRequest(request);
            rce.setHeader(csvHeaders);
            rce.export();
        });


    }


}
