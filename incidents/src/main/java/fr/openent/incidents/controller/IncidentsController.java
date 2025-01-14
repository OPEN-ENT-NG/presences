package fr.openent.incidents.controller;

import fr.openent.incidents.Incidents;
import fr.openent.incidents.constants.Actions;
import fr.openent.incidents.export.IncidentsCSVExport;
import fr.openent.incidents.security.*;
import fr.openent.incidents.service.*;
import fr.openent.incidents.worker.*;
import fr.openent.presences.common.export.*;
import fr.openent.presences.common.helper.*;
import fr.openent.presences.common.service.*;
import fr.openent.presences.core.constants.*;
import fr.openent.presences.enums.*;
import fr.wseduc.bus.BusAddress;
import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.*;
import fr.wseduc.webutils.http.*;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.*;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.http.filter.Trace;
import org.entcore.common.http.response.DefaultResponseHandler;
import org.entcore.common.user.UserUtils;

import org.entcore.common.events.EventHelper;
import org.entcore.common.events.EventStore;
import org.entcore.common.events.EventStoreFactory;

import java.util.*;
import java.util.stream.*;

public class IncidentsController extends ControllerHelper {

    private final IncidentsService incidentsService;
    private final GroupService groupService;
    private final ExportData exportData;
    private EventHelper eventHelper;

