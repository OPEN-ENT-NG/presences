package fr.openent.presences.controller;

import fr.openent.presences.Presences;
import fr.openent.presences.common.helper.WorkflowHelper;
import fr.openent.presences.constants.Actions;
import fr.openent.presences.enums.WorkflowActions;
import fr.openent.presences.export.PresencesCSVExport;
import fr.openent.presences.security.presence.ManagePresenceRight;
import fr.openent.presences.service.PresenceService;
import fr.openent.presences.service.impl.DefaultPresenceService;
import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.events.EventStore;
import org.entcore.common.events.EventStoreFactory;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.http.filter.Trace;
import org.entcore.common.http.response.DefaultResponseHandler;
import org.entcore.common.user.UserUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PresencesController extends ControllerHelper {

    private EventBus eb;
    private PresenceService presencesService;
    private EventStore eventStore;

    public PresencesController(EventBus eb) {
        this.eb = eb;
        this.presencesService = new DefaultPresenceService();
        this.eventStore = EventStoreFactory.getFactory().getEventStore(Presences.class.getSimpleName());
    }

    @Get("")
    @ApiDoc("Render view")
    @SecuredAction("view")
    public void view(HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            JsonObject action = new JsonObject()
                    .put("action", "user.getActivesStructure")
                    .put("module", Presences.dbSchema)
                    .put("structures", new JsonArray(user.getStructures()));
            eb.send(Presences.ebViescoAddress, action, event -> {
                JsonObject body = (JsonObject) event.result().body();
                if (event.failed() || "error".equals(body.getString("status"))) {
                    log.error("[Presences@PresencesController] Failed to retrieve actives structures");
                    renderError(request);
                } else {
                    renderView(request, new JsonObject().put("structures", body.getJsonArray("results", new JsonArray())));
                }
            });
        });
        eventStore.createAndStoreEvent("ACCESS", request);
    }

    @Get("/presences")
    @ApiDoc("Retrieve presences")
    @SecuredAction(Presences.READ_PRESENCE)
    public void getPresences(final HttpServerRequest request) {
        get(request, DefaultResponseHandler.arrayResponseHandler(request));
    }

    @Post("/presence")
    @ApiDoc("Create presence")
    @SecuredAction(Presences.CREATE_PRESENCE)
    @Trace(Actions.PRESENCE_CREATION)
    public void createPresence(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, presences -> {
            if (!isValidBody(presences)) {
                badRequest(request);
                return;
            }
            UserUtils.getUserInfos(eb, request, user ->
                    presencesService.create(user, presences, DefaultResponseHandler.defaultResponseHandler(request)));
        });
    }

    @Put("/presence")
    @ApiDoc("Update presence")
    @SecuredAction(Presences.MANAGE_PRESENCE)
    @Trace(Actions.PRESENCE_UPDATE)
    public void updatePresence(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, presences -> {
            if (!isValidBody(presences)) {
                badRequest(request);
                return;
            }
            presencesService.update(presences, DefaultResponseHandler.defaultResponseHandler(request));
        });
    }

    private Boolean isValidBody(JsonObject presence) {
        return presence.containsKey("structureId") &&
                presence.containsKey("startDate") &&
                presence.containsKey("endDate") &&
                presence.containsKey("markedStudents") &&
                !presence.getJsonArray("markedStudents").isEmpty();
    }

    @Delete("/presence")
    @ApiDoc("Delete presence")
    @ResourceFilter(ManagePresenceRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @Trace(Actions.PRESENCE_DELETE)
    public void deletePresence(final HttpServerRequest request) {
        if (!request.params().contains("id")) {
            badRequest(request);
            return;
        }
        String presenceId = request.getParam("id");
        presencesService.delete(presenceId, DefaultResponseHandler.defaultResponseHandler(request));
    }

    @Get("/presences/export")
    @ApiDoc("Export presences")
    @SecuredAction(Presences.READ_PRESENCE)
    public void exportPresences(final HttpServerRequest request) {
        get(request, result -> {
            if (result.isLeft()) {
                log.error("[PresencesController@exportPresences] Failed to export Presences", result.left());
                renderError(request);
                return;
            }
            JsonArray presences = result.right().getValue();

            List<String> csvHeaders = new ArrayList<>(Arrays.asList(
                    "presences.presences.csv.header.owner",
                    "presences.presences.csv.header.discipline",
                    "presences.presences.csv.header.date",
                    "presences.presences.csv.header.start.time",
                    "presences.presences.csv.header.end.time",
                    "presences.presences.csv.header.student.number"));

            PresencesCSVExport pce = new PresencesCSVExport(presences);
            pce.setRequest(request);
            pce.setHeader(csvHeaders);
            pce.export();
        });
    }

    private void get(final HttpServerRequest request, Handler<Either<String, JsonArray>> handler) {
        UserUtils.getUserInfos(eb, request, user -> {
            MultiMap params = request.params();
            String structureId = params.get("structureId");
            String startDate = params.get("startDate");
            String endDate = params.get("endDate");
            List<String> userIds = params.getAll("studentId");
            List<String> ownerIds = params.getAll("ownerId");
            ownerIds = WorkflowHelper.hasRight(user, WorkflowActions.SEARCH.toString()) ? ownerIds :
                    Collections.singletonList(user.getUserId());

            if (!request.params().contains("structureId") || !request.params().contains("startDate") ||
                    !request.params().contains("endDate")) {
                badRequest(request);
                return;
            }
            presencesService.get(structureId, startDate, endDate, userIds, ownerIds, handler);

        });
    }

}
