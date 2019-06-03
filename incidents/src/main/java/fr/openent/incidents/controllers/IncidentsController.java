package fr.openent.incidents.controllers;

import fr.openent.incidents.Incidents;
import fr.openent.incidents.export.IncidentsCSVExport;
import fr.openent.incidents.service.IncidentsService;
import fr.openent.incidents.service.impl.IncidentsServiceImpl;
import fr.openent.presences.common.helper.FutureHelper;
import fr.wseduc.rs.*;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.response.DefaultResponseHandler;
import org.entcore.common.user.UserUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class IncidentsController extends ControllerHelper {

    private IncidentsService incidentsService;
    private EventBus eb;

    public IncidentsController(EventBus eb) {
        super();
        this.incidentsService = new IncidentsServiceImpl(eb);
        this.eb = eb;
    }

    @Get("")
    @ApiDoc("Render view")
    @SecuredAction("view")
    public void view(HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            JsonObject action = new JsonObject()
                    .put("action", "user.getActivesStructure")
                    .put("module", "presences")
                    .put("structures", new JsonArray(user.getStructures()));
            eb.send("viescolaire", action, event -> {
                JsonObject body = (JsonObject) event.result().body();
                if (event.failed() || "error".equals(body.getString("status"))) {
                    log.error("[Incidents@IncidentsController] Failed to retrieve actives structures");
                    renderError(request);
                } else {
                    renderView(request, new JsonObject().put("structures", body.getJsonArray("results", new JsonArray())));
                }
            });
        });
    }

    @Get("/incidents")
    @ApiDoc("Retrieve incidents")
    public void getIncidents(final HttpServerRequest request) {
        String structureId = request.getParam("structureId");
        String startDate = request.getParam("startDate");
        String endDate = request.getParam("endDate");
        List<String> userId = request.getParam("userId") != null ? Arrays.asList(request.getParam("userId").split("\\s*,\\s*")) : null;

        String page = request.getParam("page") != null ? request.getParam("page") : "0";

        if (!request.params().contains("structureId") || !request.params().contains("startDate") ||
                !request.params().contains("endDate") || !request.params().contains("page")) {
            badRequest(request);
            return;
        }

        Future<JsonArray> incidentsFuture = Future.future();
        Future<JsonObject> pageNumberFuture = Future.future();

        CompositeFuture.all(incidentsFuture, pageNumberFuture).setHandler(event -> {
            if (event.failed()) {
                renderError(request, JsonObject.mapFrom(event.cause()));
            } else {
                JsonObject res = new JsonObject()
                        .put("page", Integer.parseInt(page))
                        .put("page_count", pageNumberFuture.result().getLong("count") / Incidents.PAGE_SIZE)
                        .put("all", incidentsFuture.result());

                renderJson(request, res);
            }
        });

        incidentsService.get(structureId, startDate, endDate, userId, page, false, FutureHelper.handlerJsonArray(incidentsFuture));
        incidentsService.getPageNumber(structureId, startDate, endDate, userId, page, FutureHelper.handlerJsonObject(pageNumberFuture));
    }

    @Get("/incidents/export")
    @ApiDoc("Export incidents")
    public void exportIncidents(HttpServerRequest request) {
        String structureId = request.getParam("structureId");
        String startDate = request.getParam("startDate");
        String endDate = request.getParam("endDate");
        List<String> userId = request.getParam("userId") != null ? Arrays.asList(request.getParam("userId").split("\\s*,\\s*")) : null;

        incidentsService.get(structureId, startDate, endDate, userId, null, false, event -> {
            if (event.isLeft()) {
                log.error("[Incidents@IncidentsController] Failed to fetch incidents", event.left().getValue());
                renderError(request);
                return;
            }

            JsonArray incidents = event.right().getValue();
            List<String> csvHeaders = new ArrayList<>(Arrays.asList(
                    "incidents.csv.header.date", "incidents.csv.header.place",
                    "incidents.csv.header.type", "incidents.csv.header.description",
                    "incidents.csv.header.seriousness", "incidents.csv.header.protagonists",
                    "incidents.csv.header.partner", "incidents.csv.header.processed"));
            IncidentsCSVExport ice = new IncidentsCSVExport(incidents);
            ice.setRequest(request);
            ice.setHeader(csvHeaders);
            ice.export();
        });
    }

    @Get("/incidents/parameter/types")
    @ApiDoc("Retrieve incidents parameter")
    public void getIncidentParameter(final HttpServerRequest request) {
        String structure_id = request.getParam("structureId");
        if (!request.params().contains("structureId")) {
            badRequest(request);
            return;
        }
        incidentsService.getIncidentParameter(structure_id, DefaultResponseHandler.defaultResponseHandler(request));
    }


    @Post("/incidents")
    @ApiDoc("Create incident")
    public void createIncident(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + "incidents", incidents -> {
            incidentsService.create(incidents, DefaultResponseHandler.arrayResponseHandler(request));
        });
    }

    @Put("/incidents/:id")
    @ApiDoc("Update incident")
    public void updateIncident(final HttpServerRequest request) {
        if (!request.params().contains("id")) {
            badRequest(request);
            return;
        }
        Number incidentId = Integer.parseInt(request.getParam("id"));
        RequestUtils.bodyToJson(request, pathPrefix + "incidents", incidents -> {
            incidentsService.update(incidentId, incidents, DefaultResponseHandler.defaultResponseHandler(request));
        });
    }

    @Delete("/incidents/:id")
    @ApiDoc("Delete incident")
    public void deleteIncident(final HttpServerRequest request) {
        if (!request.params().contains("id")) {
            badRequest(request);
            return;
        }
        String incidentId = request.getParam("id");
        incidentsService.delete(incidentId, DefaultResponseHandler.defaultResponseHandler(request));
    }
}