    public IncidentsController(CommonIncidentsServiceFactory serviceFactory) {
        this.incidentsService = serviceFactory.incidentsService();
        this.groupService = serviceFactory.groupService();
        this.exportData = serviceFactory.exportData();
        final EventStore eventStore = EventStoreFactory.getFactory().getEventStore(Incidents.class.getSimpleName());
		this.eventHelper =  new EventHelper(eventStore);
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
            eb.request("viescolaire", action, event -> {
                JsonObject body = (JsonObject) event.result().body();
                if (event.failed() || "error".equals(body.getString("status"))) {
                    log.error("[Incidents@IncidentsController] Failed to retrieve actives structures");
                    renderError(request);
                } else {
                    renderView(request, new JsonObject().put("structures", body.getJsonArray("results", new JsonArray())));
                    eventHelper.onAccess(request);
                }
            });
        });
    }

    @Get("/incidents")
    @ApiDoc("Retrieve incidents")
    @ResourceFilter(ReadIncidentRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void getListIncidents(final HttpServerRequest request) {
        String structureId = request.getParam(Field.STRUCTUREID);
        String startDate = request.getParam(Field.STARTDATE);
        String endDate = request.getParam(Field.ENDDATE);
        String field = request.params().contains(Field.ORDER) ? request.getParam(Field.ORDER) : Field.DATE;
        boolean reverse = request.params().contains(Field.REVERSE) &&
                Boolean.parseBoolean(request.getParam(Field.REVERSE));

        List<String> userId = request.getParam(Field.USERID) != null ?
                Arrays.asList(request.getParam(Field.USERID).split("\\s*,\\s*")) : null;
        List<String> audienceIds = request.getParam(Field.AUDIENCEID) != null ?
                Arrays.asList(request.getParam(Field.AUDIENCEID).split("\\s*,\\s*")) : null;


        String page = request.getParam(Field.PAGE) != null ? request.getParam(Field.PAGE) : "0";

        if (!request.params().contains(Field.STRUCTUREID) || !request.params().contains(Field.STARTDATE) ||
                !request.params().contains(Field.ENDDATE)) {
            badRequest(request);
            return;
        }

        groupService.getGroupStudents(audienceIds)
                .onFailure(fail -> renderError(request, JsonObject.mapFrom(fail.getCause().getMessage())))
                .onSuccess(groupStudents -> {
                    Promise<JsonArray> incidentsPromise = Promise.promise();
                    Promise<JsonObject> pageNumberPromise = Promise.promise();

                    List<String> studentIds = ((List<JsonObject>) groupStudents.getList()).stream()
                            .map((JsonObject s) -> s.getString(Field.ID)).collect(Collectors.toList());
                    if (userId != null) studentIds.addAll(userId);
                    studentIds.removeAll(Collections.singletonList(null));

                    CompositeFuture.all(incidentsPromise.future(), pageNumberPromise.future())
                            .onFailure(fail -> renderError(request, JsonObject.mapFrom(fail.getCause().getMessage())))
                            .onSuccess(event -> {
                                JsonObject res = new JsonObject()
                                        .put(Field.PAGE, Integer.parseInt(page))
                                        .put(Field.PAGE_COUNT, (pageNumberPromise.future().result()
                                                .getLong(Field.COUNT) <= Incidents.PAGE_SIZE) ? 0
                                                : (pageNumberPromise.future().result().getLong(Field.COUNT) / Incidents.PAGE_SIZE))
                                        .put(Field.ALL, incidentsPromise.future().result());
                                renderJson(request, res);
                            });

                    incidentsService.get(structureId, startDate, endDate,
                            studentIds, page, false, field, reverse, FutureHelper.handlerJsonArray(incidentsPromise));
                    incidentsService.getPageNumber(structureId, startDate, endDate, studentIds,
                            page, field, reverse, FutureHelper.handlerJsonObject(pageNumberPromise));

                });
    }

    @Get("/incidents/export")
    @ApiDoc("Export incidents")
    @ResourceFilter(ManageIncidentRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @SuppressWarnings("unchecked")
    public void exportIncidents(HttpServerRequest request) {
        String structureId = request.getParam(Field.STRUCTUREID);
        String startDate = request.getParam(Field.STARTDATE);
        String endDate = request.getParam(Field.ENDDATE);
        List<String> userId = request.getParam(Field.USERID) != null
                ? Arrays.asList(request.getParam(Field.USERID).split("\\s*,\\s*")) : null;
        List<String> audienceIds = request.getParam(Field.AUDIENCEID) != null ?
                Arrays.asList(request.getParam(Field.AUDIENCEID).split("\\s*,\\s*")) : null;
        String field = request.params().contains(Field.ORDER) ? request.getParam(Field.ORDER) : Field.DATE;
        boolean reverse = request.params().contains(Field.REVERSE) && Boolean.parseBoolean(request.getParam(Field.REVERSE));

        UserUtils.getUserInfos(eb, request, userInfos -> {
            groupService.getGroupStudents(audienceIds)
                    .onFailure(fail -> renderError(request, JsonObject.mapFrom(fail.getCause().getMessage())))
                    .onSuccess(groupStudents -> {
                        List<String> studentIds = ((List<JsonObject>) groupStudents.getList()).stream()
                                .map((JsonObject s) -> s.getString(Field.ID)).collect(Collectors.toList());
                        if (userId != null) studentIds.addAll(userId);
                        studentIds.removeAll(Collections.singletonList(null));

                        String domain = Renders.getHost(request);
                        String locale = I18n.acceptLanguage(request);
                        JsonObject params = new JsonObject()
                                .put(Field.STRUCTUREID, structureId)
                                .put(Field.STARTDATE, startDate)
                                .put(Field.ENDDATE, endDate)
                                .put(Field.STUDENTIDS, new JsonArray(studentIds))
                                .put(Field.FIELD, field)
                                .put(Field.REVERSE, reverse)
                                .put(Field.USER, UserInfosHelper.toJSON(userInfos))
                                .put(Field.LOCALE, locale)
                                .put(Field.DOMAIN, domain);

                        //TODO future.all
                        exportData.export(IncidentsExportWorker.class.getName(), ExportActions.EXPORT_INCIDENTS,
                                ExportType.CSV.type(), params);

                        incidentsService.get(structureId, startDate, endDate, studentIds,
                                null, false, field, reverse, event -> {
                                    if (event.isLeft()) {
                                        String message = String.format("[Incidents@%s::exportIncidents] Failed to fetch incidents",
                                                this.getClass().getSimpleName());
                                        log.error(message, event.left().getValue());
                                        renderError(request);
                                        return;
                                    }

                                    JsonArray incidents = event.right().getValue();
                                    List<String> csvHeaders = new ArrayList<>(Arrays.asList(
                                            "incidents.csv.header.date", "incidents.csv.header.place",
                                            "incidents.csv.header.type", "incidents.csv.header.description",
                                            "incidents.csv.header.seriousness", "incidents.csv.header.protagonists",
                                            "incidents.csv.header.partner", "incidents.csv.header.processed"));
                                    IncidentsCSVExport ice = new IncidentsCSVExport(incidents, domain, locale);
                                    ice.setRequest(request);
                                    ice.setHeader(csvHeaders);
                                    ice.export();
                                });
                    });
        });
    }

    @Get("/incidents/parameter/types")
    @ApiDoc("Retrieve incidents parameter")
    @ResourceFilter(ReadIncidentRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void getIncidentParameter(final HttpServerRequest request) {
        String structureId = request.getParam(Field.STRUCTUREID);
        if (!request.params().contains(Field.STRUCTUREID)) {
            badRequest(request);
            return;
        }
        incidentsService.getIncidentParameter(structureId, DefaultResponseHandler.defaultResponseHandler(request));
    }


    @Post("/incidents")
    @ApiDoc("Create incident")
    @SecuredAction(Incidents.MANAGE_INCIDENT)
    @Trace(Actions.INCIDENT_CREATION)
    public void createIncident(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + "incidents", incidents -> {
            incidentsService.create(incidents, DefaultResponseHandler.arrayResponseHandler(request));
        });
    }

    @Put("/incidents/:id")
    @ApiDoc("Update incident")
    @ResourceFilter(ManageIncidentRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @Trace(Actions.INCIDENT_UPDATE)
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
    @ResourceFilter(ManageIncidentRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @Trace(Actions.INCIDENT_DELETION)
    public void deleteIncident(final HttpServerRequest request) {
        if (!request.params().contains("id")) {
            badRequest(request);
            return;
        }
        String incidentId = request.getParam("id");
        incidentsService.delete(incidentId, DefaultResponseHandler.defaultResponseHandler(request));
    }

    @BusAddress("fr.openent.incident")
    public void busHandler(Message<JsonObject> message) {
        String action = message.body().getString("action", "");
        switch (action) {
            case "getUserIncident": {
                String structureId = message.body().getString("structureId");
                String userId = message.body().getString("userId");
                String startDate = message.body().getString("start_date");
                String endDate = message.body().getString("end_date");
                incidentsService.get(structureId, startDate, endDate, userId, event -> {
                    if (event.isLeft()) {
                        JsonObject json = (new JsonObject())
                                .put("status", "error")
                                .put("message", event.left().getValue());
                        message.reply(json);
                    } else {
                        JsonObject json = (new JsonObject())
                                .put("status", "ok")
                                .put("results", event.right().getValue());
                        message.reply(json);
                    }
                });
            }
            break;
            default: {
                log.error("[IncidentsController@busHandler] invalid action " + action);
                JsonObject json = (new JsonObject())
                        .put("status", "error")
                        .put("message", "invalid.action");
                message.reply(json);
            }
        }
    }

    @Get("/rights/read/incidents")
    @SecuredAction(Incidents.READ_INCIDENT)
    public void getIncidents(final HttpServerRequest request) {
        request.response().setStatusCode(501).end();
    }
}