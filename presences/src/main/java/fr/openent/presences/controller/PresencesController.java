package fr.openent.presences.controller;

import fr.openent.presences.Presences;
import fr.openent.presences.common.helper.WorkflowHelper;
import fr.openent.presences.enums.WorkflowActions;
import fr.openent.presences.security.presence.ManagePresenceRight;
import fr.openent.presences.service.PresenceService;
import fr.openent.presences.service.impl.DefaultPresenceService;
import fr.wseduc.rs.*;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.MultiMap;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.http.response.DefaultResponseHandler;
import org.entcore.common.user.UserUtils;

import java.util.Collections;
import java.util.List;

public class PresencesController extends ControllerHelper {

    private EventBus eb;
    private PresenceService presencesService;


    public PresencesController(EventBus eb) {
        this.eb = eb;
        this.presencesService = new DefaultPresenceService();
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
    }

    @Get("/presences")
    @ApiDoc("Retrieve presences")
    @SecuredAction(Presences.READ_PRESENCE)
    public void getPresences(final HttpServerRequest request) {
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
            presencesService.get(structureId, startDate, endDate, userIds, ownerIds, DefaultResponseHandler.arrayResponseHandler(request));
        });
    }

    @Post("/presence")
    @ApiDoc("Create presence")
    @SecuredAction(Presences.CREATE_PRESENCE)
    public void createPresence(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, presences ->
                UserUtils.getUserInfos(eb, request, user ->
                        presencesService.create(user, presences, DefaultResponseHandler.defaultResponseHandler(request))));
    }

    @Put("/presence")
    @ApiDoc("Update presence")
    @SecuredAction(Presences.MANAGE_PRESENCE)
    public void updatePresence(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, presences -> {
            presencesService.update(presences, DefaultResponseHandler.defaultResponseHandler(request));
        });
    }

    @Delete("/presence")
    @ApiDoc("Delete presence")
    @ResourceFilter(ManagePresenceRight.class)
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    public void deletePresence(final HttpServerRequest request) {
        if (!request.params().contains("id")) {
            badRequest(request);
            return;
        }
        String presenceId = request.getParam("id");
        presencesService.delete(presenceId, DefaultResponseHandler.defaultResponseHandler(request));
    }
}
